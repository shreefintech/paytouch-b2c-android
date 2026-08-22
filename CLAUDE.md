# PayTouch Consumer — Claude Context

PayTouch Consumer is a **fintech Android app** (Kotlin) for Indian consumers to pay utility bills (electricity, gas, mobile, DTH, cable, broadband, FASTag, loans, taxes) and manage a digital wallet. Single user role — every logged-in user has the same feature set. Built by Shreefintech.

> **Current state:** API wiring is **in progress**. Core modules (Auth, Electricity, Gas, Prepaid, Postpaid, DTH, FASTag, Loan, Municipal Tax, My Account) have live API integration. Newly added modules may begin with UI stubs marked `TODO(PAYTOUCH-xxx):` — do not treat pending stubs or relaxed validations as bugs.

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

Write production-ready Kotlin/MVVM Android code — readable, performant, scalable, testable. Reuse existing components, follow SOLID principles, consider lifecycle and threading in every implementation. Never sacrifice code quality for brevity.

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
├── onboarding/     # onboarding/kyc/ — KYC hub (KycActivity), identity verification (IdentityVerificationActivity), bank details (BankDetailsActivity), KYC status (KycStatusActivity); virtual account is handled server-side — no client Activity
├── home/           # Home/Dashboard screen (HomeActivity — currently at root level, will move here)
├── electricity/    # Electricity bill payment + transaction history (canonical module template)
├── gas/            # Gas bill payment + transaction history
├── prepaid/        # Mobile prepaid recharge — operator + circle + plan selection
├── postpaid/       # Mobile postpaid bill payment
├── dth/            # DTH recharge — operator + plan selection
├── fastag/         # FASTag recharge — vehicle number + amount, real-time fee, no bill-fetch
├── loan/           # Loan repayment — bill-fetch pattern; circleId = "0"
├── municipaltax/   # Municipal tax payment — bill-fetch pattern
├── myaccount/      # My Account — two-tab profile viewer (Account Info + Refer & Earn)
├── transactions/   # Shared TransactionDetailActivity + TransactionItem model (never duplicated per module)
├── adapter/        # Shared adapters: TransactionAdp, RecentTransactionAdp, PrepaidPlanAdp, DthPlanAdp
├── enums/          # Project-wide enums (LoginMode, etc.)
├── glass/          # LiquidGlassEffect custom blur UI components
├── retrofit/       # All networking (ApiService, ApiClient, ApiHelper, models)
├── utill/          # Shared utilities — NOTE: spelling "utill" is intentional, never rename
├── widget/         # Reusable custom views (LiquidGlassButton, CustomDropdown, etc.)
├── BaseActivity.kt
├── BaseBillViewModel.kt
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
10. **All API endpoints must be declared in `ApiService` or `ApiAdminService`** — never construct `OkHttpClient` or `Retrofit` directly inside a ViewModel or Activity. Each backend URL has exactly one registered client: `ApiClient` for `paytouch.in`, `ApiAdminClient` for `admin.paytouch.in`.
11. **`mapToTransactionItem()` must use `item.id?.toString() ?: "--"` for `userId`** — never use `(index + 1).toString()` or any iteration index. Using the loop index produces duplicate `userId` values on paginated appends (page 2+ resets index to 0) and loses the server-side record identity.
12. **Never generate `transaction_id` client-side** — the backend owns transaction ID generation. Do not call `Utility.generateTransactionId()` or include a `transaction_id` field in any process-payment request DTO. The server returns the transaction ID in the response; read it from there.
13. **ViewModel base class is determined by role, not by module** — follow this table exactly; never use `BaseBillViewModel` for a ViewModel that does not need balance checks:

| ViewModel role | Base class |
|---|---|
| Main payment screen (`{Category}ViewModel`) | `BaseBillViewModel` |
| Recent transactions (`{Category}RecentTransactionViewModel`) | `AndroidViewModel` |
| Transaction report (`{Category}TransactionReportViewModel`) | `AndroidViewModel` |
| Transaction status (`{Category}TransactionStatusViewModel`) | `AndroidViewModel` |
| SMS receipt (`{Category}SmsReceiptViewModel`) | `AndroidViewModel` |

`BaseBillViewModel` provides balance-check methods only needed by payment ViewModels; others use `bearerToken()` and `getString()` from `ViewModelExt.kt`. Do not flag report/status ViewModels for extending `AndroidViewModel` directly.

14. **`isMobileCategory` flag must be set in every `mapToTransactionItem()` and `mapToDisplayItem()`** — never hard-coded in the adapter. Set `isMobileCategory = true` for Mobile Prepaid, Mobile Postpaid, and DTH modules; `isMobileCategory = false` for all others. The adapter uses this flag to switch between "Mobile No" and "Consumer No" labels.

    **SMS Receipt (`{Category}SmsReceiptActivity`)** — set `binding.tvConsumerNoLabel.text` dynamically in `populateReceiptFromApi()`. Consumer-number modules → `getString(R.string.labelConsumerNo)`; mobile-number modules → `getString(R.string.labelMobileNo)`.

