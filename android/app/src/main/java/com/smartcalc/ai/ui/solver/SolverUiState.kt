package com.smartcalc.ai.ui.solver

import androidx.annotation.StringRes
import java.io.File

sealed interface SolverUiState {

    /** Waiting for the user to take or pick a picture. */
    data object AwaitingImage : SolverUiState

    /** Image is ready, the user may retake it or send it to the AI backend. */
    data class ImageReady(
        val file: File,
        @StringRes val warningRes: Int? = null
    ) : SolverUiState

    data class Solving(val file: File) : SolverUiState

    data class Solved(
        val file: File,
        val problem: String,
        val steps: List<String>,
        val solution: String,
        val saved: Boolean = false
    ) : SolverUiState

    /** The AI was not confident enough. We never invent an answer. */
    data class Unclear(val file: File?) : SolverUiState

    data class Error(
        @StringRes val messageRes: Int,
        val file: File? = null,
        val canRetrySolve: Boolean = false
    ) : SolverUiState
}
