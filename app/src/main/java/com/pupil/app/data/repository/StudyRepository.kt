package com.pupil.app.data.repository

import android.content.Context
import android.util.Log
import com.pupil.app.core.json.JsonRepair
import com.pupil.app.core.llm.LlmEngine
import com.pupil.app.core.llm.LlmMetricsTracker
import com.pupil.app.core.llm.MediaPipeLlmEngine
import com.pupil.app.core.llm.MockLlmEngine
import com.pupil.app.core.llm.ModelFileInfo
import com.pupil.app.core.pdf.StudySection
import com.pupil.app.core.prompts.Prompts
import com.pupil.app.core.revision.SpacedRevisionManager
import com.pupil.app.core.creature.CreatureDialogue
import com.pupil.app.data.local.PupilDatabase
import com.pupil.app.data.local.entity.ConceptEntity
import com.pupil.app.data.local.entity.ConceptLinkEntity
import com.pupil.app.data.local.entity.ConceptSourceEntity
import com.pupil.app.data.local.entity.CreatureEntity
import com.pupil.app.data.local.entity.SubjectEntity
import com.pupil.app.data.local.entity.SourceEntity
import com.pupil.app.data.model.Concept
import com.pupil.app.data.model.ExtractionResponse
import com.pupil.app.data.model.GradingResponse
import com.pupil.app.data.model.GradingResult
import com.pupil.app.data.model.GradingStatus
import com.pupil.app.data.model.InferenceMetrics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.Instant
import java.time.ZoneId
import java.util.UUID

data class ConceptStats(
    val totalConcepts: Int = 0,
    val understoodCount: Int = 0,
    val partialCount: Int = 0,
    val missedCount: Int = 0,
    val unstudiedCount: Int = 0
) {
    val coverageFraction: Float
        get() = if (totalConcepts > 0) understoodCount.toFloat() / totalConcepts else 0f
}

