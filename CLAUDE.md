# PayTouch Consumer — Claude Context

PayTouch Consumer is a **fintech Android app** (Kotlin) for Indian consumers to pay utility bills (electricity, gas, mobile, DTH, cable, broadband, FASTag, loans, taxes) and manage a digital wallet. Single user role — every logged-in user has the same feature set. Built by Shreefintech.

> **Current state:** This project is in the **UI implementation phase**. API wiring is pending — all network calls are currently stubbed with `TODO(PAYTOUCH-xxx):` comments. Validations and business logic will be finalized once APIs are connected. Do not treat missing API calls or relaxed validations as bugs.

> Business logic reference: `docs/business_logic.md` | Architecture rules: `docs/dos_and_donts.md` | System overview: `docs/caveman.md`

---

## Final Verification (Mandatory)

Before presenting any code, perform a complete self-review and confirm all of the following:

- No compile-time errors
- No logical bugs or incorrect assumptions
- No nullability issues (proper `?.`, `?:`, `!!` usage)
- No lifecycle problems (no Context leaks, no callbacks after destroy)
- No threading or concurrency issues (UI updates on main thread, background work off main thread)
- No memory leaks (no anonymous inner classes holding Activity/Context references beyond their scope)
- No edge-case failures (empty list, null response, position out of bounds, etc.)
- No performance regressions (no unnecessary full redraws, redundant API calls, heavy work on main thread)
- No inconsistent naming (follows project conventions: `Adp`, `Item`, `ViewModel`, etc.)
- All unused imports, variables, and dead code removed
- Implementation integrates correctly with existing codebase patterns
- All existing project rules (Architecture Rules, Naming Conventions, Network Call Pattern, RecyclerView Update Rules, Code Generation Rule) are satisfied

Only present the solution after this check passes.

---

## Code Generation Rule

Always prioritize optimized, maintainable, and production-ready code. Generate solutions as if you are a senior Android developer with extensive experience in Kotlin, Android Architecture Components, MVVM, Coroutines, Flow, Jetpack libraries, and clean architecture. Favor readability, performance, scalability, and testability over quick fixes. Avoid unnecessary object creation, redundant computations, duplicate code, and over-engineering. Reuse existing components where appropriate, follow SOLID principles, minimize memory allocations, and consider lifecycle, threading, and performance implications in every implementation. When multiple approaches are possible, choose the one that is most efficient, idiomatic, and maintainable for a long-term production codebase. Never sacrifice code quality for brevity.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin (JVM 11) |
| UI | Views with ViewBinding + DataBinding; Jetpack Compose for new isolated components only |
| Networking | Retrofit 2.11 + OkHttp 4 + Gson 2.13 |
| Architecture | Hybrid MVVM (some screens are Activity-driven, some use ViewModel) |
| Image loading | Glide 4.16 |
| Auth | Bearer token stored in SharedPreferences |
| Push | Firebase Cloud Messaging 25.0 (planned) |
| Analytics | Firebase Analytics + Crashlytics |
| Min/Target SDK | 24 / 36 |

---

## Package Structure (top-level)

```
com.shreefintech.paytouchconsumer/
├── auth/           # Login, OTP, password/MPIN flows, create-account
├── onboarding/     # KYC upload (UploadKycActivity), Virtual Account creation (CreateVirtualAccountActivity)
├── home/           # Home/Dashboard screen (HomeActivity — currently at root level, will move here)
├── electricity/    # Electricity bill payment screen
├── enums/          # Project-wide enums (LoginMode, etc.)
├── glass/          # LiquidGlassEffect custom blur UI components
├── retrofit/       # All networking (ApiService, ApiClient, ApiHelper, models) — wiring pending
├── utill/          # Shared utilities — NOTE: spelling "utill" is intentional, never rename
├── widget/         # Reusable custom views (LiquidGlassButton, CustomDropdown, etc.)
├── BaseActivity.kt
├── HomeActivity.kt
└── Constant.kt
```

---

## Architecture Rules

1. **ApiClient** is the only source for Retrofit — never construct Retrofit directly.
2. **SharedPreferenceHelper** is the only way to read/write SharedPreferences.
3. **Constant.kt** is where all hardcoded URLs and keys live — no inline strings.
4. **ToastUtil** is the only way to show user messages — never use raw `Toast.makeText`.
5. **Utility.isInternetAvailable()** must be called before every network request.
6. All Activities must extend **BaseActivity** — never extend AppCompatActivity directly.
7. Network errors must be parsed via **ApiHelper.parseErrorMessage()**.
8. Adapters must not contain business logic or network calls.
9. ViewModels must not store Activity/Context references.

---

## Naming Conventions (quick ref)

