package com.pupil.app.core.voice

import android.content.Context
import android.media.MediaPlayer
import android.speech.tts.TextToSpeech
import android.util.Log
import com.pupil.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * ElevenLabsVoiceService — Voice narration for the Bujji creature.
 * Features:
 * - SHA-256 text hash audio caching to avoid redundant API hits and conserve quota.
 * - Smooth fallback to Android's built-in TextToSpeech engine when offline or without API key.
 * - Zero user data leakage: sends only creature dialogue text to TTS.
 */
class ElevenLabsVoiceService(
    private val context: Context,
    private var customApiKey: String? = null
) : TextToSpeech.OnInitListener {

    companion object {
        private const val TAG = "ElevenLabsVoiceService"
        private const val DEFAULT_VOICE_ID = "21m00Tcm4TlvDq8ikWAM" // Friendly female voice ("Rachel")
        private const val BASE_URL = "https://api.elevenlabs.io/v1/text-to-speech"
    }

    private var ttsEngine: TextToSpeech? = null
    private var isTtsReady = false
    private var mediaPlayer: MediaPlayer? = null

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    init {
        try {
            ttsEngine = TextToSpeech(context.applicationContext, this)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to initialize Android TextToSpeech engine", e)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = ttsEngine?.setLanguage(Locale.US)
            isTtsReady = result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED
            Log.d(TAG, "Android TextToSpeech initialized. isReady=$isTtsReady")
        } else {
            Log.w(TAG, "Android TextToSpeech initialization failed with status $status")
        }
    }

    private fun getActiveApiKey(): String {
        val key = customApiKey?.trim()
        if (!key.isNullOrBlank()) return key
        return try {
            BuildConfig.ELEVENLABS_API_KEY.trim()
        } catch (e: Throwable) {
            ""
        }
    }

    fun setCustomApiKey(key: String?) {
        this.customApiKey = key
    }

    private fun hashText(text: String, voiceId: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest("$voiceId:$text".toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Speaks the given text:
     * 1. Check local audio cache (MP3) -> play if present.
     * 2. Try ElevenLabs API -> download, save to cache, play.
     * 3. Fallback to on-device Android TextToSpeech.
     */
    suspend fun speak(text: String, voiceId: String = DEFAULT_VOICE_ID, onCompleted: (() -> Unit)? = null) = withContext(Dispatchers.IO) {
        val cleanText = text.trim()
        if (cleanText.isBlank()) return@withContext

        val apiKey = getActiveApiKey()
        val cacheDir = File(context.cacheDir, "voice_cache").apply { mkdirs() }
        val cacheFile = File(cacheDir, "${hashText(cleanText, voiceId)}.mp3")

        // 1. Check cache
        if (cacheFile.exists() && cacheFile.length() > 0) {
            Log.d(TAG, "Playing cached creature voice from: ${cacheFile.name}")
            playAudioFile(cacheFile, onCompleted)
            return@withContext
        }

        // 2. Try ElevenLabs API if key configured
        if (apiKey.isNotBlank()) {
            try {
                Log.d(TAG, "Requesting creature speech from ElevenLabs...")
                val jsonBody = JSONObject().apply {
                    put("text", cleanText)
                    put("model_id", "eleven_multilingual_v2")
                    put("voice_settings", JSONObject().apply {
                        put("stability", 0.5)
                        put("similarity_boost", 0.75)
                    })
                }

                val request = Request.Builder()
                    .url("$BASE_URL/$voiceId")
                    .header("xi-api-key", apiKey)
                    .header("Content-Type", "application/json")
                    .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val bodyBytes = response.body?.bytes()
                        if (bodyBytes != null && bodyBytes.isNotEmpty()) {
                            FileOutputStream(cacheFile).use { it.write(bodyBytes) }
                            Log.d(TAG, "Saved ElevenLabs audio (${bodyBytes.size} bytes) to cache")
                            playAudioFile(cacheFile, onCompleted)
                            return@withContext
                        }
                    } else {
                        Log.w(TAG, "ElevenLabs API responded with code ${response.code}")
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "ElevenLabs speech request failed: ${e.localizedMessage}. Falling back to on-device TTS.", e)
            }
        }

        // 3. Fallback to on-device TextToSpeech
        withContext(Dispatchers.Main) {
            speakOnDeviceTts(cleanText, onCompleted)
        }
    }

    private suspend fun playAudioFile(file: File, onCompleted: (() -> Unit)?) = withContext(Dispatchers.Main) {
        try {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                setDataSource(file.absolutePath)
                setOnCompletionListener {
                    it.release()
                    mediaPlayer = null
                    onCompleted?.invoke()
                }
                setOnErrorListener { mp, _, _ ->
                    mp.release()
                    mediaPlayer = null
                    false
                }
                prepare()
                start()
            }
        } catch (e: Exception) {
            Log.w(TAG, "MediaPlayer failed to play audio file, falling back to TTS", e)
            speakOnDeviceTts(file.name, onCompleted)
        }
    }

    private fun speakOnDeviceTts(text: String, onCompleted: (() -> Unit)?) {
        if (isTtsReady && ttsEngine != null) {
            ttsEngine?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "creature_tts_${System.currentTimeMillis()}")
            onCompleted?.invoke()
        } else {
            Log.w(TAG, "On-device TTS not ready to speak.")
            onCompleted?.invoke()
        }
    }

    fun stop() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping MediaPlayer", e)
        }
        try {
            ttsEngine?.stop()
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping TextToSpeech", e)
        }
    }

    fun destroy() {
        stop()
        try {
            ttsEngine?.shutdown()
            ttsEngine = null
        } catch (e: Exception) {
            Log.w(TAG, "Error shutting down TTS", e)
        }
    }
}
