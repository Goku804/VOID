package com.core.voidapp.data.guardian

/**
 * Guardian's local dialogue system — works with zero network/AI access,
 * per rule #32 ("Guardian must still function" if the AI layer is down).
 *
 * Each event has 20+ variations, written with genuinely different
 * sentence structure and emphasis (not one template with a word swapped).
 * A short anti-repeat memory (last 5 lines per event) keeps the same
 * phrasing from firing twice in a row.
 *
 * Placeholder fields that might be null (exam info, priority, etc.) are
 * resolved through the private helpers below to a neutral, non-fabricated
 * phrase ("this subject", "the current task") rather than ever inventing
 * a specific value VOID doesn't actually have.
 */
object GuardianDialogue {

    private val recent = mutableMapOf<GuardianEvent, MutableList<Int>>()

    fun speak(event: GuardianEvent, context: GuardianSpeechContext): String {
        val bank = bankFor(event, context)
        val history = recent.getOrPut(event) { mutableListOf() }
        val pool = bank.indices.filterNot { it in history }.ifEmpty { bank.indices.toList() }
        val index = pool.random()
        history.add(index)
        if (history.size > 5) history.removeAt(0)
        return bank[index](context)
    }

    /** DISTRACTION_DETECTED escalates by warningLevel (1 = calm, 2 = firmer, 3+ = serious); every other event ignores it. */
    private fun bankFor(event: GuardianEvent, context: GuardianSpeechContext): List<(GuardianSpeechContext) -> String> =
        when (event) {
            GuardianEvent.STUDY_STARTED -> STUDY_STARTED
            GuardianEvent.DISTRACTION_DETECTED -> when {
                context.warningLevel <= 1 -> DISTRACTION_TIER_1
                context.warningLevel == 2 -> DISTRACTION_TIER_2
                else -> DISTRACTION_TIER_3
            }
            GuardianEvent.TASK_OVERDUE -> TASK_OVERDUE
            GuardianEvent.EXAM_URGENT -> EXAM_URGENT
            GuardianEvent.SESSION_COMPLETED -> SESSION_COMPLETED
            GuardianEvent.BREAK_ACTIVE -> BREAK_ACTIVE
            GuardianEvent.BREAK_ENDED -> BREAK_ENDED
            GuardianEvent.TASK_MISSED -> TASK_MISSED
            GuardianEvent.TASK_PARTIAL -> TASK_PARTIAL
            GuardianEvent.FOCUS_MODE -> FOCUS_MODE
            GuardianEvent.SERIOUS_MODE -> SERIOUS_MODE
            GuardianEvent.TEMPORARY_PLAN_URGENT -> TEMPORARY_PLAN_URGENT
            GuardianEvent.EXAM_COUNTDOWN -> EXAM_COUNTDOWN
        }

    // --- neutral, never-fabricated fallbacks when a field is absent ---
    private fun GuardianSpeechContext.subj() = subjectName ?: "this subject"
    private fun GuardianSpeechContext.task() = taskLabel ?: unitName ?: "the current task"
    private fun GuardianSpeechContext.remain() = remainingMinutes?.let { "$it minutes" } ?: "the time you have left"
    private fun GuardianSpeechContext.planned() = plannedMinutes?.let { "$it minutes" } ?: "the time you planned"
    private fun GuardianSpeechContext.progress() = progressPercent?.let { "$it% complete" } ?: "partially complete"
    private fun GuardianSpeechContext.examDays() = examDaysRemaining?.let { if (it == 1L) "1 day" else "$it days" } ?: "a few days"
    private fun GuardianSpeechContext.exam() = examType ?: "exam"
    private fun GuardianSpeechContext.deadline() = deadlineLabel ?: "soon"
    private fun GuardianSpeechContext.overdue() = overdueDays?.let { if (it == 1L) "1 day" else "$it days" } ?: "some time"
    private fun GuardianSpeechContext.brk() = breakMinutes?.let { "$it-minute" } ?: "your"