15. **Use `Utility.maskNumber()` for the account number shown in transaction list rows** — `TransactionAdp` calls `Utility.maskNumber(item.mobileNumber)` for `tvMobile`. Format: `9876*****0` (first 4 + five asterisks + last 1). Never display the raw number in a list row; only in `TransactionDetailActivity`.

16. **`Utility.formatAmount()` has two overloads — use the correct one for the DTO field type:**
    - `formatAmount(raw: String?)` — for all DTOs whose amount fields are `String?` (Gas, Prepaid, Postpaid, and future modules)
    - `formatAmount(raw: Double?)` — use only for Electricity DTOs (`ElectricityTransactionReportDataItem`) whose amount fields are `Double?`
    - Never use `"₹%.2f".format(value)` or any raw string template for currency — always go through `Utility.formatAmount()`

17. **`Utility.formatDate()` is the only date formatter** — never call `SimpleDateFormat` directly in a ViewModel. Use:
    - `Utility.formatDate(raw, "dd MMM yyyy")` in Recent Transaction `mapToDisplayItem()`
    - `Utility.formatDate(raw, "dd/MM/yyyy")` in `TransactionDetailActivity` for date-only display
    - `Utility.formatDate(raw)` (default `"dd/MM/yyyy hh:mm a"`) for full datetime fields
    - In `mapToTransactionItem()` pass `item.createdAt ?: "--"` raw — do not pre-format

18. **Confirmed backend quirks — do NOT "fix" these:**

    | Location | Apparent anomaly | Confirmed behaviour |
    |---|---|---|
    | `ApiService.kt` — `getMunicipalTaxTransactionStatus` | Uses `@POST("${AUTH}mobile-recharge/transaction-status")` (not `municipal-taxes/...`) | **Backend-side intentional routing.** The mobile-recharge transaction-status endpoint serves municipal tax queries too. Do not change this URL. |
    | `MunicipalTaxLatestPaymentDataItem.subService` | `@field:SerializedName("subservice")` has no underscore (unlike every other field) | **Confirmed from API contract.** The backend sends the key as `subservice`, not `sub_service`. Do not rename. |
    | `KycViewModel.callInitiate()` — HTTP 422 from `initiateKyc` | 422 is treated the same as success (proceeds to `submitSectionAPlaceholder`) | **Intentional contract.** 422 means KYC was already initiated for this user. The backend returns 422 instead of 200 on re-initiation; the correct response is to proceed as if initiation succeeded. Do not treat 422 as an error here. |

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
- **Adapters MUST end in `Adp`** — never `Adapter`.
- **Activities MUST end in `Activity`** — never `Screen`, `Page`, `View`.
- **ViewModels MUST end in `ViewModel`**.
- **Layouts MUST use the correct prefix** — `activity_`, `item_`, `lyt_`, `sheet_`, `dialog_`.
- **Drawables MUST use `ic_` / `bg_` / `img_`** — never bare names or other prefixes.
- **String IDs MUST use camelCase context prefixes**: `msg` (toasts), `title` (screen titles), `label` (field labels), `hint` (input hints), `btn` (button text), `err` (errors), `category` (category names).
- When two `Item` classes share the same name, qualify by context: `UserProfileItem` vs `UserItem`.

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

Every `LiquidGlassButton` in an Activity **must** call `.attach(root as ViewGroup)` in `onCreate()` after `setContentView()`. Without it the button renders without any background.

```kotlin
// In onCreate(), after setContentView():
binding.flUpload1.attach(binding.clRoot as ViewGroup)
binding.flSubmit.attach(binding.clRoot as ViewGroup)
```

Call `.attach()` on each `LiquidGlassButton` individually — never `LiquidGlassEffect.attach()`.

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
* Follow existing architecture, patterns, naming conventions, and folder structure.
* Reuse existing Activities, ViewModels, Adapters, utilities, and XML layouts; find similar implementations first.
* Do not refactor unrelated code.
* Keep changes minimal and focused; create new classes/files only when necessary.

---

## API Response Wrapper

Response structure depends on the actual API contract — there is no single mandatory wrapper for every endpoint. Match the model to what the server returns.

**`General<T>` wrapped** — use when the API envelope is `{"data": ..., "success": ..., "meta": ..., "message": ...}`:
- Declare: `Call<General<YourItem?>>`
- Success check: `response.isSuccessful && response.body()?.data != null`

