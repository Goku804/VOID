package com.core.voidapp.data.ai

import com.core.voidapp.data.*
import java.time.LocalDate

/**
 * Builds the CONTEXT block sent to the AI alongside the user's message.
 * Every line here comes straight from VoidRepository or PlanningEngine —
 * nothing is invented. This also deliberately does NOT dump the whole
 * database: it picks sections based on lightweight keyword matching on
 * the user's message, keeping requests small and avoiding sending
 * unrelated academic data for an unrelated question.
 *
 * This is NOT command matching — the keywords only decide which real
 * data sections to include. The AI still receives the user's raw message
 * and produces its own natural-language response from it.
 */
object AIContextBuilder {

    fun build(userMessage: String): String {
        val lower = userMessage.lowercase()
        val sections = mutableListOf<String>()

        sections += subjectsSummary()

        val wantsSchedule = containsAny(lower, "study", "tonight", "today", "schedule", "plan", "time", "hours", "focus", "do", "capacity")
        val wantsExam = containsAny(lower, "exam", "test", "sitting")
        val wantsProgress = containsAny(lower, "behind", "progress", "doing", "week", "grade", "mark", "score", "how am i")

        // Default to including schedule + exam context when intent is unclear —
        // cheaper to include than to have the AI guess at missing information.
        if (wantsSchedule || (!wantsExam && !wantsProgress)) sections += todayScheduleSummary()
        if (wantsExam || (!wantsSchedule && !wantsProgress)) sections += examsSummary()
        if (wantsProgress) sections += progressSummary()

        sections += focusSummary()

        return sections.filter { it.isNotBlank() }.joinToString("\n\n")
    }

    private fun containsAny(text: String, vararg keywords: String) = keywords.any { text.contains(it) }

    private fun subjectsSummary(): String {
        if (VoidRepository.subjects.isEmpty()) return "SUBJECTS: none registered yet."
        val lines = VoidRepository.subjects.joinToString("; ") { s -> "${s.name} (grade ${s.grade})" }
        return "SUBJECTS: $lines"
    }

    private fun todayScheduleSummary(): String {
        val today = todayAsVoidDay()
        val classes = VoidRepository.scheduleFor(today)
        val circlePlans = VoidRepository.circlePlansFor(today)
        val night = VoidRepository.nightAvailabilityFor(today)

        val classLine = if (classes.isEmpty()) "no classes registered for today"
        else classes.joinToString("; ") { c ->
            "${VoidRepository.subjectName(c.subjectId)} (${c.classType.name}${c.startTime?.let { " $it" } ?: ""})"
        }

        val planLine = if (circlePlans.isEmpty()) "no Circle Plan slots today"
        else circlePlans.joinToString("; ") { p ->
            "${VoidRepository.subjectName(p.subjectId)} ${p.durationMinutes}min (${p.window.name})"
        }

        val nightLine = if (night.available) "available ${night.start ?: "?"}-${night.end ?: "?"}" else "not marked available"

        return "TODAY (${today.name}): classes: $classLine. Circle Plan: $planLine. Night study: $nightLine."
    }

    private fun examsSummary(): String {
        val upcoming = VoidRepository.upcomingExamSubjects()
        if (upcoming.isEmpty()) return "EXAMS: none scheduled."
        val lines = upcoming.take(6).joinToString("; ") { es ->
            val exam = VoidRepository.examFor(es)
            "${exam?.examType?.name ?: "Exam"} ${VoidRepository.subjectName(es.subjectId)} \u2014 ${es.status().label(es.daysRemaining())}"
        }
        return "EXAMS: $lines"
    }

    private fun progressSummary(): String {
        if (VoidRepository.subjects.isEmpty()) return "PROGRESS: no subjects registered yet."
        val lines = VoidRepository.subjects.joinToString("; ") { s ->
            val graded = s.assessmentTypes.count { it.entry != null }
            "${s.name}: $graded/${s.assessmentTypes.size} assessments graded"
        }
        val overdue = VoidRepository.activeTemporaryTasks().count { it.isOverdue() }
        return "PROGRESS: $lines. Overdue tasks: $overdue."
    }

    private fun focusSummary(): String {
        val focus = PlanningEngine.rankedFocus()
        if (focus.isEmpty()) return "PLANNING ENGINE: nothing currently flagged for focus."
        val lines = focus.take(6).joinToString("; ") { f ->
            "${f.label}${if (f.minutes > 0) " (${f.minutes}min)" else ""} \u2014 ${f.reason}"
        }
        return "PLANNING ENGINE RANKED FOCUS (defer to this ordering, do not invent your own schedule): $lines"
    }

    private fun todayAsVoidDay(): DayOfWeekVoid {
        val d = LocalDate.now().dayOfWeek
        return DayOfWeekVoid.valueOf(d.name)
    }
}