    // =================================================================
    // STUDY_STARTED
    // =================================================================
    private val STUDY_STARTED: List<(GuardianSpeechContext) -> String> = listOf(
        { c -> "Your ${c.subj()} session has started. You have ${c.remain()} to work on ${c.task()}." },
        { c -> "${c.subj()} is now your active session. ${c.task()} is the target, with ${c.remain()} reserved for it." },
        { c -> "Your session on ${c.subj()} is live. ${c.remain()} is set aside for ${c.task()}." },
        { c -> "This is a ${c.subj()} session. ${c.task()} needs your attention, and ${c.remain()} is on the clock." },
        { c -> "${c.subj()} has begun. Focus on ${c.task()} — ${c.remain()} remains before the session closes." },
        { c -> "The session running now is ${c.subj()}. You committed ${c.planned()} to ${c.task()}." },
        { c -> "Guardian confirms your ${c.subj()} session is active. ${c.task()} is what this time is for." },
        { c -> "Your ${c.subj()} block has started, with ${c.remain()} to work on ${c.task()}." },
        { c -> "Session active: ${c.subj()}. ${c.remain()} remains for ${c.task()}." },
        { c -> "${c.task()} is the focus of this session. ${c.subj()} has ${c.remain()} allocated to it." },
        { c -> "You're now in a ${c.subj()} session. Use the ${c.remain()} you have for ${c.task()}." },
        { c -> "${c.subj()} time has begun. ${c.task()} is waiting, and the clock shows ${c.remain()}." },
        { c -> "This session belongs to ${c.subj()}. ${c.remain()} is what you have to work on ${c.task()}." },
        { c -> "Your commitment to ${c.subj()} starts now. ${c.task()} needs ${c.remain()} of real attention." },
        { c -> "Guardian is tracking a ${c.subj()} session. ${c.remain()} remains for ${c.task()}." },
        { c -> "${c.subj()} is active. Stay with ${c.task()} for the ${c.remain()} you planned." },
        { c -> "The planner has started your ${c.subj()} session. ${c.task()} is the priority for the next ${c.remain()}." },
        { c -> "You have ${c.remain()} for ${c.subj()}. ${c.task()} is what this session is built around." },
        { c -> "Session start: ${c.subj()}, targeting ${c.task()}. ${c.remain()} is on the clock." },
        { c -> "${c.subj()} is now running. ${c.task()} is due your attention for ${c.remain()}." },
        { c -> "Your ${c.subj()} session has begun, and ${c.task()} is what you planned to work on. Make the ${c.remain()} count." },
        { c -> "Guardian confirms: ${c.subj()}, ${c.task()}, ${c.remain()} remaining. Stay with it." }
    )

    // =================================================================
    // DISTRACTION_DETECTED — three progressive tiers
    // =================================================================
    private val DISTRACTION_TIER_1: List<(GuardianSpeechContext) -> String> = listOf(
        { c -> "Your ${c.subj()} session is still active. You've moved away from it, with ${c.remain()} remaining. If this is a break, use your break control — otherwise, return to the task." },
        { c -> "${c.subj()} is still running in the background. ${c.remain()} is left, and you're currently somewhere else." },
        { c -> "You stepped away from your ${c.subj()} session. ${c.remain()} remains, and the task is still ${c.task()}." },
        { c -> "This session on ${c.subj()} hasn't ended. You have ${c.remain()} left — return when you can." },
        { c -> "Guardian notices you've left the ${c.subj()} session. ${c.remain()} is still on the clock for ${c.task()}." },
        { c -> "Your ${c.subj()} work is paused by your attention, not the clock. ${c.remain()} remains." },
        { c -> "${c.task()} is still waiting. Your ${c.subj()} session has ${c.remain()} left — this is just a check-in." },
        { c -> "You're away from ${c.subj()} right now. ${c.remain()} remains if you'd like to continue ${c.task()}." }
    )
    private val DISTRACTION_TIER_2: List<(GuardianSpeechContext) -> String> = listOf(
        { c -> "You're still away from your ${c.subj()} session. This isn't a break you've authorized, and ${c.remain()} is being spent doing something else." },
        { c -> "Second check: ${c.subj()} is still active, and you haven't returned. ${c.task()} still needs ${c.remain()} of attention." },
        { c -> "This is your second time away from ${c.subj()}. If you need a break, take one deliberately — otherwise, return now." },
        { c -> "You committed to ${c.subj()}, and you're still not there. ${c.remain()} remains, and it's running regardless." },
        { c -> "Guardian is noting this. ${c.subj()} has been unattended twice now, with ${c.remain()} left on ${c.task()}." },
        { c -> "You haven't come back to ${c.subj()}. Either return to ${c.task()} or start an authorized break — not neither." },
        { c -> "${c.remain()} is left on ${c.subj()}, and you're still away. This is the second reminder." },
        { c -> "Your attention has left ${c.subj()} again. ${c.task()} isn't finishing itself, and ${c.remain()} remains." }
    )
    private val DISTRACTION_TIER_3: List<(GuardianSpeechContext) -> String> = listOf(
        { c -> "This is repeated. You've left ${c.subj()} multiple times now, and ${c.remain()} is still unaccounted for on ${c.task()}." },
        { c -> "Guardian has recorded several departures from this ${c.subj()} session. The commitment you made doesn't pause because you did." },
        { c -> "You are away from ${c.subj()} again. ${c.remain()} remains, and the pattern is clear — return to ${c.task()} now." },
        { c -> "This session has been interrupted repeatedly. ${c.subj()} and ${c.task()} are still waiting, with ${c.remain()} left." },
        { c -> "Guardian is not going to keep this light. You committed to ${c.subj()} — ${c.remain()} remains, and it's time to return." },
        { c -> "Multiple departures noted. ${c.task()} under ${c.subj()} still needs ${c.remain()}, and the session isn't ending itself." },
        { c -> "This is not the first reminder, and it shouldn't need to be a third. ${c.subj()} still has ${c.remain()} left on ${c.task()}." },
        { c -> "The pattern here is repeated distraction. ${c.subj()} needs ${c.remain()} more, and the choice to return is still yours to make — right now." }
    )

