package com.pupil.app.core.llm

import android.content.Context
import android.util.Log
import com.pupil.app.BuildConfig
import com.pupil.app.core.config.ModelStatus
import com.pupil.app.data.model.InferenceMetrics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * GroqEngine — Optional Cloud Boost LLM engine.
 * Calls Groq's OpenAI-compatible API endpoint (https://api.groq.com/openai/v1).
 * Features:
 * - Dynamic model discovery via /v1/models (falls back to llama-3.3-70b-versatile or llama-3.1-8b-instant).
 * - Enforces daily call budget (default max 250 calls/day).
 * - Zero user data leakage: sends only the study prompt, never credentials, audio, or files.
 */
class GroqEngine(
    private var customApiKey: String? = null
) : LlmEngine {

    companion object {
        private const val TAG = "GroqEngine"
        private const val BASE_URL = "https://api.groq.com/openai/v1"
        private const val DEFAULT_MODEL = "qwen/qwen3.8-27b"
        private const val FALLBACK_MODEL = "openai/gpt-oss-120b"
        const val MAX_DAILY_CALLS = 250
    }

    override val isMock: Boolean = false
    override val engineName: String = "Groq Cloud Boost"
    override val modelPath: String get() = selectedModel

    private var selectedModel: String = DEFAULT_MODEL
    private var dailyCallsCount: Int = 0

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    private fun getActiveApiKey(): String {
        val key = customApiKey?.trim()
        if (!key.isNullOrBlank()) return key
        return try {
            BuildConfig.GROQ_API_KEY.trim()
        } catch (e: Throwable) {
            ""
        }
    }

    override fun isModelPresent(context: Context): Boolean {
        return getActiveApiKey().isNotBlank()
    }

    override fun getModelFileInfo(context: Context): ModelFileInfo {
        val hasKey = isModelPresent(context)
        return ModelFileInfo(
            fileName = selectedModel,
            resolvedPath = if (hasKey) "$BASE_URL ($selectedModel)" else null,
            fileSizeBytes = 0L,
            isPresent = hasKey,
            checkedPaths = listOf("BuildConfig.GROQ_API_KEY", "EncryptedSharedPreferences")
        )
    }

    override fun getModelStatus(context: Context): ModelStatus {
        val key = getActiveApiKey()
        return if (key.isNotBlank()) {
            ModelStatus.Ready(engineName, selectedModel)
        } else {
            ModelStatus.Missing(listOf("GROQ_API_KEY not configured in local.properties or Settings"))
        }
    }

    override suspend fun initialize(context: Context): Result<Unit> = withContext(Dispatchers.IO) {
        val apiKey = getActiveApiKey()
        if (apiKey.isBlank()) {
            return@withContext Result.failure(IllegalStateException("Groq API key is missing. Add GROQ_API_KEY to local.properties or Settings."))
        }

        try {
            // Dynamic model discovery via /v1/models
            val request = Request.Builder()
                .url("$BASE_URL/models")
                .header("Authorization", "Bearer $apiKey")
                .get()
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    if (!body.isNullOrBlank()) {
                        val json = JSONObject(body)
                        val dataArray = json.optJSONArray("data") ?: JSONArray()
                        val availableIds = mutableListOf<String>()
                        for (i in 0 until dataArray.length()) {
                            val id = dataArray.getJSONObject(i).optString("id")
                            if (id.isNotBlank()) availableIds.add(id)
                        }

                        Log.i(TAG, "Available Groq models ($availableIds.size): $availableIds")

                        val preferredModels = listOf(
                            "qwen/qwen3.8-27b",
                            "llama-3.3-70b-versatile",
                            "llama-3.1-8b-instant",
                            "openai/gpt-oss-120b",
                            "openai/gpt-oss-20b"
                        )

                        selectedModel = preferredModels.firstOrNull { availableIds.contains(it) }
                            ?: availableIds.firstOrNull { it.contains("oss") || it.contains("llama") || it.contains("qwen") }
                            ?: DEFAULT_MODEL

                        Log.i(TAG, "Dynamic model discovery selected: $selectedModel")
                    }
                } else {
                    Log.w(TAG, "Models discovery returned ${response.code}, using default: $DEFAULT_MODEL")
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.w(TAG, "Dynamic model discovery failed, falling back to default $DEFAULT_MODEL", e)
            selectedModel = DEFAULT_MODEL
            Result.success(Unit) // Soft failure: continue with default model
        }
    }

    override suspend fun generate(prompt: String, callType: String): Pair<String, InferenceMetrics> = withContext(Dispatchers.IO) {
        val apiKey = getActiveApiKey()
        if (apiKey.isBlank()) {
            throw IllegalStateException("Groq API key is not configured.")
        }

        if (dailyCallsCount >= MAX_DAILY_CALLS) {
            throw IllegalStateException("Daily Groq call budget reached ($MAX_DAILY_CALLS calls/day limit).")
        }

        LlmMetricsTracker.measureInference(isMock = false, callType = callType) {
            val jsonBody = JSONObject().apply {
                put("model", selectedModel)
                put("messages", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", prompt)
                    })
                })
                put("temperature", 0.2)
                // If call requires JSON output, request JSON mode (Groq requires the word 'json' in the prompt)
                if (prompt.contains("json", ignoreCase = true)) {
                    put("response_format", JSONObject().apply {
                        put("type", "json_object")
                    })
                }
            }

            val requestBody = jsonBody.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("$BASE_URL/chat/completions")
                .header("Authorization", "Bearer $apiKey")
                .post(requestBody)
                .build()

            val response = httpClient.newCall(request).execute()
            response.use { res ->
                if (!res.isSuccessful) {
                    val errBody = res.body?.string() ?: ""
                    throw IOException("Groq API error ${res.code}: $errBody")
                }

                val resString = res.body?.string() ?: throw IOException("Empty response from Groq")
                val resJson = JSONObject(resString)
                val choices = resJson.optJSONArray("choices")
                if (choices == null || choices.length() == 0) {
                    throw IOException("No completion choices returned by Groq")
                }

                dailyCallsCount++
                val choiceObj = choices.getJSONObject(0)
                val messageObj = choiceObj.getJSONObject("message")
                val content = messageObj.optString("content", "").trim()
                if (content.isNotBlank()) content
                else messageObj.optString("reasoning", "").trim()
            }
        }
    }

    fun setCustomApiKey(key: String?) {
        this.customApiKey = key
    }

    override fun close() {
        // OkHttpClient cleanup
    }
}
