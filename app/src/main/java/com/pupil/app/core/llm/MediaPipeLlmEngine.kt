package com.pupil.app.core.llm

import android.content.Context
import android.util.Log
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.pupil.app.core.config.AppConfig
import com.pupil.app.data.model.InferenceMetrics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileNotFoundException

class MediaPipeLlmEngine : LlmEngine {

    companion object {
        private const val TAG = "MediaPipeLlmEngine"
    }

    override val isMock: Boolean = false
    override val engineName: String = "MediaPipe GenAI (On-Device)"
    override var modelPath: String = AppConfig.DEVICE_MODEL_PATH
        private set

    private var llmInference: LlmInference? = null
    private var lastLoadError: String? = null

    /**
     * Lists all candidate paths where the model file might be located.
     */
    fun getCandidateFiles(context: Context): List<File> {
        val candidates = mutableListOf<File>()

        // 1. App external files dir: /sdcard/Android/data/com.pupil.app/files/models/
        val extModels = context.getExternalFilesDir("models")
        if (extModels != null) {
            candidates.add(File(extModels, AppConfig.DEFAULT_MODEL_FILENAME))
            if (extModels.exists() && extModels.isDirectory) {
                extModels.listFiles()?.forEach { file ->
                    if (file.isFile && (file.extension == "bin" || file.extension == "task")) {
                        candidates.add(file)
                    }
                }
            }
        }

        // 2. App internal files:
        candidates.add(File(context.filesDir, AppConfig.DEFAULT_MODEL_FILENAME))
        val internalModels = File(context.filesDir, "models")
        candidates.add(File(internalModels, AppConfig.DEFAULT_MODEL_FILENAME))
        if (internalModels.exists() && internalModels.isDirectory) {
            internalModels.listFiles()?.forEach { file ->
                if (file.isFile && (file.extension == "bin" || file.extension == "task")) {
                    candidates.add(file)
                }
            }
        }

        // 3. System and standard shared storage locations:
        candidates.add(File(AppConfig.DEVICE_MODEL_PATH))
        candidates.add(File("/sdcard/Download/${AppConfig.DEFAULT_MODEL_FILENAME}"))
        candidates.add(File("/sdcard/Download/model.bin"))
        candidates.add(File("/sdcard/Documents/${AppConfig.DEFAULT_MODEL_FILENAME}"))
        candidates.add(File(AppConfig.ALT_DEVICE_MODEL_PATH))
        candidates.add(File("/data/local/tmp/${AppConfig.DEFAULT_MODEL_FILENAME}"))

        return candidates.distinctBy { it.absolutePath }
    }

    /**
     * Resolves the actual model file on the device by checking standard locations.
     */
    fun resolveModelFile(context: Context): File? {
        val candidates = getCandidateFiles(context)
        Log.i(TAG, "Resolving LLM model file... Checking ${candidates.size} candidate path(s):")
        for (candidate in candidates) {
            val exists = candidate.exists()
            val canRead = candidate.canRead()
            val length = if (exists) candidate.length() else 0L
            Log.d(TAG, "  Candidate: [${candidate.absolutePath}] -> exists=$exists, canRead=$canRead, size=$length bytes")
            if (exists && canRead && length > 0) {
                Log.i(TAG, ">>> Found usable model file: ${candidate.absolutePath} ($length bytes)")
                return candidate
            }
        }
        Log.w(TAG, "No usable model file found among ${candidates.size} checked paths.")
        return null
    }

    override fun isModelPresent(context: Context): Boolean {
        val file = resolveModelFile(context)
        return file != null
    }

    override fun getModelFileInfo(context: Context): ModelFileInfo {
        val candidates = getCandidateFiles(context)
        val file = resolveModelFile(context)
        val paths = candidates.map { it.absolutePath }
        return if (file != null && file.exists()) {
            ModelFileInfo(
                fileName = file.name,
                resolvedPath = file.absolutePath,
                fileSizeBytes = file.length(),
                isPresent = true,
                checkedPaths = paths
            )
        } else {
            ModelFileInfo(
                fileName = AppConfig.DEFAULT_MODEL_FILENAME,
                resolvedPath = null,
                fileSizeBytes = 0L,
                isPresent = false,
                checkedPaths = paths
            )
        }
    }

    override suspend fun initialize(context: Context): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val file = resolveModelFile(context)
                ?: return@withContext Result.failure(
                    FileNotFoundException(
                        "Model file not found. Place '${AppConfig.DEFAULT_MODEL_FILENAME}' at " +
                                "${context.getExternalFilesDir("models")?.absolutePath} or ${AppConfig.ALT_DEVICE_MODEL_PATH}"
                    )
                )

            modelPath = file.absolutePath
            Log.d(TAG, "Initializing MediaPipe LlmInference with model: $modelPath (${file.length() / (1024 * 1024)} MB)")

            val options = LlmInference.LlmInferenceOptions.builder()
                .setModelPath(file.absolutePath)
                .setMaxTokens(AppConfig.MAX_TOKENS)
                .setMaxTopK(AppConfig.TOP_K)
                .build()

            llmInference = LlmInference.createFromOptions(context.applicationContext, options)
            Log.d(TAG, "MediaPipe LlmInference initialized successfully.")
            lastLoadError = null
            Result.success(Unit)
        } catch (e: Throwable) {
            lastLoadError = e.localizedMessage ?: e.toString()
            Log.e(TAG, "Failed to initialize MediaPipe engine", e)
            Result.failure(e)
        }
    }

    override fun getModelStatus(context: Context): com.pupil.app.core.config.ModelStatus {
        val fileInfo = getModelFileInfo(context)
        val error = lastLoadError
        return when {
            error != null -> com.pupil.app.core.config.ModelStatus.FailedToLoad(error, fileInfo.checkedPaths)
            llmInference != null -> com.pupil.app.core.config.ModelStatus.Ready(engineName, modelPath)
            fileInfo.isPresent && fileInfo.resolvedPath != null -> com.pupil.app.core.config.ModelStatus.Ready(engineName, fileInfo.resolvedPath)
            else -> com.pupil.app.core.config.ModelStatus.Missing(fileInfo.checkedPaths)
        }
    }

    override suspend fun generate(prompt: String, callType: String): Pair<String, InferenceMetrics> = withContext(Dispatchers.IO) {
        val engine = llmInference
            ?: throw IllegalStateException("MediaPipe engine is not initialized or model file is missing.")

        LlmMetricsTracker.measureInference(isMock = false, callType = callType) {
            engine.generateResponse(prompt)
        }
    }

    override fun close() {
        try {
            llmInference?.close()
        } catch (e: Exception) {
            Log.w(TAG, "Error closing LlmInference", e)
        } finally {
            llmInference = null
        }
    }
}
