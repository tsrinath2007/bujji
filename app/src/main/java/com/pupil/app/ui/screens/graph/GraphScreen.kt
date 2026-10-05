package com.pupil.app.ui.screens.graph

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Dangerous
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pupil.app.data.model.GradingStatus
import com.pupil.app.ui.theme.PupilAccentLight
import com.pupil.app.ui.theme.PupilPrimaryLight
import com.pupil.app.ui.theme.StatusMisconception
import com.pupil.app.ui.theme.StatusMisconceptionBg
import com.pupil.app.ui.theme.StatusMissed
import com.pupil.app.ui.theme.StatusMissedBg
import com.pupil.app.ui.theme.StatusPartial
import com.pupil.app.ui.theme.StatusPartialBg
import com.pupil.app.ui.theme.StatusUnderstood
import com.pupil.app.ui.theme.StatusUnderstoodBg
import com.pupil.app.ui.theme.StatusUnstudied
import com.pupil.app.ui.theme.StatusUnstudiedBg

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun GraphScreen(
    subjectId: String,
    initialSelectedConceptId: String? = null,
    viewModel: GraphViewModel,
    onBackClick: () -> Unit,
    onTeachGapsClick: (subjectId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val nodes by viewModel.nodes.collectAsState()
    val edges by viewModel.edges.collectAsState()
    val selectedConcept by viewModel.selectedConcept.collectAsState()

    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset(0f, 0f)) }

    val textMeasurer = rememberTextMeasurer()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(subjectId) {
        viewModel.loadGraphForSubject(subjectId, initialSelectedConceptId)
    }

    // Auto-fit whole constellation on screen upon load
    LaunchedEffect(nodes) {
        if (nodes.isNotEmpty()) {
            val minX = nodes.minOf { it.position.x }
            val maxX = nodes.maxOf { it.position.x }
            val minY = nodes.minOf { it.position.y }
            val maxY = nodes.maxOf { it.position.y }
            val graphW = (maxX - minX).coerceAtLeast(200f)
            val graphH = (maxY - minY).coerceAtLeast(200f)
            val centerX = (minX + maxX) / 2f
            val centerY = (minY + maxY) / 2f

            val fitScale = minOf(900f / (graphW + 160f), 1400f / (graphH + 160f)).coerceIn(0.6f, 1.25f)
            scale = fitScale
            offset = Offset(500f - centerX * fitScale, 700f - centerY * fitScale)
        }
    }

    val transformState = rememberTransformableState { zoomChange, offsetChange, _ ->
        scale = (scale * zoomChange).coerceIn(0.35f, 3.5f)
        offset += offsetChange
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Brain Map",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "The creature's memory constellation",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    val hasGaps = nodes.any { it.concept.status != GradingStatus.UNDERSTOOD }
                    if (hasGaps) {
                        Button(
                            onClick = { onTeachGapsClick(subjectId) },
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PupilPrimaryLight),
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Icon(Icons.Default.School, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Teach Gaps", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    } else if (nodes.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = StatusUnderstoodBg,
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text(
                                text = "All Understood ✨",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = StatusUnderstood,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Constellation Canvas: Glowing nodes and connected edges
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .transformable(state = transformState)
                    .pointerInput(nodes, scale, offset) {
                        detectTapGestures { tapOffset ->
                            val graphX = (tapOffset.x - offset.x) / scale
                            val graphY = (tapOffset.y - offset.y) / scale
                            val clickedNode = nodes.firstOrNull { node ->
                                val distSq = (node.position.x - graphX) * (node.position.x - graphX) +
                                        (node.position.y - graphY) * (node.position.y - graphY)
                                distSq <= (48f * 48f)
                            }
                            viewModel.selectConcept(clickedNode?.concept)
                        }
                    }
            ) {
                val nodeMap = nodes.associateBy { it.concept.id }

                // 1. Draw Constellation Link Edges
                for (edge in edges) {
                    val src = nodeMap[edge.sourceId]?.position
                    val tgt = nodeMap[edge.targetId]?.position
                    if (src != null && tgt != null) {
                        val start = Offset(src.x * scale + offset.x, src.y * scale + offset.y)
                        val end = Offset(tgt.x * scale + offset.x, tgt.y * scale + offset.y)
                        drawLine(
                            color = PupilPrimaryLight.copy(alpha = 0.28f),
                            start = start,
                            end = end,
                            strokeWidth = 2.2f * scale
                        )
                    }
                }

                // 2. Draw Constellation Star Nodes
                for (node in nodes) {
                    val screenPos = Offset(
                        node.position.x * scale + offset.x,
                        node.position.y * scale + offset.y
                    )

                    val isDue = com.pupil.app.core.revision.SpacedRevisionManager.isConceptDue(node.concept)

                    val (nodeColor, glowColor) = when {
                        isDue -> Pair(PupilAccentLight, PupilAccentLight.copy(alpha = 0.35f))
                        node.concept.status == GradingStatus.UNDERSTOOD -> Pair(StatusUnderstood, StatusUnderstood.copy(alpha = 0.38f))
                        node.concept.status == GradingStatus.PARTIAL -> Pair(StatusPartial, StatusPartial.copy(alpha = 0.32f))
                        node.concept.status == GradingStatus.MISCONCEPTION -> Pair(StatusMisconception, StatusMisconception.copy(alpha = 0.32f))
                        node.concept.status == GradingStatus.MISSED -> Pair(StatusMissed, StatusMissed.copy(alpha = 0.22f))
                        else -> Pair(StatusUnstudied, StatusUnstudied.copy(alpha = 0.15f))
                    }

                    val isSelected = selectedConcept?.id == node.concept.id
                    val baseRadius = when (node.concept.status) {
                        GradingStatus.UNDERSTOOD -> 30f * scale
                        GradingStatus.PARTIAL -> 28f * scale
                        GradingStatus.UNSTUDIED -> 24f * scale
                        else -> 26f * scale
                    }
                    val nodeRadius = if (isSelected) baseRadius * 1.25f else baseRadius

                    // Glowing constellation aura
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(glowColor, Color.Transparent),
                            center = screenPos,
                            radius = nodeRadius * 2.2f
                        ),
                        radius = nodeRadius * 2.2f,
                        center = screenPos
                    )

                    // Selection highlight ring
                    if (isSelected) {
                        drawCircle(
                            color = PupilPrimaryLight.copy(alpha = 0.5f),
                            radius = nodeRadius + (12f * scale),
                            center = screenPos,
                            style = Stroke(width = 3f * scale)
                        )
                    }

                    // Node body
                    drawCircle(
                        color = nodeColor,
                        radius = nodeRadius,
                        center = screenPos
                    )

                    // Node crisp white inner border
                    drawCircle(
                        color = Color.White.copy(alpha = 0.85f),
                        radius = nodeRadius,
                        center = screenPos,
                        style = Stroke(width = 2.2f * scale)
                    )

                    // Wrapped concept label below node
                    val words = node.concept.name.split(" ")
                    val labelText = if (words.size <= 2) {
                        node.concept.name
                    } else {
                        val mid = (words.size + 1) / 2
                        "${words.take(mid).joinToString(" ")}\n${words.drop(mid).joinToString(" ")}"
                    }

                    val textLayout = textMeasurer.measure(
                        text = labelText,
                        style = TextStyle(
                            fontSize = (11f * scale).coerceIn(9f, 15f).sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    )
                    drawText(
                        textLayoutResult = textLayout,
                        topLeft = Offset(
                            screenPos.x - (textLayout.size.width / 2f),
                            screenPos.y + nodeRadius + 6f
                        )
                    )
                }
            }

            // Empty state if no nodes
            if (nodes.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Hub,
                            contentDescription = null,
                            tint = PupilPrimaryLight.copy(alpha = 0.4f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Brain Map is Empty",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Import notes to grow your creature's knowledge constellation!",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Full-Word Status Legend (Card at bottom left)
            if (nodes.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Status Legend",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            LegendItem("Understood", StatusUnderstood)
                            LegendItem("Partial", StatusPartial)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            LegendItem("Misconception", StatusMisconception)
                            LegendItem("Missed", StatusMissed)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            LegendItem("Not studied yet", StatusUnstudied)
                            LegendItem("Due for review", PupilAccentLight)
                        }
                    }
                }
            }
        }
    }

    // Modal Bottom Sheet: Beautiful card-based concept viewer (NO RAW MARKDOWN)
    if (selectedConcept != null) {
        val concept = selectedConcept!!

        val (statusColor, statusBg, statusIcon, statusLabel) = when (concept.status) {
            GradingStatus.UNDERSTOOD -> Quadruple(StatusUnderstood, StatusUnderstoodBg, Icons.Default.CheckCircle, "Understood")
            GradingStatus.PARTIAL -> Quadruple(StatusPartial, StatusPartialBg, Icons.Default.WarningAmber, "Partial")
            GradingStatus.MISSED -> Quadruple(StatusMissed, StatusMissedBg, Icons.AutoMirrored.Filled.HelpOutline, "Missed")
            GradingStatus.MISCONCEPTION -> Quadruple(StatusMisconception, StatusMisconceptionBg, Icons.Default.Dangerous, "Misconception")
            GradingStatus.UNSTUDIED -> Quadruple(StatusUnstudied, StatusUnstudiedBg, Icons.AutoMirrored.Filled.HelpOutline, "Not studied yet")
        }

        ModalBottomSheet(
            onDismissRequest = { viewModel.selectConcept(null) },
            sheetState = sheetState,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header Row: Status Badge & Close Icon
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(statusBg)
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = statusIcon,
                                contentDescription = null,
                                tint = statusColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = statusLabel,
                                color = statusColor,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    IconButton(onClick = { viewModel.selectConcept(null) }) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Concept Name
                Text(
                    text = concept.name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Source Reference
                Text(
                    text = "Source: ${concept.source}",
                    style = MaterialTheme.typography.labelSmall,
                    color = PupilPrimaryLight,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Meaning Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Meaning",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = concept.meaning,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Key Ideas Chips
                if (concept.keyIdeas.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Key Ideas",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        concept.keyIdeas.forEach { idea ->
                            SuggestionChip(
                                onClick = {},
                                label = { Text(idea, style = MaterialTheme.typography.bodySmall) },
                                shape = RoundedCornerShape(12.dp),
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            )
                        }
                    }
                }

                // Evidence Quote (if student was graded)
                if (concept.evidence.isNotBlank() && concept.evidence != "No supporting quote in explanation") {
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "What You Taught",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = statusBg.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "\u201c${concept.evidence}\u201d",
                            style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }

                // Follow-up Doubt / Question
                if (concept.followupQuestion.isNotBlank()) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = PupilPrimaryLight.copy(alpha = 0.1f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = PupilPrimaryLight,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Creature Doubt",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = PupilPrimaryLight
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = concept.followupQuestion,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun LegendItem(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(color, CircleShape)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface)
    }
}

private data class Quadruple<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)