    // =================================================================
    // TASK_OVERDUE
    // =================================================================
    private val TASK_OVERDUE: List<(GuardianSpeechContext) -> String> = listOf(
        { c -> "Your ${c.task()} for ${c.subj()} is now overdue. The deadline passed ${c.overdue()} ago, and it's still unfinished." },
        { c -> "${c.task()} has missed its deadline. ${c.subj()} work is now ${c.overdue()} overdue, and the planner has to account for it." },
        { c -> "This is overdue: ${c.task()}, ${c.subj()}. ${c.overdue()} has passed since the deadline." },
        { c -> "${c.subj()}'s ${c.task()} is past due. It's been ${c.overdue()}, and your remaining workload now reflects that." },
        { c -> "Guardian is flagging ${c.task()} as overdue. ${c.subj()} needed this by ${c.deadline()}, and it's still incomplete." },
        { c -> "The deadline for ${c.task()} has passed — ${c.overdue()} ago. ${c.subj()} still needs this work done." },
        { c -> "You're behind on ${c.task()} for ${c.subj()}. It's ${c.overdue()} past the deadline now." },
        { c -> "${c.task()} didn't make its deadline. The planner is now treating ${c.subj()} as ${c.overdue()} behind." },
        { c -> "This ${c.subj()} task, ${c.task()}, is overdue by ${c.overdue()}. The workload ahead has grown because of it." },
        { c -> "Overdue notice: ${c.task()}. ${c.subj()} was due ${c.deadline()}, and it's still open." },
        { c -> "${c.overdue()} have passed since ${c.task()} was due. ${c.subj()} still needs this resolved." },
        { c -> "Your planner now carries an overdue item — ${c.task()} for ${c.subj()}, ${c.overdue()} late." },
        { c -> "${c.task()} remains incomplete past its deadline. ${c.subj()} is ${c.overdue()} behind schedule on this one." },
        { c -> "This is a real gap now: ${c.task()} for ${c.subj()}, missed by ${c.overdue()}." },
        { c -> "Guardian confirms ${c.task()} is overdue. ${c.subj()}'s deadline was ${c.deadline()}, and the work is still pending." },
        { c -> "${c.subj()} has an unfinished task past its due date. ${c.task()} is now ${c.overdue()} late." },
        { c -> "The window on ${c.task()} closed ${c.overdue()} ago. ${c.subj()} still needs it finished." },
        { c -> "You still owe ${c.subj()} this work. ${c.task()} is ${c.overdue()} overdue, and it isn't resolving itself." },
        { c -> "${c.task()} is late. ${c.overdue()} since the deadline, and ${c.subj()} is still waiting on it." },
        { c -> "This overdue task, ${c.task()} under ${c.subj()}, needs attention. It's ${c.overdue()} past when it was due." },
        { c -> "${c.subj()}: ${c.task()} is overdue by ${c.overdue()}. The planner has already adjusted for it, but it still needs finishing." }
    )

