package com.pupil.app.ui.screens.home

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import com.pupil.app.core.creature.CreatureDialogue
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.pupil.app.core.AppConstants
import com.pupil.app.data.model.GradingStatus
import com.pupil.app.ui.components.CreatureState
import com.pupil.app.ui.components.CreatureWidget
import com.pupil.app.ui.theme.PupilAccentLight
import com.pupil.app.ui.theme.PupilPrimaryLight
import com.pupil.app.ui.theme.StatusUnderstood

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onTeachNowClick: (subjectId: String?) -> Unit,
    onOpenGraphClick: (subjectId: String?) -> Unit,
    onOpenLibraryClick: () -> Unit,
    onImportClick: () -> Unit,
    onOpenSettingsClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val creature by viewModel.creature.collectAsState()
    val latestSubject by viewModel.latestSubject.collectAsState()
    val allSubjects by viewModel.allSubjects.collectAsState()
    val allConcepts by viewModel.allConcepts.collectAsState()

    val totalConcepts = allConcepts.size
    val understoodConcepts = allConcepts.count { it.status == GradingStatus.UNDERSTOOD }
    val dueConcepts = remember(allConcepts) {
        allConcepts.filter { it.status != GradingStatus.UNDERSTOOD && it.status != GradingStatus.UNSTUDIED }
    }
    val activeSubject = latestSubject ?: allSubjects.firstOrNull()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {


            // Greeting: "Good morning\nLet's learn something new."
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "Good morning",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Let's learn something new.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Top Badges Row: "Lv 4" (blue icon) and "3 day streak" (flame icon)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Level Chip
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(PupilPrimaryLight.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = null,
                                tint = PupilPrimaryLight,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Lv ${creature.level.coerceAtLeast(1)}",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Streak Chip
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFFEDD5)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocalFireDepartment,
                                contentDescription = null,
                                tint = Color(0xFFF97316),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${creature.streakDays.coerceAtLeast(1)} day streak",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Interactive Creature Centerpiece with realistic multi-tap reactions
            var tapCount by remember { mutableStateOf(0) }
            var customSpeechText by remember { mutableStateOf<String?>(null) }
            var creatureReactionState by remember { mutableStateOf<CreatureState?>(null) }
            val baseState = when {
                dueConcepts.isNotEmpty() -> CreatureState.ANXIOUS
                allSubjects.isEmpty() -> CreatureState.SLEEPING
                else -> CreatureState.INTERACTIVE
            }

            val homeCreatureState = creatureReactionState ?: baseState

            val defaultSpeech = when (homeCreatureState) {
                CreatureState.SLEEPING -> "Zzz... Feed me knowledge to wake up!"
                CreatureState.ANXIOUS -> "We have ${dueConcepts.size} revision items due! Shall we review?"
                CreatureState.EXCITED -> "Yay! Let's learn something fun!"
                CreatureState.INTERACTIVE -> "Hi! Ready to teach me today?"
                else -> "Ready to study!"
            }

            CreatureWidget(
                state = homeCreatureState,
                level = creature.level,
                totalXp = creature.totalXp,
                streakDays = creature.streakDays,
                speechBubbleText = customSpeechText ?: defaultSpeech,
                showProgressBar = false,
                compact = false,
                onCreatureTapped = {
                    tapCount++
                    // Realistic, expressive reaction cycle
                    when {
                        // Tapped rapidly many times in succession (> 6 times)
                        tapCount > 6 -> {
                            creatureReactionState = CreatureState.CURIOUS
                            val idx = (tapCount - 7) % CreatureDialogue.RAPID_TAP_REACTIONS.size
                            customSpeechText = CreatureDialogue.RAPID_TAP_REACTIONS[idx]
                        }
                        // Currently sleeping
                        baseState == CreatureState.SLEEPING -> {
                            val idx = (tapCount - 1) % CreatureDialogue.SLEEPING_TAP_REACTIONS.size
                            customSpeechText = CreatureDialogue.SLEEPING_TAP_REACTIONS[idx]
                            creatureReactionState = if (tapCount >= 4) CreatureState.CURIOUS else CreatureState.SLEEPING
                        }
                        // Currently anxious about revision
                        baseState == CreatureState.ANXIOUS -> {
                            val idx = (tapCount - 1) % CreatureDialogue.ANXIOUS_TAP_REACTIONS.size
                            customSpeechText = CreatureDialogue.ANXIOUS_TAP_REACTIONS[idx]
                            creatureReactionState = if (tapCount % 2 == 0) CreatureState.FOCUSED else CreatureState.ANXIOUS
                        }
                        // Regular interactive state
                        else -> {
                            if (tapCount % 2 == 1) {
                                creatureReactionState = CreatureState.EXCITED
                                val idx = (tapCount / 2) % CreatureDialogue.EXCITED_TAP_REACTIONS.size
                                customSpeechText = CreatureDialogue.EXCITED_TAP_REACTIONS[idx]
                            } else {
                                creatureReactionState = CreatureState.INTERACTIVE
                                val idx = (tapCount / 2) % CreatureDialogue.INTERACTIVE_TAP_REACTIONS.size
                                customSpeechText = CreatureDialogue.INTERACTIVE_TAP_REACTIONS[idx]
                            }
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Primary Blue Button: "Teach me"
            Button(
                onClick = {
                    if (activeSubject != null) {
                        onTeachNowClick(activeSubject.id)
                    } else {
                        onOpenLibraryClick()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PupilPrimaryLight,
                    contentColor = Color.White
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
            ) {
                Text(
                    text = "Teach me",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Active Subject Card below button (Screen 2: Data Communication)
            if (activeSubject != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onTeachNowClick(activeSubject.id) },
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(PupilPrimaryLight.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = activeSubject.emoji.ifBlank { "📚" },
                                    fontSize = 20.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = activeSubject.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (totalConcepts > 0) "$understoodConcepts of $totalConcepts understood" else "Tap to open",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Circular Mini Coverage Progress Ring
                        val progressFraction = if (totalConcepts > 0) understoodConcepts.toFloat() / totalConcepts.toFloat() else 0f
                        Box(
                            modifier = Modifier.size(36.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(modifier = Modifier.size(32.dp)) {
                                drawArc(
                                    color = Color(0xFFE5E7EB),
                                    startAngle = -90f,
                                    sweepAngle = 360f,
                                    useCenter = false,
                                    style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round)
                                )
                                drawArc(
                                    color = PupilAccentLight,
                                    startAngle = -90f,
                                    sweepAngle = 360f * progressFraction,
                                    useCenter = false,
                                    style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round)
                                )
                            }
                        }
                    }
                }
            } else {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenLibraryClick() },
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("📚", fontSize = 24.sp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Create a subject",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Add subjects in Library to start learning",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
