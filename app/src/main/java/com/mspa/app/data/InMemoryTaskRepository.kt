package com.mspa.app.data

import com.mspa.app.domain.TaskDraft

class InMemoryTaskRepository {
    private val tasks = mutableListOf<TaskDraft>()

    fun add(task: TaskDraft) {
        tasks.add(task)
    }

    fun all(): List<TaskDraft> = tasks.toList()
}
