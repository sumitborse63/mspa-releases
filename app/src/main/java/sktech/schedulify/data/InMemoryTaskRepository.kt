package sktech.schedulify.data

import sktech.schedulify.domain.ParsedTaskIntent

class InMemoryTaskRepository {
    private val tasks = mutableListOf<ParsedTaskIntent>()

    fun add(task: ParsedTaskIntent) {
        tasks.add(task)
    }

    fun all(): List<ParsedTaskIntent> = tasks.toList()
}
