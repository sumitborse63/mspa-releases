package com.mspa.scheduler

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

class DaySchedulerTest {

    @Test
    fun prioritizesEarlierDeadline() {
        val now = LocalDateTime.of(2026, 3, 29, 7, 0)
        val scheduler = DayScheduler(maxBlockMinutes = 60)

        val blocks = scheduler.generate(
            tasks = listOf(
                ScheduleTask("2", "Later", 60, priority = 5, deadline = now.plusDays(2)),
                ScheduleTask("1", "Soon", 60, priority = 1, deadline = now.plusDays(1))
            ),
            dayStart = now,
            dayEnd = now.plusHours(5)
        )

        assertTrue(blocks.isNotEmpty())
        assertEquals("Soon", blocks.first().title)
    }

    @Test
    fun respectsDayCapacity() {
        val now = LocalDateTime.of(2026, 3, 29, 7, 0)
        val scheduler = DayScheduler(maxBlockMinutes = 90)

        val blocks = scheduler.generate(
            tasks = listOf(ScheduleTask("1", "Big task", 600, priority = 3)),
            dayStart = now,
            dayEnd = now.plusHours(4)
        )

        val total = blocks.sumOf { it.durationMinutes }
        assertEquals(240, total)
    }
}
