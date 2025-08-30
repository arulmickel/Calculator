package com.example.calccore

import org.junit.Assert.*
import org.junit.Test

class CalculatorEngineTest {

    @Test
    fun add_basic() {
        val eng = CalculatorEngine()
        eng.inputDigit('1'); eng.inputDigit('2')
        eng.setOperator(Operator.ADD)
        eng.inputDigit('3')
        eng.equalsPress()
        assertEquals("15", eng.displayValue)
    }

    @Test
    fun div_by_zero() {
        val eng = CalculatorEngine()
        eng.inputDigit('5')
        eng.setOperator(Operator.DIV)
        eng.inputDigit('0')
        try {
            eng.equalsPress()
            fail("Should have thrown")
        } catch (e: ArithmeticException) {
            // expected
        }
    }

    @Test
    fun trig_deg() {
        val eng = CalculatorEngine(AngleMode.DEG)
        eng.inputDigit('9'); eng.inputDigit('0')
        eng.applyTrig(TrigFunction.SIN)
        assertEquals("1", eng.displayValue) // sin(90°) ≈ 1
    }
}