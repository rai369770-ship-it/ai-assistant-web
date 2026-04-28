package com.blindtechnexus.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.blindtechnexus.app.ui.theme.OnBackground
import com.blindtechnexus.app.ui.theme.OnPrimary
import com.blindtechnexus.app.ui.theme.OnSurfaceDisabled
import com.blindtechnexus.app.ui.theme.OnSurfaceMedium
import com.blindtechnexus.app.ui.theme.Primary

@Composable
fun WelcomeScreen(
    onContinueClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Welcome to blind tech nexus",
            style = MaterialTheme.typography.titleLarge,
            color = OnBackground,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() }
        )

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Hi there 👋",
            style = MaterialTheme.typography.titleMedium,
            color = OnBackground,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = """
                We’re really glad you’re here.

                BlindTech Nexus is built to make everyday tech simpler, smoother, and more useful—especially for people who are blind or visually impaired. Everything inside this app is designed with accessibility first, so you can focus on what you want to do, not on how to do it.

                Here, you’ll find a collection of helpful tools in one place. You can explore AI tools for quick answers and assistance, productivity tools to stay organized, audio tools for listening and recording, and even video and image tools—each one made to be easy to use and screen reader friendly.

                We’ve tried to keep things clear, simple, and practical. No clutter, no confusion—just tools that work the way you expect them to.

                Whether you’re here to get things done, try something new, or just make your daily tasks easier, BlindTech Nexus is here to support you.

                Take your time, explore around, and make it your own.

                Let’s get started 🚀
            """.trimIndent(),
            style = MaterialTheme.typography.bodyLarge,
            color = OnSurfaceMedium,
            textAlign = TextAlign.Start,
            lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * 1.35
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Developed by:",
            style = MaterialTheme.typography.bodyMedium,
            color = OnSurfaceDisabled
        )
        Text(
            text = "Sujan Rai",
            style = MaterialTheme.typography.bodyLarge,
            color = OnBackground
        )
        Text(
            text = "and",
            style = MaterialTheme.typography.bodyMedium,
            color = OnSurfaceDisabled
        )
        TeamClickableLink(
            text = "Blind tech nexus team",
            url = "https://t.me/blindtechvisionary"
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "click continue to start.",
            style = MaterialTheme.typography.bodySmall,
            color = OnSurfaceDisabled,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onContinueClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .semantics { contentDescription = "Continue" },
            colors = ButtonDefaults.buttonColors(containerColor = Primary),
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

@Composable
private fun TeamClickableLink(
    text: String,
    url: String,
    modifier: Modifier = Modifier
) {
    val uriHandler = LocalUriHandler.current
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge,
        color = Primary,
        modifier = modifier
            .semantics { contentDescription = "Blind tech nexus team, opens Telegram" }
            .clickable(role = Role.Button) { uriHandler.openUri(url) }
            .padding(vertical = 4.dp)
    )
}
