# PayTouch Dashboard — Claude Context

PayTouch Dashboard is a **fintech Android app** (Kotlin) for managing KYC compliance and virtual banking accounts. It serves two user roles: **Admin** (reviews KYC, manages members/schemes/virtual accounts) and **Client** (uploads KYC documents). Built by Shreefintech.

> Full developer rules: `DEVELOPER_GUIDELINES.md` | Business logic: `docs/business_logic.md` | Dos & Don'ts for AI: `docs/dos_and_donts.md`

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
| Push | Firebase Cloud Messaging 25.0 |
| Analytics | Firebase Analytics + Crashlytics |
| Min/Target SDK | 24 / 36 |

---

## Package Structure (top-level)

```
com.shreefintech.dashboard/
├── admin/          # Admin-role screens (KYC review, members, schemes, virtual accounts)
├── auth/           # Login, OTP, password/MPIN flows
├── client/kyc/     # Client KYC upload flows
├── enums/          # Project-wide enums (Role, NextStep, EntityType, MemberStatus)
├── fcm/            # Firebase push notification service
├── glass/          # LiquidGlassEffect custom blur UI
├── retrofit/       # All networking (ApiService, ApiClient, ApiHelper, models)
├── utill/          # Shared utilities — NOTE: spelling "utill" is intentional, never rename
├── viewmodel/      # Cross-feature ViewModels (SessionViewModel)
├── widget/         # Reusable custom views (BackBtn, CustomDropdown, sidebar)
├── BaseActivity.kt
├── DashboardActivity.kt
└── SplashActivity.kt
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
| Activity | PascalCase + `Activity` | `VirtualAccountActivity` |
| Adapter | PascalCase + `Adp` | `PendingKycAdp` |
| Model/DTO | PascalCase + `Item` | `KycItem` |
| ViewModel | PascalCase + `ViewModel` | `SchemesViewModel` |
| Layout | prefix + snake_case | `activity_login`, `item_kyc`, `lyt_header` |
| Drawable | `ic_` icons, `bg_` backgrounds | `ic_upload`, `bg_card` |
| Strings | camelCase with context prefix | `msgNoInternet`, `errGeneric` |

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

## LiquidGlassButton Attachment Rule

Every `LiquidGlassButton` in an Activity **must** call `.attach(root as ViewGroup)` in `onCreate()` after `setContentView()`. This initialises the live glass-blur effect. Without it the button renders without any background.

```kotlin
// In onCreate(), after setContentView():
binding.flUpload1.attach(binding.root as ViewGroup)
binding.flUpdate.attach(binding.root as ViewGroup)
```

This applies to **every** `LiquidGlassButton` — upload triggers (`flUpload1`, `flUpload2`, …), submit/update buttons (`flUpdate`, `flSubmit`, `flSignIn`, …), and any other `LiquidGlassButton` in the layout. Call `.attach()` for each one individually. Do **not** use `LiquidGlassEffect.attach()` for these — use the widget's own `.attach()` method.

---

## Output Rules

After every task show:

1. Assumptions
2. Files Changed
3. Functions Changed
4. Reason For Each Change
5. Implementation Summary
6. Git Diff Style Summary

Always follow existing project patterns and minimize code changes.

## Project Implementation Rules

For all tasks:

* Analyze existing code before making changes.
* Follow existing project architecture, coding style, naming conventions, folder structure, and patterns.
* Reuse existing Activities, Fragments, ViewModels, Repositories, Adapters, Models, Validators, Custom Views, Utilities, and Extensions whenever possible.
* Follow existing UI, XML, navigation, Activity Result, pagination, search, validation, loader, observer, and API handling patterns.
* Prefer consistency over introducing new approaches.
* Do not refactor unrelated code.
* Keep changes minimal and focused.
* Create new classes/files only when necessary.
* Find and follow similar implementations already present in the project.

After implementation always provide:

1. Assumptions
2. Files Changed
3. Functions Changed
4. Reason for Changes
5. Diff Summary


All API responses are wrapped in `General<T>` with fields: `data`, `success`, `meta`, `message`.

## Resources & Assets

When implementing UI:

* First search the project for existing drawables, icons, images, colors, styles, dimensions, and reusable UI resources.
* Reuse existing assets whenever possible.
* Do not create duplicate resources if a suitable one already exists.
* If a required image, icon, drawable, or asset is not available, use a clear placeholder resource name and continue implementation.
* Mention all missing assets in the output.

Placeholder naming examples:

* `ic_edit_placeholder`
* `ic_profile_placeholder`
* `bg_card_placeholder`
* `img_banner_placeholder`

Output:

### Missing Assets

* List any placeholder resources used.
* Mention where each placeholder is referenced.


# Android Activity Guidelines

## Click Handling

- Use a **single centralized `onClickListener()`** with a `when (it)` block — never scatter individual `setOnClickListener()` calls.
- Prefer **Data Binding** (`android:onClickListener="@{handler.onClickListener}"`) over programmatic `setOnClickListener()`.
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

## Keyboard Handling

Apply this whenever a screen contains an `EditText` or any keyboard interaction.

**1. Manifest** — add `adjustResize` to the activity entry:

```xml
<activity
    android:name=".YourActivity"
    android:windowSoftInputMode="adjustResize" />