    // =================================================================
    // EXAM_URGENT
    // =================================================================
    private val EXAM_URGENT: List<(GuardianSpeechContext) -> String> = listOf(
        { c -> "Your ${c.exam()} in ${c.subj()} is now ${c.examDays()} away, and preparation is still incomplete. The planner has raised its priority." },
        { c -> "${c.subj()}'s ${c.exam()} is approaching — ${c.examDays()} left. Your upcoming sessions may lean toward it more." },
        { c -> "This is urgent: ${c.subj()} ${c.exam()} in ${c.examDays()}. Preparation is at ${c.progress()}, and that's not where it needs to be." },
        { c -> "${c.examDays()} remain before your ${c.subj()} ${c.exam()}. The planner has flagged this as a priority." },
        { c -> "Guardian is tracking your ${c.subj()} ${c.exam()}, now ${c.examDays()} away. Preparation still needs work." },
        { c -> "Time is tightening on ${c.subj()}. The ${c.exam()} is ${c.examDays()} out, and readiness is still building." },
        { c -> "Your ${c.exam()} for ${c.subj()} is close — ${c.examDays()} remaining. Expect more of your study time to shift here." },
        { c -> "${c.subj()} ${c.exam()}: ${c.examDays()} left. This is now one of the planner's top concerns." },
        { c -> "The countdown on ${c.subj()}'s ${c.exam()} is at ${c.examDays()}. Preparation progress is ${c.progress()}." },
        { c -> "This exam is becoming urgent. ${c.subj()} ${c.exam()} arrives in ${c.examDays()}, and the gap still needs closing." },
        { c -> "${c.examDays()} stand between now and your ${c.subj()} ${c.exam()}. The planner is adjusting for it." },
        { c -> "Guardian flags ${c.subj()} as urgent. The ${c.exam()} is ${c.examDays()} away, and preparation isn't finished." },
        { c -> "Your ${c.subj()} ${c.exam()} is near — ${c.examDays()} out. Sessions ahead may prioritize it accordingly." },
        { c -> "This is close now. ${c.subj()}'s ${c.exam()} is ${c.examDays()} away, with progress sitting at ${c.progress()}." },
        { c -> "${c.exam()} urgency: ${c.subj()}, ${c.examDays()} remaining. The planner has responded by increasing its weight." },
        { c -> "You have ${c.examDays()} left before ${c.subj()}'s ${c.exam()}. That's tight, given where preparation stands." },
        { c -> "${c.subj()} is now flagged urgent. ${c.examDays()} remain until the ${c.exam()}, and it needs real attention." },
        { c -> "The ${c.exam()} for ${c.subj()} is ${c.examDays()} away. Guardian expects your next sessions to reflect that." },
        { c -> "This exam window is closing. ${c.subj()} ${c.exam()} in ${c.examDays()}, preparation at ${c.progress()}." },
        { c -> "${c.examDays()}. That's what's left before ${c.subj()}'s ${c.exam()} — the planner has already adjusted your priorities." },
        { c -> "Guardian is not going to understate this: ${c.subj()}'s ${c.exam()} is ${c.examDays()} away, and it needs more time than it's gotten." }
    )

    // =================================================================
    // SESSION_COMPLETED
    // =================================================================
    private val SESSION_COMPLETED: List<(GuardianSpeechContext) -> String> = listOf(
        { c -> "Your ${c.subj()} session is complete. The work has been recorded, and the planner has updated what's left." },
        { c -> "${c.subj()} session finished. ${c.task()} is logged, and your remaining workload reflects it." },
        { c -> "That's a completed ${c.subj()} session. Guardian has recorded the result." },
        { c -> "Session closed: ${c.subj()}. ${c.task()} progress has been saved, and the planner has recalculated." },
        { c -> "You finished the ${c.subj()} session. The database now reflects the work on ${c.task()}." },
        { c -> "${c.subj()} is done for this session. What you completed on ${c.task()} has been recorded." },
        { c -> "Guardian confirms: ${c.subj()} session complete. The planner will account for this going forward." },
        { c -> "This ${c.subj()} block is finished. ${c.task()} has been updated, and your plan has adjusted." },
        { c -> "Session complete. ${c.subj()} and ${c.task()} are recorded, and the workload ahead reflects it." },
        { c -> "You closed out ${c.subj()} for this session. The result is saved, and the planner has moved on." },
        { c -> "${c.subj()} session: complete. Guardian has logged the outcome for ${c.task()}." },
        { c -> "That session is finished. ${c.subj()} work on ${c.task()} is recorded, and your plan updates accordingly." },
        { c -> "Your ${c.subj()} time is up, and the session closed properly. ${c.task()} progress is saved." },
        { c -> "Guardian marks this ${c.subj()} session as complete. The planner now reflects the real result." },
        { c -> "${c.task()} for ${c.subj()} has been logged as this session's outcome." },
        { c -> "Session over: ${c.subj()}. The scheduled work is recorded, and your remaining plan has been updated." },
        { c -> "You completed the ${c.subj()} session as planned. Guardian has saved the result for ${c.task()}." },
        { c -> "${c.subj()} is closed for now. The planner has taken this session's result into account." },
        { c -> "This session on ${c.subj()} is done. ${c.task()} has been updated in the database." },
        { c -> "Guardian confirms the ${c.subj()} session ended properly, and ${c.task()}'s progress has been recorded." },
        { c -> "Completed: ${c.subj()}, ${c.task()}. The planner has already recalculated your remaining workload." }
    )

