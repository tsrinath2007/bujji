package com.pupil.app.data.demo

import android.content.Context
import com.pupil.app.data.local.PupilDatabase
import com.pupil.app.data.local.entity.ConceptEntity
import com.pupil.app.data.local.entity.ConceptLinkEntity
import com.pupil.app.data.local.entity.CreatureEntity
import com.pupil.app.data.local.entity.SourceEntity
import com.pupil.app.data.local.entity.SubjectEntity
import com.pupil.app.data.local.entity.TopicEntity
import com.pupil.app.data.model.Concept
import com.pupil.app.data.model.GradingStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

object DemoDataLoader {

    /** Loads a demo subject with pre-graded biology concepts. Returns the subject id. */
    suspend fun loadDemoSubject(context: Context): String = withContext(Dispatchers.IO) {
        val db = PupilDatabase.getInstance(context)

        val subjectId = "demo_subject_photosynthesis"
        val sourceId = "demo_source_campbell"
        val topicId1 = "demo_topic_light_reactions"
        val topicId2 = "demo_topic_dark_reactions"

        // 1. Insert Subject
        db.subjectDao().insertSubject(
            SubjectEntity(
                id = subjectId,
                name = "Photosynthesis & Respiration",
                emoji = "🌿",
                colourHex = "#10B981"
            )
        )

        // 2. Insert Source
        db.sourceDao().insertSource(
            SourceEntity(
                id = sourceId,
                subjectId = subjectId,
                type = "PDF",
                displayName = "Campbell Biology Ch.8 (Demo)",
                processingState = "DONE",
                pageCount = 20,
                progress = 1f
            )
        )

        // 3. Insert Topics
        db.topicDao().insertTopic(
            TopicEntity(id = topicId1, subjectId = subjectId, name = "Light Reactions & Photosystems")
        )
        db.topicDao().insertTopic(
            TopicEntity(id = topicId2, subjectId = subjectId, name = "Calvin Cycle & Cellular Respiration")
        )

        val now = System.currentTimeMillis()

        // 4. Insert Concepts (pre-graded for demo)
        val demoConcepts = listOf(
            // Topic 1: Light Reactions
            ConceptEntity(
                id = "c_chlorophyll",
                subjectId = subjectId,
                topicId = topicId1,
                name = "Chlorophyll Photon Absorption",
                normalizedName = "chlorophyll photon absorption",
                meaning = "Primary pigment that absorbs blue and red wavelengths to excite electrons for photosynthesis.",
                keyIdeasJson = Json.encodeToString(listOf("Photon absorption", "Electron excitation", "Porphyrin ring")),
                status = GradingStatus.UNDERSTOOD.serializedValue,
                evidence = "\"Chlorophyll captures solar energy by absorbing blue and red photons.\"",
                followupQuestion = "How does electron excitation couple to water splitting?",
                studentExplanation = "Chlorophyll captures solar energy by absorbing blue and red photons and exciting electrons.",
                lastTaught = now,
                earnedXp = 10
            ),
            ConceptEntity(
                id = "c_light_reactions",
                subjectId = subjectId,
                topicId = topicId1,
                name = "Thylakoid Light Reactions",
                normalizedName = "thylakoid light reactions",
                meaning = "Reactions converting solar energy into ATP and NADPH via photolysis of water in the thylakoid.",
                keyIdeasJson = Json.encodeToString(listOf("Photosystem II & I", "Water photolysis", "Proton gradient")),
                status = GradingStatus.PARTIAL.serializedValue,
                evidence = "\"The light reaction splits water and makes ATP and NADPH, but I forgot how protons accumulate.\"",
                followupQuestion = "How does the proton gradient drive ATP synthesis in chloroplasts?",
                studentExplanation = "The light reaction splits water and makes ATP and NADPH.",
                lastTaught = now,
                earnedXp = 4
            ),
            ConceptEntity(
                id = "c_atp_synthase",
                subjectId = subjectId,
                topicId = topicId1,
                name = "ATP Synthase Chemiosmosis",
                normalizedName = "atp synthase chemiosmosis",
                meaning = "Enzyme using proton gradient across thylakoid membrane to phosphorylate ADP into ATP.",
                keyIdeasJson = Json.encodeToString(listOf("Chemiosmosis", "Proton motive force", "Rotary catalysis")),
                status = GradingStatus.UNDERSTOOD.serializedValue,
                evidence = "\"ATP synthase uses the proton gradient like a waterfall to spin and make ATP.\"",
                followupQuestion = "How many protons does it take to synthesize one ATP molecule?",
                studentExplanation = "ATP synthase uses the proton gradient like a waterfall to spin and make ATP.",
                lastTaught = now,
                earnedXp = 10
            ),
            // Topic 2: Calvin Cycle
            ConceptEntity(
                id = "c_calvin_cycle",
                subjectId = subjectId,
                topicId = topicId2,
                name = "Calvin Cycle Carbon Fixation",
                normalizedName = "calvin cycle carbon fixation",
                meaning = "Stroma reactions that fix CO₂ into organic sugar using ATP and NADPH from light reactions.",
                keyIdeasJson = Json.encodeToString(listOf("CO₂ fixation", "RuBisCO enzyme", "G3P production")),
                status = GradingStatus.MISSED.serializedValue,
                evidence = "No supporting quote found in explanation.",
                followupQuestion = "What molecule does RuBisCO attach CO₂ to, and what is the product?",
                studentExplanation = "",
                lastTaught = 0L,
                earnedXp = 0
            ),
            ConceptEntity(
                id = "c_cellular_respiration",
                subjectId = subjectId,
                topicId = topicId2,
                name = "Cellular Respiration Overview",
                normalizedName = "cellular respiration overview",
                meaning = "Catabolism of glucose to CO₂ and H₂O, harvesting energy as ATP via glycolysis, Krebs cycle, and ETC.",
                keyIdeasJson = Json.encodeToString(listOf("Glycolysis", "Krebs cycle", "Electron transport chain", "34-36 ATP yield")),
                status = GradingStatus.UNSTUDIED.serializedValue,
                evidence = "",
                followupQuestion = "",
                studentExplanation = "",
                lastTaught = 0L,
                earnedXp = 0
            ),
            ConceptEntity(
                id = "c_electron_transport",
                subjectId = subjectId,
                topicId = topicId2,
                name = "Electron Transport Chain",
                normalizedName = "electron transport chain",
                meaning = "Series of protein complexes in the inner mitochondrial membrane that transfer electrons to create a proton gradient for ATP synthesis.",
                keyIdeasJson = Json.encodeToString(listOf("NADH & FADH2 oxidation", "Complex I-IV", "Oxygen as final acceptor")),
                status = GradingStatus.UNSTUDIED.serializedValue,
                evidence = "",
                followupQuestion = "",
                studentExplanation = "",
                lastTaught = 0L,
                earnedXp = 0
            )
        )

        db.conceptDao().insertConcepts(demoConcepts)

        // 5. Insert concept links
        val links = listOf(
            ConceptLinkEntity("c_chlorophyll", "c_light_reactions"),
            ConceptLinkEntity("c_light_reactions", "c_atp_synthase"),
            ConceptLinkEntity("c_light_reactions", "c_calvin_cycle"),
            ConceptLinkEntity("c_calvin_cycle", "c_cellular_respiration"),
            ConceptLinkEntity("c_cellular_respiration", "c_electron_transport"),
            ConceptLinkEntity("c_electron_transport", "c_atp_synthase")
        )
        db.conceptLinkDao().insertLinks(links)

        // 6. Reset creature for fresh demo
        db.creatureDao().insertOrUpdate(
            CreatureEntity(id = 1, totalXp = 24, level = 2, evolutionStage = 1, streakDays = 3)
        )

        subjectId
    }
}