```

**2. Window insets** — handle both system bars and IME so content is never obscured:

```kotlin
ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
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
Utility.hideKeyboard(activity)
```

---

## Back Handling

When a screen hosts dialogs, bottom sheets, filters, or overlays, **close the topmost layer first** before exiting the screen entirely.

```kotlin
private fun onBack() {
    onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            if (overlayHelper.isVisible()) {
                overlayHelper.hide()          // dismiss overlay first
            } else {
                isEnabled = false             // then let the system handle back
                onBackPressedDispatcher.onBackPressed()
            }
        }
    })
}
```

Call `onBack()` in `onCreate()` so the callback is registered immediately.

---

## Result Handling

Use this pattern when changes on the current screen should trigger a data refresh on the previous screen.

**Current screen:**

```kotlin
// Default — no changes made
private var resultCode = 0

// Set to 1 whenever data is modified (on save, delete, update, etc.)
resultCode = 1

// Return the result when navigating back or closing
private fun onBack() {
    onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            setResult(resultCode)
            finish()
        }
    })
}
```

**Previous screen** — register an Activity Result launcher and refresh only when needed:

```kotlin
private val launcher = registerForActivityResult(
    ActivityResultContracts.StartActivityForResult()
) { result ->
    if (result.resultCode == 1) {
        refreshData()
    }
}
```

| `resultCode` | Meaning |
|---|---|
| `0` | No changes — previous screen does nothing |
| `1` | Data modified — previous screen refreshes |

---

## Bottom Sheet Pattern

Use this pattern whenever a screen needs an in-place form or detail panel that slides up from the bottom (edit forms, filters, detail views). **Never use `Dialog` for this — always use `BottomSheetBehavior` included in the layout.**

### 1. Sheet XML (`sheet_*.xml`)

Root is a `FrameLayout` (or `LinearLayout`) with `BottomSheetBehavior` attributes and `@drawable/bottom_sheet_bg`:

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

    <LinearLayout
        android:id="@+id/bottomSheet"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical"
        android:paddingTop="@dimen/margin_large"
        android:paddingBottom="40dp"
        android:paddingHorizontal="@dimen/form_item_start_end">

        <!-- Drag handle -->
        <View
            android:layout_width="40dp"
            android:layout_height="4dp"
            android:layout_gravity="center_horizontal"
            android:layout_marginBottom="@dimen/margin_extra_large"
            android:background="@color/btm_sheet_bar" />

        <!-- Title row with close button -->
        <RelativeLayout android:layout_width="match_parent" android:layout_height="wrap_content">
            <TextView android:layout_alignParentStart="true" android:layout_centerVertical="true" ... />
            <ImageView android:id="@+id/ivClose" android:layout_alignParentEnd="true"
                android:src="@drawable/ic_close" ... />
        </RelativeLayout>

        <!-- Form fields / content -->

        <!-- Action button -->
        <com.shreefintech.dashboard.widget.LiquidGlassButton android:id="@+id/flBtnUpdate" ... />

    </LinearLayout>
</FrameLayout>
```

Use DataBinding only for `showProgress` (type `androidx.databinding.ObservableBoolean`). Wire all click handlers programmatically in `setupSheet()`.

### 2. Activity Layout (`activity_*.xml`)

Include the sheet and a `viewBg` overlay as **direct children of the `CoordinatorLayout`** — not inside any nested layout:

