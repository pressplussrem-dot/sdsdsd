package com.smartcalc.ai.util

import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Tiny in-memory channel used when the user taps a history entry
 * and wants to continue working with it in the calculator.
 */
object CalculatorHandoff {
    val pendingExpression = MutableStateFlow<String?>(null)

    fun reuse(expression: String) {
        pendingExpression.value = expression
    }

    fun consume(): String? {
        val value = pendingExpression.value
        pendingExpression.value = null
        return value
    }
}
