# DESIGN.md | DFCalculator

## Executive Summary
DFCalculator is a small, testable Android calculator built as two modules:
- **`:app`** — UI shell (Activities, layouts, toolbar, theme toggle, optional History UI)
- **`:calc-core`** — pure calculation engine (no UI) with snapshot/restore

The architecture separates **presentation** from **logic**, uses a **ViewModel** to survive rotation, and a **Parcelable memento** to recover from process death. An optional branch adds a lightweight **History** feature.

---

## Goals & Non-Goals
**Goals**
- Clean separation of UI and business logic
- Resilience across rotation and process death
- Small, readable codebase with unit tests
- Easy to extend (e.g., history, parser, graphing)

**Non-Goals**
- Full scientific/expression parser with precedence
- Multi-lang/i18n beyond basic strings
- Database storage (Room) in the base solution

---

## Module & Directory Layout
DFCalculator/
├─ app/ # Android app (UI)
│ ├─ src/main/java/com/example/calculator/
│ │ ├─ MainActivity.kt 
│ │ ├─ MainViewModel.kt 
│ │ ├─ ThemePrefs.kt 
│ │ └─ history/ 
│ │ └─ HistoryStore.kt 
│ └─ src/main/res/ 
└─ calc-core/ # Android Library (logic only)
   └─ src/main/java/com/example/calccore/
   └─ CalculatorEngine.kt 


**Build**
- Kotlin (JDK 17), AGP 8.5, AndroidX
- `minSdk=24`, `target/compileSdk=34`
- ViewBinding enabled for type-safe view access
- Dependencies kept minimal (Material, AppCompat, Lifecycle-ViewModel)

---

## Key Components & Responsibilities

### `CalculatorEngine` (`:calc-core`)
- **Role:** The “brain.” Holds the mutable state and performs operations.
- **Public API (simplified)**
  - Input: `inputDigit(c)`, `inputDot()`, `toggleSign()`
  - Ops: `setOperator(ADD|SUB|MUL|DIV)`, `equalsPress()`
  - Trig: `applyTrig(SIN|COS|TAN)`, `angleMode = DEG|RAD`
  - Read-only display: `expression: String`, `displayValue: String`
  - Resilience: `snapshot(): EngineState`, `restore(state)`
- **Execution model:** *Immediate execution* (left-to-right); precedence/parentheses intentionally out of scope.
- **Memento:** `EngineState` (@Parcelize) serializes internal fields for process death recovery.

### `MainViewModel` (`:app`)
- **Role:** Long-lived holder for `CalculatorEngine` across configuration changes.
- **Why ViewModel:** Keeps the engine instance in memory during rotation without manual plumbing.

### `MainActivity` (`:app`)
- **Role:** UI “hands and eyes.” Binds buttons to engine calls and renders `expression`/`displayValue`.
- **Toolbar menu:** Inflated directly on `MaterialToolbar`; items:
  - `History` (optional branch)
  - `Dark/Light` toggle (persisted)
- **Resilience:**
  - Applies theme before inflating layout to avoid flicker.
  - Restores `EngineState` from `savedInstanceState` if process was killed.

### `ThemePrefs` (`:app`)
- **Role:** Persist a single boolean in SharedPreferences.
- **Apply:** `AppCompatDelegate.setDefaultNightMode(...)` (triggers Activity recreate; ViewModel preserves state).

### `HistoryStore` (`:app` feature branch only)
- **Role:** Lightweight, persistent history (JSON in SharedPreferences).
- **API:** `add(context, HistoryItem)`, `get(context)`, `clear(context)`
- **Policy:** Most-recent-first, max 50 entries; de-dups consecutive duplicates.

---

## UI & Interaction Flow

### Screen (portrait/landscape)
- Two-line display:
  - **Expression:** e.g., `12 × 3`
  - **Result:** big number (current input or last result)
- Grid of buttons: digits, dot, **AC**, **DEL**, sign toggle, 4 ops, **=**, sin/cos/tan, **DEG/RAD** toggle
- **Toolbar**: app title + menu (History*, Theme)
  - *History present only in `feature/history-dialog`.*

