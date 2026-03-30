package sktech.schedulify.notifications

import sktech.schedulify.scheduler.ScheduledBlock

data class NotificationPlan(
    val title: String,
    val body: String,
    val triggerEpochMillis: Long
)

class NotificationScheduler {
    fun planForBlocks(blocks: List<ScheduledBlock>): List<NotificationPlan> {
        return blocks
            .filter { it.type == sktech.schedulify.scheduler.BlockType.FOCUS }
            .map { block ->
                NotificationPlan(
                    title = "Upcoming: ${block.title}",
                    body = "Starts at ${block.start.toLocalTime()}",
                    triggerEpochMillis = block.start.minusMinutes(10).atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
                )
            }
    }
}
