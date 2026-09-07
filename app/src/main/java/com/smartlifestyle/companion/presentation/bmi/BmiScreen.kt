package com.smartlifestyle.companion.presentation.bmi

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BmiScreen(viewModel: BmiViewModel, onBack: () -> Unit = {}) {
    val state by viewModel.uiState.collectAsState()
    val scheme = MaterialTheme.colorScheme

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("BMI Calculator") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
    Column(
        modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "A quick body mass index estimate from your weight and height.",
            style = MaterialTheme.typography.bodyMedium,
            color = scheme.outline
        )

        OutlinedTextField(
            value = state.weightKg,
            onValueChange = viewModel::onWeightChange,
            label = { Text("Weight (kg)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = state.heightCm,
            onValueChange = viewModel::onHeightChange,
            label = { Text("Height (cm)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = { viewModel.calculate() },
            modifier = Modifier.fillMaxWidth(),
            enabled = state.weightKg.isNotBlank() && state.heightCm.isNotBlank()
        ) {
            Text("Calculate")
        }

        state.result?.let { bmi ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = scheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        String.format("%.1f", bmi),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = scheme.onPrimaryContainer
                    )
                    Text(
                        state.category?.label.orEmpty(),
                        style = MaterialTheme.typography.titleMedium,
                        color = scheme.onPrimaryContainer
                    )
                    if (state.saved) {
                        Text(
                            "Saved to your Insights history.",
                            style = MaterialTheme.typography.labelMedium,
                            color = scheme.onPrimaryContainer
                        )
                    }
                }
            }
        }

        Text(
            "BMI is a general screening measure and doesn't directly measure body fat " +
                "or account for factors like muscle mass. For personalised guidance, " +
                "consult a healthcare professional.",
            style = MaterialTheme.typography.labelMedium,
            color = scheme.outline
        )
    }
    }
}
