package com.smartcalc.ai.data.remote

import com.google.gson.annotations.SerializedName

/**
 * Request sent to our own backend. The backend holds the AI API key,
 * the app never sees it.
 */
data class SolveRequestDto(
    @SerializedName("imageBase64") val imageBase64: String,
    @SerializedName("mimeType") val mimeType: String = "image/jpeg",
    @SerializedName("language") val language: String = "sr"
)

/**
 * Response from the backend.
 * status is one of: "ok", "unclear", "error".
 */
data class SolveResponseDto(
    @SerializedName("status") val status: String? = null,
    @SerializedName("problem") val problem: String? = null,
    @SerializedName("steps") val steps: List<String>? = null,
    @SerializedName("solution") val solution: String? = null,
    @SerializedName("confidence") val confidence: Double? = null,
    @SerializedName("message") val message: String? = null,
    @SerializedName("code") val code: String? = null
)