    // =================================================================
    // BREAK_ACTIVE
    // =================================================================
    private val BREAK_ACTIVE: List<(GuardianSpeechContext) -> String> = listOf(
        { c -> "Your ${c.brk()} break has started. Guardian won't treat this as a distraction, and ${c.subj()} will resume when it ends." },
        { c -> "Break active: ${c.brk()}. Your ${c.subj()} session is paused, not abandoned." },
        { c -> "This is an authorized break, ${c.brk()}. ${c.subj()} will pick back up once it's over." },
        { c -> "You're on a ${c.brk()} break now. No warnings will trigger — ${c.subj()} is simply waiting." },
        { c -> "Guardian has logged a ${c.brk()} break. ${c.subj()} resumes automatically when it ends." },
        { c -> "${c.brk()} of authorized break time has begun. ${c.subj()} is paused until then." },
        { c -> "Take your break — ${c.brk()}. ${c.subj()} won't move, and Guardian won't flag this as distraction." },
        { c -> "Break started, ${c.brk()}. When it ends, your ${c.subj()} session picks up right where it was." },
        { c -> "This break is intentional and covered — ${c.brk()}. ${c.subj()} will wait for you." },
        { c -> "Guardian recognizes this as a real break, ${c.brk()}. ${c.subj()} resumes after." },
        { c -> "You've stepped away deliberately for ${c.brk()}. That's respected — ${c.subj()} isn't going anywhere." },
        { c -> "${c.brk()} break, active now. Guardian suspends enforcement until it's done." },
        { c -> "This pause is authorized, ${c.brk()}. ${c.subj()} continues once the break ends." },
        { c -> "Your break clock is running — ${c.brk()}. ${c.subj()} is on hold, not overdue." },
        { c -> "Guardian confirms the break, ${c.brk()}. No pressure until it's over." },
        { c -> "You're covered for the next stretch — ${c.brk()}. ${c.subj()} will still be there when you're back." },
        { c -> "Break in progress, ${c.brk()}. Guardian's enforcement is off until it ends." },
        { c -> "This break is yours to take, ${c.brk()}. ${c.subj()} resumes the moment it's done." },
        { c -> "Guardian has paused enforcement for ${c.brk()}. ${c.subj()} isn't being counted against you right now." },
        { c -> "The break you requested has started, ${c.brk()}. ${c.subj()} continues right after." }
    )

    // =================================================================
    // BREAK_ENDED
    // =================================================================
    private val BREAK_ENDED: List<(GuardianSpeechContext) -> String> = listOf(
        { c -> "Your break is over. ${c.subj()} is still active, with ${c.remain()} remaining — return to ${c.task()}." },
        { c -> "Break ended. ${c.remain()} is left on ${c.subj()}, and ${c.task()} is waiting." },
        { c -> "Time's up on the break. ${c.subj()} resumes now, with ${c.remain()} left for ${c.task()}." },
        { c -> "That break has finished. Get back to ${c.task()} — ${c.remain()} remains on ${c.subj()}." },
        { c -> "Guardian is resuming enforcement. Your ${c.subj()} session has ${c.remain()} left." },
        { c -> "The break is done. ${c.subj()} picks back up, and ${c.task()} still needs ${c.remain()}." },
        { c -> "Break's over — ${c.subj()} is active again. ${c.remain()} remains for ${c.task()}." },
        { c -> "You're back from break. ${c.task()} under ${c.subj()} has ${c.remain()} left to go." },
        { c -> "Guardian confirms the break has ended. Return to ${c.task()}, with ${c.remain()} remaining." },
        { c -> "That's the end of your break. ${c.subj()} continues, and ${c.remain()} is still on the clock." },
        { c -> "Break concluded. ${c.remain()} remains on your ${c.subj()} session — back to ${c.task()}." },
        { c -> "Enforcement resumes now that the break is over. ${c.task()} still needs ${c.remain()}." },
        { c -> "Your ${c.subj()} session is live again. ${c.remain()} is left, so continue with ${c.task()}." },
        { c -> "The break period has ended. ${c.remain()} remains for ${c.task()} under ${c.subj()}." },
        { c -> "Guardian is no longer pausing for the break. ${c.subj()} needs ${c.remain()} more on ${c.task()}." },
        { c -> "Back to work — the break is over, and ${c.subj()} still has ${c.remain()} left." },
        { c -> "That concludes your break. ${c.task()} is where you left it, with ${c.remain()} remaining." },
        { c -> "The break clock hit zero. ${c.subj()} resumes, ${c.remain()} left for ${c.task()}." },
        { c -> "Guardian marks the break as ended. Continue ${c.task()} — ${c.remain()} remains." },
        { c -> "Break over. ${c.subj()} is active again with ${c.remain()} left — stay with ${c.task()} until it ends." }
    )