```xml
<androidx.coordinatorlayout.widget.CoordinatorLayout ...>

    <!-- Main content (ConstraintLayout / RelativeLayout) -->
    <androidx.constraintlayout.widget.ConstraintLayout ... />

    <!-- Dimmed overlay shown when sheet is open -->
    <View
        android:id="@+id/viewBg"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:background="@color/sheet_view_bg"
        android:clickable="true"
        android:visibility="gone" />

    <!-- Bottom sheet include -->
    <include
        android:id="@+id/incUpdateSheet"
        layout="@layout/sheet_update_bank_settlement" />

</androidx.coordinatorlayout.widget.CoordinatorLayout>
```

### 3. Activity Kotlin

```kotlin
private lateinit var sheetBinding: SheetUpdateBankSettlementBinding
private lateinit var sheetBehavior: BottomSheetBehavior<View>
private var sheetProgress = ObservableBoolean(false)

// Call from onCreate()
private fun setupSheet() {
    sheetBinding = binding.incUpdateSheet
    sheetBehavior = BottomSheetBehavior.from(sheetBinding.root)
    sheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
    sheetBinding.showProgress = sheetProgress

    sheetBinding.ivClose.setOnClickListener {
        sheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
    }

    sheetBinding.flBtnUpdate.setOnClickListener {
        if (Utility.stopClick()) return@setOnClickListener
        // validate → call API
    }

    sheetBehavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
        override fun onStateChanged(bottomSheet: View, newState: Int) {
            when (newState) {
                BottomSheetBehavior.STATE_EXPANDED -> binding.viewBg.visible()
                BottomSheetBehavior.STATE_HIDDEN -> {
                    Utility.hideKeyboard(mActivity)
                    binding.viewBg.gone()
                }
                else -> {}
            }
        }
        override fun onSlide(bottomSheet: View, slideOffset: Float) {}
    })
}

// Show the sheet (prefill fields before calling)
sheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED

// onBack() must dismiss the sheet before finishing the activity
private fun onBack() {
    onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            if (sheetBehavior.state == BottomSheetBehavior.STATE_EXPANDED) {
                sheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
            } else {
                isEnabled = false
                onBackPressedDispatcher.onBackPressed()
            }
        }
    })
}
```

Also apply insets to the sheet root so it sits above the navigation bar:

```kotlin
ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
    val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
    v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
    binding.incUpdateSheet.root.setPadding(0, 0, 0, systemBars.bottom)
    insets
}
```

### Real examples in project

| Sheet XML | Activity |
|---|---|
| `sheet_update_bank_settlement.xml` | `BankSettlementListActivity` |
| `sheet_update_url.xml` | `DashboardActivity` |
| `sheet_permission_list.xml` | `MemberPermissionActivity` |
| `sheet_member_filter.xml` | `VirtualAccountActivity` |

---

## RecyclerView Update Rules

Always prefer targeted adapter updates over full list refreshes.

| Operation | Correct approach |
|---|---|
| Single item created/updated/approved/rejected | `notifyItemChanged(position)` — update that item in `mArrayList` locally first |
| Single item deleted | `mArrayList.removeAt(position)` + `notifyItemRemoved(position)` |
| Full reload (filter, search, page 1) | `mArrayList.clear()` + `notifyDataSetChanged()` |
| Pagination append | `notifyItemRangeInserted(insertStart, count)` |

**Never call a full list reload API just to reflect a single-item state change.** Update the item in `mArrayList` locally using `.copy(...)` with the known new values, then notify only that position.

```kotlin
// After approve API succeeds:
mArrayList[position] = mArrayList[position]!!.copy(status = "active")
mAdapter.notifyItemChanged(position)

// After reject API succeeds:
mArrayList[position] = mArrayList[position]!!.copy(status = "rejected", rejectedReason = reason)
mAdapter.notifyItemChanged(position)
```

---

## Project Documentation

All project docs live in `docs/`. Read the relevant files before starting any task.

| File | When to read |
|---|---|
| `caveman.md` | First — always. Gives you the full system overview in plain terms. |
| `business_logic.md` | Before writing any feature, flow, or data-related code. This is the source of truth for domain rules — not the code itself. |
| `dos_and_donts.md` | Before making any structural, architectural, or pattern decision. |

### Rules

- **Always read `caveman.md` first** on any new task — it keeps your understanding of the system grounded.
- **Never override `business_logic.md` with assumptions** — if the code contradicts it, flag it, don't silently follow the code.
- **`dos_and_donts.md` is non-negotiable** — treat it as a hard constraint, not a suggestion.
- If a task touches something not covered by any doc, **ask before proceeding**.