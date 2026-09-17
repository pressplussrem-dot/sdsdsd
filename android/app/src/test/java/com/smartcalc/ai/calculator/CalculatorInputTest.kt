package com.smartcalc.ai.calculator

import org.junit.Assert.assertEquals
import org.junit.Test

class CalculatorInputTest {

    @Test
    fun `operators are replaced instead of stacked`() {
        assertEquals("12+", CalculatorInput.operator("12+", '+'))
        assertEquals("12\u00D7", CalculatorInput.operator("12+", '\u00D7'))
    }

    @Test
    fun `only one decimal point per number`() {
        assertEquals("1.5", CalculatorInput.decimal("1.5"))
        assertEquals("1.", CalculatorInput.decimal("1"))
        assertEquals("0.", CalculatorInput.decimal(""))
    }

    @Test
    fun `parenthesis opens and closes correctly`() {
        assertEquals("(", CalculatorInput.parenthesis(""))
        assertEquals("(2)", CalculatorInput.parenthesis("(2"))
        assertEquals("(2)(", CalculatorInput.parenthesis("(2)"))
    }

    @Test
    fun `toggle sign wraps and unwraps the last number`() {
        assertEquals("(-5)", CalculatorInput.toggleSign("5"))
        assertEquals("5", CalculatorInput.toggleSign("(-5)"))
        assertEquals("12+(-5)", CalculatorInput.toggleSign("12+5"))
    }

    @Test
    fun `percent only after a number`() {
        assertEquals("50%", CalculatorInput.percent("50"))
        assertEquals("50+", CalculatorInput.percent("50+"))
    }

    @Test
    fun `backspace never fails on empty input`() {
        assertEquals("", CalculatorInput.backspace(""))
        assertEquals("1", CalculatorInput.backspace("12"))
    }
}