    // =================================================================
    // TASK_MISSED
    // =================================================================
    private val TASK_MISSED: List<(GuardianSpeechContext) -> String> = listOf(
        { c -> "${c.task()} for ${c.subj()} was missed. The window to complete it as planned has closed." },
        { c -> "Guardian is marking ${c.task()} as missed. ${c.subj()} did not get this done in time." },
        { c -> "This didn't happen: ${c.task()}, ${c.subj()}. It's now marked missed rather than overdue." },
        { c -> "${c.subj()}'s ${c.task()} has been marked missed. The planner will handle it as lost work, not pending work." },
        { c -> "You didn't complete ${c.task()} for ${c.subj()} in its window. Guardian has recorded it as missed." },
        { c -> "${c.task()} is now missed, not just late. ${c.subj()} will need this addressed differently going forward." },
        { c -> "This one didn't get done: ${c.task()} under ${c.subj()}. It's recorded as missed." },
        { c -> "Guardian confirms ${c.task()} was missed. ${c.subj()} planning will account for the gap." },
        { c -> "${c.subj()}: ${c.task()} missed. The opportunity to complete it on schedule has passed." },
        { c -> "This is now a missed task — ${c.task()}, ${c.subj()}. Recorded, not forgotten." },
        { c -> "${c.task()} slipped past without being done. ${c.subj()} now carries this as missed work." },
        { c -> "Guardian is logging ${c.task()} as missed for ${c.subj()}. The planner adjusts from here." },
        { c -> "You weren't able to get to ${c.task()} for ${c.subj()}. It's marked missed in the record." },
        { c -> "${c.subj()}'s ${c.task()} has gone unaddressed and is now marked missed." },
        { c -> "This didn't make it: ${c.task()}, ${c.subj()}. Missed, and the planner knows it." },
        { c -> "Guardian records ${c.task()} as missed. ${c.subj()} will need a different approach for this one." },
        { c -> "${c.task()} under ${c.subj()} is now missed work. The window for it as originally planned is closed." },
        { c -> "This task wasn't completed: ${c.task()}, ${c.subj()}. It's logged as missed." },
        { c -> "${c.subj()}: ${c.task()} has been marked missed rather than merely late." },
        { c -> "Guardian notes the miss. ${c.task()} for ${c.subj()} didn't happen in its planned window." },
        { c -> "${c.task()} is missed. ${c.subj()}'s plan will need to make room for it separately now." }
    )

    // =================================================================
    // TASK_PARTIAL
    // =================================================================
    private val TASK_PARTIAL: List<(GuardianSpeechContext) -> String> = listOf(
        { c -> "${c.task()} for ${c.subj()} is partially done — ${c.progress()}. The rest still needs to be finished." },
        { c -> "Guardian records ${c.task()} as partial. ${c.subj()} is at ${c.progress()}, not complete." },
        { c -> "This is partial progress: ${c.task()}, ${c.subj()}, ${c.progress()}. There's more to do." },
        { c -> "${c.subj()}'s ${c.task()} is ${c.progress()}. It's marked partial, not done." },
        { c -> "You made progress on ${c.task()} — ${c.progress()} — but ${c.subj()} still needs the rest." },
        { c -> "${c.task()} is logged as partial at ${c.progress()}. ${c.subj()} will carry the remainder forward." },
        { c -> "Guardian confirms ${c.progress()} on ${c.task()}. ${c.subj()} isn't finished with this yet." },
        { c -> "Partial result: ${c.task()} under ${c.subj()}, sitting at ${c.progress()}." },
        { c -> "${c.subj()}: ${c.task()} is ${c.progress()}. The remaining part still needs attention." },
        { c -> "This task isn't finished — ${c.task()}, ${c.progress()}. ${c.subj()} will need to return to it." },
        { c -> "${c.progress()} of ${c.task()} is done. ${c.subj()} still has the rest of this ahead." },
        { c -> "Guardian marks ${c.task()} partial. ${c.subj()} reached ${c.progress()}, and the remainder stands." },
        { c -> "${c.task()} for ${c.subj()} stopped at ${c.progress()}. That's progress, not completion." },
        { c -> "You're partway through ${c.task()} — ${c.progress()}. ${c.subj()} will need to finish the rest." },
        { c -> "This is a partial close on ${c.task()}. ${c.subj()} reached ${c.progress()} before stopping." },
        { c -> "${c.subj()}'s ${c.task()} sits at ${c.progress()}. Recorded as partial, with work still remaining." },
        { c -> "Guardian logs ${c.progress()} on ${c.task()}. ${c.subj()} isn't done with this one." },
        { c -> "${c.task()} is ${c.progress()} for ${c.subj()}. The rest carries forward." },
        { c -> "Partial progress recorded: ${c.task()}, ${c.progress()}. ${c.subj()} still has more to cover." },
        { c -> "${c.subj()}: ${c.task()} reached ${c.progress()} and stopped there. It's marked partial." },
        { c -> "This task is incomplete but not abandoned — ${c.task()} at ${c.progress()} for ${c.subj()}." }
    )