| Thing | Convention | Example |
|---|---|---|
| Activity | PascalCase + `Activity` | `HomeActivity` |
| Adapter | PascalCase + `Adp` | `TransactionAdp` |
| Model/DTO | PascalCase + `Item` | `TransactionItem` |
| ViewModel | PascalCase + `ViewModel` | `LoginViewModel` |
| Layout | prefix + snake_case | `activity_home`, `item_transaction`, `lyt_toolbar` |
| Drawable | `ic_` icons, `bg_` backgrounds, `img_` images | `ic_wallet`, `bg_category_item`, `img_screen_bg` |
| Strings | camelCase with context prefix | `msgNoInternet`, `categoryElectricity`, `btnLoadWallet` |

---

## Network Call Pattern

```kotlin
if (!Utility.isInternetAvailable(mActivity)) return
ApiClient.apiService.someEndpoint(body).enqueue(object : Callback<General<SomeItem>> {
    override fun onResponse(call, response) {
        if (response.isSuccessful) { /* handle */ }
        else { ToastUtil.show(mActivity, ApiHelper.parseErrorMessage(response)) }
    }
    override fun onFailure(call, t) { ToastUtil.show(mActivity, t.localizedMessage) }
})
```

---

## LiquidGlassButton Attachment Rule

Every `LiquidGlassButton` in an Activity **must** call `.attach(root as ViewGroup)` in `onCreate()` after `setContentView()`. This initialises the live glass-blur effect. Without it the button renders without any background.

```kotlin
// In onCreate(), after setContentView():
binding.flUpload1.attach(binding.clRoot as ViewGroup)
binding.flSubmit.attach(binding.clRoot as ViewGroup)
```

This applies to **every** `LiquidGlassButton` — upload triggers (`flUpload1`, `flUpload2`, …), submit/update buttons (`flSubmit`, `flSignIn`, …), and any other `LiquidGlassButton` in the layout. Call `.attach()` for each one individually. Do **not** use `LiquidGlassEffect.attach()` for these — use the widget's own `.attach()` method.

---

## Output Rules

After every task show:

1. Assumptions
2. Files Changed
3. Functions Changed
4. Reason For Each Change
5. Diff Summary

Always follow existing project patterns and minimize code changes.

---

## Project Implementation Rules

For all tasks:

* Analyze existing code before making changes.
* Follow existing project architecture, coding style, naming conventions, folder structure, and patterns.
* Reuse existing Activities, ViewModels, Adapters, Models, Custom Views, Utilities, and Extensions whenever possible.
* Follow existing UI, XML, navigation, Activity Result, validation, loader, observer, and API handling patterns.
* Prefer consistency over introducing new approaches.
* Do not refactor unrelated code.
* Keep changes minimal and focused.
* Create new classes/files only when necessary.
* Find and follow similar implementations already present in the project.

---

## API Response Wrapper

All API responses are wrapped in `General<T>` with fields: `data`, `success`, `meta`, `message`.

---

## Resources & Assets

When implementing UI:

* First search the project for existing drawables, icons, images, colors, styles, and dimensions.
* Reuse existing assets whenever possible — do not create duplicates.
* If a required asset is not available, use a clear placeholder name and continue implementation.
* Mention all missing assets in the output under **Missing Assets**.

Placeholder naming examples: `ic_edit_placeholder`, `bg_card_placeholder`, `img_banner_placeholder`

---

## Android Activity Guidelines

### Click Handling

- Use a **single centralized `onClickListener()`** with a `when (it)` block — never scatter individual `setOnClickListener()` calls.
- Prefer **Data Binding** (`android:onClickListener="@{onClickListener}"`) over programmatic `setOnClickListener()`.
- Always guard against **rapid double-clicks** before navigation, API calls, form submissions, or screen transitions:

```kotlin
private fun onClickListener(): View.OnClickListener {
    return View.OnClickListener {
        when (it) {
            binding.viewA -> {
                if (Utility.stopClick()) return@OnClickListener
                actionA()
            }
            binding.viewB -> {
                if (Utility.stopClick()) return@OnClickListener
                actionB()
            }
        }
    }
}
```

---

### Keyboard Handling

Apply this whenever a screen contains an `EditText` or any keyboard interaction.

**1. Manifest** — add `adjustResize` to the activity entry:

```xml
<activity
    android:name=".YourActivity"
    android:windowSoftInputMode="adjustResize" />
```

**2. Window insets** — handle both system bars and IME so content is never obscured:

```kotlin
ViewCompat.setOnApplyWindowInsetsListener(binding.clRoot) { v, insets ->
    val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
    val imeInsets  = insets.getInsets(WindowInsetsCompat.Type.ime())
    v.setPadding(
        systemBars.left,
        systemBars.top,
        systemBars.right,
        maxOf(imeInsets.bottom, systemBars.bottom)
    )
    insets
}
```

**3. Dismiss keyboard** before executing a button action when appropriate:

```kotlin
Utility.hideKeyboard(mActivity)
```

