package com.pupil.app.data.model

data class InferenceMetrics(
    val latencyMs: Long,
    val memoryUsageMb: Double,
    val isMock: Boolean,
    val callType: String = "",
    val timestamp: Long = System.currentTimeMillis()
) {
    val displayLatencyText: String
        get() = if (isMock) {
            "N/A (Mock active)"
        } else {
            "${latencyMs}ms | ${String.format("%.1f", memoryUsageMb)}MB RAM"
        }
}
