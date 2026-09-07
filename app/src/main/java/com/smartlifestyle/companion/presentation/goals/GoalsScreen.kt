package com.smartlifestyle.companion.presentation.goals

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.smartlifestyle.companion.domain.model.Goal

@Composable
fun GoalsScreen(viewModel: GoalsViewModel) {
    val state by viewModel.uiState.collectAsState()
    var showAddGoalDialog by remember { mutableStateOf(false) }
    var showAddHabitDialog by remember { mutableStateOf(false) }
    val scheme = MaterialTheme.colorScheme

    Scaffold(
        floatingActionButton = {
            Row {
                FloatingActionButton(onClick = { showAddHabitDialog = true }) {
                    Icon(Icons.Filled.LocalFireDepartment, contentDescription = "Add habit")
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Text("Goals", style = MaterialTheme.typography.headlineMedium) }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Daily goals", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    TextButton(onClick = { showAddGoalDialog = true }) {
                        Icon(Icons.Filled.Add, contentDescription = null)
                        Text(" New goal")
                    }
                }
            }

            if (state.goals.isEmpty()) {
                item { Text("No goals yet - add one to start tracking.", color = scheme.outline) }
            } else {
                state.goals.forEach { goal ->
                    item(key = "goal_${goal.id}") {
                        GoalCard(
                            goal = goal,
                            onIncrement = { viewModel.updateGoalProgress(goal, goal.currentValue + 1) },
                            onDelete = { viewModel.deleteGoal(goal) }
                        )
                    }
                }
            }

            item {
                Text(
                    "Habits & streaks",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            if (state.habits.isEmpty()) {
                item { Text("No habits yet - tap the flame button to add one.", color = scheme.outline) }
            } else {
                state.habits.forEach { habitWithStreak ->
                    item(key = "habit_${habitWithStreak.habit.id}") {
                        HabitCard(
                            item = habitWithStreak,
                            onToggle = { viewModel.toggleHabitToday(habitWithStreak.habit) },
                            onDelete = { viewModel.deleteHabit(habitWithStreak.habit) }
                        )
                    }
                }
            }
        }
    }

    if (showAddGoalDialog) {
        AddGoalDialog(
            onDismiss = { showAddGoalDialog = false },
            onConfirm = { title, target, unit ->
                viewModel.addGoal(title, target, unit)
                showAddGoalDialog = false
            }
        )
    }

    if (showAddHabitDialog) {
        AddHabitDialog(
            onDismiss = { showAddHabitDialog = false },
            onConfirm = { title ->
                viewModel.addHabit(title)
                showAddHabitDialog = false
            }
        )
    }
}

@Composable
private fun GoalCard(goal: Goal, onIncrement: () -> Unit, onDelete: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(goal.title, style = MaterialTheme.typography.titleMedium)
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete goal", tint = scheme.outline)
                }
            }
            Text(
                "${goal.currentValue.toInt()} / ${goal.targetValue.toInt()} ${goal.unit}",
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.outline
            )
            LinearProgressIndicator(
                progress = goal.progressFraction,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 8.dp)
            )
            TextButton(onClick = onIncrement, enabled = !goal.isComplete) {
                Text(if (goal.isComplete) "Complete" else "+1 ${goal.unit}")
            }
        }
    }
}

@Composable
private fun HabitCard(item: HabitWithStreak, onToggle: () -> Unit, onDelete: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(checked = item.doneToday, onCheckedChange = { onToggle() })
            Column(modifier = Modifier.padding(start = 8.dp).weight(1f)) {
                Text(item.habit.title, style = MaterialTheme.typography.titleMedium)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.LocalFireDepartment,
                        contentDescription = null,
                        tint = if (item.streak > 0) scheme.primary else scheme.outline,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    Text(
                        "${item.streak} day streak",
                        style = MaterialTheme.typography.bodyMedium,
                        color = scheme.outline
                    )
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete habit", tint = scheme.outline)
            }
        }
    }
}

@Composable
private fun AddGoalDialog(onDismiss: () -> Unit, onConfirm: (String, Float, String) -> Unit) {
    var title by remember { mutableStateOf("") }
    var target by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New goal") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Goal, e.g. Drink water") })
                OutlinedTextField(value = target, onValueChange = { target = it }, label = { Text("Target, e.g. 8") })
                OutlinedTextField(value = unit, onValueChange = { unit = it }, label = { Text("Unit, e.g. glasses") })
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val targetValue = target.toFloatOrNull() ?: return@TextButton
                    if (title.isNotBlank()) onConfirm(title, targetValue, unit)
                }
            ) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun AddHabitDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var title by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New habit") },
        text = {
            OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Habit, e.g. Meditate") })
        },
        confirmButton = {
            TextButton(onClick = { if (title.isNotBlank()) onConfirm(title) }) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