class StudyRepository(
    private val context: Context,
    private val database: PupilDatabase = PupilDatabase.getInstance(context)
) {
    companion object {
        private const val TAG = "StudyRepository"
    }

    // ──── LLM Router & Engine ─────────────────────────────────────────────────

    val llmRouter = com.pupil.app.core.llm.LlmRouter(context)

    val isMockActive: Boolean get() = llmRouter.isMockActive.value
    val activeEngineName: String get() = llmRouter.activeEngineName.value

    private val _lastMetrics = MutableStateFlow<InferenceMetrics?>(null)
    val lastMetrics: StateFlow<InferenceMetrics?> = _lastMetrics.asStateFlow()

    private val _engineStatus = MutableStateFlow("Initializing LLM...")
    val engineStatus: StateFlow<String> = _engineStatus.asStateFlow()

    suspend fun setupEngine() = withContext(Dispatchers.IO) {
        llmRouter.initialize()
        _engineStatus.value = if (llmRouter.isMockActive.value) {
            "MOCK MODE (Model file not found on device)"
        } else {
            "${llmRouter.activeEngineName.value} Ready"
        }
    }

    fun getModelFileInfo(): ModelFileInfo = com.pupil.app.core.llm.MediaPipeLlmEngine().getModelFileInfo(context)
    fun getModelStatus(): com.pupil.app.core.config.ModelStatus = llmRouter.getModelStatus()
    fun getCheckedPaths(): List<String> = llmRouter.getCheckedPaths()
    fun setCloudBoostEnabled(enabled: Boolean) = llmRouter.setCloudBoostEnabled(enabled)
    fun isCloudBoostEnabled(): Boolean = llmRouter.isCloudBoostEnabled()
    fun getAverageLatencyLast10(): Long = LlmMetricsTracker.getAverageLatencyLast10()
    fun getLastLatencyMs(): Long = LlmMetricsTracker.getLastLatency()
    fun getPeakMemoryMb(): Double = LlmMetricsTracker.getPeakMemory()

    // ──── Subjects ────────────────────────────────────────────────────────────

    fun getAllSubjectsFlow() = database.subjectDao().getAllSubjects()
    fun getLatestSubjectFlow() = database.subjectDao().getLatestSubjectFlow()
    fun getSubjectFlow(id: String) = database.subjectDao().getSubjectByIdFlow(id)
    suspend fun getSubjectById(id: String) = database.subjectDao().getSubjectById(id)

    suspend fun createSubject(name: String, emoji: String, colourHex: String): SubjectEntity {
        val subject = SubjectEntity(
            id = UUID.randomUUID().toString(),
            name = name,
            emoji = emoji,
            colourHex = colourHex
        )
        database.subjectDao().insertSubject(subject)
        return subject
    }

    suspend fun renameSubject(subjectId: String, name: String, emoji: String, colourHex: String) {
        database.subjectDao().updateSubjectDetails(subjectId, name, emoji, colourHex)
    }

    suspend fun deleteSubject(subjectId: String) {
        database.subjectDao().deleteSubjectById(subjectId)
    }

    // ──── Sources ─────────────────────────────────────────────────────────────

    fun getSourcesForSubject(subjectId: String) = database.sourceDao().getSourcesForSubject(subjectId)

    suspend fun addSource(subjectId: String, type: String, displayName: String, filePath: String?): SourceEntity {
        val source = SourceEntity(
            id = UUID.randomUUID().toString(),
            subjectId = subjectId,
            type = type,
            displayName = displayName,
            filePath = filePath,
            processingState = "QUEUED"
        )
        database.sourceDao().insertSource(source)
        return source
    }

    suspend fun updateSourceProgress(sourceId: String, state: String, progress: Float) {
        database.sourceDao().updateProgress(sourceId, state, progress)
    }

    suspend fun markSourceDone(sourceId: String, pageCount: Int) {
        database.sourceDao().markDone(sourceId, pageCount)
    }

    suspend fun markSourceFailed(sourceId: String, error: String) {
        database.sourceDao().updateFailed(sourceId, error = error)
    }

    suspend fun deleteSource(sourceId: String) {
        database.sourceDao().deleteSourceById(sourceId)
    }

    // ──── Topics ─────────────────────────────────────────────────────────────

    fun getTopicsForSubject(subjectId: String) = database.topicDao().getTopicsForSubject(subjectId)

    // ──── Concepts ────────────────────────────────────────────────────────────

    fun getConceptsForSubject(subjectId: String): Flow<List<Concept>> =
        database.conceptDao().getConceptsForSubject(subjectId).map { entities ->
            val allLinks = try {
                database.conceptLinkDao().getAllLinksForSubject(subjectId)
            } catch (_: Exception) {
                emptyList()
            }
            val linkMap = allLinks.groupBy({ it.fromConceptId }, { it.toConceptId })
            entities.map { entity ->
                val directLinks = linkMap[entity.id] ?: emptyList()
                entity.toConcept(links = directLinks)
            }
        }

    fun getConceptsForTopic(topicId: String): Flow<List<Concept>> =
        database.conceptDao().getConceptsForTopic(topicId).map { entities ->
            val conceptIds = entities.map { it.id }.toSet()
            val subjectId = entities.firstOrNull()?.subjectId
            val allLinks = if (subjectId != null) {
                try {
                    database.conceptLinkDao().getAllLinksForSubject(subjectId)
                        .filter { it.fromConceptId in conceptIds && it.toConceptId in conceptIds }
                } catch (_: Exception) {
                    emptyList()
                }
            } else emptyList()
            val linkMap = allLinks.groupBy({ it.fromConceptId }, { it.toConceptId })
            entities.map { entity ->
                entity.toConcept(links = linkMap[entity.id] ?: emptyList())
            }
        }

    suspend fun getConceptsForTopicSync(topicId: String): List<Concept> =
        database.conceptDao().getConceptsForTopicSync(topicId).map { it.toConcept() }

    suspend fun getGapConcepts(subjectId: String): List<Concept> =
        database.conceptDao().getGapConceptsForSubject(subjectId).map { it.toConcept() }

    fun getTotalConceptCount(subjectId: String) = database.conceptDao().getTotalCountForSubject(subjectId)
    fun getUnderstoodConceptCount(subjectId: String) = database.conceptDao().getUnderstoodCountForSubject(subjectId)

    fun getSubjectStats(subjectId: String): Flow<ConceptStats> =
        database.conceptDao().getConceptsForSubject(subjectId).map { list ->
            ConceptStats(
                totalConcepts = list.size,
                understoodCount = list.count { it.status.equals("UNDERSTOOD", ignoreCase = true) },
                partialCount = list.count { it.status.equals("PARTIAL", ignoreCase = true) },
                missedCount = list.count { it.status.equals("MISSED", ignoreCase = true) || it.status.equals("MISCONCEPTION", ignoreCase = true) },
                unstudiedCount = list.count { it.status.equals("UNSTUDIED", ignoreCase = true) }
            )
        }

    fun getAllStats(): Flow<ConceptStats> =
        database.conceptDao().getAllConceptsFlow().map { list ->
            ConceptStats(
                totalConcepts = list.size,
                understoodCount = list.count { it.status.equals("UNDERSTOOD", ignoreCase = true) },
                partialCount = list.count { it.status.equals("PARTIAL", ignoreCase = true) },
                missedCount = list.count { it.status.equals("MISSED", ignoreCase = true) || it.status.equals("MISCONCEPTION", ignoreCase = true) },
                unstudiedCount = list.count { it.status.equals("UNSTUDIED", ignoreCase = true) }
            )
        }

    suspend fun extractAndSaveConceptsFromText(
        subjectId: String,
        sourceId: String,
        sourceName: String,
        text: String
    ): Int = withContext(Dispatchers.IO) {
        val sections = mutableListOf<StudySection>()
        val pageChunks = text.split("--- Page ")
        if (pageChunks.size > 1) {
            val maxChunkLen = com.pupil.app.core.config.AppConfig.MAX_CHUNK_CHAR_LENGTH
            var currentBuffer = StringBuilder()
            var startPage = ""
            var lastPage = ""
            var sectionIdx = 1

            for (chunk in pageChunks) {
                if (chunk.isBlank()) continue
                val headerEnd = chunk.indexOf("---\n")
                val pageNum = if (headerEnd > 0) chunk.substring(0, headerEnd).trim() else "$sectionIdx"
                val content = if (headerEnd > 0) chunk.substring(headerEnd + 4).trim() else chunk.trim()
                if (content.isBlank()) continue

                if (startPage.isEmpty()) startPage = pageNum
                lastPage = pageNum
                currentBuffer.append(content).append("\n\n")

                if (currentBuffer.length >= maxChunkLen) {
                    val pageRef = if (startPage == lastPage) "p. $startPage" else "pp. $startPage-$lastPage"
                    sections.add(StudySection(sectionIdx, "$sourceName ($pageRef)", currentBuffer.toString().trim(), pageRef))
                    sectionIdx++
                    currentBuffer = StringBuilder()
                    startPage = ""
                }
            }

            if (currentBuffer.isNotBlank()) {
                val pageRef = if (startPage == lastPage || lastPage.isEmpty()) "p. $startPage" else "pp. $startPage-$lastPage"
                sections.add(StudySection(sectionIdx, "$sourceName ($pageRef)", currentBuffer.toString().trim(), pageRef))
            }
        } else {
            val chunkSize = com.pupil.app.core.config.AppConfig.MAX_CHUNK_CHAR_LENGTH
            val chunks = text.chunked(chunkSize)
            for ((idx, chunk) in chunks.withIndex()) {
                sections.add(StudySection(idx + 1, "$sourceName (Part ${idx + 1})", chunk, "Part ${idx + 1}"))
            }
        }

        extractAndSaveConceptsForSource(
            subjectId = subjectId,
            sourceId = sourceId,
            sections = sections,
            onProgress = { _, _, _ -> }
        )
    }

    // ──── Concept Extraction (per source) ────────────────────────────────────

    /**
     * Extracts concepts from a list of study sections belonging to one source.
     * Deduplicates against existing concepts in the subject, then persists to Room.
     */
    /**
     * Extracts concepts from a list of study sections belonging to one source.
     * Deduplicates against existing concepts in the subject, then persists to Room.
     */
    suspend fun extractAndSaveConceptsForSource(
        subjectId: String,
        sourceId: String,
        sections: List<StudySection>,
        onProgress: (current: Int, total: Int, status: String) -> Unit
    ): Int = withContext(Dispatchers.IO) {
        // Allow grounded extraction to save cleanly even in offline/airplane mode without model weights
        val allExtracted = mutableListOf<Concept>()

        for ((index, section) in sections.withIndex()) {
            onProgress(index + 1, sections.size, "Reading page ${section.pageReference}...")
            val extracted = extractSectionWithRetry(section, index + 1, subjectId)
            allExtracted.addAll(extracted)
        }

        onProgress(sections.size, sections.size, "Deduplicating and organizing into topics...")

        // Deduplicate using string normalization & similarity + LLM check for ambiguous pairs
        val dedupedConcepts = com.pupil.app.core.concept.ConceptProcessor.deduplicateConcepts(allExtracted) { c1, c2 ->
            askModelIfSameConcept(c1, c2)
        }

        val sourceEntity = database.sourceDao().getSourceById(sourceId)
        val sourceTitle = sourceEntity?.displayName?.removeSuffix(".pdf")?.removeSuffix(".PDF") ?: "Core Topics"

        // Cluster concepts into topics of 3 to 8 concepts (split if > 8)
        val clusteredTopics = com.pupil.app.core.concept.ConceptProcessor.clusterIntoTopics(dedupedConcepts, sourceTitle)

        var totalInserted = 0
        // Map from any temporary / model-generated IDs to actual persisted UUIDs
        val idMapping = mutableMapOf<String, String>()
        val pendingLinks = mutableListOf<Pair<String, List<String>>>()

        for ((topicName, topicConcepts) in clusteredTopics) {
            val topicId = UUID.randomUUID().toString()
            database.topicDao().insertTopic(
                com.pupil.app.data.local.entity.TopicEntity(
                    id = topicId,
                    subjectId = subjectId,
                    name = topicName
                )
            )

            for (concept in topicConcepts) {
                val normalized = Concept.normalizeConceptName(concept.name)
                val conceptId = UUID.randomUUID().toString()
                if (concept.id.isNotBlank()) {
                    idMapping[concept.id] = conceptId
                }
                idMapping[concept.name.trim().lowercase()] = conceptId

                val entity = ConceptEntity(
                    id = conceptId,
                    subjectId = subjectId,
                    topicId = topicId,
                    name = concept.name,
                    normalizedName = normalized,
                    meaning = concept.meaning,
                    keyIdeasJson = Json.encodeToString(concept.keyIdeas)
                )
                database.conceptDao().insertConcept(entity)

                // Insert source references
                val allSources = (concept.sources + listOf(concept.source)).filter { it.isNotBlank() }.distinct()
                val pageRefs = if (allSources.isNotEmpty()) allSources else listOf("Page 1")
                for (pageRef in pageRefs) {
                    try {
                        database.conceptSourceDao().insertConceptSource(
                            ConceptSourceEntity(conceptId = entity.id, sourceId = sourceId, pageRef = pageRef)
                        )
                    } catch (e: Exception) {
                        Log.w(TAG, "Non-critical: Failed to insert concept source link for ${entity.id}: ${e.message}")
                    }
                }

                if (concept.links.isNotEmpty()) {
                    pendingLinks.add(entity.id to concept.links)
                }

                totalInserted++
            }
        }

        // Insert concept links AFTER all concepts exist in database to prevent Foreign Key constraint 787 failure
        val existingConceptsInSubject = database.conceptDao().getConceptsForSubjectSync(subjectId).associateBy { it.id }
        for ((fromId, links) in pendingLinks) {
            for (rawTarget in links) {
                val resolvedTarget = idMapping[rawTarget] ?: idMapping[rawTarget.trim().lowercase()] ?: rawTarget
                if (resolvedTarget.isNotBlank() && resolvedTarget != fromId && existingConceptsInSubject.containsKey(resolvedTarget)) {
                    try {
                        database.conceptLinkDao().insertLink(
                            ConceptLinkEntity(fromConceptId = fromId, toConceptId = resolvedTarget)
                        )
                    } catch (e: Exception) {
                        Log.w(TAG, "Safe ignore: Could not insert link $fromId -> $resolvedTarget: ${e.message}")
                    }
                }
            }
        }

        Log.i(TAG, "Finished extracting source $sourceId: $totalInserted concepts across ${clusteredTopics.size} topics.")
        totalInserted
    }

    private suspend fun askModelIfSameConcept(c1: Concept, c2: Concept): Boolean {
        return try {
            val prompt = Prompts.buildConceptComparisonPrompt(c1.name, c1.meaning, c2.name, c2.meaning)
            val (response, _) = llmRouter.generate(prompt, callType = "Deduplication Comparison")
            val trimmed = response.trim().lowercase()
            trimmed.startsWith("yes") || trimmed.contains("yes")
        } catch (e: Exception) {
            Log.w(TAG, "Error asking model during deduplication: ${e.message}")
            false
        }
    }

    private suspend fun extractSectionWithRetry(
        section: StudySection,
        sectionIndex: Int,
        subjectId: String
    ): List<Concept> {
        val prompt = Prompts.buildConceptExtractionPrompt(section.content, section.pageReference)
        val (rawJson, metrics) = llmRouter.generate(prompt, callType = "Extraction (Page ${section.pageReference})")
        _lastMetrics.value = metrics

        val parsed = tryParseExtraction(rawJson, sectionIndex, section.pageReference, subjectId)

        // Strict grounding filter: drop any concept not grounded in the source text or not a valid noun phrase
        val grounded = parsed.filter { concept ->
            val isGrounded = com.pupil.app.core.concept.ConceptProcessor.isGroundedInSource(concept.name, section.content)
            val isValid = com.pupil.app.core.concept.ConceptProcessor.isValidConceptName(concept.name)
            if (!isGrounded) {
                Log.d(TAG, "Dropped ungrounded concept '${concept.name}' from ${section.pageReference}")
            }
            if (!isValid) {
                Log.d(TAG, "Dropped invalid concept name '${concept.name}' from ${section.pageReference}")
            }
            isGrounded && isValid
        }

        return grounded
    }

    private fun tryParseExtraction(
        rawOutput: String,
        sectionIndex: Int,
        pageReference: String,
        subjectId: String
    ): List<Concept> {
        return try {
            val repaired = JsonRepair.repair(rawOutput)
            val response = JsonRepair.jsonInstance.decodeFromString<ExtractionResponse>(repaired)
            response.concepts.mapIndexedNotNull { idx, c ->
                val cleanName = com.pupil.app.core.concept.ConceptProcessor.sanitizeConceptName(c.name)
                if (cleanName.isBlank()) null
                else {
                    c.copy(
                        id = if (c.id.isBlank()) UUID.randomUUID().toString() else c.id,
                        name = cleanName,
                        subjectId = subjectId,
                        source = if (c.source.isBlank()) pageReference else c.source
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing extraction JSON: ${e.message}\nRaw: $rawOutput", e)
            emptyList()
        }
    }

    // ──── Grading ─────────────────────────────────────────────────────────────

    /**
     * Grades a concept against the student's full conversation transcript.
     * Progressive: returns immediately, caller can observe the result flow.
     * Logs latency per call.
     */
    suspend fun gradeConcept(
        concept: Concept,
        studentExplanation: String,
        subjectId: String
    ): GradingResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        val prompt = Prompts.buildGradingPrompt(
            conceptName = concept.name,
            conceptMeaning = concept.meaning,
            keyIdeas = concept.keyIdeas,
            studentExplanation = studentExplanation
        )

        var (rawJson, metrics) = llmRouter.generate(prompt, callType = "Grading (${concept.name})")
        _lastMetrics.value = metrics

        var grading = tryParseGrading(rawJson, studentExplanation)
        if (grading == null) {
            Log.w(TAG, "Grading JSON parse failed. Retrying once for ${concept.name}")
            val retry = llmRouter.generate(prompt, callType = "Grading Retry (${concept.name})")
            _lastMetrics.value = retry.second
            grading = tryParseGrading(retry.first, studentExplanation)
        }

        val latencyMs = System.currentTimeMillis() - startTime
        Log.d(TAG, "Grading '${concept.name}' took ${latencyMs}ms — status=${grading?.status}")

        val finalStatus = grading?.let { GradingStatus.fromString(it.status) } ?: GradingStatus.MISSED
        val finalEvidence = grading?.evidence ?: "No supporting quote found in explanation."
        val finalFollowup = grading?.followupQuestion ?: "Could you elaborate on how ${concept.name} works?"

        val isReconfirmation = concept.status == GradingStatus.UNDERSTOOD && finalStatus == GradingStatus.UNDERSTOOD
        val newIntervalIndex = if (isReconfirmation) {
            SpacedRevisionManager.getNextIntervalIndex(concept.revisionIntervalIndex)
        } else {
            concept.revisionIntervalIndex
        }

        val newXp = when {
            isReconfirmation -> SpacedRevisionManager.REDUCED_REVISION_XP
            finalStatus == GradingStatus.UNDERSTOOD -> 10
            finalStatus == GradingStatus.PARTIAL -> 4
            else -> 0
        }
        val xpDelta = if (isReconfirmation) newXp else maxOf(0, newXp - concept.earnedXp)
        val finalEarnedXp = if (isReconfirmation) concept.earnedXp + newXp else maxOf(concept.earnedXp, newXp)

        val nextDue = if (finalStatus == GradingStatus.UNDERSTOOD) {
            val intervalDays = com.pupil.app.core.config.AppConfig.REVISION_INTERVALS_DAYS.getOrElse(newIntervalIndex) {
                com.pupil.app.core.config.AppConfig.REVISION_INTERVALS_DAYS.last()
            }
            System.currentTimeMillis() + intervalDays * 24L * 60L * 60L * 1000L
        } else {
            0L
        }

        recordStudySessionAndAwardXp(xpDelta)

        database.conceptDao().updateGradingResult(
            conceptId = concept.id,
            status = finalStatus.serializedValue,
            evidence = finalEvidence,
            followupQuestion = finalFollowup,
            studentExplanation = studentExplanation,
            earnedXp = finalEarnedXp,
            lastTaught = System.currentTimeMillis(),
            nextDue = nextDue,
            revisionIntervalIndex = newIntervalIndex
        )

        GradingResult(
            status = finalStatus,
            evidence = finalEvidence,
            followupQuestion = finalFollowup,
            metrics = metrics
        )
    }

    suspend fun regradeConcept(
        concept: Concept,
        subjectId: String
    ): GradingResult = withContext(Dispatchers.IO) {
        val prompt = Prompts.buildReGradePrompt(
            conceptName = concept.name,
            conceptMeaning = concept.meaning,
            keyIdeas = concept.keyIdeas,
            studentExplanation = concept.studentExplanation,
            previousStatus = concept.status.serializedValue,
            previousEvidence = concept.evidence
        )
        val (rawJson, metrics) = llmRouter.generate(prompt, callType = "Re-grade (${concept.name})")
        _lastMetrics.value = metrics

        val grading = tryParseGrading(rawJson, concept.studentExplanation)
        val finalStatus = grading?.let { GradingStatus.fromString(it.status) } ?: concept.status
        val finalEvidence = grading?.evidence ?: concept.evidence
        val finalFollowup = grading?.followupQuestion ?: concept.followupQuestion

        val newXp = when (finalStatus) {
            GradingStatus.UNDERSTOOD -> 10
            GradingStatus.PARTIAL -> 4
            else -> 0
        }
        val xpDelta = maxOf(0, newXp - concept.earnedXp)
        val finalEarnedXp = maxOf(concept.earnedXp, newXp)

        recordStudySessionAndAwardXp(xpDelta)

        database.conceptDao().updateGradingResult(
            conceptId = concept.id,
            status = finalStatus.serializedValue,
            evidence = finalEvidence,
            followupQuestion = finalFollowup,
            studentExplanation = concept.studentExplanation,
            earnedXp = finalEarnedXp,
            lastTaught = System.currentTimeMillis(),
            nextDue = concept.nextDue,
            revisionIntervalIndex = concept.revisionIntervalIndex
        )

        GradingResult(
            status = finalStatus,
            evidence = finalEvidence,
            followupQuestion = finalFollowup,
            metrics = metrics
        )
    }

    private fun tryParseGrading(rawOutput: String, studentExplanation: String): GradingResponse? {
        return try {
            val repaired = JsonRepair.repair(rawOutput)
            val res = JsonRepair.jsonInstance.decodeFromString<GradingResponse>(repaired)
            val quoteWords = res.evidence.replace("\"", "").trim()
            val hasValidQuote = quoteWords.isNotBlank() &&
                    studentExplanation.contains(quoteWords, ignoreCase = true)
            if (!hasValidQuote && res.status == "understood") {
                res.copy(
                    evidence = if (studentExplanation.isNotBlank())
                        "\"${studentExplanation.take(50)}...\""
                    else "No quote in explanation"
                )
            } else res
        } catch (e: Exception) {
            Log.e(TAG, "Error decoding grading JSON: ${e.message}\nRaw: $rawOutput", e)
            null
        }
    }

    // ──── Creature ────────────────────────────────────────────────────────────

    suspend fun getCreature(): CreatureEntity = withContext(Dispatchers.IO) {
        database.creatureDao().getCreature() ?: run {
            val initial = CreatureEntity(
                id = 1, totalXp = 0, level = 1, evolutionStage = 1,
                streakDays = 1, lastStudyDateMillis = System.currentTimeMillis()
            )
            database.creatureDao().insertOrUpdate(initial)
            initial
        }
    }

    fun getCreatureFlow(): Flow<CreatureEntity> {
        return database.creatureDao().getCreatureFlow().map { creature ->
            creature ?: CreatureEntity(
                id = 1, totalXp = 0, level = 1, evolutionStage = 1,
                streakDays = 1, lastStudyDateMillis = System.currentTimeMillis()
            )
        }
    }

    suspend fun recordStudySessionAndAwardXp(xpDelta: Int) = withContext(Dispatchers.IO) {
        if (xpDelta <= 0) return@withContext
        val current = getCreature()
        val now = System.currentTimeMillis()
        val currentDay = Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault()).toLocalDate()
        val lastDay = Instant.ofEpochMilli(current.lastStudyDateMillis).atZone(ZoneId.systemDefault()).toLocalDate()

        val newStreak = when {
            currentDay.isEqual(lastDay) -> current.streakDays
            currentDay.minusDays(1).isEqual(lastDay) -> current.streakDays + 1
            else -> 1
        }

        val newXp = current.totalXp + xpDelta
        val newLevel = CreatureDialogue.getLevelForXp(newXp)
        val newStage = CreatureDialogue.getStageForLevel(newLevel)

        database.creatureDao().insertOrUpdate(
            current.copy(
                totalXp = newXp,
                level = newLevel,
                evolutionStage = newStage,
                streakDays = newStreak,
                lastStudyDateMillis = now
            )
        )
    }

    // ──── Data Management ─────────────────────────────────────────────────────

    suspend fun clearAllData(): Unit = withContext(Dispatchers.IO) {
        database.clearAllTables()
        database.creatureDao().insertOrUpdate(
            CreatureEntity(
                id = 1, totalXp = 0, level = 1, evolutionStage = 1,
                streakDays = 1, lastStudyDateMillis = System.currentTimeMillis()
            )
        )
    }
}
