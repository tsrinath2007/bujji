package com.pupil.app.core.speech

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SpeechToTextManager(private val context: Context) {

    companion object {
        private const val TAG = "SpeechToTextManager"
        private const val ERROR_SERVER_DISCONNECTED = 11
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private var speechRecognizer: SpeechRecognizer? = null
    private var hasRetriedError11 = false

    private val _state = MutableStateFlow<SpeechState>(SpeechState.Idle)
    val state: StateFlow<SpeechState> = _state.asStateFlow()

    fun isAvailable(): Boolean {
        return SpeechRecognizer.isRecognitionAvailable(context)
    }

    private var useStandardRecognizer = false

    private fun createRecognizer(): SpeechRecognizer {
        return if (!useStandardRecognizer && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && SpeechRecognizer.isOnDeviceRecognitionAvailable(context)) {
            Log.d(TAG, "Creating on-device SpeechRecognizer (API 31+)")
            try {
                SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
            } catch (e: Exception) {
                Log.w(TAG, "createOnDeviceSpeechRecognizer failed, falling back to standard", e)
                useStandardRecognizer = true
                SpeechRecognizer.createSpeechRecognizer(context)
            }
        } else {
            Log.d(TAG, "Creating standard SpeechRecognizer")
            SpeechRecognizer.createSpeechRecognizer(context)
        }
    }

    fun startListening(isRetry: Boolean = false) {
        if (!isRetry) {
            hasRetriedError11 = false
        }

        if (!isAvailable()) {
            _state.value = SpeechState.Error("Voice recognition service is not available on this device. You can type your explanation below.")
            return
        }

        try {
            // Destroy any previous instance to ensure a fresh session
            cleanupRecognizer()

            speechRecognizer = createRecognizer().apply {
                setRecognitionListener(createListener())
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            }

            _state.value = SpeechState.Listening
            speechRecognizer?.startListening(intent)
        } catch (e: SecurityException) {
            cleanupRecognizer()
            _state.value = SpeechState.Error("Microphone permission denied. You can type your explanation below.", isPermissionDenied = true)
        } catch (e: Exception) {
            cleanupRecognizer()
            Log.e(TAG, "Failed to start speech recognizer", e)
            _state.value = SpeechState.Error("Voice input wasn't available right now. You can type your explanation below.")
        }
    }

    fun stopListening() {
        cleanupRecognizer()
        if (_state.value is SpeechState.Listening) {
            _state.value = SpeechState.Idle
        }
    }

    fun destroy() {
        stopListening()
    }

    private fun cleanupRecognizer() {
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.destroy()
        } catch (e: Exception) {
            Log.w(TAG, "Error cleaning up SpeechRecognizer", e)
        } finally {
            speechRecognizer = null
        }
    }

    private fun createListener(): RecognitionListener {
        return object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                _state.value = SpeechState.Listening
            }

            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}

            override fun onError(error: Int) {
                Log.w(TAG, "SpeechRecognizer onError: $error")
                cleanupRecognizer()

                // Automatic retry with standard recognizer for error 11 (ERROR_SERVER_DISCONNECTED), error 13 (ERROR_LANGUAGE_UNAVAILABLE / on-device speech packs missing), or client error
                if ((error == ERROR_SERVER_DISCONNECTED || error == SpeechRecognizer.ERROR_CLIENT || error == 13) && !hasRetriedError11) {
                    hasRetriedError11 = true
                    useStandardRecognizer = true
                    Log.i(TAG, "SpeechRecognizer error $error encountered with on-device model. Retrying with standard recognizer...")
                    mainHandler.postDelayed({
                        startListening(isRetry = true)
                    }, 250)
                    return
                }

                // Friendly message without raw codes, pointing to text typing fallback
                val friendlyMessage = when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> "Audio recording issue. You can type your explanation below."
                    SpeechRecognizer.ERROR_CLIENT -> "Voice service didn't respond. You can type your explanation below."
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission required for voice teaching."
                    SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Offline voice recognition is setting up. You can type your explanation below."
                    SpeechRecognizer.ERROR_NO_MATCH -> "Didn't catch that. Tap the mic to try again, or type below."
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Voice service is busy. Please try again in a moment or type below."
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech detected. Tap the mic to try again, or type below."
                    ERROR_SERVER_DISCONNECTED -> "Voice service briefly disconnected. You can type your explanation below."
                    13 -> "Voice language pack is downloading or unavailable. You can type your explanation below."
                    else -> "Voice input wasn't available right now. You can type your explanation below."
                }

                _state.value = SpeechState.Error(
                    message = friendlyMessage,
                    isPermissionDenied = (error == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS)
                )
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val recognizedText = matches?.firstOrNull() ?: ""
                _state.value = SpeechState.FinalResult(recognizedText)
                cleanupRecognizer()
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val partialText = matches?.firstOrNull() ?: ""
                if (partialText.isNotBlank()) {
                    _state.value = SpeechState.PartialResult(partialText)
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        }
    }
}
