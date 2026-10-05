package com.pupil.app.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.KeyframesSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pupil.app.core.creature.CreatureDialogue
import com.pupil.app.ui.theme.PupilAccentLight
import com.pupil.app.ui.theme.PupilPrimaryLight
import com.pupil.app.ui.theme.StatusMisconception
import com.pupil.app.ui.theme.StatusUnderstood

enum class CreatureState {
    SLEEPING,
    CURIOUS,
    CONFUSED,
    HAPPY,
    PROUD,
    INTERACTIVE,
    FOCUSED,
    EXCITED,
    ANXIOUS,
    // Backward compatibility aliases:
    WAITING,
    EVOLVING
}

/**
 * Finch & Duolingo-inspired friendly learning creature.
 * Rendered entirely on Compose Canvas with expressive faces, blinking,
 * breathing, accessories for each evolution stage, and playful speech bubbles.
 */
@Composable
fun CreatureWidget(
    state: CreatureState,
    level: Int,
    totalXp: Int,
    streakDays: Int,
    modifier: Modifier = Modifier,
    speechBubbleText: String? = null,
    speechBubbleSubtext: String? = null,
    showProgressBar: Boolean = true,
    compact: Boolean = false,
    onCreatureTapped: (() -> Unit)? = null
) {
    val tapCounter = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(0) }
    val tapScale = androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (tapCounter.value > 0) 1.12f else 1f,
        animationSpec = androidx.compose.animation.core.spring(
            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
            stiffness = androidx.compose.animation.core.Spring.StiffnessMedium
        ),
        finishedListener = {
            if (tapCounter.value > 0) tapCounter.value = 0
        },
        label = "creature_tap_scale"
    )

    val effectiveState = when (state) {
        CreatureState.WAITING -> CreatureState.CURIOUS
        CreatureState.EVOLVING -> CreatureState.PROUD
        else -> state
    }

    val stage = CreatureDialogue.getStageForLevel(level)
    val formName = CreatureDialogue.getFormForLevel(level)

    // Progression calculations
    val currentBaseXp = CreatureDialogue.getCurrentLevelBaseXp(level)
    val nextLevelXp = CreatureDialogue.getNextLevelXp(level)
    val xpInLevel = (totalXp - currentBaseXp).coerceAtLeast(0)
    val xpRequiredForLevel = (nextLevelXp - currentBaseXp).coerceAtLeast(1)
    val progress = (xpInLevel.toFloat() / xpRequiredForLevel.toFloat()).coerceIn(0f, 1f)

    // Motion animations
    val transition = rememberInfiniteTransition(label = "creature_motion")

    // Breathing pulse
    val breathScale by transition.animateFloat(
        initialValue = when (effectiveState) {
            CreatureState.FOCUSED -> 0.99f
            CreatureState.EXCITED -> 0.96f
            CreatureState.ANXIOUS -> 0.98f
            else -> 0.97f
        },
        targetValue = when (effectiveState) {
            CreatureState.FOCUSED -> 1.01f
            CreatureState.EXCITED -> 1.05f
            CreatureState.ANXIOUS -> 1.02f
            else -> 1.03f
        },
        animationSpec = infiniteRepeatable(
            animation = tween(
                when (effectiveState) {
                    CreatureState.SLEEPING -> 2400
                    CreatureState.FOCUSED -> 1800
                    CreatureState.EXCITED -> 400
                    CreatureState.ANXIOUS -> 700
                    else -> 1600
                },
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breath"
    )

    // Floating bounce
    val bounceY by transition.animateFloat(
        initialValue = when (effectiveState) {
            CreatureState.EXCITED -> -14f
            CreatureState.HAPPY -> -10f
            CreatureState.ANXIOUS -> -2f
            CreatureState.FOCUSED -> -1f
            CreatureState.INTERACTIVE -> -6f
            else -> -3f
        },
        targetValue = when (effectiveState) {
            CreatureState.EXCITED -> 8f
            CreatureState.HAPPY -> 6f
            CreatureState.ANXIOUS -> 2f
            CreatureState.FOCUSED -> 1f
            CreatureState.INTERACTIVE -> 4f
            else -> 3f
        },
        animationSpec = infiniteRepeatable(
            animation = tween(
                when (effectiveState) {
                    CreatureState.EXCITED -> 320
                    CreatureState.HAPPY -> 450
                    CreatureState.ANXIOUS -> 500
                    CreatureState.FOCUSED -> 1200
                    CreatureState.INTERACTIVE -> 900
                    else -> 1700
                },
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bounce"
    )

    // Head tilt for curiosity/confusion/anxiety
    val headTilt by transition.animateFloat(
        initialValue = when (effectiveState) {
            CreatureState.CONFUSED -> -10f
            CreatureState.ANXIOUS -> -7f
            CreatureState.CURIOUS -> -3f
            CreatureState.INTERACTIVE -> -5f
            CreatureState.EXCITED -> -4f
            else -> -1f
        },
        targetValue = when (effectiveState) {
            CreatureState.CONFUSED -> 12f
            CreatureState.ANXIOUS -> 6f
            CreatureState.CURIOUS -> 4f
            CreatureState.INTERACTIVE -> 5f
            CreatureState.EXCITED -> 4f
            else -> 1f
        },
        animationSpec = infiniteRepeatable(
            animation = tween(
                when (effectiveState) {
                    CreatureState.CONFUSED -> 1200
                    CreatureState.ANXIOUS -> 600
                    CreatureState.EXCITED -> 380
                    CreatureState.INTERACTIVE -> 800
                    else -> 2200
                },
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "tilt"
    )

    // Periodic blinking (every ~3.5 seconds, quick 150ms blink)
    val blinkProgress by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 3600
                1f at 0
                1f at 3300
                0.08f at 3400
                1f at 3500
                1f at 3600
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "blink"
    )

    // Evolution / Pride / Excited shimmer glow
    val shimmerPulse by transition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (effectiveState == CreatureState.EXCITED) 500 else 800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmer"
    )

    // Anxious fidget offset
    val anxiousFidgetX by transition.animateFloat(
        initialValue = -2.5f,
        targetValue = 2.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(220, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "fidget"
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Playful Speech Bubble
        if (!speechBubbleText.isNullOrBlank()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .padding(bottom = 12.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = when (effectiveState) {
                        CreatureState.CONFUSED -> Color(0xFFFFE8E3)
                        CreatureState.ANXIOUS -> Color(0xFFFEF2F2)
                        CreatureState.HAPPY -> Color(0xFFD1FAE5)
                        CreatureState.EXCITED -> Color(0xFFDCFCE7)
                        CreatureState.PROUD -> Color(0xFFFEF3C7)
                        CreatureState.FOCUSED -> Color(0xFFEEF2FF)
                        CreatureState.INTERACTIVE -> Color(0xFFF3E8FF)
                        else -> MaterialTheme.colorScheme.surface
                    }
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(
                        horizontal = if (compact) 12.dp else 18.dp,
                        vertical = if (compact) 8.dp else 12.dp
                    )
                ) {
                    Text(
                        text = speechBubbleText,
                        style = if (compact) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = when (effectiveState) {
                            CreatureState.CONFUSED -> Color(0xFF9C2A10)
                            CreatureState.ANXIOUS -> Color(0xFF991B1B)
                            CreatureState.HAPPY -> Color(0xFF065F46)
                            CreatureState.EXCITED -> Color(0xFF15803D)
                            CreatureState.PROUD -> Color(0xFF92400E)
                            CreatureState.FOCUSED -> Color(0xFF3730A3)
                            CreatureState.INTERACTIVE -> Color(0xFF6B21A8)
                            else -> MaterialTheme.colorScheme.onSurface
                        }
                    )
                    if (!speechBubbleSubtext.isNullOrBlank() && !compact) {
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = speechBubbleSubtext,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        val boxSize = if (compact) 96.dp else 164.dp
        val assetRes = when {
            effectiveState == CreatureState.SLEEPING -> com.pupil.app.R.drawable.bujji_seed_sleeping
            stage == 1 -> com.pupil.app.R.drawable.bujji_seed_curious
            stage == 2 -> com.pupil.app.R.drawable.bujji_sprout_curious
            else -> com.pupil.app.R.drawable.bujji_leafling_curious
        }

        // Official Bujji Mascot Asset Container
        Box(
            modifier = Modifier
                .size(boxSize)
                .scale((if (effectiveState == CreatureState.PROUD || effectiveState == CreatureState.EXCITED) shimmerPulse else breathScale) * tapScale.value)
                .rotate(headTilt)
                .clickable {
                    tapCounter.value++
                    onCreatureTapped?.invoke()
                },
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.foundation.Image(
                painter = androidx.compose.ui.res.painterResource(id = assetRes),
                contentDescription = "Bujji",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(if (compact) 4.dp else 8.dp),
                contentScale = androidx.compose.ui.layout.ContentScale.Fit
            )
        }

        // Status Card: Level, Form Name, XP Bar & Streak
        if (showProgressBar) {
            Spacer(modifier = Modifier.height(12.dp))

            Card(
                modifier = Modifier.fillMaxWidth(0.92f),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "$formName · Lv. $level",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(PupilPrimaryLight.copy(alpha = 0.12f))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = "Stage $stage",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = PupilPrimaryLight
                                )
                            }
                        }

                        // Gentle Streak badge
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(PupilAccentLight.copy(alpha = 0.15f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = "Streak",
                                tint = PupilAccentLight,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "$streakDays d streak",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // XP Progress bar with rounded caps
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = PupilPrimaryLight,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "$xpInLevel / $xpRequiredForLevel XP to Lv. ${level + 1}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "$totalXp XP",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = PupilPrimaryLight
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// Procedural Canvas Drawing Helpers
// ==========================================

private fun DrawScope.drawCreatureAura(
    stage: Int,
    state: CreatureState,
    center: Offset,
    bodyRadius: Float,
    pulse: Float
) {
    if (state == CreatureState.PROUD || state == CreatureState.EXCITED) {
        val auraColor = if (state == CreatureState.EXCITED) Color(0xFF86EFAC).copy(alpha = 0.5f) else Color(0xFFFFD54F).copy(alpha = 0.45f)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(auraColor, Color.Transparent),
                center = center,
                radius = bodyRadius * 1.8f * pulse
            ),
            radius = bodyRadius * 1.8f * pulse,
            center = center
        )
    } else if (state == CreatureState.FOCUSED) {
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFFA5B4FC).copy(alpha = 0.35f), Color.Transparent),
                center = center,
                radius = bodyRadius * 1.45f
            ),
            radius = bodyRadius * 1.45f,
            center = center
        )
    } else if (stage >= 2) {
        val auraColor = if (stage == 3) Color(0xFFFFE082).copy(alpha = 0.28f) else Color(0xFFCE93D8).copy(alpha = 0.22f)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(auraColor, Color.Transparent),
                center = center,
                radius = bodyRadius * 1.4f
            ),
            radius = bodyRadius * 1.4f,
            center = center
        )
    }
}

private fun DrawScope.drawCreatureFeet(center: Offset, bodyRadius: Float) {
    val feetY = center.y + (bodyRadius * 0.92f)
    val feetSpacing = bodyRadius * 0.38f
    val footColor = Color(0xFFFFB074)

    // Left foot
    drawOval(
        color = footColor,
        topLeft = Offset(center.x - feetSpacing - 11f, feetY - 4f),
        size = Size(20f, 12f)
    )
    // Right foot
    drawOval(
        color = footColor,
        topLeft = Offset(center.x + feetSpacing - 9f, feetY - 4f),
        size = Size(20f, 12f)
    )
}

private fun DrawScope.drawCreatureBody(
    stage: Int,
    center: Offset,
    bodyRadius: Float
) {
    // Stage 1: Warm Mint Sage (Pip/Sprout)
    // Stage 2: Soft Violet/Periwinkle (Lumina)
    // Stage 3: Warm Honey Amber (Auron)
    val bodyGradient = when (stage) {
        1 -> listOf(Color(0xFF6EE7B7), Color(0xFF10B981), Color(0xFF047857))
        2 -> listOf(Color(0xFFC4B5FD), Color(0xFF8B5CF6), Color(0xFF6D28D9))
        else -> listOf(Color(0xFFFDE68A), Color(0xFFF59E0B), Color(0xFFD97706))
    }

    // Chubby Finch-like pear/oval body
    drawOval(
        brush = Brush.radialGradient(
            colors = bodyGradient,
            center = Offset(center.x - (bodyRadius * 0.22f), center.y - (bodyRadius * 0.25f)),
            radius = bodyRadius * 1.25f
        ),
        topLeft = Offset(center.x - bodyRadius, center.y - (bodyRadius * 0.95f)),
        size = Size(bodyRadius * 2f, bodyRadius * 1.95f)
    )

    // Subtle soft rim outline
    drawOval(
        color = Color.White.copy(alpha = 0.35f),
        topLeft = Offset(center.x - bodyRadius, center.y - (bodyRadius * 0.95f)),
        size = Size(bodyRadius * 2f, bodyRadius * 1.95f),
        style = Stroke(width = 2.5f)
    )
}

private fun DrawScope.drawCreatureBelly(
    stage: Int,
    center: Offset,
    bodyRadius: Float
) {
    val bellyColor = when (stage) {
        1 -> Color(0xFFF0FDF4).copy(alpha = 0.88f)
        2 -> Color(0xFFFAF5FF).copy(alpha = 0.88f)
        else -> Color(0xFFFFFBEB).copy(alpha = 0.88f)
    }

    val bellyWidth = bodyRadius * 1.15f
    val bellyHeight = bodyRadius * 0.95f
    val bellyX = center.x - (bellyWidth / 2f)
    val bellyY = center.y - (bodyRadius * 0.05f)

    drawOval(
        color = bellyColor,
        topLeft = Offset(bellyX, bellyY),
        size = Size(bellyWidth, bellyHeight)
    )
}

private fun DrawScope.drawCreatureWings(
    state: CreatureState,
    center: Offset,
    bodyRadius: Float
) {
    val wingY = center.y + (bodyRadius * 0.12f)
    val wingColor = Color.White.copy(alpha = 0.38f)
    val isUpwardWings = state == CreatureState.HAPPY || state == CreatureState.EXCITED || state == CreatureState.INTERACTIVE

    // Left wing
    val leftWingPath = Path().apply {
        moveTo(center.x - bodyRadius + 4f, wingY)
        quadraticTo(
            center.x - bodyRadius - (if (state == CreatureState.EXCITED) 18f else 14f),
            if (isUpwardWings) wingY - 14f else wingY + 12f,
            center.x - bodyRadius + 8f,
            wingY + 20f
        )
    }
    drawPath(leftWingPath, color = wingColor, style = Stroke(width = 4f, cap = StrokeCap.Round))

    // Right wing
    val rightWingPath = Path().apply {
        moveTo(center.x + bodyRadius - 4f, wingY)
        quadraticTo(
            center.x + bodyRadius + (if (state == CreatureState.EXCITED) 18f else 14f),
            if (isUpwardWings) wingY - 14f else wingY + 12f,
            center.x + bodyRadius - 8f,
            wingY + 20f
        )
    }
    drawPath(rightWingPath, color = wingColor, style = Stroke(width = 4f, cap = StrokeCap.Round))
}

private fun DrawScope.drawCreatureHeadpiece(
    stage: Int,
    state: CreatureState,
    center: Offset,
    bodyRadius: Float
) {
    val headTopY = center.y - (bodyRadius * 0.92f)

    when (stage) {
        1 -> {
            // Stage 1: Adorable Sprout Leaf
            val stemStart = Offset(center.x, headTopY)
            val stemCurve = Offset(center.x - 3f, headTopY - 12f)
            val stemEnd = Offset(center.x + 2f, headTopY - 20f)

            val stemPath = Path().apply {
                moveTo(stemStart.x, stemStart.y)
                quadraticTo(stemCurve.x, stemCurve.y, stemEnd.x, stemEnd.y)
            }
            drawPath(stemPath, color = Color(0xFF059669), style = Stroke(width = 3.5f, cap = StrokeCap.Round))

            // Main leaf
            val leafPath = Path().apply {
                moveTo(stemEnd.x, stemEnd.y)
                cubicTo(stemEnd.x + 14f, stemEnd.y - 12f, stemEnd.x + 20f, stemEnd.y + 2f, stemEnd.x, stemEnd.y)
            }
            drawPath(leafPath, color = Color(0xFF34D399), style = Fill)
            drawPath(leafPath, color = Color(0xFF059669), style = Stroke(width = 1.5f))

            // Tiny baby bud on the left
            val budPath = Path().apply {
                moveTo(stemEnd.x, stemEnd.y + 3f)
                cubicTo(stemEnd.x - 10f, stemEnd.y - 6f, stemEnd.x - 12f, stemEnd.y + 3f, stemEnd.x, stemEnd.y + 3f)
            }
            drawPath(budPath, color = Color(0xFF6EE7B7), style = Fill)
        }
        2 -> {
            // Stage 2: Soft Feathered Ear-Tufts (wings flanking the sides of the head)
            val leftEarOrigin = Offset(center.x - (bodyRadius * 0.68f), headTopY + 12f)
            val rightEarOrigin = Offset(center.x + (bodyRadius * 0.68f), headTopY + 12f)

            // Left tuft feathers
            val leftTuft = Path().apply {
                moveTo(leftEarOrigin.x, leftEarOrigin.y)
                quadraticTo(leftEarOrigin.x - 22f, leftEarOrigin.y - 20f, leftEarOrigin.x - 26f, leftEarOrigin.y - 6f)
                quadraticTo(leftEarOrigin.x - 16f, leftEarOrigin.y + 10f, leftEarOrigin.x, leftEarOrigin.y)
            }
            drawPath(leftTuft, color = Color(0xFFDDD6FE), style = Fill)
            drawPath(leftTuft, color = Color(0xFF8B5CF6), style = Stroke(width = 2f, cap = StrokeCap.Round))

            // Right tuft feathers
            val rightTuft = Path().apply {
                moveTo(rightEarOrigin.x, rightEarOrigin.y)
                quadraticTo(rightEarOrigin.x + 22f, rightEarOrigin.y - 20f, rightEarOrigin.x + 26f, rightEarOrigin.y - 6f)
                quadraticTo(rightEarOrigin.x + 16f, rightEarOrigin.y + 10f, rightEarOrigin.x, rightEarOrigin.y)
            }
            drawPath(rightTuft, color = Color(0xFFDDD6FE), style = Fill)
            drawPath(rightTuft, color = Color(0xFF8B5CF6), style = Stroke(width = 2f, cap = StrokeCap.Round))
        }
        3 -> {
            // Stage 3: Celestial Knowledge Crown & Halo
            val crownCenter = Offset(center.x, headTopY - 14f)

            // Gentle golden halo
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFFDE68A).copy(alpha = 0.5f), Color.Transparent),
                    center = crownCenter,
                    radius = 28f
                ),
                radius = 28f,
                center = crownCenter
            )

            // 3-pointed celestial golden crown
            val crownPath = Path().apply {
                moveTo(crownCenter.x - 18f, crownCenter.y + 8f)
                lineTo(crownCenter.x - 14f, crownCenter.y - 6f) // left point
                lineTo(crownCenter.x - 6f, crownCenter.y + 2f)
                lineTo(crownCenter.x, crownCenter.y - 12f)      // high center point
                lineTo(crownCenter.x + 6f, crownCenter.y + 2f)
                lineTo(crownCenter.x + 14f, crownCenter.y - 6f) // right point
                lineTo(crownCenter.x + 18f, crownCenter.y + 8f)
                close()
            }
            drawPath(crownPath, color = Color(0xFFF59E0B), style = Fill)
            drawPath(crownPath, color = Color(0xFFD97706), style = Stroke(width = 2f, join = StrokeJoin.Round))

            // Crown jewel gems
            drawCircle(color = Color.White, radius = 2.5f, center = Offset(crownCenter.x, crownCenter.y - 10f))
            drawCircle(color = Color(0xFF818CF8), radius = 2f, center = Offset(crownCenter.x - 13f, crownCenter.y - 4f))
            drawCircle(color = Color(0xFF818CF8), radius = 2f, center = Offset(crownCenter.x + 13f, crownCenter.y - 4f))
        }
    }
}

