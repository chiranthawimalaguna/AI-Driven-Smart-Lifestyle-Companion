package com.smartlifestyle.companion.presentation.auth

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp

/** Custom-drawn compass mark - the same motif as the app's launcher icon (teal
 * disc, white ring, amber needle) - rather than a generic Material icon, so the
 * brand is consistent from the very first screen the user sees. */
@Composable
private fun BrandMark(modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    Canvas(modifier = modifier) {
        val radius = size.minDimension / 2f
        val center = Offset(size.width / 2f, size.height / 2f)

        drawCircle(color = scheme.primary, radius = radius, center = center)
        drawCircle(color = androidx.compose.ui.graphics.Color.White, radius = radius * 0.62f, center = center)

        val needle = Path().apply {
            moveTo(center.x, center.y - radius * 0.42f)
            lineTo(center.x + radius * 0.2f, center.y + radius * 0.28f)
            lineTo(center.x, center.y + radius * 0.12f)
            lineTo(center.x - radius * 0.2f, center.y + radius * 0.28f)
            close()
        }
        drawPath(needle, color = scheme.secondary)
        drawCircle(color = scheme.primary, radius = radius * 0.09f, center = center)
    }
}

/** Soft, oversized translucent circles behind the content - a common "modern app"
 * background treatment that reads as more designed than a flat gradient alone. */
@Composable
private fun DecorativeBackdrop(modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    Canvas(modifier = modifier) {
        drawCircle(
            color = scheme.secondary.copy(alpha = 0.12f),
            radius = size.width * 0.55f,
            center = Offset(size.width * 0.85f, size.height * 0.05f)
        )
        drawCircle(
            color = scheme.tertiary.copy(alpha = 0.10f),
            radius = size.width * 0.4f,
            center = Offset(size.width * 0.05f, size.height * 0.28f)
        )
    }
}

@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    onSignedIn: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isSignUpMode by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }

    LaunchedEffect(state.isSignedIn) {
        if (state.isSignedIn) onSignedIn()
    }

    if (state.isSignedIn) return

    val scheme = MaterialTheme.colorScheme
    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(scheme.primaryContainer, scheme.background)
    )

    Box(
        modifier = Modifier.fillMaxSize().background(backgroundBrush),
        contentAlignment = Alignment.Center
    ) {
        DecorativeBackdrop(modifier = Modifier.fillMaxSize())

        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            BrandMark(modifier = Modifier.size(84.dp))

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "AI-Driven Smart Lifestyle Companion",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = scheme.primary
                )
                Text(
                    "An Intelligent Personal Lifestyle Companion",
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onBackground.copy(alpha = 0.7f)
                )
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = scheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        if (isSignUpMode) "Create your account" else "Welcome back",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email") },
                        leadingIcon = { Icon(Icons.Filled.Email, contentDescription = null) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    )

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                    contentDescription = if (passwordVisible) "Hide password" else "Show password"
                                )
                            }
                        },
                        singleLine = true,
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    )

                    state.errorMessage?.let {
                        Text(it, color = scheme.error, style = MaterialTheme.typography.bodyMedium)
                    }

                    if (state.isLoading) {
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    } else {
                        Button(
                            onClick = {
                                if (isSignUpMode) viewModel.signUp(email, password)
                                else viewModel.signIn(email, password)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text(if (isSignUpMode) "Sign up" else "Sign in")
                        }
                    }

                    TextButton(
                        onClick = { isSignUpMode = !isSignUpMode },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (isSignUpMode) "Already have an account? Sign in" else "New here? Create an account")
                    }
                }
            }
        }
    }
}
