package com.stoolkit.app.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
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

/**
 * About Us screen composable
 */
@Composable
fun AboutUsScreen(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // Top bar
        AboutTopBar(
            onBackClick = { navController.popBackStack() },
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Content
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "About Blind Tech Nexus application",
                style = MaterialTheme.typography.titleLarge,
                color = OnBackground,
                modifier = Modifier.semantics { heading() }
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // App description - each line in separate TextView
            val appDescriptionLines = listOf(
                "BlindTech Nexus is an application designed to make digital life easier, more independent, and more productive for blind and visually impaired users. The main idea behind this app is simple: bring many useful tools together in one place, while keeping accessibility as the first priority in every feature.",
                "The app combines different types of tools that people often need in daily life. Instead of installing many separate apps, users can find everything inside one platform. These include AI tools for asking questions or getting help, productivity tools for managing tasks and notes, audio tools for listening and recording, and image or video tools that can describe or process visual content in an accessible way.",
                "Accessibility is not treated as an extra feature. It is the foundation of the entire application. Every screen, every button, and every interaction is designed to work smoothly with screen readers. The layout is kept clean and simple so that users can navigate easily without confusion. Important elements are clearly labeled, and unnecessary visual clutter is avoided. The goal is to reduce effort and make the experience comfortable and predictable.",
                "The AI features play an important role in the app. Users can interact with different AI models to get information, generate content, or solve problems. This is especially useful for users who rely more on voice and text interaction instead of visual interfaces. By providing multiple AI options, the app allows users to choose what works best for them.",
                "Another important part of BlindTech Nexus is productivity. The app helps users manage their daily work, whether it is writing notes, organizing ideas, or handling simple tasks. Everything is designed to be fast and easy to use, without requiring complex steps. The aim is to help users save time and stay focused.",
                "The app also includes tools related to audio and media, because sound is a key part of accessibility. Users can listen to content, convert text to speech, or interact with audio-based features in a simple way. For visual content like images or videos, the app can provide descriptions or processing tools so that users can understand what is happening without needing to see it.",
                "BlindTech Nexus is not just a collection of tools. It is built as a growing platform. New features and tools can be added over time, based on user needs. The idea is to keep improving and expanding, while always maintaining a strong focus on accessibility and ease of use.",
                "In short, BlindTech Nexus is designed to give blind and visually impaired users more control, more independence, and more convenience in using technology. It brings together useful tools, keeps the experience simple, and ensures that everything works smoothly with assistive technologies."
            )
            
            appDescriptionLines.forEach { line ->
                Text(
                    text = line,
                    style = MaterialTheme.typography.bodyMedium,
                    color = OnSurfaceMedium,
                    lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.5
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Developer and team section
            Text(
                text = "Developer and team",
                style = MaterialTheme.typography.titleLarge,
                color = OnBackground,
                modifier = Modifier.semantics { heading() }
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Developer / Founder:",
                style = MaterialTheme.typography.titleMedium,
                color = OnSurface
            )
            Text(
                text = "Sujan Rai",
                style = MaterialTheme.typography.bodyMedium,
                color = OnSurfaceMedium
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Marketter / Assistant:",
                style = MaterialTheme.typography.titleMedium,
                color = OnSurface
            )
            Text(
                text = "Team Blind Tech Nexus",
                style = MaterialTheme.typography.bodyMedium,
                color = OnSurfaceMedium
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Contact us section
            Text(
                text = "Want to contact us, click the button below.",
                style = MaterialTheme.typography.bodyMedium,
                color = OnSurfaceMedium,
                textAlign = TextAlign.Center
            )
            
            Button(
                onClick = {
                    navController.navigate("contact_us")
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Primary
                ),
                shape = MaterialTheme.shapes.medium
            ) {
                Text(
                    text = "Contact Us",
                    style = MaterialTheme.typography.titleMedium,
                    color = OnPrimary
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Feedback section
            Text(
                text = "Want to give feedback regarding this application, click the button below.",
                style = MaterialTheme.typography.bodyMedium,
                color = OnSurfaceMedium,
                textAlign = TextAlign.Center
            )
            
            Button(
                onClick = {
                    navController.navigate("feedback")
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Primary
                ),
                shape = MaterialTheme.shapes.medium
            ) {
                Text(
                    text = "Send Feedback",
                    style = MaterialTheme.typography.titleMedium,
                    color = OnPrimary
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/**
 * Top bar for About Us screen
 */
@Composable
fun AboutTopBar(
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
                    text = "About Us",
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
                    text = "About the application and developers.",
                    style = MaterialTheme.typography.bodySmall,
                    color = OnSurfaceDisabled,
                    textAlign = TextAlign.End,
                    maxLines = 2
                )
            }
        }
    }
}
