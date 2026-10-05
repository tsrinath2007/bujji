package com.pupil.app.ui.screens.teach

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pupil.app.core.speech.SpeechState
import com.pupil.app.data.model.Concept
import com.pupil.app.ui.components.AudioMicButton
import com.pupil.app.ui.components.CreatureState
import com.pupil.app.ui.components.CreatureWidget
import com.pupil.app.ui.components.LatencyBanner
import com.pupil.app.ui.components.MockModeBanner
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import com.pupil.app.data.model.GradingStatus
import com.pupil.app.ui.theme.PupilPrimaryLight
import com.pupil.app.ui.theme.StatusMisconception
import com.pupil.app.ui.theme.StatusMissed
import com.pupil.app.ui.theme.StatusPartial
import com.pupil.app.ui.theme.StatusUnderstood
import com.pupil.app.ui.theme.StatusUnstudied

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun TeachScreen(
    subjectId: String,
    topicId: String? = null,
    viewModel: TeachViewModel,
    onGradingFinished: (subjectId: String, topicId: String?) -> Unit,
    modifier: Modifier = Modifier,
    onlyGaps: Boolean = false
) {
    val rawConcepts by remember(topicId, subjectId, onlyGaps) {
        viewModel.getConceptsFlow(topicId, subjectId, onlyGaps)
    }.collectAsState(initial = emptyList())
    // A teach session must never contain more than 8 concepts or fewer than 1
    val concepts = remember(rawConcepts) { rawConcepts.take(8) }

    val creature by viewModel.creature.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val speechState by viewModel.speechState.collectAsState()
    val explanationText by viewModel.explanationText.collectAsState()
    val lastMetrics by viewModel.lastMetrics.collectAsState()

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var inlineSpeechNotice by remember { mutableStateOf<String?>(null) }

    val isImeVisible = WindowInsets.isImeVisible

    // Single prompt topic name (no "and N more" wording)
    val mainTopic = remember(concepts) {
        if (concepts.isNotEmpty()) concepts[0].name else "this lesson"
    }

    // Destroy speech recognizer when navigating away
    DisposableEffect(Unit) {
        onDispose {
            viewModel.destroySpeech()
        }
    }

    LaunchedEffect(uiState) {
        when (val state = uiState) {
            is TeachUiState.Completed -> {
                onGradingFinished(state.subjectId, state.topicId)
                viewModel.resetState()
            }
            is TeachUiState.Error -> {
                errorMessage = state.message
            }
            else -> {}
        }
    }

    LaunchedEffect(speechState) {
        if (speechState is SpeechState.Error) {
            inlineSpeechNotice = (speechState as SpeechState.Error).message
        } else if (speechState is SpeechState.Listening || speechState is SpeechState.FinalResult) {
            inlineSpeechNotice = null
        }
    }

    Scaffold(
        topBar = {
            Column {
                MockModeBanner(isMock = viewModel.isMockActive)
                LatencyBanner(metrics = lastMetrics)
            }
        },
        bottomBar = {
            if (concepts.isNotEmpty()) {
                // Persistent docked bottom bar holding BOTH Mic and Grade buttons without scrolling
                Surface(
                    tonalElevation = 6.dp,
                    shadowElevation = 8.dp,
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .imePadding()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        AudioMicButton(
                            isListening = speechState is SpeechState.Listening,
                            onStartListening = { viewModel.startListening() },
                            onStopListening = { viewModel.stopListening() },
                            onPermissionDenied = {
                                inlineSpeechNotice = "Microphone permission is required for voice input. You can type your explanation below."
                            }
                        )

                        Button(
                            onClick = {
                                viewModel.submitExplanation(
                                    topicId = topicId,
                                    subjectId = subjectId,
                                    concepts = concepts
                                )
                            },
                            enabled = explanationText.isNotBlank() && uiState !is TeachUiState.Grading,
                            shape = RoundedCornerShape(24.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PupilPrimaryLight,
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                        ) {
                            if (uiState is TeachUiState.Grading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.White,
                                    strokeWidth = 2.5.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Grading on-device...", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            } else {
                                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Grade Explanation", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        if (concepts.isEmpty()) {
            // Empty state: "Add a source first"
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.School,
                        contentDescription = null,
                        modifier = Modifier.size(56.dp),
                        tint = PupilPrimaryLight.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Add a source first",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "There are no concepts to teach yet. Import notes or slides to extract study concepts.",
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Creature with friendly speech bubble — smoothly adapts when keyboard opens
                var creatureReactionState by remember { mutableStateOf<CreatureState?>(null) }
                val defaultTeachState = when {
                    speechState is SpeechState.Listening -> CreatureState.FOCUSED
                    uiState is TeachUiState.Grading -> CreatureState.ANXIOUS
                    onlyGaps -> CreatureState.ANXIOUS
                    explanationText.isNotBlank() -> CreatureState.CURIOUS
                    else -> CreatureState.INTERACTIVE
                }

                val creatureState = creatureReactionState ?: defaultTeachState

                val creatureSpeechTitle = when (creatureState) {
                    CreatureState.FOCUSED -> "I'm listening carefully! Go on..."
                    CreatureState.ANXIOUS -> if (uiState is TeachUiState.Grading) "Checking my notes on this..." else "I'm a bit nervous about $mainTopic! Help me understand?"
                    CreatureState.EXCITED -> "Ooh! Tell me everything you know!"
                    CreatureState.CURIOUS -> "Interesting! How does that connect?"
                    CreatureState.SLEEPING -> "Zzz... Wake me with your explanation!"
                    else -> "Teach me about $mainTopic!"
                }

                val creatureSpeechSub = when (creatureState) {
                    CreatureState.FOCUSED -> "Speak naturally — I'm taking in the concepts."
                    CreatureState.ANXIOUS -> if (uiState is TeachUiState.Grading) "Analyzing your analogies and reasoning..." else "Walk me through how this works so I don't get mixed up."
                    CreatureState.EXCITED -> "I love learning new concepts with you!"
                    CreatureState.SLEEPING -> "Tap mic or type to wake me up!"
                    else -> "Explain in your own words, analogies, or examples."
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize(
                            animationSpec = androidx.compose.animation.core.tween(
                                durationMillis = 280,
                                easing = androidx.compose.animation.core.FastOutSlowInEasing
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    CreatureWidget(
                        state = creatureState,
                        level = creature.level,
                        totalXp = creature.totalXp,
                        streakDays = creature.streakDays,
                        speechBubbleText = creatureSpeechTitle,
                        speechBubbleSubtext = creatureSpeechSub,
                        showProgressBar = false,
                        compact = isImeVisible,
                        onCreatureTapped = {
                            creatureReactionState = when (creatureReactionState) {
                                null, CreatureState.INTERACTIVE -> CreatureState.EXCITED
                                CreatureState.EXCITED -> CreatureState.FOCUSED
                                CreatureState.FOCUSED -> CreatureState.ANXIOUS
                                CreatureState.ANXIOUS -> CreatureState.CURIOUS
                                CreatureState.CURIOUS -> CreatureState.SLEEPING
                                CreatureState.SLEEPING -> CreatureState.INTERACTIVE
                                else -> CreatureState.INTERACTIVE
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Friendly inline notice for speech fallback / tips
                if (inlineSpeechNotice != null) {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = PupilPrimaryLight,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = inlineSpeechNotice!!,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { inlineSpeechNotice = null },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Dismiss", modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // Concepts to teach card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Concepts to teach (${concepts.size}):",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (onlyGaps) "Tricky parts" else "Topic lesson",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            concepts.forEach { concept ->
                                val statusColor = when (concept.status) {
                                    GradingStatus.UNDERSTOOD -> StatusUnderstood
                                    GradingStatus.PARTIAL -> StatusPartial
                                    GradingStatus.MISCONCEPTION -> StatusMisconception
                                    GradingStatus.MISSED -> StatusMissed
                                    GradingStatus.UNSTUDIED -> StatusUnstudied
                                }
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = statusColor.copy(alpha = 0.12f),
                                    border = BorderStroke(1.dp, statusColor.copy(alpha = 0.35f)),
                                    modifier = Modifier.clickable {
                                        // Student asks Bujji to explain this concept / topic
                                        val brief = if (concept.meaning.isNotBlank()) concept.meaning else "A key concept in this study unit."
                                        val ideas = if (concept.keyIdeas.isNotEmpty()) " (Key ideas: ${concept.keyIdeas.joinToString(", ")})" else ""
                                        inlineSpeechNotice = "Bujji says: \"${concept.name}: $brief$ideas\""
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(statusColor)
                                        )
                                        Text(
                                            text = concept.name,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Explanation Text Input
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Your Explanation:",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = explanationText,
                            onValueChange = { viewModel.updateExplanationText(it) },
                            placeholder = {
                                Text(
                                    text = "Speak using the mic or type your explanation here. Intuitive analogies count!",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(if (isImeVisible) 110.dp else 160.dp),
                            shape = RoundedCornerShape(18.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PupilPrimaryLight,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    // Friendly Grading Progress Dialog
    if (uiState is TeachUiState.Grading) {
        val gradingState = uiState as TeachUiState.Grading
        AlertDialog(
            onDismissRequest = {},
            confirmButton = {},
            shape = RoundedCornerShape(24.dp),
            title = {
                Text(
                    text = "Checking Understanding",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                ) {
                    CircularProgressIndicator(
                        color = PupilPrimaryLight,
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(44.dp)
                    )
                    Spacer(modifier = Modifier.height(18.dp))
                    Text(
                        text = "${gradingState.currentConceptIndex} of ${gradingState.totalConcepts}",
                        style = MaterialTheme.typography.labelSmall,
                        color = PupilPrimaryLight,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = gradingState.conceptName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Evaluating your explanation on-device...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        )
    }

    // Friendly Error Dialog
    if (errorMessage != null) {
        AlertDialog(
            onDismissRequest = { errorMessage = null },
            confirmButton = {
                Button(
                    onClick = { errorMessage = null },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PupilPrimaryLight)
                ) {
                    Text("Got it")
                }
            },
            shape = RoundedCornerShape(24.dp),
            title = { Text("Note from ${com.pupil.app.core.AppConstants.APP_NAME}", fontWeight = FontWeight.Bold) },
            text = { Text(errorMessage ?: "") }
        )
    }
}
