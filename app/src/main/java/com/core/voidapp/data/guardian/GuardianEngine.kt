package com.core.voidapp.data.guardian

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import com.core.voidapp.data.StudySession
import com.core.voidapp.data.VoidRepository
import com.core.voidapp.data.daysRemaining
import com.core.voidapp.data.daysUntilDeadline
import com.core.voidapp.data.isOverdue
import com.core.voidapp.data.remainingMinutes
import java.time.LocalDateTime

/**
 * Guardian State Engine — sits between the Planning Engine and the
 * orb/voice, exactly per the architecture in the spec:
 *
 *   DATABASE -> REPOSITORIES -> PLANNING ENGINE -> GUARDIAN STATE ENGINE -> GUARDIAN
 *
 * Every public entry point starts with a commitment/enforcement check.
 * Without an active commitment, every function here is a silent no-op —
 * that's rule #15-16 ("no active commitment = no enforcement") enforced
 * in exactly one place rather than scattered across callers.
 */
object GuardianEngine {
    /** Compose-observable so a future orb UI redraws automatically — safe to read from a @Composable. */
    val orbState = mutableStateOf(GuardianOrbState.IDLE)

    private var warningState: GuardianWarningState? = null
    private var activeBreak: GuardianBreak? = null
    private var focusModeAnnounced = false
    private var seriousModeAnnounced = false
    private val announcedOverdueTaskIds = mutableSetOf<String>()
    private val announcedUrgentExamIds = mutableSetOf<String>()

    init {
        GuardianNotifier.setOrbStateListener { state ->
            orbState.value = if (state == GuardianOrbState.SPEAKING) state else restingState()
        }
    }

    private fun restingState(): GuardianOrbState = when {
        activeBreak?.isActive() == true -> GuardianOrbState.BREAK
        VoidRepository.activeSession() != null -> GuardianOrbState.FOCUS
        else -> GuardianOrbState.IDLE
    }

    fun isEnforcementActive(): Boolean =
        GuardianRepository.isCommitted() && GuardianRepository.settings().enforcementMode != EnforcementMode.OFF

    fun isOnAuthorizedBreak(now: LocalDateTime = LocalDateTime.now()): Boolean = activeBreak?.isActive(now) == true

    fun currentBreak(): GuardianBreak? = activeBreak

    fun currentWarningCount(sessionId: String): Int =
        warningState?.takeIf { it.sessionId == sessionId }?.warningCount ?: 0

    private fun speak(context: Context, event: GuardianEvent, ctx: GuardianSpeechContext) {
        GuardianNotifier.notify(context, GuardianDialogue.speak(event, ctx))
    }

    private fun contextForSession(session: StudySession, warningLevel: Int = 0): GuardianSpeechContext {
        val subject = VoidRepository.subjects.find { it.id == session.subjectId }
        val urgentExam = subject?.let { s -> VoidRepository.urgentExamSubjects().find { it.subjectId == s.id } }
        return GuardianSpeechContext(
            subjectName = subject?.name,
            taskLabel = session.label,
            remainingMinutes = session.remainingMinutes(),
            plannedMinutes = session.plannedMinutes,
            examDaysRemaining = urgentExam?.daysRemaining(),
            warningLevel = warningLevel
        )
    }

    fun onSessionStarted(context: Context, session: StudySession) {
        if (!GuardianRepository.isCommitted()) return
        warningState = GuardianWarningState(sessionId = session.id)
        focusModeAnnounced = false
        seriousModeAnnounced = false
        orbState.value = GuardianOrbState.FOCUS
        speak(context, GuardianEvent.STUDY_STARTED, contextForSession(session))
    }

    fun onSessionCompleted(context: Context, session: StudySession) {
        if (!GuardianRepository.isCommitted()) return
        warningState = null
        orbState.value = GuardianOrbState.COMPLETED
        speak(context, GuardianEvent.SESSION_COMPLETED, contextForSession(session))
    }

    /** Abandoned sessions are quiet on purpose — Guardian doesn't editorialize about a session the user chose to end; it just stops watching it. */
    fun onSessionAbandoned(context: Context, session: StudySession) {
        warningState = null
        orbState.value = GuardianOrbState.IDLE
    }

