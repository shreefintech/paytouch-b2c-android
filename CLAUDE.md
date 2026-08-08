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
├── onboarding/     # Virtual Account creation (CreateVirtualAccountActivity); onboarding/kyc/ — KYC hub (KycActivity), identity verification, bank details
├── home/           # Home/Dashboard screen (HomeActivity — currently at root level, will move here)
├── electricity/    # Electricity bill payment screen
├── gas/            # Gas bill payment screen
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
10. **All API endpoints must be declared in `ApiService` or `ApiAdminService`** — never construct `OkHttpClient` or `Retrofit` directly inside a ViewModel or Activity. Each backend URL has exactly one registered client: `ApiClient` for `paytouch.in`, `ApiAdminClient` for `admin.paytouch.in`. Add a new endpoint to the appropriate service interface; do not bypass it with a raw HTTP call.
11. **`mapToTransactionItem()` must use `item.id?.toString() ?: "--"` for `userId`** — never use `(index + 1).toString()` or any iteration index. The API response DTO always carries an `id` field; using the loop index produces duplicate `userId` values on paginated appends (page 2+ resets index to 0) and loses the server-side identity of the record.
12. **Never generate `transaction_id` client-side** — the backend owns transaction ID generation. Do not call `Utility.generateTransactionId()` or any equivalent before a payment API call. Do not include a `transaction_id` field in any process-payment request DTO (`ElectricityProcessPaymentRequest`, `GasProcessPaymentRequest`, or any future module). The server returns the transaction ID in the response; read it from there. (`Utility.generateTransactionId()` was removed in review — flagged as incorrect ownership of ID generation.)
13. **ViewModel base class is determined by role, not by module** — follow this table exactly; never use `BaseBillViewModel` for a ViewModel that does not need balance checks:

| ViewModel role | Base class |
|---|---|
| Main payment screen (`{Category}ViewModel`) | `BaseBillViewModel` |
| Recent transactions (`{Category}RecentTransactionViewModel`) | `AndroidViewModel` |
| Transaction report (`{Category}TransactionReportViewModel`) | `AndroidViewModel` |
| Transaction status (`{Category}TransactionStatusViewModel`) | `AndroidViewModel` |
| SMS receipt (`{Category}SmsReceiptViewModel`) | `AndroidViewModel` |

`BaseBillViewModel` provides `checkVpsBalance()` and `checkWalletBalance()` — methods only needed by payment-flow ViewModels. Report/status/receipt ViewModels use `bearerToken()` and `getString()` from `ViewModelExt.kt` instead. Do not flag the report/status ViewModels as inconsistent for extending `AndroidViewModel` directly.

14. **`isMobileCategory` flag must be set in every `mapToTransactionItem()` and `mapToDisplayItem()`** — never hard-coded in the adapter. Set `isMobileCategory = true` for Mobile Prepaid, Mobile Postpaid, and DTH modules; `isMobileCategory = false` for all others (Electricity, Gas, etc.). The adapter uses this flag to switch between "Mobile No" and "Consumer No" labels — if it is missing the flag defaults to `false` (consumer label) which is silently wrong for mobile modules.

    **SMS Receipt (`{Category}SmsReceiptActivity`)** — set `binding.tvConsumerNoLabel.text` dynamically in `populateReceiptFromApi()`. Never rely on the XML default alone. Consumer-number modules → `getString(R.string.labelConsumerNo)`; mobile-number modules → `getString(R.string.labelMobileNo)`.

15. **Use `Utility.maskNumber()` for the account number shown in transaction list rows** — `TransactionAdp` calls `Utility.maskNumber(item.mobileNumber)` for `tvMobile`. The format is `9876*****0` (first 4 chars + five asterisks + last 1 char). Never display the raw unmasked number in a list row; the full number is only shown in `TransactionDetailActivity`.

16. **`Utility.formatAmount()` has two overloads — use the correct one for the DTO field type:**
    - `formatAmount(raw: String?)` — for all DTOs whose amount fields are `String?` (Gas, Prepaid, Postpaid, and future modules)
    - `formatAmount(raw: Double?)` — delegates to the String overload via `raw?.toString()`; use only for Electricity DTOs (`ElectricityTransactionReportDataItem`) whose amount fields are `Double?`
    - Never use `"₹%.2f".format(value)` or any raw string template for currency — always go through `Utility.formatAmount()`

