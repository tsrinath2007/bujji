package com.pupil.app.ui.components

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

@Composable
fun AudioMicButton(
    isListening: Boolean,
    onStartListening: () -> Unit,
    onStopListening: () -> Unit,
    onPermissionDenied: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            onStartListening()
        } else {
            onPermissionDenied()
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = if (isListening) 0.05f else 0.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isListening) 1.28f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    val barTransition = rememberInfiniteTransition(label = "hologram_bars")
    val barHeight1 by barTransition.animateFloat(
        initialValue = 8f, targetValue = 28f,
        animationSpec = infiniteRepeatable(tween(380, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "bh1"
    )
    val barHeight2 by barTransition.animateFloat(
        initialValue = 18f, targetValue = 38f,
        animationSpec = infiniteRepeatable(tween(480, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "bh2"
    )
    val barHeight3 by barTransition.animateFloat(
        initialValue = 12f, targetValue = 32f,
        animationSpec = infiniteRepeatable(tween(320, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "bh3"
    )

    val buttonColor by androidx.compose.animation.animateColorAsState(
        targetValue = if (isListening) Color(0xFFEF4444) else MaterialTheme.colorScheme.primary,
        animationSpec = tween(300),
        label = "button_color"
    )

    // Dedicated container with holographic audio waveform visualizer
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(76.dp)
    ) {
        if (isListening) {
            // Holographic ambient halo ring
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .scale(pulseScale)
                    .background(Color(0xFFEF4444).copy(alpha = pulseAlpha), CircleShape)
            )

            // Holographic sound wave bars surrounding the mic button
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 2.dp)
            ) {
                listOf(barHeight1, barHeight2, barHeight3, barHeight2, barHeight1).forEach { barH ->
                    Box(
                        modifier = Modifier
                            .width(3.dp)
                            .height(barH.dp)
                            .background(
                                color = Color(0xFFEF4444).copy(alpha = 0.85f),
                                shape = RoundedCornerShape(2.dp)
                            )
                    )
                }
            }
        }

        FloatingActionButton(
            onClick = {
                if (isListening) {
                    onStopListening()
                } else {
                    val hasPerm = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.RECORD_AUDIO
                    ) == PackageManager.PERMISSION_GRANTED
                    if (hasPerm) {
                        onStartListening()
                    } else {
                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                }
            },
            containerColor = buttonColor,
            contentColor = Color.White,
            shape = CircleShape,
            modifier = Modifier.size(56.dp),
            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp, pressedElevation = 8.dp)
        ) {
            androidx.compose.animation.AnimatedContent(
                targetState = isListening,
                label = "mic_icon"
            ) { listening ->
                if (listening) {
                    Icon(
                        imageVector = Icons.Default.Stop,
                        contentDescription = "Stop voice input",
                        modifier = Modifier.size(26.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Start voice input",
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        }
    }
}
