package sktech.schedulify.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import sktech.schedulify.scheduling.ScheduleViewModel
import sktech.schedulify.scheduler.ScheduledBlock
import java.time.format.DateTimeFormatter

@Composable
fun SchedulifyApp(viewModel: ScheduleViewModel = remember { ScheduleViewModel() }) {
    val state = viewModel.state
    var showWeekly by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Schedulify • AI-first planner", style = MaterialTheme.typography.headlineSmall)

        OutlinedTextField(
            value = viewModel.prompt,
            onValueChange = viewModel::onPromptChange,
            label = { Text("Enter goals/tasks (study, work, personal, fitness...)") },
            modifier = Modifier.fillMaxWidth()
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Button(onClick = viewModel::addTaskAndGenerate, modifier = Modifier.weight(1f)) {
                Text("Generate AI Plan")
            }
            Button(onClick = { showWeekly = !showWeekly }, modifier = Modifier.weight(1f)) {
                Text(if (showWeekly) "Daily View" else "Weekly View")
            }
        }

        Text(
            "Tasks: ${state.tasksCount} • Confidence: ${(state.confidence * 100).toInt()}%",
            style = MaterialTheme.typography.titleMedium
        )

        if (state.aiSummary.isNotBlank()) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("AI Interpretation", style = MaterialTheme.typography.titleSmall)
                    Text(state.aiSummary)
                }
            }
        }

        if (state.parserQuestions.isNotEmpty()) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Clarifications", style = MaterialTheme.typography.titleSmall)
                    state.parserQuestions.forEach { Text("• $it") }
                }
            }
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (showWeekly) {
                state.weeklyBlocks.forEach { (day, blocks) ->
                    item {
                        Text(day.name.lowercase().replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.titleMedium)
                    }
                    items(blocks) { block -> BlockCard(block) }
                }
            } else {
                items(state.blocks) { block ->
                    BlockCard(block)
                }
            }

            if (state.unscheduledReasons.isNotEmpty()) {
                item { Text("Unscheduled", style = MaterialTheme.typography.titleMedium) }
                items(state.unscheduledReasons) { reason ->
                    Text("• $reason")
                }
            }

            if (state.rescheduleSuggestions.isNotEmpty()) {
                item { Text("Smart Reschedule", style = MaterialTheme.typography.titleMedium) }
                items(state.rescheduleSuggestions) { suggestion ->
                    Text("• $suggestion")
                }
            }

            if (state.reminders.isNotEmpty()) {
                item { Text("Reminder Preview", style = MaterialTheme.typography.titleMedium) }
                items(state.reminders) { reminder ->
                    Text("• ${reminder.title}")
                }
            }
        }
    }
}

@Composable
private fun BlockCard(block: ScheduledBlock) {
    val formatter = DateTimeFormatter.ofPattern("HH:mm")
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("${block.title} (${block.category.name.lowercase()})", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("${block.start.format(formatter)} - ${block.end.format(formatter)}")
                Text("${block.durationMinutes} min")
            }
        }
    }
}