private fun DrawScope.drawCreatureEyes(
    state: CreatureState,
    blinkProgress: Float,
    center: Offset,
    bodyRadius: Float
) {
    val eyeSpacing = bodyRadius * 0.38f
    val eyeY = center.y - (bodyRadius * 0.16f)
    val leftEyeCenter = Offset(center.x - eyeSpacing, eyeY)
    val rightEyeCenter = Offset(center.x + eyeSpacing, eyeY)

    if (state == CreatureState.SLEEPING || (blinkProgress < 0.25f && state != CreatureState.FOCUSED && state != CreatureState.EXCITED)) {
        // Peaceful curved closed arcs: ⌒ ⌒
        val leftArc = Path().apply {
            moveTo(leftEyeCenter.x - 9f, leftEyeCenter.y + 2f)
            quadraticTo(leftEyeCenter.x, leftEyeCenter.y - 7f, leftEyeCenter.x + 9f, leftEyeCenter.y + 2f)
        }
        val rightArc = Path().apply {
            moveTo(rightEyeCenter.x - 9f, rightEyeCenter.y + 2f)
            quadraticTo(rightEyeCenter.x, rightEyeCenter.y - 7f, rightEyeCenter.x + 9f, rightEyeCenter.y + 2f)
        }
        drawPath(leftArc, color = Color(0xFF1E293B), style = Stroke(width = 3.5f, cap = StrokeCap.Round))
        drawPath(rightArc, color = Color(0xFF1E293B), style = Stroke(width = 3.5f, cap = StrokeCap.Round))
        return
    }

    when (state) {
        CreatureState.HAPPY, CreatureState.EXCITED -> {
            // Joyful upward crescents: ^ ^ with sparkle glints for EXCITED
            val leftArc = Path().apply {
                moveTo(leftEyeCenter.x - 10f, leftEyeCenter.y + 3f)
                quadraticTo(leftEyeCenter.x, leftEyeCenter.y - (if (state == CreatureState.EXCITED) 10f else 8f), leftEyeCenter.x + 10f, leftEyeCenter.y + 3f)
            }
            val rightArc = Path().apply {
                moveTo(rightEyeCenter.x - 10f, rightEyeCenter.y + 3f)
                quadraticTo(rightEyeCenter.x, rightEyeCenter.y - (if (state == CreatureState.EXCITED) 10f else 8f), rightEyeCenter.x + 10f, rightEyeCenter.y + 3f)
            }
            drawPath(leftArc, color = Color(0xFF1E293B), style = Stroke(width = 4.2f, cap = StrokeCap.Round))
            drawPath(rightArc, color = Color(0xFF1E293B), style = Stroke(width = 4.2f, cap = StrokeCap.Round))

            if (state == CreatureState.EXCITED) {
                // Tiny star sparkle above eye
                drawStar(Offset(leftEyeCenter.x - 4f, leftEyeCenter.y - 10f), 3.5f, Color(0xFFFFD54F))
                drawStar(Offset(rightEyeCenter.x + 4f, rightEyeCenter.y - 10f), 3.5f, Color(0xFFFFD54F))
            }
        }

        CreatureState.FOCUSED -> {
            // Attentive, alert gaze: listening intently with focused pupils and specular reflections
            val radius = 8.5f
            drawCircle(color = Color(0xFF1E293B), radius = radius, center = leftEyeCenter)
            drawCircle(color = Color(0xFF1E293B), radius = radius, center = rightEyeCenter)

            // High specular highlights towards center (listening intently)
            drawCircle(color = Color.White, radius = 3.2f, center = Offset(leftEyeCenter.x + 1.5f, leftEyeCenter.y - 2f))
            drawCircle(color = Color.White, radius = 3.2f, center = Offset(rightEyeCenter.x - 1.5f, rightEyeCenter.y - 2f))

            // Attentive gentle focus eyebrows
            val leftBrow = Path().apply {
                moveTo(leftEyeCenter.x - 8f, leftEyeCenter.y - 10f)
                lineTo(leftEyeCenter.x + 6f, leftEyeCenter.y - 9f)
            }
            val rightBrow = Path().apply {
                moveTo(rightEyeCenter.x - 6f, rightEyeCenter.y - 9f)
                lineTo(rightEyeCenter.x + 8f, rightEyeCenter.y - 10f)
            }
            drawPath(leftBrow, color = Color(0xFF1E293B), style = Stroke(width = 2.5f, cap = StrokeCap.Round))
            drawPath(rightBrow, color = Color(0xFF1E293B), style = Stroke(width = 2.5f, cap = StrokeCap.Round))
        }

        CreatureState.ANXIOUS -> {
            // Worried, questioning eyes with tilted anxious brows
            val radius = 8.8f
            drawCircle(color = Color(0xFF1E293B), radius = radius, center = leftEyeCenter)
            drawCircle(color = Color(0xFF1E293B), radius = radius, center = rightEyeCenter)

            // Specular reflection shifted down
            drawCircle(color = Color.White, radius = 2.8f, center = Offset(leftEyeCenter.x - 2f, leftEyeCenter.y - 2f))
            drawCircle(color = Color.White, radius = 2.8f, center = Offset(rightEyeCenter.x - 2f, rightEyeCenter.y - 2f))

            // Anxious slanted eyebrows (/ \)
            val leftBrow = Path().apply {
                moveTo(leftEyeCenter.x - 8f, leftEyeCenter.y - 9f)
                lineTo(leftEyeCenter.x + 7f, leftEyeCenter.y - 13f)
            }
            val rightBrow = Path().apply {
                moveTo(rightEyeCenter.x - 7f, rightEyeCenter.y - 13f)
                lineTo(rightEyeCenter.x + 8f, rightEyeCenter.y - 9f)
            }
            drawPath(leftBrow, color = Color(0xFF1E293B), style = Stroke(width = 3f, cap = StrokeCap.Round))
            drawPath(rightBrow, color = Color(0xFF1E293B), style = Stroke(width = 3f, cap = StrokeCap.Round))

            // Nervous sweat drop on upper right forehead
            drawNervousSweatDrop(center, bodyRadius)
        }

        CreatureState.CONFUSED -> {
            // One big curious eye, one quizzical tilted squint with raised eyebrow
            drawCircle(color = Color(0xFF1E293B), radius = 9f, center = leftEyeCenter)
            drawCircle(color = Color.White, radius = 3.5f, center = Offset(leftEyeCenter.x + 2f, leftEyeCenter.y - 2.5f))

            // Right eye quizzical squint
            val squint = Path().apply {
                moveTo(rightEyeCenter.x - 8f, rightEyeCenter.y - 3f)
                lineTo(rightEyeCenter.x + 8f, rightEyeCenter.y + 3f)
            }
            drawPath(squint, color = Color(0xFF1E293B), style = Stroke(width = 3.5f, cap = StrokeCap.Round))

            // Raised eyebrow over right eye
            val eyebrow = Path().apply {
                moveTo(rightEyeCenter.x - 8f, rightEyeCenter.y - 10f)
                quadraticTo(rightEyeCenter.x, rightEyeCenter.y - 14f, rightEyeCenter.x + 8f, rightEyeCenter.y - 11f)
            }
            drawPath(eyebrow, color = Color(0xFF1E293B), style = Stroke(width = 2.5f, cap = StrokeCap.Round))
        }

        CreatureState.PROUD -> {
            // Confident sparkly eyes with high specular glint
            val radius = 9f
            drawCircle(color = Color(0xFF1E293B), radius = radius, center = leftEyeCenter)
            drawCircle(color = Color(0xFF1E293B), radius = radius, center = rightEyeCenter)

            // Sparkle star glints
            drawCircle(color = Color.White, radius = 3.5f, center = Offset(leftEyeCenter.x - 2f, leftEyeCenter.y - 2.5f))
            drawCircle(color = Color.White, radius = 1.8f, center = Offset(leftEyeCenter.x + 3f, leftEyeCenter.y + 2f))
            drawCircle(color = Color.White, radius = 3.5f, center = Offset(rightEyeCenter.x - 2f, rightEyeCenter.y - 2.5f))
            drawCircle(color = Color.White, radius = 1.8f, center = Offset(rightEyeCenter.x + 3f, rightEyeCenter.y + 2f))
        }

        else -> {
            // CURIOUS / Default: Large adorable chibi eyes with double specular reflections
            val radius = 9.5f * blinkProgress.coerceAtLeast(0.3f)
            drawOval(
                color = Color(0xFF1E293B),
                topLeft = Offset(leftEyeCenter.x - radius, leftEyeCenter.y - radius),
                size = Size(radius * 2f, radius * 2f)
            )
            drawOval(
                color = Color(0xFF1E293B),
                topLeft = Offset(rightEyeCenter.x - radius, rightEyeCenter.y - radius),
                size = Size(radius * 2f, radius * 2f)
            )

            // Primary shine (top-left)
            drawCircle(color = Color.White, radius = 3.2f, center = Offset(leftEyeCenter.x - 2f, leftEyeCenter.y - 2.5f))
            drawCircle(color = Color.White, radius = 3.2f, center = Offset(rightEyeCenter.x - 2f, rightEyeCenter.y - 2.5f))

            // Secondary shine (bottom-right)
            drawCircle(color = Color.White, radius = 1.5f, center = Offset(leftEyeCenter.x + 3f, leftEyeCenter.y + 2.5f))
            drawCircle(color = Color.White, radius = 1.5f, center = Offset(rightEyeCenter.x + 3f, rightEyeCenter.y + 2.5f))
        }
    }
}