17. **`Utility.formatDate()` is the only date formatter** — never call `SimpleDateFormat` directly in a ViewModel. Use:
    - `Utility.formatDate(raw, "dd MMM yyyy")` in Recent Transaction `mapToDisplayItem()` (e.g. `11 Jul 2026`)
    - `Utility.formatDate(raw, "dd/MM/yyyy")` in `TransactionDetailActivity` for date-only display
    - `Utility.formatDate(raw)` (default `"dd/MM/yyyy hh:mm a"`) for full datetime fields
    - In `mapToTransactionItem()` pass `item.createdAt ?: "--"` raw — do not pre-format; `TransactionDetailActivity` formats it at display time

18. **Confirmed backend quirks — do NOT "fix" these:**

    | Location | Apparent anomaly | Confirmed behaviour |
    |---|---|---|
    | `ApiService.kt` — `getMunicipalTaxTransactionStatus` | Uses `@POST("${AUTH}mobile-recharge/transaction-status")` (not `municipal-taxes/...`) | **Backend-side intentional routing.** The mobile-recharge transaction-status endpoint serves municipal tax queries too. Do not change this URL. |
    | `MunicipalTaxLatestPaymentDataItem.subService` | `@field:SerializedName("subservice")` has no underscore (unlike every other field) | **Confirmed from API contract.** The backend sends the key as `subservice`, not `sub_service`. Do not rename. |

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

### Naming Hard Rules (non-negotiable)

- **Models/DTOs MUST end in `Item`** — never `Response`, `Model`, `Dto`, `Data`, or `Entity`.
  - ✅ `LoginItem`, `UserProfileItem`, `MessageItem`, `TransactionItem`
  - ❌ `LoginResponse`, `UserModel`, `MessageDto`
- **Adapters MUST end in `Adp`** — never `Adapter`.
  - ✅ `TransactionAdp` ❌ `TransactionAdapter`
- **Activities MUST end in `Activity`** — never `Screen`, `Page`, `View`.
- **ViewModels MUST end in `ViewModel`**.
- **Layouts MUST use the correct prefix** — `activity_`, `item_`, `lyt_`, `sheet_`, `dialog_`.
- **Drawables MUST use `ic_` / `bg_` / `img_`** — never bare names or other prefixes.
- **String IDs MUST use camelCase context prefixes**: `msg` (messages/toasts), `title` (screen titles), `label` (field labels), `hint` (input hints), `btn` (button text), `err` (error strings), `category` (category names).
- When two `Item` classes would share the same name, qualify the outer context: `UserProfileItem` (user from GET /user) vs `UserItem` (user nested in login response).

---

## Network Call Pattern

All Retrofit endpoints must return `Call<T>` — **never** `suspend fun` / `Response<T>`. Always invoke with `.enqueue()`. The callback already runs on the main thread.