**Flat / direct response** — use when the API returns a top-level object with no `data` key:
- Declare: `Call<YourItem>` with the fields your endpoint returns
- Success check: `response.isSuccessful && response.body()?.success == true` (if has `success` field), else `response.isSuccessful`

**Auth endpoints** (`/api/login`, `/api/register`, `/api/user`, `/api/password/*`, `/api/mpin/*`) always return flat/unwrapped JSON:

| Endpoint | Response model | Success check |
|---|---|---|
| `GET /api/user` | `UserProfileItem` | `response.isSuccessful` |
| `POST /api/login` | `LoginItem` | `response.isSuccessful` |
| `POST /api/register` | `RegisterItem` | `response.isSuccessful` |
| `POST /api/*/send-otp`, `verify-otp`, `reset` | `MessageItem` | `response.isSuccessful && body?.success == true` |

**Rule:** Never guess or assume `General<T>` — use whatever the real API returns.

**Nullability rules differ by DTO type:**

- **API response DTOs — every field must be nullable (`?`), no exceptions.** Gson silently sets any missing field to `null`; a non-nullable field crashes at runtime. Apply `?: fallback` at the call site, never at the model definition. This covers numeric types too (`Int?`, `Double?`, `Long?`).

- **Local DTOs (activity-to-activity passing) — make a field nullable only if it can genuinely be absent.** Declare non-nullable when the value is always provided; nullable only when legitimately optional.

**`@field:SerializedName` is required on API response DTOs only.** Every field parsed from a Retrofit/Gson response must carry `@field:SerializedName("snake_case_key")` to protect against ProGuard/R8 obfuscation. Local DTOs used only for activity-to-activity passing do **not** require it.

```kotlin
// API response DTO — all fields nullable + @field:SerializedName required
data class SomeItem(
    @field:SerializedName("id")     val id: Int?,
    @field:SerializedName("amount") val amount: Double?,
    @field:SerializedName("name")   val name: String?
)

// Local activity-to-activity DTO — no annotation; nullable only where genuinely optional
data class SomeLocalItem(
    val id: Int,
    val amount: Double,
    val note: String?      // optional field
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

`MaterialCardView` handles: solid-color backgrounds (`app:cardBackgroundColor`), rounded corners (`app:cardCornerRadius`), stroke borders (`app:strokeColor` + `app:strokeWidth`), pill shapes (large `cardCornerRadius`), and elevation/shadow.

Only create a `drawable` shape file for gradient fills, complex multi-layer shapes, or vector path shapes.

**Exception — status badge chips:** Small inline status indicators (Success / Failed / Pending) may use `<shape>` drawables since wrapping in `MaterialCardView` adds unnecessary hierarchy depth for non-interactive elements.

---

## Bill Payment Modules — Shared UI Pattern

The **Electricity Bill Payment** screen is the canonical design reference for all bill payment modules.

- Each module gets its **own Activity** and `activity_*.xml` — do not share Activities across modules.
- **RecyclerView item layouts** (`item_*.xml`) are **shared across modules** — check if `item_operator.xml`, `item_plan.xml`, etc. already exist before creating new ones.
- **In short:** different Activity + same item layouts. Never copy-paste item XML; reuse directly.

**No cross-module Activity navigation — ever.** If the target Activity is not yet built, leave the click handler empty with a `TODO(PAYTOUCH-xxx): navigate to XxxActivity when implemented` comment.

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

**Per-module README timing:** Add `{module}/README.md` only once the module is fully complete (payment flow + all transaction screens). Updating the root `README.md` module table is fine as soon as `HomeActivity` wires the module.

---

## Transaction Screens — Shared Structure Across All Modules

Every bill payment module has three transaction screens: **Status**, **Report**, and **Detail**. The UI is identical across all modules — only the API endpoint and category icon differ.

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

Each new module needs only:
- `{Category}TransactionStatusActivity` + `activity_{category}_transaction_status.xml`
- `{Category}TransactionReportActivity` + `activity_{category}_transaction_report.xml`
- `{Category}TransactionStatusViewModel` — calls `api/{category}/transaction-status`
- `{Category}TransactionReportViewModel` — calls `api/{category}/payment-report`
- `{Category}TransactionReportDataItem` — response DTO
- `{Category}TransactionStatusRequest` + `{Category}TransactionReportRequest` — request bodies
- 2 `ApiService` entries

### Rules

- **Never create a per-module `TransactionDetailActivity`, adapter, or item layout** — share the ones in `transactions/` and `adapter/`.
- **`TransactionDetailActivity` has no SMS Receipt button** — it was removed. Do not add it back.
- Copy status/report layouts from the Electricity versions (title string only changes); mirror `ElectricityTransactionStatusActivity` and `TransactionReportActivity` exactly.
- The category icon is set in the ViewModel's `mapToTransactionItem()` — pass `R.drawable.ic_{category}` there.

---

## Android Activity Guidelines

### Hub / Dashboard Tap Handling

Every tappable entry on a hub screen must have an **explicit, intentional outcome** — never a silent no-op.

- **Module implemented** → navigate to its Activity.
- **Module pending** → leave handler empty with a `TODO(PAYTOUCH-xxx): navigate to XxxActivity when implemented` comment.

```kotlin
// ✅ Correct — pending module
binding.cardPrepaid -> {
    if (Utility.stopClick()) return@OnClickListener
    // TODO(PAYTOUCH-520): navigate to PrepaidActivity when implemented
}

