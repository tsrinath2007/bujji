package com.pupil.app.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GradingResponse(
    @SerialName("status")
    val status: String = "missed",

    @SerialName("evidence")
    val evidence: String = "",

    @SerialName("followup_question")
    val followupQuestion: String = ""
)

data class GradingResult(
    val status: GradingStatus,
    val evidence: String,
    val followupQuestion: String,
    val metrics: InferenceMetrics? = null
)
