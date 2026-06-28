package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun LoveAssistantPopup(
    isVisible: Boolean,
    isListening: Boolean,
    isProcessing: Boolean,
    actionExecuting: String? = null,
    onMicClick: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition()
    
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isListening || isProcessing) 1.2f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    val waveHeight1 by infiniteTransition.animateFloat(
        initialValue = 12f,
        targetValue = if (isListening || isProcessing) 24f else 12f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )
    val waveHeight2 by infiniteTransition.animateFloat(
        initialValue = 20f,
        targetValue = if (isListening || isProcessing) 10f else 20f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )
    val waveHeight3 by infiniteTransition.animateFloat(
        initialValue = 12f,
        targetValue = if (isListening || isProcessing) 24f else 12f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(initialOffsetY = { it }, animationSpec = tween(400, easing = FastOutSlowInEasing)) + fadeIn(tween(400)),
        exit = slideOutVertically(targetOffsetY = { it }, animationSpec = tween(300)) + fadeOut(tween(300)),
        modifier = modifier.fillMaxWidth().wrapContentHeight()
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().wrapContentHeight(),
            contentAlignment = Alignment.BottomCenter
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .padding(bottom = 24.dp)
                    .clip(RoundedCornerShape(32.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color(0xFF2C3E50), Color(0xFF1A1A1A))
                        )
                    )
                    .border(
                        width = 1.dp,
                        color = Color.White.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(32.dp)
                    )
                    .clickable(enabled = false) {}
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(bottom = 12.dp)) {
                // Drag Handle
                Box(
                    modifier = Modifier
                        .padding(top = 12.dp, bottom = 8.dp)
                        .fillMaxWidth()
                        .height(24.dp)
                        .clickable { onClose() },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .width(40.dp)
                            .height(4.dp)
                            .background(Color.White.copy(alpha = 0.3f), CircleShape)
                    )
                }
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp)
                    ) {
                        // Sparkle Icon
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Assistant",
                            tint = Color(0xFFFF6FA5),
                            modifier = Modifier.size(28.dp).scale(if(isProcessing) pulseScale else 1f)
                        )
                        
                        Spacer(modifier = Modifier.width(16.dp))
                        
                        // Text
                        Text(
                            text = if (actionExecuting != null) "Opening $actionExecuting..." else if (isProcessing) "Processing..." else if (isListening) "Speak now" else "Hi Boss, Ask Love",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 18.sp
                            ),
                            color = Color.White,
                            modifier = Modifier.weight(1f)
                        )
                        
                        if (actionExecuting != null) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp).padding(end = 16.dp),
                                color = Color(0xFFFF6FA5),
                                strokeWidth = 2.dp
                            )
                        } else {
                            // Wave icon
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.height(32.dp).padding(end = 16.dp)
                            ) {
                                Box(modifier = Modifier.width(4.dp).height(waveHeight1.dp).background(Color(0xFFFF6FA5), CircleShape))
                                Box(modifier = Modifier.width(4.dp).height(waveHeight2.dp).background(Color(0xFFFF6FA5), CircleShape))
                                Box(modifier = Modifier.width(4.dp).height(waveHeight3.dp).background(Color(0xFFFF6FA5), CircleShape))
                            }
                        }
                        
                        // Mic Button
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isListening) Color(0xFFFF6FA5)
                                    else Color.White.copy(alpha = 0.1f)
                                )
                                .border(
                                    width = 1.dp,
                                    color = if(isListening) Color.Transparent else Color.White.copy(alpha = 0.2f),
                                    shape = CircleShape
                                )
                                .clickable { onMicClick() },
                            contentAlignment = Alignment.Center
                        ) {
                            Box(modifier = Modifier.matchParentSize().scale(if (isListening) pulseScale else 1f).background(if(isListening) Color(0xFFFF6FA5).copy(alpha = 0.3f) else Color.Transparent, CircleShape))
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "Mic",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp).scale(if(isListening) pulseScale else 1f)
                            )
                        }
                    }
                }
            }
        }
    }
}
