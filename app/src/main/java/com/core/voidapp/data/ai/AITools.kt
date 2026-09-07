package com.core.voidapp.data.ai

import com.core.voidapp.data.ContentStrategy
import com.core.voidapp.data.DayOfWeekVoid
import com.core.voidapp.data.ExamSession
import com.core.voidapp.data.ExamType
import com.core.voidapp.data.PlanPriority
import com.core.voidapp.data.PlanTaskStatus
import com.core.voidapp.data.PreferredWindow
import com.core.voidapp.data.TemporaryPlanType
import com.core.voidapp.data.VoidRepository
import com.core.voidapp.data.status
import com.core.voidapp.data.subjectsSorted
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalTime

/**
 * One tool the AI can call, in Anthropic tool / OpenAI function-calling
 * shape (AIApiClient translates this into whichever wire format the
 * configured provider expects).
 */
data class AIToolDef(val name: String, val description: String, val inputSchema: JSONObject)

/**
 * The full surface of things the AI is granted permission to do inside
 * VOID: create, edit, delete, and read (analysis) access to subjects,
 * tasks, exams, Circle Plan slots, and scores. Every tool maps straight
 * onto an existing VoidRepository function — the AI can't do anything
 * through a tool call that the UI itself couldn't also do, and nothing
 * here touches AI provider credentials or reaches outside VoidRepository.
 *
 * Gating: whether these are even offered to the model is decided by
 * AIRepository, which checks AIConfigStore.areToolsEnabled() first. This
 * object only defines what's possible when that permission is on.
 */
object AITools {

    fun definitions(): List<AIToolDef> = listOf(
        AIToolDef("list_subjects", "List every registered subject with its grade and code.", obj()),
        AIToolDef(
            "create_subject", "Create a new academic subject.",
            obj(
                "name" to str("Subject name, e.g. Mathematics"),
                "grade" to int_("Grade/year level, e.g. 11"),
                "code" to str("Optional short code, e.g. MATH"),
                required = listOf("name", "grade")
            )
        ),
        AIToolDef(
            "create_task", "Create a one-off task, assignment, homework, or test item on the temporary plan.",
            obj(
                "title" to str("Task title"),
                "type" to str(
                    "Task type",
                    enum = listOf("TEST", "ASSIGNMENT", "HOMEWORK", "EXTRA_CLASS", "TEACHER_REQUEST", "PROJECT", "MAKE_UP_CLASS", "URGENT_REVISION", "MISSED_WORK_RECOVERY", "OTHER")
                ),
                "subject_name" to str("Subject this task belongs to, if any — must match an existing subject name exactly"),
                "deadline" to str("Deadline date, ISO format YYYY-MM-DD"),
                "required_minutes" to int_("Estimated minutes needed to complete it, 0 if unknown"),
                "priority" to str("Priority", enum = listOf("LOW", "NORMAL", "HIGH")),
                "notes" to str("Optional free-text notes"),
                required = listOf("title", "deadline")
            )
        ),
        AIToolDef(
            "update_task_status", "Change the status of an existing task (mark complete, cancelled, etc). Matches by exact task title.",
            obj(
                "title" to str("Exact title of the task to update"),
                "status" to str("New status", enum = listOf("PLANNED", "IN_PROGRESS", "COMPLETED", "PARTIAL", "MISSED", "CANCELLED")),
                required = listOf("title", "status")
            )
        ),
        AIToolDef(
            "log_task_progress", "Add completed study minutes toward a task's required time. Matches by exact task title.",
            obj(
                "title" to str("Exact title of the task"),
                "minutes" to int_("Minutes to add — can be negative to correct an over-log"),
                required = listOf("title", "minutes")
            )
        ),
        AIToolDef(
            "delete_task", "Permanently delete a task. Matches by exact task title. Only use when the user clearly asked to remove it.",
            obj("title" to str("Exact title of the task to delete"), required = listOf("title"))
        ),
        AIToolDef(
            "create_exam",
            "Register an exam sitting (Test, Mid, Final, or Mock) for a subject. Calling this again with the same subject already covered creates a separate exam — to add a second subject under the SAME exam period (e.g. Physics also sitting the same Final), the user should register it through the app's multi-subject exam form instead; this tool always creates one exam per call.",
            obj(
                "exam_type" to str("Exam type", enum = EXAM_TYPES),
                "subject_name" to str("Subject name — must match an existing subject exactly"),
                "date" to str("Exam date, ISO format YYYY-MM-DD"),
                "time" to str("Optional time, 24h HH:mm"),
                "session" to str("Optional session", enum = listOf("MORNING", "AFTERNOON", "EVENING", "CUSTOM")),
                "location" to str("Optional location"),
                "notes" to str("Optional notes"),
                "grades" to str("Optional comma-separated grade levels this covers, e.g. '9,10,11' — meaningful mainly for MOCK"),
                required = listOf("exam_type", "subject_name", "date")
            )
        ),
        AIToolDef(
            "delete_exam", "Delete an exam sitting. Matches by subject name and exam type. Only use when the user clearly asked to remove it.",
            obj(
                "subject_name" to str("Subject name of the exam sitting to delete"),
                "exam_type" to str("Exam type", enum = EXAM_TYPES),
                required = listOf("subject_name", "exam_type")
            )
        ),
        AIToolDef(
            "create_circle_plan", "Create a recurring weekly Circle Plan study slot for a subject.",
            obj(
                "day" to str("Day of week", enum = DAYS),
                "subject_name" to str("Subject name — must match an existing subject exactly"),
                "duration_minutes" to int_("Duration of the slot in minutes"),
                "window" to str("Preferred time window", enum = listOf("MORNING", "AFTERNOON", "NIGHT", "ANYTIME")),
                "priority" to str("Priority", enum = listOf("LOW", "NORMAL", "HIGH")),
                required = listOf("day", "subject_name", "duration_minutes", "window")
            )
        ),
        AIToolDef(
            "delete_circle_plan", "Delete a recurring Circle Plan slot. Matches by day and subject name. Only use when the user clearly asked to remove it.",
            obj(
                "day" to str("Day of week", enum = DAYS),
                "subject_name" to str("Subject name of the slot to delete"),
                required = listOf("day", "subject_name")
            )
        ),
        AIToolDef(
            "record_score", "Record or update the score for an existing graded assessment (Test, Assignment, etc) on a subject. Matches by subject name and exact assessment label.",
            obj(
                "subject_name" to str("Subject name"),
                "assessment_label" to str("Exact label of the assessment, e.g. 'Chapter 3 Test'"),
                "score" to num("Score achieved, out of that assessment's max score"),
                required = listOf("subject_name", "assessment_label", "score")
            )
        ),
        AIToolDef(
            "get_full_snapshot",
            "Read the complete current app state — every subject, task, exam, and Circle Plan slot — for deeper analysis than the default context provides. Call this before answering questions that need full detail, totals, or comparisons across everything.",
            obj()
        )
    )

