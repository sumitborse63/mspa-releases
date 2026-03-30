package sktech.schedulify.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import sktech.schedulify.scheduler.RecurrencePattern
import sktech.schedulify.scheduler.TaskCategory

class TaskPromptParserTest {

    @Test
    fun parsesHoursAndDeadline() {
        val draft = parsePrompt("Study DBMS for 8 hours before friday")

        assertEquals("Study DBMS", draft.title)
        assertEquals(480, draft.estimatedMinutes)
        assertTrue(draft.deadline != null)
        assertEquals(TaskCategory.STUDY, draft.category)
    }

    @Test
    fun defaultsToOneHourWhenNoDurationProvided() {
        val draft = parsePrompt("Prepare for interview")

        assertEquals(60, draft.estimatedMinutes)
        assertEquals("Prepare for interview", draft.title)
        assertFalse(draft.questions.isEmpty())
    }

    @Test
    fun parsesFitnessRecurrenceAndTimePreference() {
        val draft = parsePrompt("Gym daily for 1 hour in the evening")

        assertEquals(TaskCategory.FITNESS, draft.category)
        assertEquals(RecurrencePattern.DAILY, draft.recurrence)
        assertEquals(18, draft.preferredStartHour)
        assertTrue(draft.confidence > 0.7)
    }
}
