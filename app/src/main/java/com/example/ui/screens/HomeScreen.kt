package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ChatMessage
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun HomeScreen(
    onNavigateToSettings: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToPermissions: () -> Unit,
    isListening: Boolean,
    isProcessing: Boolean,
    onStartListening: () -> Unit,
    onStopListening: (String?) -> Unit,
    latestMessage: ChatMessage?,
    onTriggerPopup: () -> Unit
) {
    val gradientBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFFFFF5F8),
            Color(0xFFFFDDEB)
        )
    )

    val context = LocalContext.current
    val recordAudioPermission = rememberPermissionState(Manifest.permission.RECORD_AUDIO)
    val currentOnStopListening by rememberUpdatedState(onStopListening)

    Scaffold(
        topBar = {
            HomeTopBar(
                onNavigateToSettings = onNavigateToSettings, 
                onNavigateToHistory = onNavigateToHistory,
                onNavigateToPermissions = onNavigateToPermissions
            )
        },
        containerColor = Color.Transparent,
        modifier = Modifier.background(gradientBrush)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "Welcome,",
                    style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.ExtraLight),
                    color = Color(0xFF4A4A4A)
                )
                Text(
                    text = "Boss.",
                    style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFFFF6FA5)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (isProcessing) "Processing..." else if (isListening) "Listening..." else "Say 'Hello Love' or tap orb",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = Color(0xFF94A3B8)
                )
            }

            VoiceOrb(
                isListening = isListening || isProcessing,
                onClick = {
                    if (recordAudioPermission.status.isGranted) {
                        onTriggerPopup()
                    } else {
                        recordAudioPermission.launchPermissionRequest()
                    }
                },
                modifier = Modifier.size(260.dp)
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            if (latestMessage != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White.copy(alpha = 0.5f), RoundedCornerShape(32.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.8f), RoundedCornerShape(32.dp))
                        .padding(16.dp)
                ) {
                    Column {
                        Text(
                            text = "LATEST COMMAND",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = 2.sp
                            ),
                            color = Color(0xFFFF6FA5).copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (latestMessage.sender == "User") "\"${latestMessage.message}\"" else latestMessage.message,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF4A4A4A),
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeTopBar(onNavigateToSettings: () -> Unit, onNavigateToHistory: () -> Unit, onNavigateToPermissions: () -> Unit) {
    TopAppBar(
        title = {
            Column {
                Text(
                    text = "LOVE",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-1).sp
                    ),
                    color = Color(0xFFFF6FA5)
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(Color(0xFF4ADE80), shape = CircleShape)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "ALWAYS READY",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp
                        ),
                        color = Color.Black.copy(alpha = 0.4f)
                    )
                }
            }
        },
        actions = {
            GlassButton(onClick = onNavigateToPermissions, icon = Icons.Default.Security)
            Spacer(modifier = Modifier.width(8.dp))
            GlassButton(onClick = onNavigateToHistory, icon = Icons.Default.History)
            Spacer(modifier = Modifier.width(8.dp))
            GlassButton(onClick = onNavigateToSettings, icon = Icons.Default.Settings)
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent
        ),
        modifier = Modifier.padding(horizontal = 16.dp)
    )
}

@Composable
fun GlassButton(onClick: () -> Unit, icon: ImageVector) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .background(Color.White.copy(alpha = 0.4f), shape = RoundedCornerShape(16.dp))
            .border(1.dp, Color.White.copy(alpha = 0.6f), shape = RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFF4A4A4A)
        )
    }
}

@Composable
fun VoiceOrb(isListening: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "orb_pulse")
    
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = if (isListening) 1.1f else 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isListening) 600 else 2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )
    
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 10000, easing = LinearEasing)
        ),
        label = "rotation"
    )

    Box(
        modifier = modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize().scale(pulseScale)) {
            val center = Offset(size.width / 2, size.height / 2)
            val baseRadius = size.minDimension / 2 * 0.9f
            
            // Outer dashed border
            drawCircle(
                color = Color.White.copy(alpha = 0.6f),
                radius = baseRadius,
                center = center,
                style = Stroke(
                    width = 2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 20f), 0f)
                )
            )
            
            // Rotating outer thin border
            rotate(rotation, center) {
                drawCircle(
                    color = Color(0xFFFF6FA5).copy(alpha = 0.4f),
                    radius = baseRadius + 15.dp.toPx(),
                    center = center,
                    style = Stroke(width = 1.dp.toPx())
                )
            }

            // Inner solid orb with gradient
            val orbRadius = baseRadius * 0.75f
            drawCircle(
                brush = Brush.linearGradient(
                    colors = listOf(Color.White, Color(0xFFFFB6C1), Color(0xFFFF6FA5)),
                    start = Offset(center.x - orbRadius, center.y - orbRadius),
                    end = Offset(center.x + orbRadius, center.y + orbRadius)
                ),
                radius = orbRadius,
                center = center
            )
            
            // White border for orb
            drawCircle(
                color = Color.White.copy(alpha = 0.6f),
                radius = orbRadius,
                center = center,
                style = Stroke(width = 4.dp.toPx())
            )
        }
    }
}

