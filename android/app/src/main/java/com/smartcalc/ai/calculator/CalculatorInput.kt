package com.smartcalc.ai.calculator

/**
 * Pure functions that transform the current expression string when a key is pressed.
 * Keeping them free of Android types makes them unit-testable.
 */
object CalculatorInput {

    private const val OPERATORS = "+-\u00D7\u00F7"
    private val numberTail = Regex("(\\d+\\.?\\d*)$")
    private val negatedTail = Regex("\\(-(\\d+\\.?\\d*)\\)$")

    fun digit(expression: String, digit: Char): String {
        // Do not allow a number to start with several leading zeros.
        val tail = numberTail.find(expression)?.value
        if (tail == "0") return expression.dropLast(1) + digit
        return expression + digit
    }

    fun decimal(expression: String): String {
        val tail = numberTail.find(expression)?.value
        return when {
            tail == null -> if (expression.isEmpty() || expression.last() == '(') {
                "${expression}0."
            } else if (expression.last() in OPERATORS) {
                "${expression}0."
            } else {
                expression // after ')' or '%' a decimal point makes no sense
            }
            tail.contains('.') -> expression
            else -> "$expression."
        }
    }

    fun operator(expression: String, op: Char): String {
        if (expression.isEmpty()) return if (op == '-') "-" else expression
        val last = expression.last()
        return when {
            last in OPERATORS -> expression.dropLast(1) + op          // replace operator
            last == '(' -> if (op == '-') expression + op else expression
            last == '.' -> expression.dropLast(1) + op
            else -> expression + op
        }
    }

    fun parenthesis(expression: String): String {
        val open = expression.count { it == '(' }
        val close = expression.count { it == ')' }
        if (expression.isEmpty()) return "("
        val last = expression.last()
        val canClose = open > close && (last.isDigit() || last == ')' || last == '%')
        return if (canClose) "$expression)" else "$expression("
    }

    fun percent(expression: String): String {
        if (expression.isEmpty()) return expression
        val last = expression.last()
        return if (last.isDigit() || last == ')') "$expression%" else expression
    }

    fun toggleSign(expression: String): String {
        if (expression.isEmpty()) return "-"
        negatedTail.find(expression)?.let { match ->
            val number = match.groupValues[1]
            return expression.removeRange(match.range) + number
        }
        numberTail.find(expression)?.let { match ->
            val number = match.groupValues[1]
            return expression.removeRange(match.range) + "(-$number)"
        }
        return expression
    }

    fun backspace(expression: String): String =
        if (expression.isEmpty()) expression else expression.dropLast(1)

    fun clear(): String = ""
}
