package com.blindtechnexus.app.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast as AndroidToast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenu
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

@Composable
fun AboutScreen(
    onBackClick: () -> Unit,
    onContactClick: () -> Unit,
    onFeedbackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val aboutParagraphs = listOf(
        "BlindTech Nexus is an application designed to make digital life easier, more independent, and more productive for blind and visually impaired users. The main idea behind this app is simple: bring many useful tools together in one place, while keeping accessibility as the first priority in every feature.",
        "The app combines different types of tools that people often need in daily life. Instead of installing many separate apps, users can find everything inside one platform. These include AI tools for asking questions or getting help, productivity tools for managing tasks and notes, audio tools for listening and recording, and image or video tools that can describe or process visual content in an accessible way.",
        "Accessibility is not treated as an extra feature. It is the foundation of the entire application. Every screen, every button, and every interaction is designed to work smoothly with screen readers. The layout is kept clean and simple so that users can navigate easily without confusion. Important elements are clearly labeled, and unnecessary visual clutter is avoided. The goal is to reduce effort and make the experience comfortable and predictable.",
        "The AI features play an important role in the app. Users can interact with different AI models to get information, generate content, or solve problems. This is especially useful for users who rely more on voice and text interaction instead of visual interfaces. By providing multiple AI options, the app allows users to choose what works best for them.",
        "Another important part of BlindTech Nexus is productivity. The app helps users manage their daily work, whether it is writing notes, organizing ideas, or handling simple tasks. Everything is designed to be fast and easy to use, without requiring complex steps. The aim is to help users save time and stay focused.",
        "The app also includes tools related to audio and media, because sound is a key part of accessibility. Users can listen to content, convert text to speech, or interact with audio-based features in a simple way. For visual content like images or videos, the app can provide descriptions or processing tools so that users can understand what is happening without needing to see it.",
        "BlindTech Nexus is not just a collection of tools. It is built as a growing platform. New features and tools can be added over time, based on user needs. The idea is to keep improving and expanding, while always maintaining a strong focus on accessibility and ease of use.",
        "In short, BlindTech Nexus is designed to give blind and visually impaired users more control, more independence, and more convenience in using technology. It brings together useful tools, keeps the experience simple, and ensures that everything works smoothly with assistive technologies."
    )

    ScreenWithTopBar(
        title = "About us",
        rightDescription = "About the application and developers.",
        onBackClick = onBackClick,
        modifier = modifier
    ) {
        HeadingText("about blind tech nexus application.")
        Spacer(modifier = Modifier.height(8.dp))
        aboutParagraphs.forEach { paragraph ->
            Text(text = paragraph, style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(8.dp))
        }

        HeadingText("Developer and team")
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "Developer / Founder:", fontWeight = FontWeight.SemiBold)
        Text(text = "Sujan rai.")
        Text(text = "Marketter / assistant:", fontWeight = FontWeight.SemiBold)
        Text(text = "Team blind tech nexus.")

        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "Want to contact us, click the button below.")
        Button(onClick = onContactClick, modifier = Modifier.fillMaxWidth()) {
            Text("Contact us")
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "want to give feedback regarding this application, click the button below.")
        Button(onClick = onFeedbackClick, modifier = Modifier.fillMaxWidth()) {
            Text("Send feedback")
        }
    }
}

@Composable
fun ContactUsScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    fun openLink(url: String) {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }

    ScreenWithTopBar(
        title = "Contact us",
        rightDescription = "Contact with us in various social media platforms.",
        onBackClick = onBackClick,
        modifier = modifier
    ) {
        HeadingText("WhatsApp chat")
        Button(onClick = { openLink("https://wa.me/9779708340992") }, modifier = Modifier.fillMaxWidth()) {
            Text("Open WhatsApp chat button")
        }

        Spacer(modifier = Modifier.height(12.dp))
        HeadingText("telegram")
        Button(onClick = { openLink("https://t.me/blindtechnexus") }, modifier = Modifier.fillMaxWidth()) {
            Text("Open telegram chat button")
        }

        Spacer(modifier = Modifier.height(12.dp))
        HeadingText("YouTube")
        Button(onClick = { openLink("https://YouTube.com/@aiforblinds") }, modifier = Modifier.fillMaxWidth()) {
            Text("Open YouTube button")
        }
    }
}

