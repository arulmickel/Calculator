package com.example.calculator

import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.calculator.databinding.ActivityMainBinding
import com.example.calccore.AngleMode
import com.example.calccore.CalculatorEngine
import com.example.calccore.Operator
import com.example.calccore.TrigFunction

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var engine = CalculatorEngine(AngleMode.DEG)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        fun updateDisplay() {
            binding.tvExpression.text = engine.expression
            binding.tvResult.text = engine.displayValue
        }

        listOf(binding.btn0, binding.btn1, binding.btn2, binding.btn3, binding.btn4,
            binding.btn5, binding.btn6, binding.btn7, binding.btn8, binding.btn9).forEach { btn ->
            btn.setOnClickListener {
                engine.inputDigit((it as Button).text.first())
                updateDisplay()
            }
        }

        binding.btnDot.setOnClickListener { engine.inputDot(); updateDisplay() }

        binding.btnAdd.setOnClickListener { engine.setOperator(Operator.ADD); updateDisplay() }
        binding.btnSub.setOnClickListener { engine.setOperator(Operator.SUB); updateDisplay() }
        binding.btnMul.setOnClickListener { engine.setOperator(Operator.MUL); updateDisplay() }
        binding.btnDiv.setOnClickListener { engine.setOperator(Operator.DIV); updateDisplay() }

        binding.btnEq.setOnClickListener {
            try {
                engine.equalsPress(); updateDisplay()
            } catch (e: ArithmeticException) {
                Toast.makeText(this, getString(R.string.error_div_zero), Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnAC.setOnClickListener { engine.allClear(); updateDisplay() }
        binding.btnDel.setOnClickListener { engine.deleteLast(); updateDisplay() }
        binding.btnSign.setOnClickListener { engine.toggleSign(); updateDisplay() }

        binding.btnSin.setOnClickListener { engine.applyTrig(TrigFunction.SIN); updateDisplay() }
        binding.btnCos.setOnClickListener { engine.applyTrig(TrigFunction.COS); updateDisplay() }
        binding.btnTan.setOnClickListener { engine.applyTrig(TrigFunction.TAN); updateDisplay() }

        binding.btnMode.setOnClickListener {
            engine.angleMode = if (engine.angleMode == AngleMode.DEG) AngleMode.RAD else AngleMode.DEG
            binding.btnMode.text = if (engine.angleMode == AngleMode.DEG) getString(R.string.deg) else getString(R.string.rad)
            updateDisplay()
        }

        updateDisplay()
    }
}