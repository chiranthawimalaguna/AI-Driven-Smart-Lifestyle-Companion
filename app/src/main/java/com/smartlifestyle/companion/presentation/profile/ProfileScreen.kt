package com.smartlifestyle.companion.presentation.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
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
import com.smartlifestyle.companion.data.local.entity.NotificationEntity
import com.smartlifestyle.companion.data.repository.UserProfile
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ProfileScreen(
    email: String?,
    viewModel: ProfileViewModel,
    onNavigateToBmi: () -> Unit,
    onSignOut: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val scheme = MaterialTheme.colorScheme

    var name by remember(state.profile) { mutableStateOf(state.profile.name) }
    var age by remember(state.profile) { mutableStateOf(state.profile.age?.toString() ?: "") }
    var height by remember(state.profile) { mutableStateOf(state.profile.heightCm?.toString() ?: "") }
    var weight by remember(state.profile) { mutableStateOf(state.profile.weightKg?.toString() ?: "") }
    var stepGoal by remember(state.profile) { mutableStateOf(state.profile.stepGoal.toString()) }
    var waterGoal by remember(state.profile) { mutableStateOf(state.profile.waterGoal.toString()) }
    var notificationsEnabled by remember(state.profile) { mutableStateOf(state.profile.notificationsEnabled) }
    var imperial by remember(state.profile) { mutableStateOf(state.profile.preferredUnits == "imperial") }
    var showNotifications by remember { mutableStateOf(false) }

    fun persist() {
        viewModel.save(
            UserProfile(
                name = name,
                age = age.toIntOrNull(),
                heightCm = height.toFloatOrNull(),
                weightKg = weight.toFloatOrNull(),
                preferredUnits = if (imperial) "imperial" else "metric",
                notificationsEnabled = notificationsEnabled,
                stepGoal = stepGoal.toIntOrNull() ?: 8000,
                waterGoal = waterGoal.toIntOrNull() ?: 8
            )
        )
    }

    if (showNotifications) {
        NotificationsDialog(
            notifications = state.notifications,
            onDismiss = { showNotifications = false }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Text("Profile", style = MaterialTheme.typography.headlineMedium) }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(
                        modifier = Modifier.size(56.dp).background(scheme.primaryContainer, CircleShape)
                    ) {
                        Icon(
                            Icons.Filled.Person,
                            contentDescription = null,
                            tint = scheme.onPrimaryContainer,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                    Column(modifier = Modifier.padding(start = 14.dp)) {
                        Text("Signed in as", style = MaterialTheme.typography.labelMedium, color = scheme.outline)
                        Text(email ?: "Loading\u2026", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        item {
            SectionCard(title = "Personal information") {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = age, onValueChange = { age = it }, label = { Text("Age") }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
                OutlinedTextField(value = height, onValueChange = { height = it }, label = { Text("Height (cm)") }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
                OutlinedTextField(value = weight, onValueChange = { weight = it }, label = { Text("Weight (kg)") }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
                TextButton(onClick = onNavigateToBmi, modifier = Modifier.padding(top = 4.dp)) {
                    Text("Calculate BMI \u2192")
                }
            }
        }

        item {
            SectionCard(title = "Preferences") {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Use imperial units")
                    Switch(checked = imperial, onCheckedChange = { imperial = it })
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .clickable {
                            showNotifications = true
                            viewModel.loadNotifications()
                        },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.NotificationsNone,
                            contentDescription = null,
                            tint = scheme.outline,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text("Notifications")
                    }
                    Switch(checked = notificationsEnabled, onCheckedChange = { notificationsEnabled = it })
                }
                Text(
                    "Tap \u201CNotifications\u201D to see your latest smart nudges.",
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.outline,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        item {
            SectionCard(title = "Goals") {
                Text(
                    "These targets currently display here for reference. Live syncing " +
                            "into the Today tab's Life Score is a documented next step - see the setup guide.",
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.outline
                )
                OutlinedTextField(
                    value = stepGoal, onValueChange = { stepGoal = it }, label = { Text("Daily step goal") },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                )
                OutlinedTextField(
                    value = waterGoal, onValueChange = { waterGoal = it }, label = { Text("Daily water goal (glasses)") },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                )
            }
        }

        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Button(onClick = { persist() }, modifier = Modifier.fillMaxWidth()) {
                    Text(if (state.justSaved) "Saved" else "Save changes")
                }
                if (state.saveError) {
                    Text(
                        "Couldn't save - check you're signed in and try again.",
                        color = scheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
            }
        }

        item {
            SectionCard(title = "Settings") {
                Text(
                    "AI-Driven Smart Lifestyle Companion. Routine tasks, goals, and habits sync to the cloud; " +
                            "step count and ambient light are read from this device's own sensors, and heart rate/sleep " +
                            "sync from your watch when available.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        item {
            Button(
                onClick = onSignOut,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = scheme.errorContainer, contentColor = scheme.onErrorContainer)
            ) {
                Text("Sign out")
            }
        }
    }
}

@Composable
private fun NotificationsDialog(notifications: List<NotificationEntity>, onDismiss: () -> Unit) {
    val formatter = remember { SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
        title = { Text("Latest notifications") },
        text = {
            if (notifications.isEmpty()) {
                Text(
                    "No notifications yet - smart nudges (overdue tasks, hydration reminders) " +
                            "will appear here once triggered."
                )
            } else {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 360.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(notifications, key = { it.id }) { n ->
                        Column {
                            Text(n.title, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                            Text(n.message, style = MaterialTheme.typography.bodySmall)
                            Text(
                                formatter.format(Date(n.timestampMillis)),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
            }
        }
    )
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline)
            Column(modifier = Modifier.padding(top = 8.dp)) { content() }
        }
    }
}