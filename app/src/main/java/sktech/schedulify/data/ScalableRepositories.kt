package sktech.schedulify.data

import java.time.LocalDate

interface UserProfileRepository {
    fun get(): UserProfile
    fun save(profile: UserProfile)
}

interface TaskRepository {
    fun upsert(task: TaskEntity)
    fun all(): List<TaskEntity>
}

interface ScheduleRepository {
    fun replaceForDate(date: LocalDate, blocks: List<ScheduleBlockEntity>)
    fun forDate(date: LocalDate): List<ScheduleBlockEntity>
}

class InMemoryUserProfileRepository : UserProfileRepository {
    private var profile = UserProfile()
    override fun get(): UserProfile = profile
    override fun save(profile: UserProfile) {
        this.profile = profile
    }
}

class InMemoryScalableTaskRepository : TaskRepository {
    private val tasks = linkedMapOf<String, TaskEntity>()
    override fun upsert(task: TaskEntity) {
        tasks[task.id] = task
    }

    override fun all(): List<TaskEntity> = tasks.values.toList()
}

class InMemoryScheduleRepository : ScheduleRepository {
    private val blocksByDate = linkedMapOf<LocalDate, MutableList<ScheduleBlockEntity>>()

    override fun replaceForDate(date: LocalDate, blocks: List<ScheduleBlockEntity>) {
        blocksByDate[date] = blocks.toMutableList()
    }

    override fun forDate(date: LocalDate): List<ScheduleBlockEntity> = blocksByDate[date]?.toList().orEmpty()
}
