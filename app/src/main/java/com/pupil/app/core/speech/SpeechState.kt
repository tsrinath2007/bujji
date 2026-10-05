package com.pupil.app.core.speech

sealed class SpeechState {
    object Idle : SpeechState()
    object Listening : SpeechState()
    data class PartialResult(val text: String) : SpeechState()
    data class FinalResult(val text: String) : SpeechState()
    data class Error(val message: String, val isPermissionDenied: Boolean = false) : SpeechState()
}
