package com.example.calculator

import androidx.lifecycle.ViewModel  //used this one to survive rotation
import com.example.calccore.AngleMode
import com.example.calccore.CalculatorEngine

class MainViewModel : ViewModel() {
    //// kept one engine instance here, so it lives across rotation
    val engine = CalculatorEngine(AngleMode.DEG)
}
