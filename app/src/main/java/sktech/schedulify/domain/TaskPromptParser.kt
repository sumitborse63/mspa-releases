package sktech.schedulify.domain

import java.time.DayOfWeek
import java.time.LocalDate
import sktech.schedulify.scheduler.RecurrencePattern
import sktech.schedulify.scheduler.TaskCategory

private val DURATION_REGEX = Regex("""for\s+(\d+)\s*(hours?|hrs?|minutes?|mins?)""", RegexOption.IGNORE_CASE)
private val DEADLINE_REGEX = Regex("""before\s+(monday|tuesday|wednesday|thursday|friday|saturday|sunday)""", RegexOption.IGNORE_CASE)
private val IN_DAYS_REGEX = Regex("""in\s+(\d+)\s+days?""", RegexOption.IGNORE_CASE)

fun parsePrompt(prompt: String): ParsedTaskIntent {
    val trimmed = prompt.trim().ifBlank { "Untitled task" }
    val durationMatch = DURATION_REGEX.find(trimmed)
    val amount = durationMatch?.groupValues?.get(1)?.toIntOrNull() ?: 1
    val unit = durationMatch?.groupValues?.get(2)?.lowercase() ?: "hour"
    val durationMinutes = if (unit.startsWith("min")) amount else amount * 60

    val title = trimmed
        .replace(DURATION_REGEX, "")
        .replace(DEADLINE_REGEX, "")
        .replace(IN_DAYS_REGEX, "")
        .replace(Regex("""daily|every day|weekly|urgent|high priority|morning|afternoon|evening""", RegexOption.IGNORE_CASE), "")
        .replace(Regex("""\s+"""), " ")
        .trim()
        .ifBlank { "Planned task" }

    val deadline = parseDeadline(trimmed)
    val category = inferCategory(trimmed)
    val recurrence = inferRecurrence(trimmed)
    val preferredStartHour = inferPreferredStartHour(trimmed)
    val priority = inferPriority(trimmed)
    val ambiguities = mutableListOf<String>()
    if (!trimmed.contains(DURATION_REGEX)) ambiguities += "Duration not explicit"
    if (deadline == null) ambiguities += "Deadline not specified"

    return ParsedTaskIntent(
        title = title,
        estimatedMinutes = durationMinutes.coerceAtLeast(15),
        deadline = deadline,
        priority = priority,
        category = category,
        recurrence = recurrence,
        preferredStartHour = preferredStartHour,
        confidence = confidenceScore(trimmed, deadline, ambiguities),
        questions = buildQuestions(ambiguities, title)
    )
}

private fun parseDeadline(prompt: String): LocalDate? {
    DEADLINE_REGEX.find(prompt)?.groupValues?.get(1)?.let { weekday ->
        return nextDayOfWeek(weekday)
    }
    IN_DAYS_REGEX.find(prompt)?.groupValues?.get(1)?.toIntOrNull()?.let { days ->
        return LocalDate.now().plusDays(days.toLong())
    }
    return null
}

private fun inferCategory(prompt: String): TaskCategory {
    val normalized = prompt.lowercase()
    return when {
        normalized.contains("study") || normalized.contains("exam") || normalized.contains("revision") -> TaskCategory.STUDY
        normalized.contains("work") || normalized.contains("meeting") || normalized.contains("project") || normalized.contains("coding") -> TaskCategory.WORK
        normalized.contains("gym") || normalized.contains("workout") || normalized.contains("run") -> TaskCategory.FITNESS
        normalized.contains("health") || normalized.contains("meditation") || normalized.contains("walk") -> TaskCategory.HEALTH
        normalized.contains("family") || normalized.contains("personal") || normalized.contains("home") -> TaskCategory.PERSONAL
        else -> TaskCategory.OTHER
    }
}

private fun inferRecurrence(prompt: String): RecurrencePattern {
    val normalized = prompt.lowercase()
    return when {
        normalized.contains("daily") || normalized.contains("every day") -> RecurrencePattern.DAILY
        normalized.contains("weekly") -> RecurrencePattern.WEEKLY
        else -> RecurrencePattern.NONE
    }
}

private fun inferPreferredStartHour(prompt: String): Int? {
    val normalized = prompt.lowercase()
    return when {
        normalized.contains("morning") -> 7
        normalized.contains("afternoon") -> 13
        normalized.contains("evening") -> 18
        else -> null
    }
}

private fun inferPriority(prompt: String): Int {
    val normalized = prompt.lowercase()
    return when {
        "urgent" in normalized || "asap" in normalized -> 5
        "high priority" in normalized || "important" in normalized -> 4
        else -> 3
    }
}

private fun confidenceScore(
    prompt: String,
    deadline: LocalDate?,
    ambiguities: List<String>
): Double {
    var score = 0.55
    if (DURATION_REGEX.containsMatchIn(prompt)) score += 0.2
    if (deadline != null) score += 0.15
    if (inferCategory(prompt) != TaskCategory.OTHER) score += 0.1
    if (ambiguities.isNotEmpty()) score -= ambiguities.size * 0.05
    return score.coerceIn(0.2, 0.98)
}

private fun buildQuestions(ambiguities: List<String>, title: String): List<String> {
    val questions = mutableListOf<String>()
    if (ambiguities.any { it.contains("Duration") }) {
        questions += "How long should \"$title\" take?"
    }
    if (ambiguities.any { it.contains("Deadline") }) {
        questions += "When is the deadline for \"$title\"?"
    }
    return questions
}

private fun nextDayOfWeek(weekday: String): LocalDate {
    val target = DayOfWeek.valueOf(weekday.uppercase())
    var date = LocalDate.now()
    repeat(7) {
        date = date.plusDays(1)
        if (date.dayOfWeek == target) return date
    }
    return date
}

data class ParsedTaskIntent(
    val title: String,
    val estimatedMinutes: Int,
    val deadline: LocalDate?,
    val priority: Int,
    val category: TaskCategory,
    val recurrence: RecurrencePattern,
    val preferredStartHour: Int?,
    val confidence: Double,
    val questions: List<String>
)
