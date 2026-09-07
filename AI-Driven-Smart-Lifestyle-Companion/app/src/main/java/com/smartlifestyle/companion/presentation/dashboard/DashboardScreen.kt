package com.smartlifestyle.companion.presentation.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.smartlifestyle.companion.data.local.entity.MetricSource
import com.smartlifestyle.companion.domain.model.RoutineTask
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

private val scheduleOptions = listOf(
    "In 15 min" to TimeUnit.MINUTES.toMillis(15),
    "In 1 hour" to TimeUnit.HOURS.toMillis(1),
    "This evening" to TimeUnit.HOURS.toMillis(6),
    "Tomorrow" to TimeUnit.DAYS.toMillis(1)
)

private fun greeting(): String = when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
    in 5..11 -> "Good morning"
    in 12..16 -> "Good afternoon"
    in 17..21 -> "Good evening"
    else -> "Good night"
}

private fun sourceLabel(source: MetricSource?): String = when (source) {
    MetricSource.HEALTH_CONNECT -> "watch"
    MetricSource.DEVICE_SENSOR -> "phone"
    MetricSource.MANUAL_ENTRY -> "manual"
    null -> "no data"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(viewModel: DashboardViewModel) {
    val state by viewModel.uiState.collectAsState()
    var showAddTaskDialog by remember { mutableStateOf(false) }
    var showManualStepsDialog by remember { mutableStateOf(false) }
    var taskBeingEdited by remember { mutableStateOf<RoutineTask?>(null) }
    var taskPendingDelete by remember { mutableStateOf<RoutineTask?>(null) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("AI-Driven Smart Lifestyle Companion") },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddTaskDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Add task")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { GreetingBanner() }
            state.lifeScore?.let { score -> item { LifeScoreCard(score) } }
            state.topRecommendation?.let { rec -> item { AiRecommendationCard(rec) } }
            item { StatusCard(state = state, onEnterManually = { showManualStepsDialog = true }) }
            item { WaterCard(glasses = state.waterGlassesToday, onAddGlass = { viewModel.logWaterGlass() }) }
            item { Text("Today's tasks", style = MaterialTheme.typography.titleMedium) }

            if (state.tasks.isEmpty()) {
                item { EmptyTasksState() }
            } else {
                items(state.tasks, key = { it.id }) { task ->
                    TaskRow(
                        task = task,
                        onToggleComplete = { viewModel.toggleTaskCompletion(task) },
                        onEdit = { taskBeingEdited = task },
                        onDelete = { taskPendingDelete = task }
                    )
                }
            }
        }
    }

    if (showAddTaskDialog) {
        TaskDialog(
            title = "New task",
            initialTitle = "",
            confirmLabel = "Add",
            onDismiss = { showAddTaskDialog = false },
            onConfirm = { title, delayMillis ->
                viewModel.addTask(title, System.currentTimeMillis() + delayMillis)
                showAddTaskDialog = false
            }
        )
    }

    taskBeingEdited?.let { task ->
        TaskDialog(
            title = "Edit task",
            initialTitle = task.title,
            confirmLabel = "Save",
            onDismiss = { taskBeingEdited = null },
            onConfirm = { title, delayMillis ->
                viewModel.editTask(task, title, System.currentTimeMillis() + delayMillis)
                taskBeingEdited = null
            }
        )
    }

    taskPendingDelete?.let { task ->
        AlertDialog(
            onDismissRequest = { taskPendingDelete = null },
            title = { Text("Delete task?") },
            text = { Text("\u201c${task.title}\u201d will be removed permanently.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteTask(task)
                    taskPendingDelete = null
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { taskPendingDelete = null }) { Text("Cancel") }
            }
        )
    }

    if (showManualStepsDialog) {
        ManualStepsDialog(
            onDismiss = { showManualStepsDialog = false },
            onConfirm = { steps ->
                viewModel.recordManualSteps(steps)
                showManualStepsDialog = false
            }
        )
    }
}

@Composable
private fun GreetingBanner() {
    val scheme = MaterialTheme.colorScheme
    val brush = Brush.horizontalGradient(listOf(scheme.primary, scheme.tertiary))
    Surface(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
        Box(modifier = Modifier.fillMaxWidth().background(brush).padding(20.dp)) {
            Column {
                Text(
                    greeting(),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = scheme.onPrimary
                )
                Text(
                    "Here's what's ahead today.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onPrimary.copy(alpha = 0.85f)
                )
            }
        }
    }
}