```kotlin
// ApiService declaration
fun someEndpoint(@Field("x") x: String): Call<SomeItem>

// ViewModel call
if (!Utility.isInternetAvailable(mActivity)) return
ApiClient.apiService.someEndpoint(body).enqueue(object : Callback<SomeItem> {
    override fun onResponse(call: Call<SomeItem>, response: Response<SomeItem>) {
        if (response.isSuccessful) { /* handle */ }
        else { ToastUtil.show(mActivity, ApiHelper.parseErrorMessage(mActivity, response.code(), response.errorBody()?.string())) }
    }
    override fun onFailure(call: Call<SomeItem>, t: Throwable) { ToastUtil.show(mActivity, t.localizedMessage) }
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

1. Files Changed
2. Reason For Each Change
3. Diff Summary

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

Response structure depends on the actual API contract — there is no single mandatory wrapper for every endpoint. Match the model to what the server returns.

**`General<T>` wrapped** — use when the API envelope is `{"data": ..., "success": ..., "meta": ..., "message": ...}`:
- Declare: `Call<General<YourItem?>>`
- Success check: `response.isSuccessful && response.body()?.data != null`

**Flat / direct response** — use when the API returns a top-level object with no `data` key:
- Declare: `Call<YourItem>` with the fields your endpoint returns
- Success check depends on fields present:
  - Has `success: Boolean` field → `response.isSuccessful && response.body()?.success == true`
  - No `success` field → `response.isSuccessful` is sufficient

**Auth endpoints** (`/api/login`, `/api/register`, `/api/user`, `/api/password/*`, `/api/mpin/*`) always return flat/unwrapped JSON:

| Endpoint | Response model | Success check |
|---|---|---|
| `GET /api/user` | `UserProfileItem` | `response.isSuccessful` |
| `POST /api/login` | `LoginItem` | `response.isSuccessful` |
| `POST /api/register` | `RegisterItem` | `response.isSuccessful` |
| `POST /api/*/send-otp`, `verify-otp`, `reset` | `MessageItem` | `response.isSuccessful && body?.success == true` |

**Rule:** Never guess or assume `General<T>` — use whatever the real API returns.

**Nullability rules differ by DTO type:**

- **API response DTOs — every field must be nullable (`?`), no exceptions.** Gson silently sets any missing field to `null`; a non-nullable field crashes at runtime when the API omits it. Declare every field with `?` and apply `?: fallback` at the call site, never at the model definition. This covers numeric types too (`Int?`, `Double?`, `Long?`).

- **Local DTOs (activity-to-activity passing) — make a field nullable only if it can genuinely be absent.** There is no Gson parsing risk for local DTOs; the sender and receiver share the same compiled class. Declare fields as non-nullable (`val name: String`) when the value is always provided, and nullable (`val name: String?`) only when the field is legitimately optional.

**`@field:SerializedName` is required on API response DTOs only.** Every field in an `*Item` class that is parsed directly from a Retrofit/Gson API response must carry `@field:SerializedName("snake_case_key")` — this protects against ProGuard/R8 field-name obfuscation in release builds. **Local DTOs** used solely for activity-to-activity data passing (serialised with `Gson().toJson()` / `Gson().fromJson()` inside the app, never crossing a network boundary) do **not** require `@field:SerializedName`; plain field names are sufficient since both sender and receiver share the same compiled class.

```kotlin
// API response DTO — all fields nullable + @field:SerializedName required
data class SomeItem(
    @field:SerializedName("id")     val id: Int?,
    @field:SerializedName("amount") val amount: Double?,
    @field:SerializedName("name")   val name: String?
)

// Local activity-to-activity DTO — no annotation; nullable only where genuinely optional
data class SomeLocalItem(
    val id: Int,           // always present — non-nullable is correct
    val amount: Double,    // always present — non-nullable is correct
    val note: String?      // optional field — nullable is correct
)

// Wrong — non-nullable fields on an API response DTO crash when the server omits the field
data class SomeItem(
    @field:SerializedName("id")     val id: Int,
    @field:SerializedName("amount") val amount: Double
)
```

---

## Resources & Assets

When implementing UI:

* First search the project for existing drawables, icons, images, colors, styles, and dimensions.
* Reuse existing assets whenever possible — do not create duplicates.
* If a required asset is not available, use a clear placeholder name and continue implementation.
* Mention all missing assets in the output under **Missing Assets**.

Placeholder naming examples: `ic_edit_placeholder`, `bg_card_placeholder`, `img_banner_placeholder`

---

## Card Background Rule

**Always use `MaterialCardView` for card/container backgrounds. Never create a new `drawable` shape file for backgrounds that `MaterialCardView` can achieve.**

`MaterialCardView` handles:
- White or solid-color card backgrounds → `app:cardBackgroundColor`
- Rounded corners → `app:cardCornerRadius`
- Bordered stroke → `app:strokeColor` + `app:strokeWidth`
- Pill / badge shapes → `app:cardCornerRadius="20dp"` (or any large value)
- Elevation / shadow → `app:cardElevation`

Only create a `drawable` shape file when `MaterialCardView` genuinely cannot fulfill the requirement — e.g., gradient fills, complex multi-layer shapes, or vector path shapes. Default to `MaterialCardView` first; reach for a drawable shape only as a last resort.

**Exception — status badge chips:** Small inline status indicators (Success / Failed / Pending chips) may use a custom `drawable` shape file instead of `MaterialCardView`. These are typically `<shape>` ovals or rectangles with a solid fill and are acceptable as drawables since wrapping them in `MaterialCardView` adds unnecessary view hierarchy depth for a purely decorative, non-interactive element.

---

## Bill Payment Modules — Shared UI Pattern

The **Electricity Bill Payment** screen is the canonical design reference for all bill payment modules (Gas, Water, Broadband, Mobile, DTH, Cable, FASTag, Loans, Taxes, etc.).

**What "reusable UI" means here:**

- Each module gets its **own Activity** and its own `activity_*.xml` — do not share Activities across modules.
- **RecyclerView item layouts** (`item_*.xml`) and other shared XML components (operator selector items, plan card items, etc.) are **shared across modules** — do not duplicate XML files. Reference the same layout from each module's adapter.
- When implementing a new bill payment module, always check if the required item layout already exists (e.g., `item_operator.xml`, `item_plan.xml`) before creating a new one.
- If the design is identical to an existing item layout, reuse it directly. Only create a new layout file when the structure genuinely differs.

**In short:** different Activity + same item layouts. Never copy-paste XML from one module's item file into another — always `include` or reuse the existing layout.

**No cross-module Activity navigation — ever.** Never start one module's Activity from another module, even as a temporary stand-in. If the target module's Activity is not yet built, leave the click handler empty and add a `TODO(PAYTOUCH-xxx): navigate to XxxActivity when implemented` comment at that call site. Starting `ElectricityActivity` from the Gas module, for example, is a review blocker.

```kotlin
// ✅ Correct — pending module, placeholder with TODO
binding.cardGas -> {
    if (Utility.stopClick()) return@OnClickListener
    // TODO(PAYTOUCH-585): navigate to GasActivity when implemented
}