private fun DrawScope.drawCheeks(
    state: CreatureState,
    center: Offset,
    bodyRadius: Float
) {
    val cheekY = center.y - (bodyRadius * 0.02f)
    val cheekSpacing = bodyRadius * 0.58f
    val blushColor = Color(0xFFFF8597).copy(alpha = if (state == CreatureState.HAPPY || state == CreatureState.EXCITED || state == CreatureState.PROUD || state == CreatureState.INTERACTIVE) 0.60f else 0.38f)

    drawOval(
        color = blushColor,
        topLeft = Offset(center.x - cheekSpacing - 8f, cheekY - 4f),
        size = Size(16f, 10f)
    )
    drawOval(
        color = blushColor,
        topLeft = Offset(center.x + cheekSpacing - 8f, cheekY - 4f),
        size = Size(16f, 10f)
    )
}

private fun DrawScope.drawCreatureMouth(
    state: CreatureState,
    center: Offset,
    bodyRadius: Float
) {
    val mouthY = center.y + (bodyRadius * 0.14f)

    when (state) {
        CreatureState.HAPPY, CreatureState.EXCITED -> {
            // Big open joyful smile with tongue
            val mouthDepth = if (state == CreatureState.EXCITED) 14f else 12f
            val mouthPath = Path().apply {
                moveTo(center.x - 10f, mouthY)
                quadraticTo(center.x, mouthY + mouthDepth, center.x + 10f, mouthY)
                close()
            }
            drawPath(mouthPath, color = Color(0xFF1E293B), style = Fill)

            // Cute pink tongue
            val tonguePath = Path().apply {
                moveTo(center.x - 6f, mouthY + 5f)
                quadraticTo(center.x, mouthY + (mouthDepth - 1f), center.x + 6f, mouthY + 5f)
            }
            drawPath(tonguePath, color = Color(0xFFFF70A6), style = Fill)
        }

        CreatureState.ANXIOUS -> {
            // Nervous hesitant wavy mouth ~_~
            val mouthPath = Path().apply {
                moveTo(center.x - 7f, mouthY + 3f)
                quadraticTo(center.x - 3f, mouthY - 1f, center.x, mouthY + 2f)
                quadraticTo(center.x + 4f, mouthY + 5f, center.x + 7f, mouthY + 2f)
            }
            drawPath(mouthPath, color = Color(0xFF1E293B), style = Stroke(width = 2.8f, cap = StrokeCap.Round))
        }

        CreatureState.FOCUSED -> {
            // Small attentive concentrated "o" mouth listening carefully
            drawOval(
                color = Color(0xFF1E293B),
                topLeft = Offset(center.x - 3.5f, mouthY),
                size = Size(7f, 6.5f)
            )
        }

        CreatureState.CONFUSED -> {
            // Puzzled squiggly mouth ~
            val mouthPath = Path().apply {
                moveTo(center.x - 8f, mouthY + 2f)
                quadraticTo(center.x - 4f, mouthY - 3f, center.x, mouthY + 2f)
                quadraticTo(center.x + 4f, mouthY + 5f, center.x + 8f, mouthY + 1f)
            }
            drawPath(mouthPath, color = Color(0xFF1E293B), style = Stroke(width = 3f, cap = StrokeCap.Round))
        }

        CreatureState.SLEEPING -> {
            // Tiny peaceful mouth
            val mouthPath = Path().apply {
                moveTo(center.x - 4f, mouthY)
                lineTo(center.x + 4f, mouthY)
            }
            drawPath(mouthPath, color = Color(0xFF1E293B), style = Stroke(width = 2.5f, cap = StrokeCap.Round))
        }

        CreatureState.PROUD, CreatureState.INTERACTIVE -> {
            // Warm confident smile
            val mouthPath = Path().apply {
                moveTo(center.x - 8f, mouthY)
                quadraticTo(center.x, mouthY + 7f, center.x + 8f, mouthY)
            }
            drawPath(mouthPath, color = Color(0xFF1E293B), style = Stroke(width = 3.5f, cap = StrokeCap.Round))
        }

        else -> {
            // Sweet Finch beak / smile
            val mouthPath = Path().apply {
                moveTo(center.x - 6f, mouthY)
                quadraticTo(center.x, mouthY + 5f, center.x + 6f, mouthY)
            }
            drawPath(mouthPath, color = Color(0xFF1E293B), style = Stroke(width = 3f, cap = StrokeCap.Round))
        }
    }
}