### Event Flow (sequence)
1. User taps a key → `MainActivity` calls the corresponding `CalculatorEngine` method.
2. Engine mutates internal state and computes as needed.
3. `MainActivity` calls `updateDisplay()` to rebind `expression`/`displayValue`.
4. On `=`:
   - (Feature branch) capture `expression` pre-equals → compute → save `(expression,result)` via `HistoryStore`.

---

## State Management & Lifecycle

**Rotation**
- Activity is recreated; `MainViewModel` returns the existing `CalculatorEngine`.
- UI re-binds using `engine.expression`/`engine.displayValue`.

**Process Death**
- `onSaveInstanceState` stores `engine.snapshot()`; `onCreate` restores it if available.

**Theme Toggle**
- Persisted; applied before view inflation on cold start.
- At runtime, toggling triggers Activity recreate; engine survives via ViewModel/snapshot.

**Threading**
- All interactions occur on the main (UI) thread. Engine is **not** thread-safe by design.

---

## Error Handling & Edge Cases
- **Divide by zero:** Engine throws `ArithmeticException`; Activity shows a Toast and keeps UI responsive.
- **Floating point noise:** Results formatted to 10 decimals then trimmed (`1.000000 → 1`).
- **Trig singularities:** Uses platform `sin/cos/tan`; extreme values may appear (e.g., `tan(90°)`).

---

## Testing Strategy

**Unit tests (`:calc-core`)**
- Addition: `12 + 3 = 15`
- Divide by zero: throws `ArithmeticException`
- Trig sanity: `sin(90°) ≈ 1` (degree mode)

**What would be added next**
- Tolerant comparisons for floating point (`assertEquals(expected, actual, eps)`)
- Repeated equals behavior
- Sign toggle + delete edge cases

**Instrumentation (future)**
- Espresso smoke: enter digits/op/equals; rotate; toggle theme; open History (feature branch)

---

## Build, Tooling, CI (suggested)
- **Gradle Wrapper** checked in (`gradlew`, `gradle/wrapper/*`)
- **Lint** and **unit tests** on CI (GitHub Actions) per PR:
  - `./gradlew :calc-core:test :app:lint :app:assembleDebug`
- Optional: ktlint/Detekt for style/static analysis

---

## Accessibility & UX
- Material components for default a11y (touch targets, ripple feedback)
- Content descriptions on display elements
- Dark/Light theme; readable contrast by default
- Future: improved TalkBack labels, large-text testing, landscape “tape” pane

---

## Security & Privacy
- No network calls; no external storage.
- History (feature branch) stored locally in SharedPreferences.
- No PII collected; no analytics.

---

## Decisions & Trade-offs

| Decision | Rationale | Alternative | Trade-off |
|---|---|---|---|
| Split into `:app` + `:calc-core` | Testability, reuse, thin UI | Single module | Slightly more setup |
| ViewModel + Parcelable snapshot | Covers rotation **and** process death | Only ViewModel or only saved state | More code, but robust |
| Immediate execution | Simpler UX & engine | Full parser (precedence, parentheses) | Less “scientific”; roadmap item |
| SharedPreferences for history (feature branch) | Lightweight, no schema/migrations | Room DB | Fewer query features |
| Toolbar-inflated menu | Simple, explicit | `onCreateOptionsMenu` | Either works; toolbar inflation is self-contained |

---

## Extension Points (Roadmap)
1. **Expression Parser** (Shunting-Yard/AST): precedence, parentheses, power, roots, logs
2. **Room DB History**: favorites, search, timestamps, share/reuse
3. **Graphing**: plot `y = f(x)` with pinch/zoom on a custom view
4. **Unit/Currency Converter**: bottom sheet with offline table (+ optional rates API)
5. **Compose UI**: drop-in composable screen using the same engine API

---

## Branch Strategy (repo)
- **`main`** — Required challenge implementation (calculator, trig, DEG/RAD, theme toggle, state resilience)
- **`feature/history-dialog`** — Optional enhancement (persistent History via SharedPreferences with a simple dialog UI)

> The tagged commit `challenge-submission-YYYY-MM-DD` denotes the exact submission for review.

---