// ❌ Wrong — cross-module reuse
binding.cardGas -> {
    if (Utility.stopClick()) return@OnClickListener
    ElectricityActivity.start(mActivity) // never do this for a different module
}
```

---

## Transaction Screens — Shared Structure Across All Modules

Every bill payment module (Electricity, Gas, Water, DTH, Mobile, etc.) has three transaction screens: **Status**, **Report**, and **Detail**. The UI is identical across all modules — only the API endpoint and category icon differ.

### What is shared (never duplicate)

| Component | Location | Shared By |
|---|---|---|
| `TransactionAdp` | `adapter/TransactionAdp.kt` | All modules — category-agnostic, icon driven by `item.categoryIconRes` |
| `TransactionItem` | `transactions/model/TransactionItem.kt` | All modules — UI model, no API fields |
| `TransactionDetailActivity` | `transactions/TransactionDetailActivity.kt` | All modules — reused as-is |
| `TransactionFilterHelper` | `utill/TransactionFilterHelper.kt` | All report screens |
| `item_transaction.xml` | `res/layout/item_transaction.xml` | All modules via `TransactionAdp` |
| `lyt_shimmer_transaction_item.xml` | `res/layout/` | All transaction activity layouts |
| `sheet_filter.xml` | `res/layout/sheet_filter.xml` | All report screens |

### What is created per module

Each new module (e.g. Gas) needs only:
- `{Category}TransactionStatusActivity` + `activity_{category}_transaction_status.xml`
- `{Category}TransactionReportActivity` + `activity_{category}_transaction_report.xml`
- `{Category}TransactionStatusViewModel` — calls `api/{category}/transaction-status`
- `{Category}TransactionReportViewModel` — calls `api/{category}/payment-report`
- `{Category}TransactionReportDataItem` — response DTO
- `{Category}TransactionStatusRequest` + `{Category}TransactionReportRequest` — request bodies
- 2 `ApiService` entries

### Rules

- **Never create a new `TransactionDetailActivity` per module** — all modules share the one in `transactions/`.
- **Never create a new adapter or item layout per module** — `TransactionAdp` and `item_transaction.xml` are category-agnostic.
- **`TransactionDetailActivity` has no SMS Receipt button** — it was removed. Do not add it back.
- The activity layouts for status and report are **copied from the electricity versions** with only the title string changed. Do not redesign them.
- The category icon is set in the ViewModel's `mapToTransactionItem()` — pass `R.drawable.ic_{category}` there.
- Electricity is the canonical reference — when implementing any module's transaction screens, mirror `ElectricityTransactionStatusActivity` and `TransactionReportActivity` exactly.

---

## Android Activity Guidelines

### Hub / Dashboard Tap Handling

Every tappable entry on a hub screen (Home, Dashboard, category grid) must have an **explicit, intentional outcome** — never a silent no-op.

- **Module implemented** → navigate to its Activity.
- **Module pending** → leave the handler body empty and add a `TODO(PAYTOUCH-xxx): navigate to XxxActivity when implemented` comment. The TODO must name the ticket number and the target Activity class.

```kotlin
// ✅ Correct — pending module
binding.cardPrepaid -> {
    if (Utility.stopClick()) return@OnClickListener
    // TODO(PAYTOUCH-520): navigate to PrepaidActivity when implemented
}