    private val DAYS = listOf("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY")
    private val EXAM_TYPES = listOf("TEST", "MID", "FINAL", "MOCK")

    // ---- JSON schema helpers ----
    private fun str(desc: String, enum: List<String>? = null): JSONObject {
        val o = JSONObject().put("type", "string").put("description", desc)
        if (enum != null) o.put("enum", JSONArray(enum))
        return o
    }

    private fun num(desc: String) = JSONObject().put("type", "number").put("description", desc)
    private fun int_(desc: String) = JSONObject().put("type", "integer").put("description", desc)

    private fun obj(vararg props: Pair<String, JSONObject>, required: List<String> = emptyList()): JSONObject {
        val properties = JSONObject()
        props.forEach { (k, v) -> properties.put(k, v) }
        val o = JSONObject().put("type", "object").put("properties", properties)
        if (required.isNotEmpty()) o.put("required", JSONArray(required))
        return o
    }
}

/**
 * Executes a single tool call against VoidRepository and returns a plain
 * text result to hand back to the model as the tool_result content.
 * Deliberately never throws past this boundary — failures come back as a
 * short "ERROR: ..." string so the model can adapt (re-check a subject
 * name, ask the user, etc.) instead of the whole chat request failing.
 */
object AIToolExecutor {

    fun execute(name: String, input: JSONObject): String = try {
        when (name) {
            "list_subjects" -> listSubjects()
            "create_subject" -> createSubject(input)
            "create_task" -> createTask(input)
            "update_task_status" -> updateTaskStatus(input)
            "log_task_progress" -> logTaskProgress(input)
            "delete_task" -> deleteTask(input)
            "create_exam" -> createExam(input)
            "delete_exam" -> deleteExam(input)
            "create_circle_plan" -> createCirclePlan(input)
            "delete_circle_plan" -> deleteCirclePlan(input)
            "record_score" -> recordScore(input)
            "get_full_snapshot" -> fullSnapshot()
            else -> "ERROR: unknown tool '$name'."
        }
    } catch (e: Exception) {
        "ERROR: ${e.message ?: "tool execution failed"}."
    }

