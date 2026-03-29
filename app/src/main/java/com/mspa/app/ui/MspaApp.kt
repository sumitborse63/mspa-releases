package com.mspa.app.ui

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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mspa.app.scheduling.ScheduleViewModel
import com.mspa.scheduler.ScheduledBlock
import java.time.format.DateTimeFormatter

@Composable
fun MspaApp(viewModel: ScheduleViewModel = remember { ScheduleViewModel() }) {
    val state = viewModel.state

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("AI Schedule Generator", style = MaterialTheme.typography.headlineSmall)

        OutlinedTextField(
            value = viewModel.prompt,
            onValueChange = viewModel::onPromptChange,
            label = { Text("Enter task prompt") },
            modifier = Modifier.fillMaxWidth()
        )

        Button(onClick = viewModel::addTaskAndGenerate, modifier = Modifier.fillMaxWidth()) {
            Text("Add & Generate Day Plan")
        }

        Text("Tasks: ${state.tasksCount}", style = MaterialTheme.typography.titleMedium)

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(state.blocks) { block ->
                BlockCard(block)
            }
        }
    }
}

@Composable
private fun BlockCard(block: ScheduledBlock) {
    val formatter = DateTimeFormatter.ofPattern("HH:mm")
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(block.title, style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("${block.start.format(formatter)} - ${block.end.format(formatter)}")
                Text("${block.durationMinutes} min")
            }
        }
    }
}