private fun DrawScope.drawNervousSweatDrop(center: Offset, bodyRadius: Float) {
    val dropX = center.x + (bodyRadius * 0.72f)
    val dropY = center.y - (bodyRadius * 0.58f)
    val dropColor = Color(0xFF38BDF8)

    val sweatPath = Path().apply {
        moveTo(dropX, dropY - 8f)
        cubicTo(dropX + 5f, dropY - 2f, dropX + 5f, dropY + 5f, dropX, dropY + 5f)
        cubicTo(dropX - 5f, dropY + 5f, dropX - 5f, dropY - 2f, dropX, dropY - 8f)
        close()
    }
    drawPath(sweatPath, color = dropColor, style = Fill)
    drawPath(sweatPath, color = Color(0xFF0284C7), style = Stroke(width = 1.2f))
}

private fun DrawScope.drawSleepZzz(center: Offset, bodyRadius: Float) {
    val zColor = Color(0xFF94A3B8)
    val startX = center.x + (bodyRadius * 0.72f)
    val startY = center.y - (bodyRadius * 0.45f)

    // Small z
    drawZLetter(startX, startY, 7f, zColor)
    // Medium z
    drawZLetter(startX + 10f, startY - 12f, 10f, zColor.copy(alpha = 0.8f))
}

private fun DrawScope.drawZLetter(x: Float, y: Float, size: Float, color: Color) {
    val path = Path().apply {
        moveTo(x, y)
        lineTo(x + size, y)
        lineTo(x, y + size)
        lineTo(x + size, y + size)
    }
    drawPath(path, color = color, style = Stroke(width = 2.5f, cap = StrokeCap.Round, join = StrokeJoin.Round))
}

private fun DrawScope.drawSparkles(center: Offset, bodyRadius: Float, pulse: Float) {
    val starColor = Color(0xFFFFD54F)
    val leftStar = Offset(center.x - (bodyRadius * 1.15f), center.y - (bodyRadius * 0.5f))
    val rightStar = Offset(center.x + (bodyRadius * 1.15f), center.y - (bodyRadius * 0.5f))

    drawStar(leftStar, 7f * pulse, starColor)
    drawStar(rightStar, 8f * pulse, starColor)
}

private fun DrawScope.drawStar(center: Offset, radius: Float, color: Color) {
    val path = Path().apply {
        moveTo(center.x, center.y - radius)
        quadraticTo(center.x, center.y, center.x + radius, center.y)
        quadraticTo(center.x, center.y, center.x, center.y + radius)
        quadraticTo(center.x, center.y, center.x - radius, center.y)
        quadraticTo(center.x, center.y, center.x, center.y - radius)
        close()
    }
    drawPath(path, color = color, style = Fill)
}