@Composable
private fun StatusCard(state: DashboardUiState, onEnterManually: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = scheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatPill(
                    icon = Icons.Filled.DirectionsWalk,
                    value = "${state.stepsToday}",
                    label = "steps \u00b7 ${sourceLabel(state.stepsSource)}",
                    containerColor = scheme.primaryContainer,
                    contentColor = scheme.onPrimaryContainer,
                    modifier = Modifier.weight(1f)
                )
                val weatherText = state.temperatureCelsius?.let { temp ->
                    "${temp.toInt()}\u00b0" + if (state.isRaining) " \u00b7 rain" else ""
                } ?: "N/A"
                StatPill(
                    icon = Icons.Filled.WbSunny,
                    value = weatherText,
                    label = "weather",
                    containerColor = scheme.secondaryContainer,
                    contentColor = scheme.onSecondaryContainer,
                    modifier = Modifier.weight(1f)
                )
                StatPill(
                    icon = Icons.Filled.LightMode,
                    value = state.ambientLux?.let { "${it.toInt()}" } ?: "N/A",
                    label = "lux",
                    containerColor = scheme.surface,
                    contentColor = scheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                StatPill(
                    icon = Icons.Filled.Favorite,
                    value = state.heartRateBpm?.let { "${it.toInt()}" } ?: "N/A",
                    label = "bpm \u00b7 ${sourceLabel(state.heartRateSource)}",
                    containerColor = scheme.errorContainer,
                    contentColor = scheme.onErrorContainer,
                    modifier = Modifier.weight(1f)
                )
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onEnterManually) { Text("Enter steps manually") }
            }
        }
    }
}

@Composable
private fun StatPill(
    icon: ImageVector,
    value: String,
    label: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(shape = RoundedCornerShape(16.dp), color = containerColor, modifier = modifier) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 6.dp),
            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = label, tint = contentColor, modifier = Modifier.size(20.dp))
            Spacer(Modifier.height(4.dp))
            Text(value, color = contentColor, style = MaterialTheme.typography.labelLarge)
            Text(label, color = contentColor, style = MaterialTheme.typography.labelMedium)
        }
    }
}

private const val WATER_DAILY_GOAL = 8

@Composable
private fun LifeScoreCard(score: com.smartlifestyle.companion.domain.usecase.LifeScoreResult) {
    val scheme = MaterialTheme.colorScheme
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(56.dp).background(scheme.primaryContainer, androidx.compose.foundation.shape.CircleShape),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                Text(
                    "${score.score}",
                    style = MaterialTheme.typography.titleLarge,
                    color = scheme.onPrimaryContainer,
                    fontWeight = FontWeight.Bold
                )
            }
            Column(modifier = Modifier.padding(start = 14.dp).weight(1f)) {
                Text("Life Score", style = MaterialTheme.typography.labelMedium, color = scheme.outline)
                Text(score.label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    "Steps ${score.breakdown["Steps"]} \u00b7 Water ${score.breakdown["Water"]} \u00b7 Tasks ${score.breakdown["Tasks"]} \u00b7 Sleep ${score.breakdown["Sleep"]}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.outline
                )
            }
        }
    }
}

@Composable
private fun AiRecommendationCard(recommendation: String) {
    val scheme = MaterialTheme.colorScheme
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = scheme.secondaryContainer)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.WbSunny,
                contentDescription = null,
                tint = scheme.onSecondaryContainer,
                modifier = Modifier.size(24.dp)
            )
            Column(modifier = Modifier.padding(start = 12.dp)) {
                Text("AI recommendation", style = MaterialTheme.typography.labelMedium, color = scheme.onSecondaryContainer)
                Text(recommendation, style = MaterialTheme.typography.bodyLarge, color = scheme.onSecondaryContainer)
            }
        }
    }
}

@Composable
private fun WaterCard(glasses: Int, onAddGlass: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.LocalDrink,
                contentDescription = null,
                tint = scheme.primary,
                modifier = Modifier.size(28.dp)
            )
            Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
                Text("Water intake", style = MaterialTheme.typography.titleMedium)
                Text(
                    "$glasses / $WATER_DAILY_GOAL glasses today",
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.outline
                )
                LinearProgressIndicator(
                    progress = (glasses.toFloat() / WATER_DAILY_GOAL).coerceIn(0f, 1f),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                )
            }
            IconButton(onClick = onAddGlass) {
                Icon(Icons.Filled.Add, contentDescription = "Log a glass of water")
            }
        }
    }
}

/** Custom illustration (not a single glyph icon) shown when there are no tasks -
 * a simple hand-drawn clipboard/checklist graphic built from primitive shapes. */
