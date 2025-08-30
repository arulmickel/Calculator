# DFCalculator — Android Challenge

A clean calculator supporting **Add, Subtract, Multiply, Divide, Sine, Cosine, Tangent** with a UI that shows both **expression** and **result**.
Logic is placed in a separate **Android Library** module (`calc-core`); the app depends on it.

> **Angle mode:** Trig defaults to **degrees**; tap **DEG/RAD** to toggle.

## Tech Stack
- Kotlin (JDK 17), Android SDK
- UI: XML + Material + ViewBinding
- Modules: `app` and `calc-core` (library with `CalculatorEngine`)
- Tests: JUnit for core engine

## Run
Open in **Android Studio (Jellyfish or newer)**, ensure **JDK 17**, then run the `app` module.

## Structure
```
DFCalculator/
├─ app/                      # Android app (UI)
└─ calc-core/                # Library with CalculatorEngine + tests
```