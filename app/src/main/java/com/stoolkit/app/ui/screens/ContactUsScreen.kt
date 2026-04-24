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
 * Contact Us screen composable
 */
@Composable
fun ContactUsScreen(
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
        ContactTopBar(
            onBackClick = { navController.popBackStack() },
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Content
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // WhatsApp section
            Text(
                text = "WhatsApp chat",
                style = MaterialTheme.typography.titleLarge,
                color = OnBackground,
                modifier = Modifier.semantics { heading() }
            )
            
            Button(
                onClick = {
                    openWhatsApp(context)
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
                    text = "Open WhatsApp Chat",
                    style = MaterialTheme.typography.titleMedium,
                    color = OnPrimary
                )
            }
            
            Divider(color = Border, thickness = 1.dp)
            
            // Telegram section
            Text(
                text = "Telegram",
                style = MaterialTheme.typography.titleLarge,
                color = OnBackground,
                modifier = Modifier.semantics { heading() }
            )
            
            Button(
                onClick = {
                    openTelegram(context)
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
                    text = "Open Telegram Chat",
                    style = MaterialTheme.typography.titleMedium,
                    color = OnPrimary
                )
            }
            
            Divider(color = Border, thickness = 1.dp)
            
            // YouTube section
            Text(
                text = "YouTube",
                style = MaterialTheme.typography.titleLarge,
                color = OnBackground,
                modifier = Modifier.semantics { heading() }
            )
            
            Button(
                onClick = {
                    openYouTube(context)
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
                    text = "Open YouTube Channel",
                    style = MaterialTheme.typography.titleMedium,
                    color = OnPrimary
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/**
 * Top bar for Contact Us screen
 */
@Composable
fun ContactTopBar(
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
                    text = "Contact Us",
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
                    text = "Contact with us in various social media platforms.",
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
 * Open WhatsApp chat
 */
private fun openWhatsApp(context: Context) {
    try {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("https://wa.me/9779708340992")
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

/**
 * Open Telegram chat
 */
private fun openTelegram(context: Context) {
    try {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("https://t.me/blindtechnexus")
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

/**
 * Open YouTube channel
 */
private fun openYouTube(context: Context) {
    try {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("https://youtube.com/@aiforblinds")
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
