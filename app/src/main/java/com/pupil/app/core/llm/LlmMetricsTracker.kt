package com.pupil.app.core.llm

import android.os.SystemClock
import com.pupil.app.data.model.InferenceMetrics
import java.util.concurrent.ConcurrentLinkedDeque

object LlmMetricsTracker {

    private val recentLatencies = ConcurrentLinkedDeque<Long>()
    @Volatile var lastMetrics: InferenceMetrics? = null
        private set
    @Volatile var peakMemoryObservedMb: Double = 0.0
        private set

    fun record(metrics: InferenceMetrics) {
        lastMetrics = metrics
        if (metrics.memoryUsageMb > peakMemoryObservedMb) {
            peakMemoryObservedMb = metrics.memoryUsageMb
        }
        if (!metrics.isMock && metrics.latencyMs > 0) {
            recentLatencies.addLast(metrics.latencyMs)
            while (recentLatencies.size > 10) {
                recentLatencies.pollFirst()
            }
        }
    }

    fun getAverageLatencyLast10(): Long {
        val list = recentLatencies.toList()
        if (list.isEmpty()) return 0L
        return list.sum() / list.size
    }

    fun getLastLatency(): Long = lastMetrics?.latencyMs ?: 0L

    fun getPeakMemory(): Double = maxOf(peakMemoryObservedMb, getUsedMemoryMb())

    /**
     * Executes block and records wall-clock duration and memory footprint.
     */
    inline fun <T> measureInference(
        isMock: Boolean,
        callType: String = "",
        block: () -> T
    ): Pair<T, InferenceMetrics> {
        val startMem = getUsedMemoryMb()
        val startTime = SystemClock.elapsedRealtime()

        val result = block()

        val duration = SystemClock.elapsedRealtime() - startTime
        val endMem = getUsedMemoryMb()
        val peakMem = maxOf(startMem, endMem)

        val metrics = InferenceMetrics(
            latencyMs = duration,
            memoryUsageMb = peakMem,
            isMock = isMock,
            callType = callType
        )
        record(metrics)
        return Pair(result, metrics)
    }

    suspend inline fun <T> measureInferenceSuspend(
        isMock: Boolean,
        callType: String = "",
        crossinline block: suspend () -> T
    ): Pair<T, InferenceMetrics> {
        val startMem = getUsedMemoryMb()
        val startTime = SystemClock.elapsedRealtime()

        val result = block()

        val duration = SystemClock.elapsedRealtime() - startTime
        val endMem = getUsedMemoryMb()
        val peakMem = maxOf(startMem, endMem)

        val metrics = InferenceMetrics(
            latencyMs = duration,
            memoryUsageMb = peakMem,
            isMock = isMock,
            callType = callType
        )
        record(metrics)
        return Pair(result, metrics)
    }

    fun getUsedMemoryMb(): Double {
        val runtime = Runtime.getRuntime()
        val usedBytes = runtime.totalMemory() - runtime.freeMemory()
        return usedBytes / (1024.0 * 1024.0)
    }
}
