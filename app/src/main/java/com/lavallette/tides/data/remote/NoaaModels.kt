package com.lavallette.tides.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NoaaPredictionsResponse(
    val predictions: List<NoaaPrediction>? = null,
    val error: NoaaError? = null
)

@Serializable
data class NoaaPrediction(
    val t: String,
    val v: String,
    val type: String? = null
)

@Serializable
data class NoaaError(
    @SerialName("message") val message: String? = null
)
