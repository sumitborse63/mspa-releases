package sktech.schedulify.data

import sktech.schedulify.domain.TaskDraft

class InMemoryTaskRepository {
    private val tasks = mutableListOf<TaskDraft>()

    fun add(task: TaskDraft) {
        tasks.add(task)
    }

    fun all(): List<TaskDraft> = tasks.toList()
}
