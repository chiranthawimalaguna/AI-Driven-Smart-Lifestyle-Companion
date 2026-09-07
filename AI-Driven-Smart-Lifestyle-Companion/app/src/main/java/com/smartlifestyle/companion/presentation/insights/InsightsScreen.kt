package com.smartlifestyle.companion.presentation.insights

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.Mood
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.smartlifestyle.companion.domain.usecase.MetricInsight

@Composable
fun InsightsScreen(viewModel: InsightsViewModel) {
    val state by viewModel.uiState.collectAsState()
    val scheme = MaterialTheme.colorScheme

    LazyColumn(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Text("Insights", style = MaterialTheme.typography.headlineMedium) }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = scheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = scheme.onPrimaryContainer)
                        Text(
                            "  Weekly AI report",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = scheme.onPrimaryContainer
                        )
                    }
                    Text(
                        state.weeklyAiReport,
                        style = MaterialTheme.typography.bodyMedium,
                        color = scheme.onPrimaryContainer,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }

        item {
            InsightMetricCard(
                icon = Icons.Filled.DirectionsWalk,
                title = "Steps",
                insight = state.stepsInsight
            )
        }
        item {
            InsightMetricCard(
                icon = Icons.Filled.Favorite,
                title = "Heart rate",
                insight = state.heartRateInsight
            )
        }
        item {
            InsightMetricCard(
                icon = Icons.Filled.NightsStay,
                title = "Sleep",
                insight = state.sleepInsight
            )
        }
        item {
            InsightMetricCard(
                icon = Icons.Filled.LocalDrink,
                title = "Water",
                insight = state.waterInsight
            )
        }
        item {
            MoodInsightCard(
                insight = state.moodInsight,
                onLogMood = { viewModel.logMood(it) }
            )
        }
    }
}

@Composable
private fun InsightMetricCard(icon: ImageVector, title: String, insight: MetricInsight?) {
    val scheme = MaterialTheme.colorScheme
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = scheme.primary)
                Text("  $title", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            Text(
                insight?.message?.ifBlank { "Not enough data yet." } ?: "Loading\u2026",
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.outline,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
    }
}

/** Mood has no sensor, so its card also offers a quick 1-5 log button row - the
 * only Insights card that lets the user add data directly rather than just
 * displaying it. */
@Composable
private fun MoodInsightCard(insight: MetricInsight?, onLogMood: (Int) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Mood, contentDescription = null, tint = scheme.primary)
                Text("  Mood", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            Text(
                insight?.message?.ifBlank { "Not enough data yet." } ?: "Loading\u2026",
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.outline,
                modifier = Modifier.padding(top = 6.dp, bottom = 8.dp)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                (1..5).forEach { score ->
                    TextButton(onClick = { onLogMood(score) }) { Text("$score") }
                }
            }
        }
    }
}
