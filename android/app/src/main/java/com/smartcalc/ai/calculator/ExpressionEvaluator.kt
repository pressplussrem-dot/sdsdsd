package com.smartcalc.ai.calculator

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.util.Locale
import kotlin.math.abs
import kotlin.math.pow

/**
 * Offline expression evaluator with correct operator precedence.
 *
 * Grammar:
 *   expression := term (('+' | '-') term)*
 *   term       := unary (('*' | '/') unary | '(' ... ')')*      // implicit multiplication
 *   unary      := ('-' | '+') unary | power
 *   power      := postfix ('^' unary)?                          // right associative
 *   postfix    := primary '%'*
 *   primary    := number | '(' expression ')'
 *
 * Percent follows normal calculator semantics:
 *   200 + 10%  -> 220     (10% of the left operand)
 *   200 - 10%  -> 180
 *   200 * 10%  -> 20      (10% == 0.1)
 *   50%        -> 0.5
 */
object ExpressionEvaluator {

    private sealed interface Node
    private data class Num(val value: Double) : Node
    private data class Group(val inner: Node) : Node
    private data class Neg(val inner: Node) : Node
    private data class Pct(val inner: Node) : Node
    private data class Bin(val op: Char, val left: Node, val right: Node) : Node

    /** Evaluates [raw] and returns the numeric result. Throws [CalculationException]. */
    fun evaluate(raw: String): Double {
        val normalized = autoCloseParentheses(normalize(raw))
        if (normalized.isBlank()) throw CalculationException(CalcErrorType.EMPTY_EXPRESSION)

        val result = eval(Parser(normalized).parse())
        if (result.isNaN() || result.isInfinite()) {
            throw CalculationException(CalcErrorType.NUMBER_TOO_LARGE)
        }
        return result
    }

    /** Convenience: evaluate and format in one call. */
    fun evaluateToString(raw: String): String = format(evaluate(raw))

    /** Best-effort evaluation used for the live preview; null instead of an exception. */
    fun tryEvaluateToString(raw: String): String? = try {
        evaluateToString(raw)
    } catch (e: CalculationException) {
        null
    } catch (e: Exception) {
        null
    }

    /** Human friendly formatting: no trailing zeros, scientific notation for extremes. */
    fun format(value: Double): String {
        if (value.isNaN() || value.isInfinite()) {
            throw CalculationException(CalcErrorType.NUMBER_TOO_LARGE)
        }
        if (value == 0.0) return "0"

        val magnitude = abs(value)
        if (magnitude >= 1e12 || magnitude < 1e-9) {
            return String.format(Locale.US, "%.6E", value)
        }
        val trimmed = BigDecimal(value)
            .round(MathContext(12))
            .setScale(10, RoundingMode.HALF_UP)
            .stripTrailingZeros()
        return trimmed.toPlainString()
    }

    /** Maps display characters to plain ASCII operators. */
    fun normalize(input: String): String = input
        .replace('\u00D7', '*')   // ×
        .replace('\u00F7', '/')   // ÷
        .replace('\u2212', '-')   // −
        .replace('\u2013', '-')   // –
        .replace(',', '.')
        .replace('\u00A0', ' ')
        .trim()

    private fun autoCloseParentheses(input: String): String {
        val open = input.count { it == '(' }
        val close = input.count { it == ')' }
        return if (open > close) input + ")".repeat(open - close) else input
    }

    private fun eval(node: Node): Double = when (node) {
        is Num -> node.value
        is Group -> eval(node.inner)
        is Neg -> -eval(node.inner)
        is Pct -> eval(node.inner) / 100.0
        is Bin -> {
            val left = eval(node.left)
            val right = if ((node.op == '+' || node.op == '-') && node.right is Pct) {
                left * eval((node.right as Pct).inner) / 100.0
            } else {
                eval(node.right)
            }
            when (node.op) {
                '+' -> left + right
                '-' -> left - right
                '*' -> left * right
                '/' -> {
                    if (right == 0.0) throw CalculationException(CalcErrorType.DIVISION_BY_ZERO)
                    left / right
                }
                '^' -> left.pow(right)
                else -> throw CalculationException(CalcErrorType.INVALID_EXPRESSION)
            }
        }
    }

    private class Parser(private val src: String) {
        private var pos = 0

        fun parse(): Node {
            val node = parseExpression()
            if (peek() != null) throw CalculationException(CalcErrorType.INVALID_EXPRESSION)
            return node
        }

        private fun peek(): Char? {
            while (pos < src.length && src[pos] == ' ') pos++
            return if (pos < src.length) src[pos] else null
        }

        private fun parseExpression(): Node {
            var left = parseTerm()
            while (true) {
                val c = peek() ?: return left
                if (c == '+' || c == '-') {
                    pos++
                    left = Bin(c, left, parseTerm())
                } else {
                    return left
                }
            }
        }

        private fun parseTerm(): Node {
            var left = parseUnary()
            while (true) {
                when (peek()) {
                    '*', '/' -> {
                        val op = src[pos]
                        pos++
                        left = Bin(op, left, parseUnary())
                    }
                    '(' -> left = Bin('*', left, parseUnary()) // 2(3+4) == 2*(3+4)
                    else -> return left
                }
            }
        }

        private fun parseUnary(): Node {
            val c = peek() ?: throw CalculationException(CalcErrorType.INVALID_EXPRESSION)
            return when (c) {
                '-' -> { pos++; Neg(parseUnary()) }
                '+' -> { pos++; parseUnary() }
                else -> parsePower()
            }
        }

        private fun parsePower(): Node {
            val base = parsePostfix()
            if (peek() == '^') {
                pos++
                return Bin('^', base, parseUnary())
            }
            return base
        }

        private fun parsePostfix(): Node {
            var node = parsePrimary()
            while (peek() == '%') {
                pos++
                node = Pct(node)
            }
            return node
        }

        private fun parsePrimary(): Node {
            val c = peek() ?: throw CalculationException(CalcErrorType.INVALID_EXPRESSION)
            if (c == '(') {
                pos++
                val inner = parseExpression()
                if (peek() != ')') throw CalculationException(CalcErrorType.INVALID_EXPRESSION)
                pos++
                return Group(inner)
            }
            if (c.isDigit() || c == '.') {
                val start = pos
                var seenDot = false
                loop@ while (pos < src.length) {
                    val ch = src[pos]
                    when {
                        ch.isDigit() -> pos++
                        ch == '.' && !seenDot -> { seenDot = true; pos++ }
                        else -> break@loop
                    }
                }
                val text = src.substring(start, pos)
                val value = text.toDoubleOrNull()
                    ?: throw CalculationException(CalcErrorType.INVALID_EXPRESSION)
                return Num(value)
            }
            throw CalculationException(CalcErrorType.INVALID_EXPRESSION)
        }
    }
}
