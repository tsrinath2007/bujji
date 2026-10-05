package com.pupil.app.core.llm

import android.content.Context
import com.pupil.app.data.model.InferenceMetrics

data class ModelFileInfo(
    val fileName: String,
    val resolvedPath: String?,
    val fileSizeBytes: Long,
    val isPresent: Boolean,
    val checkedPaths: List<String> = emptyList()
)

interface LlmEngine {
    val isMock: Boolean
    val engineName: String
    val modelPath: String

    /**
     * Checks if the required local model file is accessible on device storage.
     */
    fun isModelPresent(context: Context): Boolean

    /**
     * Gets file information about the model weights on disk.
     */
    fun getModelFileInfo(context: Context): ModelFileInfo

    /**
     * Initializes the inference engine (e.g. loads weights into GPU/NPU/CPU memory).
     */
    suspend fun initialize(context: Context): Result<Unit>

    /**
     * Runs prompt inference synchronously on a background dispatcher,
     * measuring latency and memory.
     */
    suspend fun generate(prompt: String, callType: String = ""): Pair<String, InferenceMetrics>

    /**
     * Returns the structured status of the model (Ready, Missing, FailedToLoad).
     */
    fun getModelStatus(context: Context): com.pupil.app.core.config.ModelStatus {
        val info = getModelFileInfo(context)
        return if (info.isPresent && info.resolvedPath != null) {
            com.pupil.app.core.config.ModelStatus.Ready(engineName, info.resolvedPath)
        } else {
            com.pupil.app.core.config.ModelStatus.Missing(info.checkedPaths)
        }
    }

    /**
     * Releases native memory/model weights.
     */
    fun close()
}