    /**
     * Call whenever the foreground app changes to something outside the
     * active session (Phase 4 wires a real UsageStatsManager detector to
     * this). Does nothing unless every one of rule #9's trigger
     * conditions actually holds.
     */
    fun reportDistraction(context: Context) {
        if (!isEnforcementActive()) return
        val session = VoidRepository.activeSession() ?: return
        if (isOnAuthorizedBreak()) return

        val current = warningState?.takeIf { it.sessionId == session.id } ?: GuardianWarningState(session.id)
        val updated = current.copy(warningCount = current.warningCount + 1, lastWarningAt = LocalDateTime.now())
        warningState = updated

        val tier = updated.warningCount
        orbState.value = if (tier >= 3) GuardianOrbState.URGENT else GuardianOrbState.WARNING
        speak(context, GuardianEvent.DISTRACTION_DETECTED, contextForSession(session, warningLevel = tier))

        val mode = GuardianRepository.settings().enforcementMode
        if (mode == EnforcementMode.FOCUS && tier == 2 && !focusModeAnnounced) {
            focusModeAnnounced = true
            speak(context, GuardianEvent.FOCUS_MODE, contextForSession(session))
        }
        if (mode == EnforcementMode.STRICT && tier >= 3 && !seriousModeAnnounced) {
            seriousModeAnnounced = true
            speak(context, GuardianEvent.SERIOUS_MODE, contextForSession(session))
        }
    }

    fun startBreak(context: Context, minutes: Int) {
        if (!GuardianRepository.isCommitted()) return
        activeBreak = GuardianBreak(startedAt = LocalDateTime.now(), durationMinutes = minutes)
        orbState.value = GuardianOrbState.BREAK
        val ctx = VoidRepository.activeSession()?.let { contextForSession(it) } ?: GuardianSpeechContext()
        speak(context, GuardianEvent.BREAK_ACTIVE, ctx.copy(breakMinutes = minutes))
    }

    fun endBreakEarly(context: Context) {
        if (activeBreak == null) return
        activeBreak = null
        finishBreak(context)
    }

    /** Call periodically (e.g. once a second from EXECUTE's own countdown) — fires BREAK_ENDED exactly once when the break's clock runs out on its own. */
    fun checkBreakExpiry(context: Context) {
        val brk = activeBreak ?: return
        if (!brk.isActive()) {
            activeBreak = null
            finishBreak(context)
        }
    }

    private fun finishBreak(context: Context) {
        orbState.value = restingState()
        val session = VoidRepository.activeSession() ?: return
        speak(context, GuardianEvent.BREAK_ENDED, contextForSession(session))
    }

    /**
     * Scans real overdue Temporary Tasks and Urgent-window exam sittings
     * and announces each once per app run. This never invents the list —
     * it's exactly what the Planning Engine already flags via
     * TemporaryTask.isOverdue() and VoidRepository.urgentExamSubjects().
     * Call this at a natural checkpoint (EXECUTE screen open is enough
     * for this phase; a precise background scan is a later addition).
     */
    fun scanForUrgentSituations(context: Context) {
        if (!GuardianRepository.isCommitted()) return

        VoidRepository.temporaryTasks
            .filter { it.isOverdue() && it.id !in announcedOverdueTaskIds }
            .forEach { task ->
                announcedOverdueTaskIds += task.id
                val subject = VoidRepository.subjects.find { it.id == task.subjectId }
                speak(
                    context, GuardianEvent.TASK_OVERDUE,
                    GuardianSpeechContext(
                        subjectName = subject?.name,
                        taskLabel = task.title,
                        deadlineLabel = task.deadline.toString(),
                        overdueDays = -task.daysUntilDeadline()
                    )
                )
            }

        VoidRepository.urgentExamSubjects()
            .filter { it.id !in announcedUrgentExamIds }
            .forEach { examSubject ->
                announcedUrgentExamIds += examSubject.id
                val subject = VoidRepository.subjects.find { it.id == examSubject.subjectId }
                val exam = VoidRepository.exams.find { it.id == examSubject.examId }
                speak(
                    context, GuardianEvent.EXAM_URGENT,
                    GuardianSpeechContext(
                        subjectName = subject?.name,
                        examType = exam?.examType?.name,
                        examDaysRemaining = examSubject.daysRemaining()
                    )
                )
            }
    }
}
