package sktech.schedulify.scheduling

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import sktech.schedulify.data.InMemoryTaskRepository
import sktech.schedulify.domain.parsePrompt
import sktech.schedulify.scheduler.DayScheduler
import sktech.schedulify.scheduler.ScheduleTask
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.UUID

class ScheduleViewModel : ViewModel() {
    private val taskRepository = InMemoryTaskRepository()
    private val scheduler = DayScheduler()

    var prompt by mutableStateOf("")
        private set

    var state by mutableStateOf(ScheduleUiState())
        private set

    fun onPromptChange(newValue: String) {
        prompt = newValue
    }

    fun addTaskAndGenerate() {
        val draft = parsePrompt(prompt)
        taskRepository.add(draft)
        prompt = ""

        val now = LocalDateTime.now()
        val scheduled = scheduler.generate(
            tasks = taskRepository.all().map {
                ScheduleTask(
                    id = UUID.randomUUID().toString(),
                    title = it.title,
                    estimatedMinutes = it.estimatedMinutes,
                    priority = it.priority,
                    deadline = it.deadline?.atTime(LocalTime.of(23, 0))
                )
            },
            dayStart = now.withHour(7).withMinute(0),
            dayEnd = now.withHour(23).withMinute(0)
        )

        state = ScheduleUiState(
            tasksCount = taskRepository.all().size,
            blocks = scheduled
        )
    }
}

data class ScheduleUiState(
    val tasksCount: Int = 0,
    val blocks: List<sktech.schedulify.scheduler.ScheduledBlock> = emptyList()
)