@Composable
private fun EmptyTasksState() {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 8.dp),
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
    ) {
        Canvas(modifier = Modifier.size(120.dp)) {
            val boardColor = scheme.primary.copy(alpha = 0.15f)
            val lineColor = scheme.primary
            val w = size.width
            val h = size.height

            // Clipboard body
            drawRoundRect(
                color = boardColor,
                topLeft = Offset(w * 0.15f, h * 0.12f),
                size = androidx.compose.ui.geometry.Size(w * 0.7f, h * 0.8f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(16f, 16f)
            )
            // Clip at top
            drawRoundRect(
                color = lineColor,
                topLeft = Offset(w * 0.38f, h * 0.06f),
                size = androidx.compose.ui.geometry.Size(w * 0.24f, h * 0.1f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f, 8f)
            )
            // Checklist lines
            val lineYs = listOf(h * 0.38f, h * 0.55f, h * 0.72f)
            lineYs.forEach { y ->
                drawLine(
                    color = lineColor.copy(alpha = 0.6f),
                    start = Offset(w * 0.42f, y),
                    end = Offset(w * 0.78f, y),
                    strokeWidth = 6f,
                    cap = StrokeCap.Round
                )
            }
            // Checkmark next to the first line
            drawLine(
                color = lineColor,
                start = Offset(w * 0.26f, h * 0.38f),
                end = Offset(w * 0.31f, h * 0.43f),
                strokeWidth = 6f,
                cap = StrokeCap.Round
            )
            drawLine(
                color = lineColor,
                start = Offset(w * 0.31f, h * 0.43f),
                end = Offset(w * 0.39f, h * 0.32f),
                strokeWidth = 6f,
                cap = StrokeCap.Round
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "No tasks yet - tap + to add your first one.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline
        )
    }
}

@Composable
private fun priorityBadge(task: RoutineTask): Triple<String, Color, Color> {
    val scheme = MaterialTheme.colorScheme
    return when {
        task.isCompleted -> Triple("Done", scheme.surfaceVariant, scheme.onSurfaceVariant)
        task.priorityScore >= 40f -> Triple("High", scheme.errorContainer, scheme.onErrorContainer)
        task.priorityScore >= 10f -> Triple("Medium", scheme.secondaryContainer, scheme.onSecondaryContainer)
        else -> Triple("Low", scheme.surfaceVariant, scheme.onSurfaceVariant)
    }
}

private fun formatDueTime(scheduledTimeMillis: Long, isCompleted: Boolean): String {
    if (isCompleted) return "Completed"

    val now = Calendar.getInstance()
    val due = Calendar.getInstance().apply { timeInMillis = scheduledTimeMillis }
    val timeFmt = SimpleDateFormat("h:mm a", Locale.getDefault())
    val isOverdue = scheduledTimeMillis < System.currentTimeMillis()
    val prefix = if (isOverdue) "Overdue \u00b7 " else ""

    val sameDay = now.get(Calendar.YEAR) == due.get(Calendar.YEAR) &&
        now.get(Calendar.DAY_OF_YEAR) == due.get(Calendar.DAY_OF_YEAR)
    if (sameDay) return "$prefix" + "Today, " + timeFmt.format(Date(scheduledTimeMillis))

    val tomorrow = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
    val isTomorrow = tomorrow.get(Calendar.YEAR) == due.get(Calendar.YEAR) &&
        tomorrow.get(Calendar.DAY_OF_YEAR) == due.get(Calendar.DAY_OF_YEAR)
    if (isTomorrow) return "${prefix}Tomorrow, ${timeFmt.format(Date(scheduledTimeMillis))}"

    return "$prefix" + SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date(scheduledTimeMillis))
}

@Composable
private fun TaskRow(
    task: RoutineTask,
    onToggleComplete: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val (badgeLabel, badgeContainer, badgeContent) = priorityBadge(task)
    var menuExpanded by remember { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Checkbox(checked = task.isCompleted, onCheckedChange = { onToggleComplete() })
            Column(modifier = Modifier.padding(start = 8.dp).weight(1f)) {
                Text(
                    task.title,
                    style = MaterialTheme.typography.bodyLarge,
                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                )
                Text(
                    formatDueTime(task.scheduledTimeMillis, task.isCompleted),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.outline
                )
            }
            Surface(shape = RoundedCornerShape(12.dp), color = badgeContainer) {
                Text(
                    badgeLabel,
                    color = badgeContent,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "Task options")
                }
                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                    DropdownMenuItem(
                        text = { Text("Edit") },
                        leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null) },
                        onClick = { menuExpanded = false; onEdit() }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete") },
                        leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null) },
                        onClick = { menuExpanded = false; onDelete() }
                    )
                }
            }
        }
    }
}

@Composable
private fun TaskDialog(
    title: String,
    initialTitle: String,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (title: String, delayMillis: Long) -> Unit
) {
    var taskTitle by remember { mutableStateOf(initialTitle) }
    var selectedOption by remember { mutableStateOf(scheduleOptions.first()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = taskTitle,
                    onValueChange = { taskTitle = it },
                    label = { Text("Task title") },
                    modifier = Modifier.fillMaxWidth()
                )
                Text("When?")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    scheduleOptions.forEach { option ->
                        FilterChip(
                            selected = selectedOption == option,
                            onClick = { selectedOption = option },
                            label = { Text(option.first) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (taskTitle.isNotBlank()) onConfirm(taskTitle, selectedOption.second) },
                enabled = taskTitle.isNotBlank()
            ) { Text(confirmLabel) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun ManualStepsDialog(
    onDismiss: () -> Unit,
    onConfirm: (steps: Int) -> Unit
) {
    var stepsText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Enter today's steps") },
        text = {
            Column {
                Text("Use this if no sensor (watch or phone) has reported steps, or the reading looks wrong.")
                OutlinedTextField(
                    value = stepsText,
                    onValueChange = { input -> if (input.all { it.isDigit() }) stepsText = input },
                    label = { Text("Steps") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { stepsText.toIntOrNull()?.let(onConfirm) },
                enabled = stepsText.toIntOrNull() != null
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
