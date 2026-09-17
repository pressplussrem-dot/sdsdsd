package com.smartcalc.ai.calculator

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExpressionEvaluatorTest {

    private fun eval(expression: String) = ExpressionEvaluator.evaluate(expression)

    @Test
    fun `operator precedence`() {
        assertEquals(27.0, eval("12 + 5 \u00D7 3"), 1e-9)
        assertEquals(27.0, eval("12+5*3"), 1e-9)
        assertEquals(16.0, eval("8\u00F72\u00D7(2+2)"), 1e-9)
    }

    @Test
    fun `parentheses and implicit multiplication`() {
        assertEquals(20.0, eval("(2+3)*4"), 1e-9)
        assertEquals(14.0, eval("2(3+4)"), 1e-9)
        assertEquals(5.0, eval("((2+3)"), 1e-9) // unclosed parentheses are auto closed
    }

    @Test
    fun `decimals and negative numbers`() {
        assertEquals(2.5, eval("10/4"), 1e-9)
        assertEquals(-2.0, eval("-5+3"), 1e-9)
        assertEquals(5.0, eval("2-(-3)"), 1e-9)
        assertEquals(3.0, eval("1.5*2"), 1e-9)
    }

    @Test
    fun `percent semantics`() {
        assertEquals(220.0, eval("200+10%"), 1e-9)
        assertEquals(180.0, eval("200-10%"), 1e-9)
        assertEquals(20.0, eval("200*10%"), 1e-9)
        assertEquals(0.5, eval("50%"), 1e-9)
    }

    @Test
    fun `powers`() {
        assertEquals(8.0, eval("2^3"), 1e-9)
        assertEquals(-4.0, eval("-2^2"), 1e-9)
    }

    @Test
    fun `division by zero is reported`() {
        val error = runCatching { eval("100/(5-5)") }.exceptionOrNull()
        assertTrue(error is CalculationException)
        assertEquals(CalcErrorType.DIVISION_BY_ZERO, (error as CalculationException).type)
    }

    @Test
    fun `invalid expression is reported`() {
        val error = runCatching { eval("5++") }.exceptionOrNull()
        assertTrue(error is CalculationException)
        assertEquals(CalcErrorType.INVALID_EXPRESSION, (error as CalculationException).type)
        assertNull(ExpressionEvaluator.tryEvaluateToString("5++"))
    }

    @Test
    fun `formatting trims trailing zeros`() {
        assertEquals("27", ExpressionEvaluator.format(27.0))
        assertEquals("2.5", ExpressionEvaluator.format(2.5))
        assertEquals("0", ExpressionEvaluator.format(0.0))
        assertEquals("0.3333333333", ExpressionEvaluator.format(1.0 / 3.0))
    }
}