    private fun findSubject(name: String) =
        VoidRepository.subjects.find { it.name.equals(name, ignoreCase = true) }

    private fun findTask(title: String) =
        VoidRepository.temporaryTasks.find { it.title.equals(title, ignoreCase = true) }

    private fun listSubjects(): String {
        if (VoidRepository.subjects.isEmpty()) return "No subjects registered yet."
        return VoidRepository.subjects.joinToString("; ") { "${it.name} (grade ${it.grade}${if (it.code.isNotBlank()) ", ${it.code}" else ""})" }
    }

    private fun createSubject(input: JSONObject): String {
        val name = input.getString("name")
        val grade = input.optInt("grade", 0)
        val code = input.optString("code", "")
        val subject = VoidRepository.addSubject(name = name, grade = grade, code = code)
        return "Created subject '${subject.name}' (grade ${subject.grade})."
    }

    private fun createTask(input: JSONObject): String {
        val title = input.getString("title")
        val type = runCatching { TemporaryPlanType.valueOf(input.optString("type", "OTHER")) }.getOrDefault(TemporaryPlanType.OTHER)
        val subjectName = input.optString("subject_name", "")
        val subject = if (subjectName.isNotBlank()) findSubject(subjectName) else null
        if (subjectName.isNotBlank() && subject == null) return "ERROR: no subject named '$subjectName'. Call list_subjects to see valid names."
        val deadline = runCatching { LocalDate.parse(input.getString("deadline")) }.getOrNull()
            ?: return "ERROR: deadline must be ISO format YYYY-MM-DD."
        val minutes = input.optInt("required_minutes", 0)
        val priority = runCatching { PlanPriority.valueOf(input.optString("priority", "NORMAL")) }.getOrDefault(PlanPriority.NORMAL)
        val notes = input.optString("notes", "")
        val task = VoidRepository.addTemporaryTask(
            title = title, type = type, subjectId = subject?.id, startDate = null,
            deadline = deadline, requiredMinutes = minutes, priority = priority, notes = notes
        )
        return "Created task '${task.title}' due ${task.deadline}."
    }

    private fun updateTaskStatus(input: JSONObject): String {
        val title = input.getString("title")
        val task = findTask(title) ?: return "ERROR: no task titled '$title'."
        val status = runCatching { PlanTaskStatus.valueOf(input.getString("status")) }.getOrNull()
            ?: return "ERROR: invalid status."
        VoidRepository.setTaskStatus(task.id, status)
        return "Task '${task.title}' set to $status."
    }

    private fun logTaskProgress(input: JSONObject): String {
        val title = input.getString("title")
        val task = findTask(title) ?: return "ERROR: no task titled '$title'."
        val minutes = input.getInt("minutes")
        VoidRepository.addProgress(task.id, minutes)
        return "Logged $minutes minute(s) on '${task.title}'."
    }

    private fun deleteTask(input: JSONObject): String {
        val title = input.getString("title")
        val task = findTask(title) ?: return "ERROR: no task titled '$title'."
        VoidRepository.deleteTemporaryTask(task.id)
        return "Deleted task '${task.title}'."
    }

    private fun createExam(input: JSONObject): String {
        val examType = runCatching { ExamType.valueOf(input.getString("exam_type")) }.getOrNull()
            ?: return "ERROR: invalid exam_type."
        val subjectName = input.getString("subject_name")
        val subject = findSubject(subjectName) ?: return "ERROR: no subject named '$subjectName'."
        val date = runCatching { LocalDate.parse(input.getString("date")) }.getOrNull()
            ?: return "ERROR: date must be ISO format YYYY-MM-DD."
        val time = input.optString("time", "").takeIf { it.isNotBlank() }
            ?.let { runCatching { LocalTime.parse(it) }.getOrNull() }
        val session = input.optString("session", "").takeIf { it.isNotBlank() }
            ?.let { runCatching { ExamSession.valueOf(it) }.getOrNull() }
        val location = input.optString("location", "")
        val notes = input.optString("notes", "")
        val grades = input.optString("grades", "")
            .split(",").mapNotNull { it.trim().toIntOrNull() }
        VoidRepository.registerExam(
            examType = examType, subjectId = subject.id, date = date, time = time,
            session = session, location = location, notes = notes, grades = grades
        )
        return "Registered ${examType.name} exam for '${subject.name}' on $date."
    }

