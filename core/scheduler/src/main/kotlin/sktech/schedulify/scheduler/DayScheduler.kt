package sktech.schedulify.scheduler

import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import kotlin.math.max

class DayScheduler(
    private val maxBlockMinutes: Int = 90,
    private val breakMinutes: Int = 15
) {
    fun generate(
        tasks: List<ScheduleTask>,
        dayStart: LocalDateTime,
        dayEnd: LocalDateTime,
        noWorkWindows: List<TimeWindowConstraint> = emptyList()
    ): ScheduleResult {
        if (dayEnd <= dayStart) return ScheduleResult(emptyList(), emptyList())

        val sorted = tasks.sortedByDescending { scoreTask(it, dayStart) }
        val unscheduled = mutableListOf<UnscheduledTask>()
        var cursor = dayStart
        var focusStreakMinutes = 0
        val blocks = mutableListOf<ScheduledBlock>()

        sorted.forEach { task ->
            if (task.fixedStart != null && task.fixedEnd != null) {
                if (task.fixedStart >= dayStart &&
                    task.fixedEnd <= dayEnd &&
                    !overlapsNoWorkWindow(task.fixedStart, task.fixedEnd, noWorkWindows)
                ) {
                    blocks.add(
                        ScheduledBlock(
                            taskId = task.id,
                            title = task.title,
                            start = task.fixedStart,
                            end = task.fixedEnd,
                            priority = task.priority,
                            category = task.category
                        )
                    )
                } else {
                    unscheduled.add(UnscheduledTask(task.id, task.title, "Fixed slot is out of available bounds"))
                }
                return@forEach
            }

            var remaining = task.estimatedMinutes.coerceAtLeast(15)
            var scheduledAnyChunk = false
            while (remaining > 0 && cursor < dayEnd) {
                cursor = skipNoWorkWindow(cursor, noWorkWindows)
                if (cursor >= dayEnd) break

                task.preferredStartHour?.let { preferred ->
                    if (cursor.hour < preferred) {
                        cursor = cursor.withHour(preferred).withMinute(0)
                    }
                }

                val chunk = minOf(maxBlockMinutes, remaining)
                val end = cursor.plusMinutes(chunk.toLong())
                val violatesDeadline = task.deadline != null && end > task.deadline
                val overlapsNoWork = overlapsNoWorkWindow(cursor, end, noWorkWindows)
                if (end > dayEnd || overlapsNoWork || violatesDeadline) {
                    if (end > dayEnd) break
                    cursor = cursor.plusMinutes(15)
                    continue
                }

                blocks.add(
                    ScheduledBlock(
                        taskId = task.id,
                        title = task.title,
                        start = cursor,
                        end = end,
                        priority = task.priority,
                        category = task.category
                    )
                )
                cursor = end
                remaining -= chunk
                scheduledAnyChunk = true
                focusStreakMinutes += chunk

                if (focusStreakMinutes >= 90) {
                    val breakEnd = cursor.plusMinutes(breakMinutes.toLong())
                    if (breakEnd <= dayEnd && !overlapsNoWorkWindow(cursor, breakEnd, noWorkWindows)) {
                        blocks.add(
                            ScheduledBlock(
                                taskId = "break-${task.id}-${cursor.toLocalTime()}",
                                title = "Break",
                                start = cursor,
                                end = breakEnd,
                                priority = 0,
                                category = TaskCategory.ROUTINE,
                                type = BlockType.BREAK
                            )
                        )
                        cursor = breakEnd
                    }
                    focusStreakMinutes = 0
                }
            }

            if (!scheduledAnyChunk) {
                unscheduled.add(UnscheduledTask(task.id, task.title, "No feasible slot available"))
            } else if (remaining > 0) {
                unscheduled.add(UnscheduledTask(task.id, task.title, "Insufficient time remaining in the day"))
            }
        }

        return ScheduleResult(
            blocks = blocks.sortedBy { it.start },
            unscheduled = unscheduled
        )
    }

    fun generateWeek(
        tasks: List<ScheduleTask>,
        weekStart: LocalDateTime,
        preferences: UserPreferences = UserPreferences()
    ): WeeklyScheduleResult {
        val byDay = linkedMapOf<java.time.DayOfWeek, List<ScheduledBlock>>()
        val unscheduled = mutableListOf<UnscheduledTask>()
        val wakeHour = preferences.wakeHour.coerceIn(0, 23)
        val sleepHour = max(preferences.sleepHour.coerceIn(1, 24), wakeHour + 1)

        (0..6).forEach { dayOffset ->
            val dayDate = weekStart.toLocalDate().plusDays(dayOffset.toLong())
            val dayStart = dayDate.atTime(LocalTime.of(wakeHour, 0))
            val dayEnd = dayDate.atTime(LocalTime.of(sleepHour % 24, 0)).let {
                if (sleepHour == 24) dayDate.plusDays(1).atStartOfDay() else it
            }

            val tasksForDay = tasks.filter { task ->
                when (task.recurrence) {
                    RecurrencePattern.DAILY -> true
                    RecurrencePattern.WEEKLY -> task.deadline?.toLocalDate()?.dayOfWeek == dayDate.dayOfWeek
                    RecurrencePattern.NONE -> task.deadline?.toLocalDate() == dayDate || task.deadline == null
                }
            }

            val result = generate(tasksForDay, dayStart, dayEnd, preferences.noWorkWindows)
            byDay[dayDate.dayOfWeek] = result.blocks
            unscheduled += result.unscheduled
        }

        return WeeklyScheduleResult(byDay = byDay, unscheduled = unscheduled)
    }

    fun suggestRescheduleOptions(
        missedTasks: List<MissedTask>,
        from: LocalDateTime,
        to: LocalDateTime
    ): List<RescheduleOption> {
        if (to <= from) return emptyList()
        val options = mutableListOf<RescheduleOption>()
        var cursor = from
        missedTasks.sortedByDescending { it.task.priority }.forEach { missed ->
            val duration = missed.task.estimatedMinutes.coerceAtLeast(15).toLong()
            val end = cursor.plusMinutes(duration)
            if (end <= to) {
                options += RescheduleOption(
                    taskId = missed.task.id,
                    title = missed.task.title,
                    suggestedStart = cursor,
                    suggestedEnd = end
                )
                cursor = end.plusMinutes(15)
            }
        }
        return options
    }

    private fun scoreTask(task: ScheduleTask, referenceTime: LocalDateTime): Int {
        val deadlineUrgency = task.deadline?.let {
            val hours = Duration.between(referenceTime, it).toHours()
            when {
                hours <= 0 -> 40
                hours <= 24 -> 25
                hours <= 72 -> 12
                else -> 4
            }
        } ?: 0

        val categoryWeight = when (task.category) {
            TaskCategory.STUDY -> 6
            TaskCategory.WORK -> 6
            TaskCategory.FITNESS, TaskCategory.HEALTH -> 4
            TaskCategory.PERSONAL -> 3
            TaskCategory.ROUTINE -> 2
            TaskCategory.OTHER -> 1
        }

        return task.priority * 10 + deadlineUrgency + categoryWeight
    }

    private fun overlapsNoWorkWindow(
        start: LocalDateTime,
        end: LocalDateTime,
        windows: List<TimeWindowConstraint>
    ): Boolean {
        return windows.any { window ->
            val windowStart = start.toLocalDate().atTime(window.start)
            val windowEnd = start.toLocalDate().atTime(window.end)
            start < windowEnd && end > windowStart
        }
    }

    private fun skipNoWorkWindow(
        time: LocalDateTime,
        windows: List<TimeWindowConstraint>
    ): LocalDateTime {
        var cursor = time
        var moved: Boolean
        do {
            moved = false
            windows.forEach { window ->
                val windowStart = cursor.toLocalDate().atTime(window.start)
                val windowEnd = cursor.toLocalDate().atTime(window.end)
                if (!cursor.isBefore(windowStart) && cursor.isBefore(windowEnd)) {
                    cursor = windowEnd
                    moved = true
                }
            }
        } while (moved)
        return cursor
    }
}
