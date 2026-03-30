package sktech.schedulify.data

import java.time.LocalDateTime
import sktech.schedulify.scheduler.BlockType
import sktech.schedulify.scheduler.TaskCategory

data class UserProfile(
    val wakeHour: Int = 7,
    val sleepHour: Int = 23,
    val focusPreference: FocusPreference = FocusPreference.MORNING,
    val timezone: String = "UTC"
)

enum class FocusPreference {
    MORNING,
    AFTERNOON,
    EVENING
}

data class TaskEntity(
    val id: String,
    val title: String,
    val durationMinutes: Int,
    val deadline: LocalDateTime?,
    val priority: Int,
    val category: TaskCategory,
    val recurrence: sktech.schedulify.scheduler.RecurrencePattern
)

data class ScheduleBlockEntity(
    val id: String,
    val taskId: String,
    val title: String,
    val start: LocalDateTime,
    val end: LocalDateTime,
    val blockType: BlockType
)
