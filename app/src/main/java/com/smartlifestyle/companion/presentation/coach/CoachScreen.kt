package com.smartlifestyle.companion.presentation.coach

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.smartlifestyle.companion.domain.model.ChatMessage
import com.smartlifestyle.companion.domain.model.ChatSender

private val tabTitles = listOf("Chat", "Advice", "Daily Plan")

/** Ten starter questions chosen to match AiCoachEngine's real keyword branches
 * (water, sleep, steps, tasks, mood, weather, greeting) so every one of these
 * gets a genuine, data-grounded reply rather than a canned response. */
private val sampleQuestions = listOf(
    "How much water have I had today?",
    "How did I sleep last night?",
    "How many steps have I taken today?",
    "Do I have any tasks due?",
    "What's my mood been like this week?",
    "What's the weather like right now?",
    "Should I go for a walk?",
    "Am I drinking enough water?",
    "What should I focus on today?",
    "Hi, what can you help with?"
)

@Composable
fun CoachScreen(viewModel: CoachViewModel) {
    val state by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            "AI Coach",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(16.dp)
        )
        TabRow(selectedTabIndex = selectedTab) {
            tabTitles.forEachIndexed { index, title ->
                Tab(selected = selectedTab == index, onClick = { selectedTab = index }, text = { Text(title) })
            }
        }
        when (selectedTab) {
            0 -> ChatTab(messages = state.messages, isSending = state.isSending, onSend = viewModel::sendMessage)
            1 -> AdviceTab(advice = state.advice, onRefresh = viewModel::refreshAdviceAndPlan)
            else -> DailyPlanTab(plan = state.dailyPlan, onRefresh = viewModel::refreshAdviceAndPlan)
        }
    }
}

@Composable
private fun ChatTab(messages: List<ChatMessage>, isSending: Boolean, onSend: (String) -> Unit) {
    var input by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        if (messages.isEmpty()) {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Text(
                        "Ask about your water, sleep, steps, tasks, mood, or the weather \u2014 " +
                                "or try one of these:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
                items(sampleQuestions) { question ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !isSending) { onSend(question) },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Text(
                            question,
                            modifier = Modifier.padding(14.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(messages, key = { it.id }) { message -> ChatBubble(message) }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Ask your coach\u2026") },
                enabled = !isSending
            )
            IconButton(
                onClick = { onSend(input); input = "" },
                enabled = !isSending && input.isNotBlank()
            ) {
                Icon(Icons.Filled.Send, contentDescription = "Send")
            }
        }
    }
}

@Composable
private fun ChatBubble(message: ChatMessage) {
    val scheme = MaterialTheme.colorScheme
    val isUser = message.sender == ChatSender.USER
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (isUser) scheme.primaryContainer else scheme.secondaryContainer
            )
        ) {
            Text(
                message.text,
                modifier = Modifier.padding(12.dp),
                color = if (isUser) scheme.onPrimaryContainer else scheme.onSecondaryContainer
            )
        }
    }
}

@Composable
private fun AdviceTab(advice: List<String>, onRefresh: () -> Unit) {
    LaunchedEffect(Unit) { onRefresh() }
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Text("Personalized advice", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold) }
        items(advice) { tip ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Text(tip, modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun DailyPlanTab(plan: List<String>, onRefresh: () -> Unit) {
    LaunchedEffect(Unit) { onRefresh() }
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Text("Today's plan", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold) }
        items(plan.withIndex().toList()) { (index, step) ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "${index + 1}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(end = 12.dp)
                    )
                    Text(step, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}