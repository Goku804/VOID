package com.core.voidapp.data.guardian

import java.time.Duration
import java.time.LocalDateTime

// ---------------------------------------------------------------------
// Commitment — the fixed-duration authority the user voluntarily grants
// Guardian. See rules #16-18: not bypassable via a plain ON/OFF toggle.
// ---------------------------------------------------------------------

enum class CommitmentDuration(val days: Long, val label: String) {
    TWO_WEEKS(14, "2 Weeks"),
    ONE_MONTH(30, "1 Month"),
    TWO_MONTHS(60, "2 Months"),
    THREE_MONTHS(90, "3 Months"),
    SIX_MONTHS(180, "6 Months"),
    ONE_YEAR(365, "1 Year")
}

enum class CommitmentStatus { ACTIVE, ENDED_NATURALLY, ENDED_EARLY }

data class GuardianCommitment(
    val id: String,
    val duration: CommitmentDuration,
    val startedAt: LocalDateTime,
    val endsAt: LocalDateTime,
    val status: CommitmentStatus
)

fun GuardianCommitment.isCurrentlyActive(now: LocalDateTime = LocalDateTime.now()): Boolean =
    status == CommitmentStatus.ACTIVE && now.isBefore(endsAt)

fun GuardianCommitment.daysRemaining(now: LocalDateTime = LocalDateTime.now()): Long =
    Duration.between(now, endsAt).toDays().coerceAtLeast(0)

// ---------------------------------------------------------------------
// Enforcement + settings
// ---------------------------------------------------------------------

enum class EnforcementMode { OFF, GENTLE, FOCUS, STRICT }

data class GuardianSettings(
    val enforcementMode: EnforcementMode = EnforcementMode.OFF,
    val allowedPackages: Set<String> = emptySet()
)

// ---------------------------------------------------------------------
// Orb + events
// ---------------------------------------------------------------------

enum class GuardianOrbState { IDLE, LISTENING, THINKING, SPEAKING, FOCUS, WARNING, URGENT, BREAK, COMPLETED, ERROR }

enum class GuardianEvent {
    STUDY_STARTED, DISTRACTION_DETECTED, TASK_OVERDUE, EXAM_URGENT, SESSION_COMPLETED,
    BREAK_ACTIVE, BREAK_ENDED, TASK_MISSED, TASK_PARTIAL, FOCUS_MODE, SERIOUS_MODE,
    TEMPORARY_PLAN_URGENT, EXAM_COUNTDOWN
}

/**
 * The ONLY information Guardian dialogue (and, later, the AI layer) ever
 * sees for a given moment — deliberately not "the whole database" (rules
 * #33). Every field is optional: GuardianDialogue is written to degrade
 * to a neutral, non-fabricated phrase when a field isn't populated,
 * rather than ever inventing a specific value that wasn't real.
 */
data class GuardianSpeechContext(
    val subjectName: String? = null,
    val taskLabel: String? = null,
    val unitName: String? = null,
    val remainingMinutes: Int? = null,
    val plannedMinutes: Int? = null,
    val progressPercent: Int? = null,
    val priority: String? = null,
    val examDaysRemaining: Long? = null,
    val examType: String? = null,
    val deadlineLabel: String? = null,
    val overdueDays: Long? = null,
    val warningLevel: Int = 0,
    val breakMinutes: Int? = null
)

// ---------------------------------------------------------------------
// Ephemeral runtime state — in-memory only, reset per session/break.
// Nothing here needs to survive a process death mid-session; if VOID is
// killed, the session itself (StudySession) is what's durable.
// ---------------------------------------------------------------------

data class GuardianWarningState(
    val sessionId: String,
    val warningCount: Int = 0,
    val lastWarningAt: LocalDateTime? = null,
    val recentLines: List<String> = emptyList()
)

data class GuardianBreak(val startedAt: LocalDateTime, val durationMinutes: Int)

fun GuardianBreak.endsAt(): LocalDateTime = startedAt.plusMinutes(durationMinutes.toLong())
fun GuardianBreak.isActive(now: LocalDateTime = LocalDateTime.now()): Boolean = now.isBefore(endsAt())
fun GuardianBreak.remainingMinutes(now: LocalDateTime = LocalDateTime.now()): Int =
    Duration.between(now, endsAt()).toMinutes().toInt().coerceAtLeast(0)
