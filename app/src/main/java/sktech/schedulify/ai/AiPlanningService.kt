package sktech.schedulify.ai

import sktech.schedulify.domain.ParsedTaskIntent

interface AiPlanningService {
    fun summarizeIntent(intent: ParsedTaskIntent): String
    fun buildPromptForWeeklyPlan(intents: List<ParsedTaskIntent>, constraints: String): String
}

class HybridAiPlanningService : AiPlanningService {
    override fun summarizeIntent(intent: ParsedTaskIntent): String {
        val deadline = intent.deadline?.toString() ?: "no deadline"
        return "${intent.title} (${intent.category.name.lowercase()}, ${intent.estimatedMinutes}m, $deadline, confidence=${"%.2f".format(intent.confidence)})"
    }

    override fun buildPromptForWeeklyPlan(intents: List<ParsedTaskIntent>, constraints: String): String {
        val tasks = intents.joinToString(separator = "\n") {
            "- ${it.title} (${it.estimatedMinutes} min, priority ${it.priority}, ${it.category.name.lowercase()})"
        }
        return """
            Generate a structured weekly schedule JSON.
            Tasks:
            $tasks
            Constraints:
            $constraints
            Return only JSON grouped by day of week.
        """.trimIndent()
    }
}
