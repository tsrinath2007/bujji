package com.pupil.app.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class GradingStatus(val serializedValue: String, val displayName: String) {
    @SerialName("understood")
    UNDERSTOOD("understood", "Understood"),

    @SerialName("partial")
    PARTIAL("partial", "Partially Understood"),

    @SerialName("missed")
    MISSED("missed", "Missed"),

    @SerialName("misconception")
    MISCONCEPTION("misconception", "Misconception"),

    @SerialName("unstudied")
    UNSTUDIED("unstudied", "Unstudied");

    companion object {
        fun fromString(value: String): GradingStatus {
            return when (value.trim().lowercase()) {
                "understood" -> UNDERSTOOD
                "partial" -> PARTIAL
                "misconception" -> MISCONCEPTION
                "unstudied" -> UNSTUDIED
                else -> MISSED
            }
        }
    }
}
