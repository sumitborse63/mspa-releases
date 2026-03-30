package sktech.schedulify.domain

import java.time.DayOfWeek
import java.time.LocalDate

private val DURATION_REGEX = Regex("""for\s+(\d+)\s*(hours?|hrs?|minutes?|mins?)""", RegexOption.IGNORE_CASE)
private val DEADLINE_REGEX = Regex("""before\s+(monday|tuesday|wednesday|thursday|friday|saturday|sunday)""", RegexOption.IGNORE_CASE)

fun parsePrompt(prompt: String): TaskDraft {
    val trimmed = prompt.trim().ifBlank { "Untitled task" }
    val durationMatch = DURATION_REGEX.find(trimmed)
    val amount = durationMatch?.groupValues?.get(1)?.toIntOrNull() ?: 1
    val unit = durationMatch?.groupValues?.get(2)?.lowercase() ?: "hour"
    val durationMinutes = if (unit.startsWith("min")) amount else amount * 60

    val title = trimmed
        .replace(DURATION_REGEX, "")
        .replace(DEADLINE_REGEX, "")
        .replace(Regex("""\s+"""), " ")
        .trim()
        .ifBlank { "Planned task" }

    val deadline = DEADLINE_REGEX.find(trimmed)?.groupValues?.get(1)?.let { weekday ->
        nextDayOfWeek(weekday)
    }

    return TaskDraft(
        title = title,
        estimatedMinutes = durationMinutes.coerceAtLeast(15),
        deadline = deadline,
        priority = if (trimmed.contains("urgent", ignoreCase = true)) 5 else 3
    )
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

data class TaskDraft(
    val title: String,
    val estimatedMinutes: Int,
    val deadline: LocalDate?,
    val priority: Int
)
