package com.smartcalc.ai.calculator

/** Every way the offline calculator can fail. Never crashes the app. */
enum class CalcErrorType {
    EMPTY_EXPRESSION,
    INVALID_EXPRESSION,
    DIVISION_BY_ZERO,
    NUMBER_TOO_LARGE
}

class CalculationException(val type: CalcErrorType) : Exception(type.name)
