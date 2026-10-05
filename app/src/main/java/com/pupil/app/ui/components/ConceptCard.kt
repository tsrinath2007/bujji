package com.pupil.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Dangerous
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pupil.app.data.model.Concept
import com.pupil.app.data.model.GradingStatus
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ConceptCard(
    concept: Concept,
    modifier: Modifier = Modifier,
    isGrading: Boolean = false,
    showGradingDetails: Boolean = false,
    onDisagreeClick: (() -> Unit)? = null,
    onViewNoteClick: (() -> Unit)? = null,
    onTeachThisClick: (() -> Unit)? = null
) {
    val statusColor: Color
    val statusBg: Color
    val statusIcon: ImageVector

    when (concept.status) {
        GradingStatus.UNDERSTOOD -> {
            statusColor = StatusUnderstood
            statusBg = StatusUnderstoodBg
            statusIcon = Icons.Default.CheckCircle
        }
        GradingStatus.PARTIAL -> {
            statusColor = StatusPartial
            statusBg = StatusPartialBg
            statusIcon = Icons.Default.WarningAmber
        }
        GradingStatus.MISSED -> {
            statusColor = StatusMissed
            statusBg = StatusMissedBg
            statusIcon = Icons.AutoMirrored.Filled.HelpOutline
        }
        GradingStatus.MISCONCEPTION -> {
            statusColor = StatusMisconception
            statusBg = StatusMisconceptionBg
            statusIcon = Icons.Default.Dangerous
        }
        GradingStatus.UNSTUDIED -> {
            statusColor = StatusUnstudied
            statusBg = StatusUnstudiedBg
            statusIcon = Icons.AutoMirrored.Filled.HelpOutline
        }
    }

    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.5.dp, if (showGradingDetails) statusColor.copy(alpha = 0.35f) else MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .clickable { expanded = !expanded }
                .padding(18.dp)
        ) {
            // Header: Name & Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = concept.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(statusBg)
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = statusIcon,
                            contentDescription = null,
                            tint = statusColor,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = concept.status.displayName,
                            color = statusColor,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Meaning (always clean and readable)
            Text(
                text = concept.meaning,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // 1-line reason / evidence preview if present
            if (showGradingDetails && concept.evidence.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(statusBg.copy(alpha = 0.45f))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Column {
                        Text(
                            text = "What you explained:",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = statusColor
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "“${concept.evidence}”",
                            style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Expandable details (Key ideas, followup questions, links)
            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 10.dp)) {
                    if (concept.keyIdeas.isNotEmpty()) {
                        Text(
                            text = "Key ideas:",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            concept.keyIdeas.forEach { idea ->
                                SuggestionChip(
                                    onClick = {},
                                    label = { Text(idea, fontSize = 11.sp) },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = SuggestionChipDefaults.suggestionChipColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                )
                            }
                        }
                    }

                    if (concept.followupQuestion.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = PupilPrimaryLight,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Doubt: ${concept.followupQuestion}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = PupilPrimaryLight
                            )
                        }
                    }
                }
            }

            // Bottom bar: Expand toggle & Action buttons
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { expanded = !expanded }
                ) {
                    Text(
                        text = if (expanded) "Show less" else "Details",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Icon(
                        imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (onTeachThisClick != null && concept.status != GradingStatus.UNDERSTOOD) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(PupilPrimaryLight.copy(alpha = 0.12f))
                                .clickable { onTeachThisClick() }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "Teach me this",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = PupilPrimaryLight
                            )
                        }
                    }

                    if (onDisagreeClick != null && showGradingDetails) {
                        if (isGrading) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Re-checking...", style = MaterialTheme.typography.labelSmall)
                            }
                        } else if (!concept.regraded) {
                            OutlinedButton(
                                onClick = onDisagreeClick,
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = ButtonDefaults.TextButtonContentPadding
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("I Disagree", style = MaterialTheme.typography.labelSmall)
                            }
                        } else {
                            Text(
                                text = "Re-graded",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
