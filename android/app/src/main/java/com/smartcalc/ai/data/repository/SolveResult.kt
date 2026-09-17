package com.smartcalc.ai.data.repository

/** Everything the solver can hand back to the UI. No exceptions leak upwards. */
sealed interface SolveResult {

    data class Success(
        val problem: String,
        val steps: List<String>,
        val solution: String,
        val confidence: Double?
    ) : SolveResult

    /** The AI could not read the problem reliably. Never invent an answer. */
    data object Unclear : SolveResult

    data class Failure(val reason: FailureReason) : SolveResult
}

enum class FailureReason {
    NO_INTERNET,
    BACKEND_UNAVAILABLE,
    TIMEOUT,
    AI_ERROR,
    INVALID_RESPONSE,
    INVALID_IMAGE
}
