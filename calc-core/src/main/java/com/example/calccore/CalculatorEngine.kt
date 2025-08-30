package com.example.calccore

import kotlin.math.*

enum class Operator { ADD, SUB, MUL, DIV }
enum class TrigFunction { SIN, COS, TAN }
enum class AngleMode { DEG, RAD }

class CalculatorEngine(var angleMode: AngleMode = AngleMode.DEG) {

    private var input: String = ""
    private var storedValue: Double? = null
    private var pendingOp: Operator? = null
    private var lastResult: Double? = null
    private var afterEquals: Boolean = false

    val expression: String
        get() {
            val lhs = storedValue?.let { trimDouble(it) } ?: ""
            val op = pendingOp?.let { " " + opSymbol(it) + " " } ?: ""
            val rhs = if (input.isEmpty()) "" else input
            return (lhs + op + rhs).trim()
        }

    val displayValue: String
        get() = when {
            input.isNotEmpty() -> input
            lastResult != null -> trimDouble(lastResult!!)
            storedValue != null -> trimDouble(storedValue!!)
            else -> "0"
        }

    fun allClear() {
        input = ""
        storedValue = null
        pendingOp = null
        lastResult = null
        afterEquals = false
    }

    fun deleteLast() { if (input.isNotEmpty()) input = input.dropLast(1) }

    fun toggleSign() {
        input = if (input.startsWith("-")) input.drop(1) else if (input.isNotEmpty()) "-$input" else input
    }

    fun inputDigit(c: Char) {
        if (!c.isDigit()) return
        if (afterEquals) allClear()
        if (input == "0") input = c.toString() else input += c
    }

    fun inputDot() {
        if (afterEquals) allClear()
        if (!input.contains(".")) input = if (input.isEmpty()) "0." else "$input."
    }

    fun setOperator(op: Operator) {
        if (pendingOp != null && input.isNotEmpty()) {
            computeEquals()
        } else if (storedValue == null && input.isNotEmpty()) {
            storedValue = input.toDoubleOrNull()
        } else if (storedValue == null && input.isEmpty() && lastResult != null) {
            storedValue = lastResult
        }
        pendingOp = op
        input = ""
        afterEquals = false
    }

    fun applyTrig(fn: TrigFunction) {
        val value = when {
            input.isNotEmpty() -> input.toDoubleOrNull()
            lastResult != null -> lastResult
            storedValue != null -> storedValue
            else -> 0.0
        } ?: 0.0

        val arg = if (angleMode == AngleMode.DEG) Math.toRadians(value) else value
        val res = when (fn) {
            TrigFunction.SIN -> sin(arg)
            TrigFunction.COS -> cos(arg)
            TrigFunction.TAN -> tan(arg)
        }

        input = trimDouble(res)
        lastResult = res
    }

    fun equalsPress() {
        computeEquals()
        afterEquals = true
    }

    private fun computeEquals() {
        val rhs = input.toDoubleOrNull() ?: storedValue ?: lastResult ?: 0.0
        val lhs = storedValue ?: lastResult ?: 0.0
        val op = pendingOp
        val result = if (op == null) rhs else {
            when (op) {
                Operator.ADD -> lhs + rhs
                Operator.SUB -> lhs - rhs
                Operator.MUL -> lhs * rhs
                Operator.DIV -> {
                    if (rhs == 0.0) throw ArithmeticException("Divide by zero")
                    lhs / rhs
                }
            }
        }
        lastResult = result
        storedValue = result
        pendingOp = null
        input = ""
    }

    private fun opSymbol(op: Operator) = when (op) {
        Operator.ADD -> "+"
        Operator.SUB -> "−"
        Operator.MUL -> "×"
        Operator.DIV -> "÷"
    }

    private fun trimDouble(d: Double): String {
        val s = "%,.10f".format(d).replace(",", "")
        return s.trimEnd('0').trimEnd('.')
    }
}
