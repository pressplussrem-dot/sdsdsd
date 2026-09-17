package com.smartcalc.ai.navigation

object Routes {
    const val CALCULATOR = "calculator"
    const val HISTORY = "history"

    /** source is either "camera" or "gallery". */
    const val SOLVER = "solver/{source}"

    fun solver(source: SolverSource): String = "solver/${source.value}"
}

enum class SolverSource(val value: String) {
    CAMERA("camera"),
    GALLERY("gallery");

    companion object {
        fun from(value: String?): SolverSource =
            entries.firstOrNull { it.value == value } ?: CAMERA
    }
}
