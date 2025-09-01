package com.example.calculator

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.example.calculator.databinding.ActivityMainBinding
import com.example.calculator.history.HistoryItem
import com.example.calculator.history.HistoryStore
import com.example.calccore.AngleMode
import com.example.calccore.CalculatorEngine.EngineState
import com.example.calccore.Operator
import com.example.calccore.TrigFunction

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val vm: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        // Apply saved theme BEFORE inflating views (prevents flicker)
        AppCompatDelegate.setDefaultNightMode(
            if (ThemePrefs.isDark(this)) AppCompatDelegate.MODE_NIGHT_YES
            else AppCompatDelegate.MODE_NIGHT_NO
        )

        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inflate the menu directly on the toolbar
        binding.toolbar.inflateMenu(R.menu.menu_main)

        // Set initial title & icon based on saved mode
        binding.toolbar.menu.findItem(R.id.action_theme)?.title =
            if (ThemePrefs.isDark(this)) getString(R.string.theme_light) else getString(R.string.theme_dark)

        // Handle clicks(which switch theme)
        binding.toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_history -> {
                    showHistoryDialog()
                    true
                }
                R.id.action_theme -> {
                    val newDark = !ThemePrefs.isDark(this)
                    ThemePrefs.setDark(this, newDark) // triggers Activity recreate
                    // Update the title immediately for feedback
                    item.title = if (newDark) getString(R.string.theme_light) else getString(R.string.theme_dark)
                    true
                }
                else -> false
            }
        }

        // - Restore engine snapshot if present (handles process "OnDeath" state too) -
        savedInstanceState?.let { bundle ->
            val restored: EngineState? = if (Build.VERSION.SDK_INT >= 33) {
                bundle.getParcelable("engine", EngineState::class.java)
            } else {
                @Suppress("DEPRECATION")
                bundle.getParcelable("engine")
            }
            restored?.let { vm.engine.restore(it) }
        }

        fun updateDisplay() {
            binding.tvExpression.text = vm.engine.expression
            binding.tvResult.text = vm.engine.displayValue
        }

        // assigning numbers to these buttons
        listOf(
            binding.btn0, binding.btn1, binding.btn2, binding.btn3, binding.btn4,
            binding.btn5, binding.btn6, binding.btn7, binding.btn8, binding.btn9
        ).forEach { btn ->
            btn.setOnClickListener {
                vm.engine.inputDigit((it as Button).text.first())
                updateDisplay()
            }
        }

        binding.btnDot.setOnClickListener { vm.engine.inputDot(); updateDisplay() }

        // assigning operators "OnClick" state activity
        binding.btnAdd.setOnClickListener { vm.engine.setOperator(Operator.ADD); updateDisplay() }
        binding.btnSub.setOnClickListener { vm.engine.setOperator(Operator.SUB); updateDisplay() }
        binding.btnMul.setOnClickListener { vm.engine.setOperator(Operator.MUL); updateDisplay() }
        binding.btnDiv.setOnClickListener { vm.engine.setOperator(Operator.DIV); updateDisplay() }

        // this is the one which equals with divide-by-zero safety block
        binding.btnEq.setOnClickListener {
            // Capture the expression BEFORE equals (engine may clear input/replace state)
            val expr = vm.engine.expression.ifBlank { binding.tvExpression.text.toString() }
            try {
                vm.engine.equalsPress()
                updateDisplay()
                val res = vm.engine.displayValue
                if (expr.isNotBlank()) {
                    HistoryStore.add(this, HistoryItem(expression = expr, result = res))
                }
            } catch (e: ArithmeticException) {
                Toast.makeText(this, getString(R.string.error_div_zero), Toast.LENGTH_SHORT).show()
            }
        }

        // misc
        binding.btnAC.setOnClickListener { vm.engine.allClear(); updateDisplay() }
        binding.btnDel.setOnClickListener { vm.engine.deleteLast(); updateDisplay() }
        binding.btnSign.setOnClickListener { vm.engine.toggleSign(); updateDisplay() }

        // trig functions
        binding.btnSin.setOnClickListener { vm.engine.applyTrig(TrigFunction.SIN); updateDisplay() }
        binding.btnCos.setOnClickListener { vm.engine.applyTrig(TrigFunction.COS); updateDisplay() }
        binding.btnTan.setOnClickListener { vm.engine.applyTrig(TrigFunction.TAN); updateDisplay() }

        // angle unit(toggle option)
        binding.btnMode.setOnClickListener {
            vm.engine.angleMode =
                if (vm.engine.angleMode == AngleMode.DEG) AngleMode.RAD else AngleMode.DEG
            binding.btnMode.text =
                if (vm.engine.angleMode == AngleMode.DEG) getString(R.string.deg) else getString(R.string.rad)
            updateDisplay()
        }

        updateDisplay()
    }

    // - History UI helpers (class members) -
    private fun showHistoryDialog() {
        val items = HistoryStore.get(this)
        if (items.isEmpty()) {
            AlertDialog.Builder(this)
                .setTitle(getString(R.string.history))
                .setMessage(getString(R.string.history_empty))
                .setPositiveButton(android.R.string.ok, null)
                .show()
            return
        }

        val display = items.map { "${it.expression} = ${it.result}" }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.history))
            .setItems(display) { _, which ->
                val chosen = items[which]
                copyToClipboard(chosen.result)
                Toast.makeText(this, getString(R.string.copied), Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(getString(R.string.history_clear)) { _, _ ->
                HistoryStore.clear(this)
            }
            .setPositiveButton(android.R.string.cancel, null)
            .show()
    }

    private fun copyToClipboard(text: String) {
        val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("result", text))
    }

    //- Save engine snapshot so state persists across process "OnDeath" state while switch over other apps -
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable("engine", vm.engine.snapshot())
    }
}