// ❌ Wrong — silent no-op with no TODO
binding.cardPrepaid -> { }
```

A missing branch or silent empty handler with no TODO is a review blocker.

---

### Click Handling

- Use a **single centralized `onClickListener()`** with a `when (it)` block — never scatter individual `setOnClickListener()` calls.
- Prefer **Data Binding** (`android:onClickListener="@{onClickListener}"`) over programmatic `setOnClickListener()`.
- Always guard against **rapid double-clicks** with `Utility.stopClick()`.

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

Apply whenever a screen contains an `EditText`.

**1. Manifest** — add `adjustResize`:

```xml
<activity
    android:name=".YourActivity"
    android:windowSoftInputMode="adjustResize" />
```

**2. Window insets** — handle system bars and IME:

```kotlin
ViewCompat.setOnApplyWindowInsetsListener(binding.clRoot) { v, insets ->
    val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
    val imeInsets  = insets.getInsets(WindowInsetsCompat.Type.ime())
    v.setPadding(systemBars.left, systemBars.top, systemBars.right, maxOf(imeInsets.bottom, systemBars.bottom))
    insets
}
```

**3. Dismiss keyboard** before button actions: `Utility.hideKeyboard(mActivity)`

---

### Back Handling

When a screen hosts dialogs, bottom sheets, or overlays, **close the topmost layer first**.

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

Call `onBack()` in `onCreate()`.

---

### Result Handling

Use when changes on the current screen should trigger a refresh on the previous screen.

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

**Exception — simple primitive-only screens:** When an Activity only receives 1–2 plain primitive values (e.g. a URL string and a display title) that do not represent a domain object, individual `putExtra` fields are acceptable. Use `private const` keys in the companion object and declare each as a `by lazy` property in the receiver. Create a Gson-wrapped DTO only when the data is a structured domain entity (an account, a transaction, a plan, etc.).

---

### Bottom Sheet Pattern

Use this pattern whenever a screen needs an in-place form or detail panel that slides up from the bottom. **Dialogs vs Bottom Sheets: Use `Dialog` for confirmation/alert modals. Use `BottomSheetBehavior` only for UI specifically designed as a bottom sheet. Do not require `BottomSheetBehavior` for confirmation dialogs.**

**Sheet XML** (`sheet_*.xml`):

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

> `sheet_filter.xml` (TransactionReportActivity) is the canonical reference implementation.

---

## Button & List Loading State Rule

**This is a hard rule for all screens — no exceptions.**

### Button-triggered API calls

Every button that triggers an API call must show a `ProgressBar` **inside the button itself**. Never use a full-screen loader or only alpha dimming.

**Pattern:**

1. Add `ObservableBoolean` variables per button in the layout `<data>` block:

```xml
<data>
    <import type="android.view.View" />
    <variable name="onClickListener" type="android.view.View.OnClickListener" />
    <variable name="showProgressFetch" type="androidx.databinding.ObservableBoolean" />
    <variable name="showProgressPay"   type="androidx.databinding.ObservableBoolean" />
</data>
```

2. Inside the button container, toggle label and `ProgressBar` with the `ObservableBoolean`:

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

4. Guard the click handler so the button cannot be tapped while loading:

```kotlin
binding.llFetchBill -> {
    if (Utility.stopClick()) return@OnClickListener
    if (showProgressFetch.get()) return@OnClickListener
    onFetchBill()
}
```

### List / dropdown field loading

Show a small `ProgressBar` **inside the field slot** — replacing the arrow icon — while loading.

```xml
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

All project docs live in `docs/`. **Read the relevant files before starting any task — not after.**

| File | What it contains | Read when |
|---|---|---|
| `docs/caveman.md` | Plain-English system overview | **Always first**, on every new task |
| `docs/business_logic.md` | Domain rules: fee tiers, onboarding, routing, validation | Before any feature or data-related code |
| `docs/dos_and_donts.md` | Explicit DOs and DON'Ts for architecture, API, naming, UI | Before any structural or architectural decision |
| `docs/screens_and_navigation.md` | Screen list, navigation graph, back-stack rules, intent extras | Before implementing a new screen or navigation flow |

If a task touches something not covered by any doc, **ask before proceeding**.