@Serializable
data class FeedbackRequest(
    @SerialName("name") val name: String,
    @SerialName("email") val email: String,
    @SerialName("category") val category: String,
    @SerialName("subject") val subject: String,
    @SerialName("message") val message: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedbackScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val client = remember { OkHttpClient() }

    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    var subject by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var showSuccessDialog by remember { mutableStateOf(false) }

    val categories = listOf(
        "Select",
        "Feature request",
        "bug information",
        "recommendations",
        "contribution",
        "join us",
        "other"
    )

    ScreenWithTopBar(
        title = "feedback",
        rightDescription = "Send your feedback to us regarding this application.",
        onBackClick = onBackClick,
        modifier = modifier
    ) {
        Text("Fill form below to complete feedback submition.")
        HeadingText("Feedback form")

        Text("Full Name")
        OutlinedTextField(
            value = fullName,
            onValueChange = { fullName = it },
            placeholder = { Text("Write your full name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(8.dp))
        Text("Email")
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            placeholder = { Text("Write your email") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(8.dp))
        Text("Category")
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = if (category.isBlank()) "Select a category" else category,
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                categories.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            category = option
                            expanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text("Title")
        OutlinedTextField(
            value = subject,
            onValueChange = { subject = it },
            placeholder = { Text("Write your subject of the feedback") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(8.dp))
        Text("Message")
        OutlinedTextField(
            value = message,
            onValueChange = { message = it },
            placeholder = { Text("Write your full feedback") },
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))
        Text("Note: All forms must be filled for successful submition.")

        Spacer(modifier = Modifier.height(12.dp))
        Button(
            onClick = {
                if (fullName.isBlank() || email.isBlank() || subject.isBlank() || message.isBlank() || category.isBlank() || category == "Select") {
                    AndroidToast.makeText(context, "Please fill all fields", AndroidToast.LENGTH_SHORT).show()
                    return@Button
                }

                scope.launch {
                    val success = withContext(Dispatchers.IO) {
                        runCatching {
                            val payload = Json.encodeToString(
                                FeedbackRequest(
                                    name = fullName.trim(),
                                    email = email.trim(),
                                    category = category,
                                    subject = subject.trim(),
                                    message = message.trim()
                                )
                            )
                            val request = Request.Builder()
                                .url("https://blind-tech-nexus-feedback-submition.vercel.app/feedback")
                                .post(payload.toRequestBody("application/json".toMediaType()))
                                .build()

                            client.newCall(request).execute().use { response ->
                                response.isSuccessful
                            }
                        }.getOrDefault(false)
                    }

                    if (success) {
                        showSuccessDialog = true
                    } else {
                        AndroidToast.makeText(context, "Unable to submit feedback right now", AndroidToast.LENGTH_SHORT).show()
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Submit feedback")
        }
    }

    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = { showSuccessDialog = false },
            title = { Text("Feedback submition successful.") },
            text = {
                Text(
                    "Your feedback has been successfully submitted. Developer team will review it soon. Thanks for your truly important time for this."
                )
            },
            confirmButton = {
                TextButton(onClick = { showSuccessDialog = false }) {
                    Text("Ok")
                }
            }
        )
    }
}

@Composable
private fun ScreenWithTopBar(
    title: String,
    rightDescription: String,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBackClick, modifier = Modifier.size(72.dp, 40.dp)) {
                Text("Back")
            }
            Text(
                text = title,
                modifier = Modifier.weight(1f),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = rightDescription,
                modifier = Modifier.weight(1.2f),
                style = MaterialTheme.typography.labelSmall
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            content()
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun HeadingText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.semantics { heading() }
    )
}

fun openAccessibilitySettingsIntent(): Intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
