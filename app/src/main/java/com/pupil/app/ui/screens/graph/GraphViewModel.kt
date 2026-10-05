package com.pupil.app.ui.screens.graph

import android.app.Application
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pupil.app.data.model.Concept
import com.pupil.app.data.repository.StudyRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

data class GraphNode(
    val concept: Concept,
    var position: Offset = Offset.Zero,
    var velocity: Offset = Offset.Zero
)

data class GraphEdge(
    val sourceId: String,
    val targetId: String
)

class GraphViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = StudyRepository(application)

    private val _nodes = MutableStateFlow<List<GraphNode>>(emptyList())
    val nodes: StateFlow<List<GraphNode>> = _nodes.asStateFlow()

    private val _edges = MutableStateFlow<List<GraphEdge>>(emptyList())
    val edges: StateFlow<List<GraphEdge>> = _edges.asStateFlow()

    private val _selectedConcept = MutableStateFlow<Concept?>(null)
    val selectedConcept: StateFlow<Concept?> = _selectedConcept.asStateFlow()

    private val positionCache = mutableMapOf<String, Offset>()

    /** Load Brain Map for a subject (new schema) */
    fun loadGraphForSubject(subjectId: String, initialSelectedId: String? = null) {
        viewModelScope.launch {
            repository.getConceptsForSubject(subjectId).collect { concepts ->
                buildGraph(concepts, subjectId, initialSelectedId)
            }
        }
    }

    /** Load Brain Map for a specific topic */
    fun loadGraphForTopic(topicId: String, initialSelectedId: String? = null) {
        viewModelScope.launch {
            repository.getConceptsForTopic(topicId).collect { concepts ->
                buildGraph(concepts, topicId, initialSelectedId)
            }
        }
    }

    private fun buildGraph(concepts: List<Concept>, cacheKey: String, initialSelectedId: String?) {
        if (concepts.isEmpty()) {
            _nodes.value = emptyList()
            _edges.value = emptyList()
            return
        }

        val edgeList = mutableListOf<GraphEdge>()
        val existingConceptIds = concepts.map { it.id }.toSet()

        // 1. Direct explicit links from extraction
        for (c in concepts) {
            for (link in c.links) {
                if (link in existingConceptIds && link != c.id) {
                    edgeList.add(GraphEdge(c.id, link))
                }
            }
        }

        // 2. Ensure NO disconnected / floating nodes:
        // Connect nodes within each topic or sequentially if nodes have 0 edges
        val connectedNodeIds = edgeList.flatMap { listOf(it.sourceId, it.targetId) }.toSet()
        val isolatedNodes = concepts.filter { it.id !in connectedNodeIds }

        if (isolatedNodes.isNotEmpty()) {
            // Group by topic first
            val byTopic = concepts.groupBy { it.topicId ?: "DEFAULT" }
            for ((_, topicConcepts) in byTopic) {
                for (i in 0 until topicConcepts.size - 1) {
                    val src = topicConcepts[i].id
                    val tgt = topicConcepts[i + 1].id
                    if (edgeList.none { (it.sourceId == src && it.targetId == tgt) || (it.sourceId == tgt && it.targetId == src) }) {
                        edgeList.add(GraphEdge(src, tgt))
                    }
                }
            }

            // If still any single isolated nodes remain, connect them to their nearest neighbor
            val stillConnected = edgeList.flatMap { listOf(it.sourceId, it.targetId) }.toSet()
            concepts.forEachIndexed { index, c ->
                if (c.id !in stillConnected && concepts.size > 1) {
                    val partnerIndex = if (index > 0) index - 1 else 1
                    val partnerId = concepts[partnerIndex].id
                    edgeList.add(GraphEdge(c.id, partnerId))
                }
            }
        }

        _edges.value = edgeList

        val nodeCount = concepts.size
        val center = Offset(500f, 500f)
        // Adjust radius dynamically based on node count to eliminate congestion
        val baseRadius = when {
            nodeCount <= 5 -> 220f
            nodeCount <= 10 -> 320f
            nodeCount <= 20 -> 420f
            else -> 520f
        }

        val computedNodes = concepts.mapIndexed { index, concept ->
            val pos = positionCache.getOrPut("${cacheKey}_${concept.id}") {
                // If more than 10 concepts, split into inner and outer constellation rings
                val isOuterRing = nodeCount > 8 && (index % 2 == 1)
                val ringRadius = if (isOuterRing) baseRadius * 1.35f else baseRadius
                val ringCount = if (nodeCount > 8) (nodeCount + 1) / 2 else nodeCount
                val ringIndex = if (nodeCount > 8) index / 2 else index
                val angle = (2 * Math.PI / ringCount) * ringIndex + (if (isOuterRing) Math.PI / ringCount else 0.0)

                val jitterX = (Random.nextFloat() - 0.5f) * 20f
                val jitterY = (Random.nextFloat() - 0.5f) * 20f
                Offset(
                    (center.x + ringRadius * cos(angle) + jitterX).toFloat(),
                    (center.y + ringRadius * sin(angle) + jitterY).toFloat()
                )
            }
            GraphNode(concept = concept, position = pos)
        }

        _nodes.value = computedNodes

        if (initialSelectedId != null) {
            _selectedConcept.value = concepts.firstOrNull { it.id == initialSelectedId }
        }
    }

    fun selectConcept(concept: Concept?) {
        _selectedConcept.value = concept
    }
}
