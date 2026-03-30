package sktech.schedulify.scheduler

import java.time.LocalDateTime

class DayScheduler(
    private val maxBlockMinutes: Int = 90
) {
    fun generate(
        tasks: List<ScheduleTask>,
        dayStart: LocalDateTime,
        dayEnd: LocalDateTime
    ): List<ScheduledBlock> {
        if (dayEnd <= dayStart) return emptyList()

        val sorted = tasks.sortedWith(
            compareBy<ScheduleTask> { it.deadline ?: LocalDateTime.MAX }
                .thenByDescending { it.priority }
                .thenBy { it.title }
        )

        var cursor = dayStart
        val blocks = mutableListOf<ScheduledBlock>()

        sorted.forEach { task ->
            var remaining = task.estimatedMinutes.coerceAtLeast(15)
            while (remaining > 0 && cursor < dayEnd) {
                val chunk = minOf(maxBlockMinutes, remaining)
                val end = cursor.plusMinutes(chunk.toLong())
                if (end > dayEnd) {
                    return@forEach
                }

                blocks.add(
                    ScheduledBlock(
                        taskId = task.id,
                        title = task.title,
                        start = cursor,
                        end = end,
                        priority = task.priority
                    )
                )
                cursor = end
                remaining -= chunk
            }
        }

        return blocks
    }
}

data class ScheduleTask(
    val id: String,
    val title: String,
    val estimatedMinutes: Int,
    val priority: Int,
    val deadline: LocalDateTime? = null
)

data class ScheduledBlock(
    val taskId: String,
    val title: String,
    val start: LocalDateTime,
    val end: LocalDateTime,
    val priority: Int
) {
    val durationMinutes: Long
        get() = java.time.Duration.between(start, end).toMinutes()
}