binding.cardDth -> {
    if (Utility.stopClick()) return@OnClickListener
    // TODO(PAYTOUCH-521): navigate to DthActivity when implemented
}

// ❌ Wrong — silent no-op with no TODO
binding.cardPrepaid -> { }
```

A missing branch or a silent empty handler with no TODO is a review blocker.

---

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

### Intent Data Passing (Object Transfer Rule)

**Always pass a single object between Activities as a JSON string — never as individual `putExtra` fields.**

Serialize with `Gson().toJson(item)` into one `putExtra(EXTRA_ITEM, json)`. Deserialize lazily in the receiving Activity. This avoids fragile multi-field extraction and keeps the companion `start()` contract clean as the model evolves.

```kotlin
// Sender — companion object of the receiving Activity
private const val EXTRA_ITEM = "extra_item"

fun start(context: Context, item: MyItem) {
    context.startActivity(
        Intent(context, MyDetailActivity::class.java).apply {
            putExtra(EXTRA_ITEM, Gson().toJson(item))
        }
    )
}

// Receiver — field in the Activity
private val myItem: MyItem? by lazy {
    intent.getStringExtra(EXTRA_ITEM)?.let { Gson().fromJson(it, MyItem::class.java) }
}
```

**Rules:**
- One extra key (`EXTRA_ITEM`) — never one `putExtra` call per field.
- Declare `EXTRA_ITEM` as a `private const` in the companion object.
- Access the lazy property in `onCreate()` or later — never before `super.onCreate()`.
- Guard every function that uses the item with `val item = myItem ?: return`.

---

### Bottom Sheet Pattern

Use this pattern whenever a screen needs an in-place form or detail panel that slides up from the bottom. **Never use `Dialog` — always use `BottomSheetBehavior` embedded in the layout.**

**Sheet XML** (`sheet_*.xml`) — root ViewGroup (`ConstraintLayout`, `FrameLayout`, etc.) with `BottomSheetBehavior` attributes + `@drawable/bottom_sheet_bg`:

```xml
<androidx.constraintlayout.widget.ConstraintLayout
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:background="@drawable/bottom_sheet_bg"
    app:behavior_hideable="true"
    app:behavior_peekHeight="0dp"
    app:behavior_skipCollapsed="true"
    app:layout_behavior="com.google.android.material.bottomsheet.BottomSheetBehavior">
    <!-- Drag handle, title row with close button, form fields, action button -->
</androidx.constraintlayout.widget.ConstraintLayout>
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

> `sheet_filter.xml` (TransactionReportActivity) is the canonical reference implementation for this pattern.

---

## Button & List Loading State Rule

**This is a hard rule for all screens — no exceptions.**

### Button-triggered API calls

Every button that triggers an API call must show a `ProgressBar` **inside the button itself** while the call is in flight. Never use a full-screen loader, never use only alpha dimming for button actions.

**Pattern:**

1. Add `ObservableBoolean` variables per button in the layout `<data>` block, plus `import android.view.View`:

```xml
<data>
    <import type="android.view.View" />
    <variable name="onClickListener" type="android.view.View.OnClickListener" />
    <variable name="showProgressFetch" type="androidx.databinding.ObservableBoolean" />
    <variable name="showProgressPay"   type="androidx.databinding.ObservableBoolean" />
</data>
```

2. Inside the button container, add both the label/icon and a `ProgressBar`. Toggle them with the `ObservableBoolean`:

```xml
<LinearLayout
    android:id="@+id/llFetchBill"
    android:layout_width="100dp"
    android:layout_height="@dimen/btn_height"
    android:background="@drawable/bg_toggle_selected"
    android:gravity="center"
    android:onClickListener="@{onClickListener}">

    <androidx.appcompat.widget.AppCompatTextView
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="@string/btnFetchBill"
        android:textColor="@color/white"
        android:textSize="@dimen/text_size_12"
        android:visibility="@{showProgressFetch ? View.GONE : View.VISIBLE}" />

    <ProgressBar
        style="?android:attr/progressBarStyleSmall"
        android:layout_width="20dp"
        android:layout_height="20dp"
        android:indeterminateTint="@color/white"
        android:visibility="@{showProgressFetch ? View.VISIBLE : View.GONE}" />
</LinearLayout>
```

