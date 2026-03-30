package sktech.schedulify.scheduling

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import java.time.DayOfWeek
import java.time.LocalDate
import sktech.schedulify.domain.parsePrompt
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.UUID
import sktech.schedulify.ai.AiPlanningService
import sktech.schedulify.ai.HybridAiPlanningService
import sktech.schedulify.data.FocusPreference
import sktech.schedulify.data.InMemoryScalableTaskRepository
import sktech.schedulify.data.InMemoryScheduleRepository
import sktech.schedulify.data.InMemoryUserProfileRepository
import sktech.schedulify.data.ScheduleBlockEntity
import sktech.schedulify.data.TaskEntity
import sktech.schedulify.notifications.NotificationPlan
import sktech.schedulify.notifications.NotificationScheduler
import sktech.schedulify.scheduler.DayScheduler
import sktech.schedulify.scheduler.MissedTask
import sktech.schedulify.scheduler.RecurrencePattern
import sktech.schedulify.scheduler.ScheduleTask
import sktech.schedulify.scheduler.TimeWindowConstraint
import sktech.schedulify.scheduler.UserPreferences

class ScheduleViewModel : ViewModel() {
    private val profileRepository = InMemoryUserProfileRepository()
    private val taskRepository = InMemoryScalableTaskRepository()
    private val scheduleRepository = InMemoryScheduleRepository()
    private val notificationScheduler = NotificationScheduler()
    private val aiPlanningService: AiPlanningService = HybridAiPlanningService()
    private val scheduler = DayScheduler()

    var prompt by mutableStateOf("")
        private set

    var state by mutableStateOf(ScheduleUiState())
        private set

    fun onPromptChange(newValue: String) {
        prompt = newValue
    }

    fun addTaskAndGenerate() {
        if (prompt.isBlank()) return

        val draft = parsePrompt(prompt)
        val taskId = UUID.randomUUID().toString()
        taskRepository.upsert(
            TaskEntity(
                id = taskId,
                title = draft.title,
                durationMinutes = draft.estimatedMinutes,
                deadline = draft.deadline?.atTime(23, 0),
                priority = draft.priority,
                category = draft.category
            )
        )
        prompt = ""

        val now = LocalDateTime.now()
        val profile = profileRepository.get()
        val dayStart = now.withHour(profile.wakeHour).withMinute(0)
        val dayEnd = now.withHour(profile.sleepHour).withMinute(0)

        val noWorkWindows = when (profile.focusPreference) {
            FocusPreference.MORNING -> listOf(TimeWindowConstraint(LocalTime.of(13, 0), LocalTime.of(14, 0)))
            FocusPreference.AFTERNOON -> listOf(TimeWindowConstraint(LocalTime.of(9, 0), LocalTime.of(10, 0)))
            FocusPreference.EVENING -> listOf(TimeWindowConstraint(LocalTime.of(15, 0), LocalTime.of(16, 0)))
        }

        val scheduledResult = scheduler.generate(
            tasks = taskRepository.all().map { entity ->
                ScheduleTask(
                    id = entity.id,
                    title = entity.title,
                    estimatedMinutes = entity.durationMinutes,
                    priority = entity.priority,
                    deadline = entity.deadline,
                    category = entity.category,
                    recurrence = if (entity.title.contains("daily", ignoreCase = true)) RecurrencePattern.DAILY else RecurrencePattern.NONE,
                    preferredStartHour = draft.preferredStartHour
                )
            },
            dayStart = dayStart,
            dayEnd = dayEnd,
            noWorkWindows = noWorkWindows
        )

        val weeklyResult = scheduler.generateWeek(
            tasks = taskRepository.all().map { entity ->
                ScheduleTask(
                    id = entity.id,
                    title = entity.title,
                    estimatedMinutes = entity.durationMinutes,
                    priority = entity.priority,
                    deadline = entity.deadline,
                    category = entity.category
                )
            },
            weekStart = now,
            preferences = UserPreferences(
                date = LocalDate.now(),
                wakeHour = profile.wakeHour,
                sleepHour = profile.sleepHour,
                noWorkWindows = noWorkWindows
            )
        )

        scheduleRepository.replaceForDate(
            now.toLocalDate(),
            scheduledResult.blocks.map {
                ScheduleBlockEntity(
                    id = UUID.randomUUID().toString(),
                    taskId = it.taskId,
                    title = it.title,
                    start = it.start,
                    end = it.end,
                    blockType = it.type
                )
            }
        )

        val suggestionWindowStart = dayEnd.plusMinutes(30)
        val suggestionWindowEnd = dayEnd.plusHours(3)
        val rescheduleOptions = scheduler.suggestRescheduleOptions(
            missedTasks = scheduledResult.unscheduled.mapNotNull { missed ->
                taskRepository.all().find { it.id == missed.taskId }?.let {
                    MissedTask(
                        task = ScheduleTask(
                            id = it.id,
                            title = it.title,
                            estimatedMinutes = it.durationMinutes,
                            priority = (it.priority + 1).coerceAtMost(5),
                            deadline = it.deadline,
                            category = it.category
                        ),
                        missedAt = now
                    )
                }
            },
            from = suggestionWindowStart,
            to = suggestionWindowEnd
        )

        val reminders: List<NotificationPlan> = notificationScheduler.planForBlocks(scheduledResult.blocks)
        val aiSummary = aiPlanningService.summarizeIntent(draft)

        state = ScheduleUiState(
            tasksCount = taskRepository.all().size,
            blocks = scheduledResult.blocks,
            weeklyBlocks = weeklyResult.byDay,
            unscheduledReasons = scheduledResult.unscheduled.map { "${it.title}: ${it.reason}" },
            aiSummary = aiSummary,
            parserQuestions = draft.questions,
            confidence = draft.confidence,
            reminders = reminders,
            rescheduleSuggestions = rescheduleOptions.map { "${it.title}: ${it.suggestedStart.toLocalTime()}-${it.suggestedEnd.toLocalTime()}" }
        )
    }
}

data class ScheduleUiState(
    val tasksCount: Int = 0,
    val blocks: List<sktech.schedulify.scheduler.ScheduledBlock> = emptyList(),
    val weeklyBlocks: Map<DayOfWeek, List<sktech.schedulify.scheduler.ScheduledBlock>> = emptyMap(),
    val unscheduledReasons: List<String> = emptyList(),
    val aiSummary: String = "",
    val parserQuestions: List<String> = emptyList(),
    val confidence: Double = 0.0,
    val reminders: List<NotificationPlan> = emptyList(),
    val rescheduleSuggestions: List<String> = emptyList()
)