    private fun deleteExam(input: JSONObject): String {
        val subjectName = input.getString("subject_name")
        val examType = runCatching { ExamType.valueOf(input.getString("exam_type")) }.getOrNull()
            ?: return "ERROR: invalid exam_type."
        val subject = findSubject(subjectName) ?: return "ERROR: no subject named '$subjectName'."
        val examSubject = VoidRepository.examSubjects.find {
            it.subjectId == subject.id && VoidRepository.examFor(it)?.examType == examType
        } ?: return "ERROR: no ${examType.name} exam found for '${subject.name}'."
        VoidRepository.deleteExam(examSubject.examId)
        return "Deleted ${examType.name} exam for '${subject.name}'."
    }

    private fun createCirclePlan(input: JSONObject): String {
        val day = runCatching { DayOfWeekVoid.valueOf(input.getString("day")) }.getOrNull()
            ?: return "ERROR: invalid day."
        val subjectName = input.getString("subject_name")
        val subject = findSubject(subjectName) ?: return "ERROR: no subject named '$subjectName'."
        val duration = input.getInt("duration_minutes")
        val window = runCatching { PreferredWindow.valueOf(input.getString("window")) }.getOrNull()
            ?: return "ERROR: invalid window."
        val priority = runCatching { PlanPriority.valueOf(input.optString("priority", "NORMAL")) }.getOrDefault(PlanPriority.NORMAL)
        VoidRepository.addCirclePlan(
            day = day, subjectId = subject.id, durationMinutes = duration, window = window,
            strategy = ContentStrategy.CONTINUE_NEXT_UNIT, priority = priority
        )
        return "Created Circle Plan: ${subject.name} on ${day.name}, ${duration}min ($window)."
    }

    private fun deleteCirclePlan(input: JSONObject): String {
        val day = runCatching { DayOfWeekVoid.valueOf(input.getString("day")) }.getOrNull()
            ?: return "ERROR: invalid day."
        val subjectName = input.getString("subject_name")
        val subject = findSubject(subjectName) ?: return "ERROR: no subject named '$subjectName'."
        val plan = VoidRepository.circlePlans.find { it.day == day && it.subjectId == subject.id }
            ?: return "ERROR: no Circle Plan found for '${subject.name}' on ${day.name}."
        VoidRepository.deleteCirclePlan(plan.id)
        return "Deleted Circle Plan for '${subject.name}' on ${day.name}."
    }

    private fun recordScore(input: JSONObject): String {
        val subjectName = input.getString("subject_name")
        val subject = findSubject(subjectName) ?: return "ERROR: no subject named '$subjectName'."
        val label = input.getString("assessment_label")
        val type = subject.assessmentTypes.find { it.label.equals(label, ignoreCase = true) }
            ?: return "ERROR: no assessment labeled '$label' on '${subject.name}'."
        val score = input.getDouble("score")
        VoidRepository.recordScore(subject.id, type.id, score)
        return "Recorded score $score for '${type.label}' on '${subject.name}'."
    }

    private fun fullSnapshot(): String {
        val sb = StringBuilder()
        sb.append("SUBJECTS: ").append(
            VoidRepository.subjects.joinToString("; ") { s ->
                val assessments = s.assessmentTypes.joinToString(", ") { "${it.label}:${it.entry?.score ?: "ungraded"}/${it.maxScore}" }
                "${s.name} (grade ${s.grade}${if (s.code.isNotBlank()) ", ${s.code}" else ""}) assessments=[$assessments]"
            }.ifBlank { "none" }
        )
        sb.append("\nTASKS: ").append(
            VoidRepository.temporaryTasks.joinToString("; ") { t ->
                "${t.title} [${t.status}] due ${t.deadline} (${t.completedMinutes}/${t.requiredMinutes}min, ${t.priority})"
            }.ifBlank { "none" }
        )
        sb.append("\nEXAMS: ").append(
            VoidRepository.exams.joinToString("; ") { exam ->
                val subjects = exam.subjectsSorted().joinToString(", ") { es ->
                    "${VoidRepository.subjectName(es.subjectId)} on ${es.date}${es.time?.let { " $it" } ?: ""} [${es.status()}]"
                }
                val gradesPart = if (exam.grades.isNotEmpty()) " grades=${exam.grades.sorted()}" else ""
                "${exam.examType.name}(id=${exam.id})$gradesPart: $subjects"
            }.ifBlank { "none" }
        )
        sb.append("\nCIRCLE PLANS: ").append(
            VoidRepository.circlePlans.joinToString("; ") { p ->
                "${VoidRepository.subjectName(p.subjectId)} ${p.day} ${p.durationMinutes}min (${p.window})"
            }.ifBlank { "none" }
        )
        return sb.toString()
    }
}
