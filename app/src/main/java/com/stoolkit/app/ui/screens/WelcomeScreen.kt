package com.stoolkit.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.stoolkit.app.ui.theme.*

/**
 * Welcome screen composable
 */
@Composable
fun WelcomeScreen(
    onContinueClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Title
        Text(
            text = "Welcome to SToolkit",
            style = MaterialTheme.typography.headlineLarge,
            color = OnBackground,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .semantics { contentDescription = "Welcome to SToolkit" }
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Welcome description
        Text(
            text = "Welcome to Stoolkit, your all-in-one multi-toolkit for Android. This app brings together powerful AI tools, productivity features, and utilities for documents, images, audio, and video—all in one place. Designed to simplify your daily tasks, Stoolkit helps you create, manage, and explore with ease. Discover useful articles, smart features, and practical resources that support your work and learning. Simple, fast, and efficient, Stoolkit gives you everything you need to do more in a single app.",
            style = MaterialTheme.typography.bodyLarge,
            color = OnSurfaceMedium,
            textAlign = TextAlign.Center,
            lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * 1.5,
            modifier = Modifier.semantics { contentDescription = "Welcome description text" }
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Developer label
        Text(
            text = "Developed by:",
            style = MaterialTheme.typography.bodySmall,
            color = OnSurfaceDisabled,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        // Developer link - using Android's clickable link system
        ClickableLink(
            text = "Blind Tech Nexus",
            url = "https://blindtechnexus.pages.dev",
            contentDescription = "Opens Blind Tech Nexus website"
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Instruction text
        Text(
            text = "Click continue to start using the app.",
            style = MaterialTheme.typography.bodySmall,
            color = OnSurfaceDisabled,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Continue button
        Button(
            onClick = onContinueClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .semantics { contentDescription = "Continue to app" },
            colors = ButtonDefaults.buttonColors(
                containerColor = Primary
            ),
            shape = MaterialTheme.shapes.medium
        ) {
            Text(
                text = "Continue",
                style = MaterialTheme.typography.titleMedium,
                color = OnPrimary
            )
        }
    }
}

/**
 * Composable for displaying clickable links using Android's system
 */
@Composable
fun ClickableLink(
    text: String,
    url: String,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
    
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge.copy(
            color = Primary
        ),
        textAlign = TextAlign.Center,
        modifier = modifier
            .semantics { this.contentDescription = contentDescription }
    )
}
