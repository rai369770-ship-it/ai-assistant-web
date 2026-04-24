package com.stoolkit.app.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.stoolkit.app.ui.theme.*
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * Feedback screen composable
 */
@Composable
fun FeedbackScreen(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var title by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    
    var showDialog by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    
    val categories = listOf(
        "Select",
        "Feature request",
        "Bug information",
        "Recommendations",
        "Contribution",
        "Join us",
        "Other"
    )
    
    var expanded by remember { mutableStateOf(false) }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // Top bar
        FeedbackTopBar(
            onBackClick = { navController.popBackStack() },
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Content
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Fill form below to complete feedback submission.",
                style = MaterialTheme.typography.bodyLarge,
                color = OnSurfaceMedium,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Feedback form",
                style = MaterialTheme.typography.titleLarge,
                color = OnBackground,
                modifier = Modifier.semantics { heading() }
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Full Name field
            Text(
                text = "Full Name",
                style = MaterialTheme.typography.titleMedium,
                color = OnSurface
            )
            OutlinedTextField(
                value = fullName,
                onValueChange = { fullName = it },
                placeholder = { Text("Write your full name") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Primary,
                    unfocusedBorderColor = Border
                )
            )
            
            // Email field
            Text(
                text = "Email",
                style = MaterialTheme.typography.titleMedium,
                color = OnSurface
            )
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                placeholder = { Text("Write your email") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Primary,
                    unfocusedBorderColor = Border
                )
            )
            
            // Category dropdown
            Text(
                text = "Category",
                style = MaterialTheme.typography.titleMedium,
                color = OnSurface
            )
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = selectedCategory ?: "Select a category",
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Primary,
                        unfocusedBorderColor = Border
                    )
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    categories.forEach { category ->
                        DropdownMenuItem(
                            text = { Text(category) },
                            onClick = {
                                selectedCategory = category
                                expanded = false
                            }
                        )
                    }
                }
            }
            
            // Title field
            Text(
                text = "Title",
                style = MaterialTheme.typography.titleMedium,
                color = OnSurface
            )
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                placeholder = { Text("Write your subject of the feedback") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Primary,
                    unfocusedBorderColor = Border
                )
            )
            
            // Message field
            Text(
                text = "Message",
                style = MaterialTheme.typography.titleMedium,
                color = OnSurface
            )
            OutlinedTextField(
                value = message,
                onValueChange = { message = it },
                placeholder = { Text("Write your full feedback") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
                maxLines = 6,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Primary,
                    unfocusedBorderColor = Border
                )
            )
            
            Text(
                text = "Note: All forms must be filled for successful submission.",
                style = MaterialTheme.typography.bodySmall,
                color = OnSurfaceDisabled,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Submit button
            Button(
                onClick = {
                    if (fullName.isBlank() || email.isBlank() || 
                        selectedCategory == null || selectedCategory == "Select" ||
                        title.isBlank() || message.isBlank()) {
                        Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show()
                    } else {
                        isLoading = true
                        scope.launch {
                            submitFeedback(
                                context = context,
                                name = fullName,
                                email = email,
                                category = selectedCategory!!,
                                subject = title,
                                message = message,
                                onSuccess = {
                                    isLoading = false
                                    showDialog = true
                                },
                                onError = {
                                    isLoading = false
                                    Toast.makeText(context, "Failed to submit feedback", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Primary
                ),
                shape = MaterialTheme.shapes.medium
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = OnPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "Submit Feedback",
                        style = MaterialTheme.typography.titleMedium,
                        color = OnPrimary
                    )
                }
            }
        }
    }
    
    // Success dialog
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = {
                Text(
                    text = "Feedback submission successful",
                    style = MaterialTheme.typography.titleLarge,
                    color = OnBackground
                )
            },
            text = {
                Text(
                    text = "Your feedback has been successfully submitted. Developer team will review it soon. Thanks for your truly important time for this.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = OnSurfaceMedium
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDialog = false
                        navController.popBackStack()
                    }
                ) {
                    Text(
                        text = "OK",
                        color = Primary
                    )
                }
            },
            containerColor = Surface,
            titleContentColor = OnSurface,
            textContentColor = OnSurfaceMedium
        )
    }
}

/**
 * Top bar for Feedback screen
 */
@Composable
fun FeedbackTopBar(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = SurfaceTransparent,
        shadowElevation = 1.dp,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left - Back button
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterStart
            ) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier.size(40.dp)
                ) {
                    Text(
                        text = "←",
                        style = MaterialTheme.typography.titleLarge,
                        color = OnBackground
                    )
                }
            }
            
            // Center - Title
            Box(
                modifier = Modifier.weight(2f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Feedback",
                    style = MaterialTheme.typography.titleLarge,
                    color = OnSurfaceMedium
                )
            }
            
            // Right - Description text
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterEnd
            ) {
                Text(
                    text = "Send your feedback to us regarding this application.",
                    style = MaterialTheme.typography.bodySmall,
                    color = OnSurfaceDisabled,
                    textAlign = TextAlign.End,
                    maxLines = 2
                )
            }
        }
    }
}

/**
 * Submit feedback to API
 */
private suspend fun submitFeedback(
    context: Context,
    name: String,
    email: String,
    category: String,
    subject: String,
    message: String,
    onSuccess: () -> Unit,
    onError: () -> Unit
) {
    try {
        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
        
        val json = Json { encodeDefaults = true }
        val jsonObject = buildJsonObject {
            put("name", name)
            put("email", email)
            put("category", category)
            put("subject", subject)
            put("message", message)
        }
        
        val mediaType = "application/json; charset=utf-8".toMediaType()
        val body = jsonObject.toString().toRequestBody(mediaType)
        
        val request = okhttp3.Request.Builder()
            .url("https://blind-tech-nexus-feedback-submition.vercel.app/feedback")
            .post(body)
            .addHeader("Content-Type", "application/json")
            .build()
        
        val response = client.newCall(request).execute()
        
        if (response.isSuccessful) {
            onSuccess()
        } else {
            onError()
        }
    } catch (e: Exception) {
        e.printStackTrace()
        onError()
    }
}