    // =================================================================
    // FOCUS_MODE
    // =================================================================
    private val FOCUS_MODE: List<(GuardianSpeechContext) -> String> = listOf(
        { c -> "Guardian is now in Focus mode for this session. Distractions on ${c.subj()} will be tracked more closely." },
        { c -> "Focus mode is active. ${c.subj()} has your full attention now, and Guardian is watching for departures." },
        { c -> "This session is under Focus mode. Expect firmer responses if you leave ${c.subj()}." },
        { c -> "Guardian has switched to Focus mode. ${c.subj()} needs uninterrupted attention from here." },
        { c -> "Focus mode: engaged. Distraction detection is stronger for the rest of this ${c.subj()} session." },
        { c -> "You're in Focus mode now. Guardian will respond more directly to any drift from ${c.subj()}." },
        { c -> "This is a Focus session. ${c.subj()} is what should have your attention." },
        { c -> "Guardian has raised this to Focus mode. Departures from ${c.subj()} will be handled more seriously." },
        { c -> "Focus mode active for ${c.subj()}. Guardian's tolerance for distraction is lower here." },
        { c -> "You're now under Focus mode. Stay with ${c.subj()} — Guardian is paying closer attention too." },
        { c -> "This session just moved to Focus mode. ${c.subj()} deserves the attention it's about to get." },
        { c -> "Guardian confirms: Focus mode. ${c.subj()} is the priority, and drift will be noticed faster." },
        { c -> "Focus mode has engaged for this ${c.subj()} session. Expect quicker, firmer check-ins." },
        { c -> "You've entered Focus mode. Guardian will be less patient with distraction from ${c.subj()}." },
        { c -> "This is now a Focus session on ${c.subj()}. Guardian is watching more closely than usual." },
        { c -> "Focus mode is on. ${c.subj()} needs your attention, and Guardian will say so sooner if it doesn't get it." },
        { c -> "Guardian has tightened enforcement — Focus mode, for ${c.subj()}." },
        { c -> "This session is stricter now. Focus mode means ${c.subj()} gets priority over everything else." },
        { c -> "Stronger enforcement is active, as requested. Stay with ${c.subj()}." },
        { c -> "Focus mode active. Guardian's response to leaving ${c.subj()} will come faster from here." },
        { c -> "This is Focus mode. ${c.subj()} is what matters for this session, and Guardian will treat it that way." }
    )

    // =================================================================
    // SERIOUS_MODE
    // =================================================================
    private val SERIOUS_MODE: List<(GuardianSpeechContext) -> String> = listOf(
        { c -> "Guardian is treating this seriously now. The pattern of distraction has gone beyond what Focus mode allows for." },
        { c -> "This is Serious mode. Your commitment to ${c.subj()} is being enforced without much room left." },
        { c -> "Guardian isn't going to soften this further. ${c.subj()} needs your attention, and the tolerance for drift is gone." },
        { c -> "Strict enforcement is active, and this is what it looks like. ${c.subj()} is not optional right now." },
        { c -> "Serious mode, engaged. The pattern here has made lighter reminders ineffective." },
        { c -> "This has escalated. Guardian is done being gentle about ${c.subj()}." },
        { c -> "Strict enforcement is active. ${c.subj()} is the only acceptable use of this time." },
        { c -> "Guardian is being direct: this pattern needs to stop. ${c.subj()} is what you committed to." },
        { c -> "This is the serious version of Guardian. ${c.subj()} matters, and the warnings are over." },
        { c -> "You committed to this. Serious mode exists because gentler modes didn't hold." },
        { c -> "Guardian has moved past reminders. ${c.subj()} needs your full attention, immediately." },
        { c -> "This is Strict mode. The commitment you made is being held to, without exception." },
        { c -> "Guardian is not raising its voice — it's raising its seriousness. ${c.subj()}, now." },
        { c -> "The lighter approach didn't work. This is where Guardian stops repeating itself gently." },
        { c -> "Serious mode means no more soft check-ins. ${c.subj()} is the expectation, plainly stated." },
        { c -> "You gave Guardian this authority. It's being used now, for ${c.subj()}." },
        { c -> "This is as direct as Guardian gets. ${c.subj()} needs to happen, not eventually — now." },
        { c -> "Guardian is holding the line here. ${c.subj()} was the commitment, and it still is." },
        { c -> "Strict mode doesn't ask twice. ${c.subj()} is where your attention belongs." },
        { c -> "This is serious because the pattern made it serious. ${c.subj()}, without further negotiation." },
        { c -> "Guardian has nothing softer left to say. Return to ${c.subj()}." }
    )

