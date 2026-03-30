package sktech.schedulify.scheduler

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalTime
import java.time.LocalDateTime

class DaySchedulerTest {

    @Test
    fun prioritizesEarlierDeadline() {
        val now = LocalDateTime.of(2026, 3, 29, 7, 0)
        val scheduler = DayScheduler(maxBlockMinutes = 60)

        val result = scheduler.generate(
            tasks = listOf(
                ScheduleTask("2", "Later", 60, priority = 5, deadline = now.plusDays(2)),
                ScheduleTask("1", "Soon", 60, priority = 1, deadline = now.plusDays(1))
            ),
            dayStart = now,
            dayEnd = now.plusHours(5)
        )

        val blocks = result.blocks.filter { it.type == BlockType.FOCUS }
        assertTrue(blocks.isNotEmpty())
        assertEquals("Soon", blocks.first().title)
    }

    @Test
    fun respectsDayCapacity() {
        val now = LocalDateTime.of(2026, 3, 29, 7, 0)
        val scheduler = DayScheduler(maxBlockMinutes = 90)

        val result = scheduler.generate(
            tasks = listOf(ScheduleTask("1", "Big task", 600, priority = 3)),
            dayStart = now,
            dayEnd = now.plusHours(4)
        )

        val blocks = result.blocks.filter { it.type == BlockType.FOCUS }
        val total = blocks.sumOf { it.durationMinutes }
        assertEquals(240, total)
    }

    @Test
    fun insertsBreakAfterNinetyMinutesOfFocus() {
        val now = LocalDateTime.of(2026, 3, 29, 7, 0)
        val scheduler = DayScheduler(maxBlockMinutes = 90, breakMinutes = 15)

        val result = scheduler.generate(
            tasks = listOf(ScheduleTask("1", "Deep Work", 180, priority = 5, category = TaskCategory.WORK)),
            dayStart = now,
            dayEnd = now.plusHours(5)
        )

        assertTrue(result.blocks.any { it.type == BlockType.BREAK })
    }

    @Test
    fun respectsNoWorkWindow() {
        val now = LocalDateTime.of(2026, 3, 29, 7, 0)
        val scheduler = DayScheduler(maxBlockMinutes = 60)

        val result = scheduler.generate(
            tasks = listOf(ScheduleTask("1", "Work Task", 120, priority = 4)),
            dayStart = now,
            dayEnd = now.plusHours(4),
            noWorkWindows = listOf(TimeWindowConstraint(LocalTime.of(8, 0), LocalTime.of(9, 0)))
        )

        assertTrue(result.blocks.none { it.start.toLocalTime() < LocalTime.of(9, 0) && it.end.toLocalTime() > LocalTime.of(8, 0) })
    }
}
