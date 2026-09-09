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
import com.core.voidapp.data.isFullyGraded
import com.core.voidapp.data.status
import com.core.voidapp.data.subjectsSorted
import com.core.voidapp.data.totalWeight
import com.core.voidapp.data.weightedTotal
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
            "update_subject", "Edit an existing subject's name, grade, or code. Matches by exact current subject name. Only fields provided are changed.",
            obj(
                "subject_name" to str("Current exact name of the subject to edit"),
                "new_name" to str("New name, if changing it"),
                "new_grade" to int_("New grade/year level, if changing it"),
                "new_code" to str("New short code, if changing it"),
                required = listOf("subject_name")
            )
        ),
        AIToolDef(
            "delete_subject",
            "Permanently delete a subject and everything attached only to it: its units, marks/assessment scores, weekly class periods, Circle Plan slots, and its sittings inside any exam. Matches by exact subject name. Only use when the user clearly asked to remove it — this cannot be undone.",
            obj("subject_name" to str("Exact name of the subject to delete"), required = listOf("subject_name"))
        ),
        AIToolDef(
            "create_unit", "Create a syllabus unit within a subject (e.g. 'Unit 2 — Functions'), used by Circle Plan to track study progress through the subject.",
            obj(
                "subject_name" to str("Subject this unit belongs to — must match an existing subject exactly"),
                "unit_number" to int_("Ordering number of the unit within the subject, e.g. 2"),
                "name" to str("Unit name, e.g. 'Functions'"),
                "description" to str("Optional short description"),
                "estimated_minutes" to int_("Optional estimated study minutes to complete this unit, 0 if unknown"),
                required = listOf("subject_name", "unit_number", "name")
            )
        ),
        AIToolDef(
            "update_unit", "Edit an existing unit's number, name, description, or estimated study minutes. Matches by subject name and current unit name. Only fields provided are changed.",
            obj(
                "subject_name" to str("Subject the unit belongs to"),
                "unit_name" to str("Current exact name of the unit to edit"),
                "new_unit_number" to int_("New ordering number, if changing it"),
                "new_name" to str("New unit name, if changing it"),
                "new_description" to str("New description, if changing it"),
                "new_estimated_minutes" to int_("New estimated study minutes, if changing it"),
                required = listOf("subject_name", "unit_name")
            )
        ),
        AIToolDef(
            "delete_unit", "Permanently delete a unit. Matches by subject name and unit name. Only use when the user clearly asked to remove it.",
            obj(
                "subject_name" to str("Subject the unit belongs to"),
                "unit_name" to str("Exact name of the unit to delete"),
                required = listOf("subject_name", "unit_name")
            )
        ),
        AIToolDef(
            "list_units", "List every unit registered under a subject, in order, with study-progress read access for analysis.",
            obj("subject_name" to str("Subject to list units for"), required = listOf("subject_name"))
        ),
        AIToolDef(
            "create_mark", "Create a new gradable mark (assessment) on a subject, e.g. 'Chapter 3 Test' worth 20% out of 100 — the score itself is set afterward with record_score.",
            obj(
                "subject_name" to str("Subject this mark belongs to — must match an existing subject exactly"),
                "kind" to str("Assessment kind", enum = listOf("TEST", "ASSIGNMENT", "MID_EXAM", "FINAL_EXAM", "MOCK_EXAM", "QUIZ", "OTHER")),
                "label" to str("Display label, e.g. 'Chapter 3 Test'"),
                "weight_percent" to num("Weight of this mark toward the subject's total, e.g. 20"),
                "max_score" to num("Maximum possible score for this mark, e.g. 100"),
                required = listOf("subject_name", "kind", "label", "weight_percent", "max_score")
            )
        ),
        AIToolDef(
            "update_mark", "Edit an existing mark's label, weight, or max score (not the recorded score itself — use record_score for that). Matches by subject name and exact current label.",
            obj(
                "subject_name" to str("Subject the mark belongs to"),
                "label" to str("Current exact label of the mark to edit"),
                "new_label" to str("New label, if changing it"),
                "new_weight_percent" to num("New weight percent, if changing it"),
                "new_max_score" to num("New max score, if changing it"),
                required = listOf("subject_name", "label")
            )
        ),
        AIToolDef(
            "delete_mark", "Permanently delete a mark (assessment definition) and whatever score was recorded on it. Matches by subject name and exact label. Only use when the user clearly asked to remove it.",
            obj(
                "subject_name" to str("Subject the mark belongs to"),
                "label" to str("Exact label of the mark to delete"),
                required = listOf("subject_name", "label")
            )
        ),
        AIToolDef(
            "analyze_subject",
            "Deep analysis of one subject: every mark with its weight/score/grading status, the weighted running total, whether it's fully graded, and every unit with its study progress. Call this before answering questions about a specific subject's performance or standing.",
            obj("subject_name" to str("Subject to analyze"), required = listOf("subject_name"))
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
            "update_subject" -> updateSubject(input)
            "delete_subject" -> deleteSubject(input)
            "create_unit" -> createUnit(input)
            "update_unit" -> updateUnit(input)
            "delete_unit" -> deleteUnit(input)
            "list_units" -> listUnits(input)
            "create_mark" -> createMark(input)
            "update_mark" -> updateMark(input)
            "delete_mark" -> deleteMark(input)
            "analyze_subject" -> analyzeSubject(input)
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

    private fun updateSubject(input: JSONObject): String {
        val subjectName = input.getString("subject_name")
        val subject = findSubject(subjectName) ?: return "ERROR: no subject named '$subjectName'."
        val newName = input.optString("new_name", "").takeIf { it.isNotBlank() }
        val newGrade = if (input.has("new_grade")) input.optInt("new_grade") else null
        val newCode = if (input.has("new_code")) input.optString("new_code") else null
        val updated = VoidRepository.updateSubject(subject.id, name = newName, grade = newGrade, code = newCode)
            ?: return "ERROR: could not update subject."
        return "Updated subject '${updated.name}' (grade ${updated.grade}${if (updated.code.isNotBlank()) ", ${updated.code}" else ""})."
    }

    private fun deleteSubject(input: JSONObject): String {
        val subjectName = input.getString("subject_name")
        val subject = findSubject(subjectName) ?: return "ERROR: no subject named '$subjectName'."
        VoidRepository.deleteSubject(subject.id)
        return "Deleted subject '${subject.name}' and everything attached only to it (units, marks, class periods, Circle Plan slots, exam sittings)."
    }

    private fun findUnit(subjectId: String, unitName: String) =
        VoidRepository.unitsFor(subjectId).find { it.name.equals(unitName, ignoreCase = true) }

    private fun findAssessmentType(subject: com.core.voidapp.data.Subject, label: String) =
        subject.assessmentTypes.find { it.label.equals(label, ignoreCase = true) }

    private fun createUnit(input: JSONObject): String {
        val subjectName = input.getString("subject_name")
        val subject = findSubject(subjectName) ?: return "ERROR: no subject named '$subjectName'."
        val unitNumber = input.getInt("unit_number")
        val name = input.getString("name")
        val description = input.optString("description", "")
        val estimatedMinutes = input.optInt("estimated_minutes", 0)
        val unit = VoidRepository.addUnit(
            subjectId = subject.id, unitNumber = unitNumber, name = name,
            description = description, estimatedStudyMinutes = estimatedMinutes
        )
        return "Created unit '${unit.name}' (unit ${unit.unitNumber}) under '${subject.name}'."
    }

    private fun updateUnit(input: JSONObject): String {
        val subjectName = input.getString("subject_name")
        val subject = findSubject(subjectName) ?: return "ERROR: no subject named '$subjectName'."
        val unitName = input.getString("unit_name")
        val unit = findUnit(subject.id, unitName) ?: return "ERROR: no unit named '$unitName' under '${subject.name}'."
        val newNumber = if (input.has("new_unit_number")) input.optInt("new_unit_number") else null
        val newName = input.optString("new_name", "").takeIf { it.isNotBlank() }
        val newDescription = if (input.has("new_description")) input.optString("new_description") else null
        val newMinutes = if (input.has("new_estimated_minutes")) input.optInt("new_estimated_minutes") else null
        val updated = VoidRepository.updateUnit(
            unit.id, unitNumber = newNumber, name = newName,
            description = newDescription, estimatedStudyMinutes = newMinutes
        ) ?: return "ERROR: could not update unit."
        return "Updated unit '${updated.name}' (unit ${updated.unitNumber}) under '${subject.name}'."
    }

    private fun deleteUnit(input: JSONObject): String {
        val subjectName = input.getString("subject_name")
        val subject = findSubject(subjectName) ?: return "ERROR: no subject named '$subjectName'."
        val unitName = input.getString("unit_name")
        val unit = findUnit(subject.id, unitName) ?: return "ERROR: no unit named '$unitName' under '${subject.name}'."
        VoidRepository.deleteUnit(unit.id)
        return "Deleted unit '${unit.name}' from '${subject.name}'."
    }

    private fun listUnits(input: JSONObject): String {
        val subjectName = input.getString("subject_name")
        val subject = findSubject(subjectName) ?: return "ERROR: no subject named '$subjectName'."
        val units = VoidRepository.unitsFor(subject.id)
        if (units.isEmpty()) return "No units registered under '${subject.name}' yet."
        return units.joinToString("; ") { "Unit ${it.unitNumber}: ${it.name}${if (it.estimatedStudyMinutes > 0) " (~${it.estimatedStudyMinutes}min)" else ""}" }
    }

    private fun createMark(input: JSONObject): String {
        val subjectName = input.getString("subject_name")
        val subject = findSubject(subjectName) ?: return "ERROR: no subject named '$subjectName'."
        val kind = runCatching { com.core.voidapp.data.AssessmentKind.valueOf(input.getString("kind")) }.getOrNull()
            ?: return "ERROR: invalid kind."
        val label = input.getString("label")
        val weight = input.getDouble("weight_percent")
        val maxScore = input.getDouble("max_score")
        VoidRepository.addAssessmentType(subject.id, kind = kind, label = label, weightPercent = weight, maxScore = maxScore)
        return "Created mark '$label' on '${subject.name}' (${weight}% of total, out of $maxScore)."
    }

    private fun updateMark(input: JSONObject): String {
        val subjectName = input.getString("subject_name")
        val subject = findSubject(subjectName) ?: return "ERROR: no subject named '$subjectName'."
        val label = input.getString("label")
        val type = findAssessmentType(subject, label) ?: return "ERROR: no mark labeled '$label' on '${subject.name}'."
        val newLabel = input.optString("new_label", "").takeIf { it.isNotBlank() }
        val newWeight = if (input.has("new_weight_percent")) input.optDouble("new_weight_percent") else null
        val newMax = if (input.has("new_max_score")) input.optDouble("new_max_score") else null
        val updated = VoidRepository.updateAssessmentType(
            subject.id, type.id, label = newLabel, weightPercent = newWeight, maxScore = newMax
        ) ?: return "ERROR: could not update mark."
        return "Updated mark '${updated.label}' on '${subject.name}' (${updated.weightPercent}% of total, out of ${updated.maxScore})."
    }

    private fun deleteMark(input: JSONObject): String {
        val subjectName = input.getString("subject_name")
        val subject = findSubject(subjectName) ?: return "ERROR: no subject named '$subjectName'."
        val label = input.getString("label")
        val type = findAssessmentType(subject, label) ?: return "ERROR: no mark labeled '$label' on '${subject.name}'."
        VoidRepository.deleteAssessmentType(subject.id, type.id)
        return "Deleted mark '$label' from '${subject.name}'."
    }

    private fun analyzeSubject(input: JSONObject): String {
        val subjectName = input.getString("subject_name")
        val subject = findSubject(subjectName) ?: return "ERROR: no subject named '$subjectName'."
        val sb = StringBuilder()
        sb.append("SUBJECT: ${subject.name} (grade ${subject.grade}${if (subject.code.isNotBlank()) ", ${subject.code}" else ""})\n")
        sb.append("MARKS: ").append(
            subject.assessmentTypes.joinToString("; ") {
                val status = it.entry?.let { e -> "${e.score}/${it.maxScore}" } ?: "ungraded"
                "${it.label} [${it.kind}] weight=${it.weightPercent}% score=$status"
            }.ifBlank { "none" }
        )
        sb.append("\nWEIGHTED TOTAL: ${subject.weightedTotal()}/100 (total weight defined: ${subject.totalWeight()}%)")
        sb.append("\nFULLY GRADED: ${subject.isFullyGraded()}")
        sb.append("\nUNITS: ").append(
            VoidRepository.unitsFor(subject.id).joinToString("; ") { "Unit ${it.unitNumber}: ${it.name}" }.ifBlank { "none" }
        )
        return sb.toString()
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
