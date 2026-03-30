package sktech.schedulify.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TaskPromptParserTest {

    @Test
    fun parsesHoursAndDeadline() {
        val draft = parsePrompt("Study DBMS for 8 hours before friday")

        assertEquals("Study DBMS", draft.title)
        assertEquals(480, draft.estimatedMinutes)
        assertTrue(draft.deadline != null)
    }

    @Test
    fun defaultsToOneHourWhenNoDurationProvided() {
        val draft = parsePrompt("Prepare for interview")

        assertEquals(60, draft.estimatedMinutes)
        assertEquals("Prepare for interview", draft.title)
    }
}