    // =================================================================
    // TEMPORARY_PLAN_URGENT
    // =================================================================
    private val TEMPORARY_PLAN_URGENT: List<(GuardianSpeechContext) -> String> = listOf(
        { c -> "Your ${c.task()} for ${c.subj()} is still incomplete, and the deadline is ${c.deadline()}. The planner has marked this high priority." },
        { c -> "${c.task()} is urgent now. ${c.subj()}'s deadline is ${c.deadline()}, and your remaining time needs to account for it." },
        { c -> "This Temporary Plan is pressing: ${c.task()}, ${c.subj()}, due ${c.deadline()}." },
        { c -> "${c.subj()}'s ${c.task()} hasn't been finished, and ${c.deadline()} is close. Guardian is flagging it." },
        { c -> "Guardian marks ${c.task()} urgent. ${c.subj()} needs this done by ${c.deadline()}." },
        { c -> "Time is short on ${c.task()} for ${c.subj()} — the deadline is ${c.deadline()}." },
        { c -> "This temporary plan needs attention: ${c.task()}, due ${c.deadline()}, still open for ${c.subj()}." },
        { c -> "${c.task()} is close to its deadline — ${c.deadline()}. ${c.subj()} still needs this finished." },
        { c -> "Guardian is treating ${c.task()} as urgent. ${c.subj()}'s deadline of ${c.deadline()} is approaching fast." },
        { c -> "${c.subj()}: ${c.task()} is still incomplete, with ${c.deadline()} coming up. This needs priority now." },
        { c -> "This is a Temporary Plan running out of room — ${c.task()}, ${c.deadline()}, ${c.subj()}." },
        { c -> "${c.task()} deserves attention today. ${c.subj()}'s deadline is ${c.deadline()}, and it's still unfinished." },
        { c -> "Guardian flags ${c.task()} as urgent for ${c.subj()}. ${c.deadline()} doesn't leave much time." },
        { c -> "The deadline on ${c.task()} is ${c.deadline()}. ${c.subj()} still needs real time spent on it." },
        { c -> "This temporary commitment is urgent: ${c.task()}, ${c.subj()}, due ${c.deadline()}." },
        { c -> "${c.subj()}'s ${c.task()} needs to move up your list — ${c.deadline()} is close." },
        { c -> "Guardian is raising ${c.task()}'s priority. ${c.subj()} has until ${c.deadline()} to finish it." },
        { c -> "${c.task()} is not finished, and ${c.deadline()} is near. ${c.subj()} needs this handled." },
        { c -> "This is time-sensitive: ${c.task()} for ${c.subj()}, deadline ${c.deadline()}." },
        { c -> "Guardian confirms ${c.task()} as urgent. ${c.subj()} still has work left before ${c.deadline()}." },
        { c -> "${c.subj()}: ${c.task()} is due ${c.deadline()} and still open. Treat it as the priority it's become." }
    )

    // =================================================================
    // EXAM_COUNTDOWN — a neutral marker, distinct in tone from EXAM_URGENT
    // =================================================================
    private val EXAM_COUNTDOWN: List<(GuardianSpeechContext) -> String> = listOf(
        { c -> "Your ${c.subj()} ${c.exam()} is ${c.examDays()} away." },
        { c -> "${c.examDays()} remain until your ${c.subj()} ${c.exam()}." },
        { c -> "Guardian is tracking the countdown: ${c.subj()} ${c.exam()}, ${c.examDays()} to go." },
        { c -> "${c.subj()}'s ${c.exam()} is ${c.examDays()} out. Preparation continues on schedule." },
        { c -> "The countdown to your ${c.subj()} ${c.exam()} stands at ${c.examDays()}." },
        { c -> "${c.examDays()} until ${c.subj()}'s ${c.exam()}. Nothing urgent yet, just tracking." },
        { c -> "Guardian notes ${c.examDays()} remaining before the ${c.subj()} ${c.exam()}." },
        { c -> "Your ${c.exam()} in ${c.subj()} is ${c.examDays()} away — on the radar, not yet urgent." },
        { c -> "${c.subj()} ${c.exam()}: ${c.examDays()} left on the countdown." },
        { c -> "This is a routine check-in — ${c.subj()}'s ${c.exam()} is ${c.examDays()} out." },
        { c -> "${c.examDays()} to ${c.subj()}'s ${c.exam()}. Preparation is proceeding." },
        { c -> "Guardian confirms the countdown: ${c.examDays()} until ${c.exam()} in ${c.subj()}." },
        { c -> "${c.subj()}'s ${c.exam()} approaches in ${c.examDays()}." },
        { c -> "Just a marker: ${c.examDays()} remain before your ${c.subj()} ${c.exam()}." },
        { c -> "The ${c.subj()} ${c.exam()} countdown reads ${c.examDays()}." },
        { c -> "${c.examDays()} out from ${c.subj()}'s ${c.exam()} — plenty of runway still." },
        { c -> "Guardian is keeping time on ${c.subj()}'s ${c.exam()}: ${c.examDays()} left." },
        { c -> "${c.subj()} ${c.exam()} in ${c.examDays()}. Preparation continues as planned." },
        { c -> "This is your marker for the ${c.subj()} ${c.exam()}: ${c.examDays()} out." },
        { c -> "${c.examDays()} until ${c.exam()}, ${c.subj()}. Nothing to escalate yet." },
        { c -> "Guardian logs ${c.examDays()} remaining before ${c.subj()}'s ${c.exam()}." }
    )
}
