package com.pupil.app.core.llm

import android.content.Context
import android.util.Log
import com.pupil.app.core.config.ModelStatus
import com.pupil.app.data.model.InferenceMetrics
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * LlmRouter — coordinates local on-device inference, optional Groq Cloud Boost,
 * and deterministic mock fallback.
 *
 * Ground rules:
 * - Default is ALWAYS local on-device (MediaPipe).
 * - Cloud Boost is strictly opt-in by the student.
 * - If Cloud Boost is enabled but fails (e.g. offline, rate limited), smoothly falls back to local.
 * - If local model weights are missing, falls back to MockLlmEngine with visible demo indicator.
 */
class LlmRouter(
    private val context: Context,
    private val localEngine: MediaPipeLlmEngine = MediaPipeLlmEngine(),
    private val groqEngine: GroqEngine = GroqEngine(),
    private val mockEngine: MockLlmEngine = MockLlmEngine()
) {

    companion object {
        private const val TAG = "LlmRouter"
    }

    private val prefs = context.getSharedPreferences("pupil_prefs", Context.MODE_PRIVATE)

    private val _activeEngineName = MutableStateFlow(localEngine.engineName)
    val activeEngineName: StateFlow<String> = _activeEngineName.asStateFlow()

    private val _isMockActive = MutableStateFlow(false)
    val isMockActive: StateFlow<Boolean> = _isMockActive.asStateFlow()

    init {
        updateEngineState()
    }

    private fun updateEngineState() {
        val hasGroq = isCloudBoostEnabled() && groqEngine.isModelPresent(context)
        val hasLocal = localEngine.isModelPresent(context)

        if (hasGroq) {
            _isMockActive.value = false
            _activeEngineName.value = groqEngine.engineName
        } else if (hasLocal) {
            _isMockActive.value = false
            _activeEngineName.value = localEngine.engineName
        } else {
            _isMockActive.value = true
            _activeEngineName.value = mockEngine.engineName
        }
    }

    fun isCloudBoostEnabled(): Boolean {
        val hasGroq = groqEngine.isModelPresent(context)
        val hasLocal = localEngine.isModelPresent(context)
        val defaultVal = hasGroq && !hasLocal
        return prefs.getBoolean("cloud_boost_enabled", defaultVal)
    }

    fun setCloudBoostEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("cloud_boost_enabled", enabled).apply()
        updateEngineState()
    }

    fun setGroqApiKey(apiKey: String?) {
        groqEngine.setCustomApiKey(apiKey)
        updateEngineState()
    }

    /**
     * Initializes candidate engines based on preferences and availability.
     */
    suspend fun initialize() {
        if (isCloudBoostEnabled() && groqEngine.isModelPresent(context)) {
            val res = groqEngine.initialize(context)
            if (res.isSuccess) {
                Log.i(TAG, "Groq Cloud Boost initialized successfully.")
                _isMockActive.value = false
                _activeEngineName.value = groqEngine.engineName
                return
            } else {
                Log.w(TAG, "Groq Cloud Boost init failed: ${res.exceptionOrNull()?.message}")
            }
        }

        if (localEngine.isModelPresent(context)) {
            val res = localEngine.initialize(context)
            if (res.isSuccess) {
                _isMockActive.value = false
                _activeEngineName.value = localEngine.engineName
                return
            }
        }

        // Neither local nor cloud is ready: mock engine is active
        _isMockActive.value = true
        _activeEngineName.value = mockEngine.engineName
        Log.w(TAG, "Neither local nor cloud models ready. Falling back to Mock engine.")
    }

    /**
     * Obtains the primary active LlmEngine.
     */
    fun getPrimaryEngine(): LlmEngine {
        return when {
            isCloudBoostEnabled() && groqEngine.isModelPresent(context) -> groqEngine
            localEngine.isModelPresent(context) -> localEngine
            else -> mockEngine
        }
    }

    /**
     * Executes prompt generation with resilient fallback:
     * Groq Cloud Boost (if enabled) -> MediaPipe Local -> Mock
     */
    suspend fun generate(prompt: String, callType: String = ""): Pair<String, InferenceMetrics> {
        if (isCloudBoostEnabled() && groqEngine.isModelPresent(context)) {
            try {
                Log.d(TAG, "Attempting inference via Groq Cloud Boost...")
                val result = groqEngine.generate(prompt, callType)
                _activeEngineName.value = groqEngine.engineName
                _isMockActive.value = false
                return result
            } catch (e: Exception) {
                Log.w(TAG, "Groq Cloud Boost failed (${e.localizedMessage}). Falling back to local engine.", e)
            }
        }

        if (localEngine.isModelPresent(context)) {
            try {
                val result = localEngine.generate(prompt, callType)
                _activeEngineName.value = localEngine.engineName
                _isMockActive.value = false
                return result
            } catch (e: Exception) {
                Log.w(TAG, "Local MediaPipe engine failed (${e.localizedMessage}). Falling back to mock.", e)
            }
        }

        // Final fallback: Mock
        _activeEngineName.value = mockEngine.engineName
        _isMockActive.value = true
        return mockEngine.generate(prompt, callType)
    }

    /**
     * Retrieves the overall model status (reports local status primarily).
     */
    fun getModelStatus(): ModelStatus {
        return if (isCloudBoostEnabled() && groqEngine.isModelPresent(context)) {
            groqEngine.getModelStatus(context)
        } else {
            localEngine.getModelStatus(context)
        }
    }

    fun getCheckedPaths(): List<String> {
        return localEngine.getCandidateFiles(context).map { it.absolutePath }
    }

    fun close() {
        localEngine.close()
        groqEngine.close()
        mockEngine.close()
    }
}
