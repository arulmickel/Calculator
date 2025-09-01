package com.example.calculator

import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.Toast
import androidx.activity.viewModels   // created this to keep the results even screen changes to landscape or portrait
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.example.calculator.databinding.ActivityMainBinding
import com.example.calccore.AngleMode
import com.example.calccore.Operator
import com.example.calccore.TrigFunction
import com.example.calccore.CalculatorEngine.EngineState   // using this one for restoring state(Snapshot)

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val vm: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {

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
        val themeItem = binding.toolbar.menu.findItem(R.id.action_theme)
        val isDarkNow = ThemePrefs.isDark(this)
        themeItem.title = if (isDarkNow) getString(R.string.theme_light) else getString(R.string.theme_dark)
        themeItem.setIcon(if (isDarkNow) R.drawable.ic_light_mode_24 else R.drawable.ic_dark_mode_24)

        // Handle clicks(which switch theme)
        binding.toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_theme -> {
                    val newDark = !ThemePrefs.isDark(this)
                    ThemePrefs.setDark(this, newDark) // triggers theme change

                    // Updating menu icon
                    item.title = if (newDark) getString(R.string.theme_light) else getString(R.string.theme_dark)
                    item.setIcon(if (newDark) R.drawable.ic_light_mode_24 else R.drawable.ic_dark_mode_24)
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
        listOf(binding.btn0, binding.btn1, binding.btn2, binding.btn3, binding.btn4,
            binding.btn5, binding.btn6, binding.btn7, binding.btn8, binding.btn9).forEach { btn ->
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
            try {
                vm.engine.equalsPress(); updateDisplay()
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
            vm.engine.angleMode = if (vm.engine.angleMode == AngleMode.DEG) AngleMode.RAD else AngleMode.DEG
            binding.btnMode.text = if (vm.engine.angleMode == AngleMode.DEG) getString(R.string.deg) else getString(R.string.rad)
            updateDisplay()
        }

        updateDisplay()
    }

    // - tried toolbar menu to keep dark/light modes -
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        // Set the initial title based on current mode
        menu.findItem(R.id.action_theme)?.title =
            if (ThemePrefs.isDark(this)) getString(R.string.theme_light) else getString(R.string.theme_dark)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_theme -> {
                val newDark = !ThemePrefs.isDark(this)
                ThemePrefs.setDark(this, newDark)
                // Title will be refreshed after recreate; update now for UX
                item.title = if (newDark) getString(R.string.theme_light) else getString(R.string.theme_dark)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    //- Save engine snapshot so state persists across process "OnDeath" state while switch over other apps -
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable("engine", vm.engine.snapshot())
    }
}