---

### Back Handling

When a screen hosts dialogs, bottom sheets, filters, or overlays, **close the topmost layer first** before exiting the screen entirely.

```kotlin
private fun onBack() {
    onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            if (overlayHelper.isVisible()) {
                overlayHelper.hide()
            } else {
                isEnabled = false
                onBackPressedDispatcher.onBackPressed()
            }
        }
    })
}
```

Call `onBack()` in `onCreate()` so the callback is registered immediately.

---

### Result Handling

Use this pattern when changes on the current screen should trigger a data refresh on the previous screen.

**Current screen:**

```kotlin
private var resultCode = 0   // 0 = no changes, 1 = data modified

private fun onBack() {
    onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            setResult(resultCode)
            finish()
        }
    })
}
```

**Previous screen:**

```kotlin
private val launcher = registerForActivityResult(
    ActivityResultContracts.StartActivityForResult()
) { result ->
    if (result.resultCode == 1) refreshData()
}
```

---

### Bottom Sheet Pattern

Use this pattern whenever a screen needs an in-place form or detail panel that slides up from the bottom. **Never use `Dialog` — always use `BottomSheetBehavior` embedded in the layout.**

**Sheet XML** (`sheet_*.xml`) — root `FrameLayout` with `BottomSheetBehavior` + `@drawable/bottom_sheet_bg`:

```xml
<FrameLayout
    android:id="@+id/flSheetMain"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:background="@drawable/bottom_sheet_bg"
    app:behavior_hideable="true"
    app:behavior_peekHeight="0dp"
    app:behavior_skipCollapsed="true"
    app:layout_behavior="com.google.android.material.bottomsheet.BottomSheetBehavior">
    <!-- Drag handle, title row with close button, form fields, action button -->
</FrameLayout>
```

**Activity layout** — sheet and `viewBg` overlay as direct children of `CoordinatorLayout`:

```xml
<androidx.coordinatorlayout.widget.CoordinatorLayout>
    <androidx.constraintlayout.widget.ConstraintLayout ... />   <!-- main content -->
    <View android:id="@+id/viewBg" ... android:visibility="gone" />  <!-- dim overlay -->
    <include android:id="@+id/incSheet" layout="@layout/sheet_xyz" />
</androidx.coordinatorlayout.widget.CoordinatorLayout>
```

**Activity Kotlin:**

```kotlin
private lateinit var sheetBinding: SheetXyzBinding
private lateinit var sheetBehavior: BottomSheetBehavior<View>

private fun setupSheet() {
    sheetBinding = binding.incSheet
    sheetBehavior = BottomSheetBehavior.from(sheetBinding.root)
    sheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN

    sheetBehavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
        override fun onStateChanged(bottomSheet: View, newState: Int) {
            when (newState) {
                BottomSheetBehavior.STATE_EXPANDED -> binding.viewBg.visibility = View.VISIBLE
                BottomSheetBehavior.STATE_HIDDEN   -> {
                    Utility.hideKeyboard(mActivity)
                    binding.viewBg.visibility = View.GONE
                }
                else -> {}
            }
        }
        override fun onSlide(bottomSheet: View, slideOffset: Float) {}
    })
}
```

Also apply insets so the sheet sits above the navigation bar:

```kotlin
binding.incSheet.root.setPadding(0, 0, 0, systemBars.bottom)
```

> No bottom sheets are implemented yet in this project. The first screen to add one should follow this pattern exactly.

---

## RecyclerView Update Rules

Always prefer targeted adapter updates over full list refreshes.

| Operation | Correct approach |
|---|---|
| Single item created/updated | `notifyItemChanged(position)` — update item in `mArrayList` first |
| Single item deleted | `mArrayList.removeAt(position)` + `notifyItemRemoved(position)` |
| Full reload (filter, search, page 1) | `mArrayList.clear()` + `notifyDataSetChanged()` |
| Pagination append | `notifyItemRangeInserted(insertStart, count)` |

**Never call a full list reload API just to reflect a single-item state change.**

---

## Project Documentation

All project docs live in `docs/`. Read the relevant files before starting any task.

| File | When to read |
|---|---|
| `caveman.md` | First — always. Plain-English system overview. |
| `business_logic.md` | Before writing any feature, flow, or data-related code. Source of truth for domain rules. |
| `dos_and_donts.md` | Before making any structural or architectural decision. |
| `screens_and_navigation.md` | When implementing a new screen or navigation flow. |
| `api_reference.md` | When wiring up any API call. |

### Rules

- **Always read `caveman.md` first** on any new task.
- **Never override `business_logic.md` with assumptions** — if code contradicts it, flag it.
- **`dos_and_donts.md` is non-negotiable** — treat it as a hard constraint, not a suggestion.
- If a task touches something not covered by any doc, **ask before proceeding**.
