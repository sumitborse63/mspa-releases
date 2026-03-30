package sktech.schedulify.scheduler

import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

enum class TaskCategory {
    STUDY,
    WORK,
    PERSONAL,
    FITNESS,
    HEALTH,
    ROUTINE,
    OTHER
}

enum class RecurrencePattern {
    NONE,
    DAILY,
    WEEKLY
}

enum class BlockType {
    FOCUS,
    BREAK
}

data class TimeWindowConstraint(
    val start: LocalTime,
    val end: LocalTime
)

data class ScheduleTask(
    val id: String,
    val title: String,
    val estimatedMinutes: Int,
    val priority: Int,
    val deadline: LocalDateTime? = null,
    val category: TaskCategory = TaskCategory.OTHER,
    val recurrence: RecurrencePattern = RecurrencePattern.NONE,
    val preferredStartHour: Int? = null,
    val fixedStart: LocalDateTime? = null,
    val fixedEnd: LocalDateTime? = null
)

data class ScheduledBlock(
    val taskId: String,
    val title: String,
    val start: LocalDateTime,
    val end: LocalDateTime,
    val priority: Int,
    val category: TaskCategory = TaskCategory.OTHER,
    val type: BlockType = BlockType.FOCUS
) {
    val durationMinutes: Long
        get() = Duration.between(start, end).toMinutes()
}

data class UnscheduledTask(
    val taskId: String,
    val title: String,
    val reason: String
)

data class ScheduleResult(
    val blocks: List<ScheduledBlock>,
    val unscheduled: List<UnscheduledTask>
)

data class WeeklyScheduleResult(
    val byDay: Map<DayOfWeek, List<ScheduledBlock>>,
    val unscheduled: List<UnscheduledTask>
)

data class MissedTask(
    val task: ScheduleTask,
    val missedAt: LocalDateTime
)

data class RescheduleOption(
    val taskId: String,
    val title: String,
    val suggestedStart: LocalDateTime,
    val suggestedEnd: LocalDateTime
)

data class UserPreferences(
    val date: LocalDate = LocalDate.now(),
    val wakeHour: Int = 7,
    val sleepHour: Int = 23,
    val noWorkWindows: List<TimeWindowConstraint> = emptyList()
)
