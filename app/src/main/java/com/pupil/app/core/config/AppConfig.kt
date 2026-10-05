package com.pupil.app.core.config

object AppConfig {
    // Model configuration
    // The MediaPipe LLM model file (.bin or .task) can be pushed to either of these paths
    const val DEFAULT_MODEL_FILENAME = "gemma-2b-it-gpu-int4.bin"
    const val DEVICE_MODEL_PATH = "/sdcard/Android/data/com.pupil.app/files/models/$DEFAULT_MODEL_FILENAME"
    const val ALT_DEVICE_MODEL_PATH = "/data/local/tmp/model.bin"

    // Inference parameters
    const val MAX_TOKENS = 1024
    const val TOP_K = 40
    const val TEMPERATURE = 0.2f
    const val RANDOM_SEED = 42

    // Teaching & extraction chunking
    const val TARGET_CONCEPTS_PER_SECTION_MIN = 3
    const val TARGET_CONCEPTS_PER_SECTION_MAX = 6
    const val MAX_CHUNK_CHAR_LENGTH = 2000

    // Branding
    const val APP_NAME = com.pupil.app.core.AppConstants.APP_NAME
    const val CREATURE_NAME = com.pupil.app.core.AppConstants.APP_NAME
    const val TAGLINE = "Teach it. It grows."

    // Spaced revision intervals (Days)
    val REVISION_INTERVALS_DAYS = listOf(7, 14, 30)
}

sealed class ModelStatus {
    data class Ready(val engineName: String, val modelPath: String) : ModelStatus()
    data class Missing(val checkedPaths: List<String>) : ModelStatus()
    data class FailedToLoad(val error: String, val checkedPaths: List<String>) : ModelStatus()
}