3. In Kotlin, declare one `ObservableBoolean` per button, assign to binding in `onCreate()`, and toggle on API start/finish:

```kotlin
private val showProgressFetch = ObservableBoolean(false)
private val showProgressPay   = ObservableBoolean(false)

// in onCreate(), after binding setup:
binding.showProgressFetch = showProgressFetch
binding.showProgressPay   = showProgressPay

// when starting the API call:
showProgressFetch.set(true)

// in both onResponse() and onFailure():
showProgressFetch.set(false)
```

4. Guard the click handler so the button cannot be tapped while its progress is showing:

```kotlin
binding.llFetchBill -> {
    if (Utility.stopClick()) return@OnClickListener
    if (showProgressFetch.get()) return@OnClickListener
    onFetchBill()
}
```

### List / dropdown field loading

When a field's data is loaded via API (e.g. an operator dropdown), show a small `ProgressBar` **inside the field slot** — replacing the arrow or trailing icon — while loading. Do NOT show a full-screen overlay or affect any other part of the screen.

```xml
<!-- inside the field row, wrap the trailing icon in a FrameLayout -->
<FrameLayout android:layout_width="16dp" android:layout_height="16dp">

    <androidx.appcompat.widget.AppCompatImageView
        android:id="@+id/ivCompanyArrow"
        android:layout_width="14dp"
        android:layout_height="14dp"
        android:layout_gravity="center"
        android:src="@drawable/ic_down_arrow"
        android:tint="@color/primary" />

    <ProgressBar
        android:id="@+id/pbCompanyLoading"
        style="?android:attr/progressBarStyleSmall"
        android:layout_width="16dp"
        android:layout_height="16dp"
        android:indeterminateTint="@color/primary"
        android:visibility="gone" />
</FrameLayout>
```

```kotlin
private fun setOperatorLoading(loading: Boolean) {
    binding.pbCompanyLoading.visibility = if (loading) View.VISIBLE else View.GONE
    binding.ivCompanyArrow.visibility   = if (loading) View.GONE else View.VISIBLE
    binding.flCompanyAnchor.isClickable = !loading
    binding.flCompanyAnchor.isFocusable = !loading
}
```

### What NOT to do

| Anti-pattern | Correct approach |
|---|---|
| Full-screen progress dialog / overlay | In-button ProgressBar |
| `view.alpha = 0.5f` as the only loading signal | In-button ProgressBar (alpha may be used additionally, never alone) |
| Single shared `isLoading` flag for multiple buttons | One `ObservableBoolean` per button |
| Showing a spinner that covers the whole screen for a button tap | Show only inside that button |

---
### Temporary Cross-Module Navigation Exception

Cross-module Activity navigation is prohibited by default.

Exception:
- Temporary reuse is allowed only when Product explicitly requires an existing screen until the module-specific screen is implemented.
- The code must include a `TODO(ticket-id)` referencing the follow-up work.
- The temporary navigation must be removed before the module-specific Activity is released.

**Active exception — Postpaid plan selection (B2C-59):**
`PostpaidActivity.onBrowsePlan()` currently launches `PrepaidPlanSelectionActivity` as a temporary stand-in because the `mobile-postpaid/plans` API is under construction. Once that API is ready, replace with a dedicated `PostpaidPlanSelectionActivity` + `PostpaidPlanSelectionViewModel` that calls the postpaid plans endpoint. The temporary call is marked with `TODO(B2C-59)` in `PostpaidActivity.kt`.
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

All project docs live in `docs/`. **Read the relevant files before starting any task — not after.** Assumptions made without reading the docs will contradict established rules.

| File | What it contains | Read when |
|---|---|---|
| `docs/caveman.md` | Plain-English system overview — what the app does, who uses it, how the pieces connect | **Always first**, on every new task |
| `docs/business_logic.md` | Domain rules: fee tiers, onboarding sequence, routing flags, validation rules | Before any feature, flow, or data-related code |
| `docs/dos_and_donts.md` | Explicit DOs and DON'Ts for architecture, API, naming, RecyclerView, UI patterns | Before any structural or architectural decision |
| `docs/screens_and_navigation.md` | Screen list, navigation graph, back-stack rules, intent extras | Before implementing a new screen or navigation flow |

If a task touches something not covered by any doc, **ask before proceeding**.
