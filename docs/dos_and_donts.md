# PayTouch Consumer — DOs and DON'Ts

Every rule here is derived from a specific pattern or requirement for this project. The reason is given for each so you can judge edge cases rather than blindly following the rule.

---

## DOs — Patterns to Keep and Enforce

### Architecture

**DO use `ApiClient` as the only Retrofit source.**
This project has exactly one `ApiClient` class. ViewModels call `ApiClient.apiService` directly for standard API calls. No Activity should create its own Retrofit instance. If additional base URLs are needed (e.g., VPS backend), create one named client per URL with a clear, stable name.

**DO use `SharedPreferenceHelper` as the ONLY SharedPreferences access point.**
Every read and write must go through `SharedPreferenceHelper`. No Activity or ViewModel should call `getSharedPreferences()` directly. Having multiple scattered stores for the same user data causes bugs and confusion.

**DO put all URLs, keys, and string constants in `Constant.kt`.**
Never hardcode a string that is used in more than one place. Every URL, API key, SharedPreferences key name, and bundle key belongs in `Constant.kt`.

**DO call `Utility.isInternetAvailable()` before every network request.**
This is a critical UX requirement. Never make an API call without this check. Show a user-facing message via `ToastUtil` if there is no internet.

**DO generate transaction IDs client-side before submitting a payment.**
The format `PYTCH[DDMMYYYYHHMMSS]M` is correct and must be preserved. This ID is the link between local DB records and server records.

**DO enforce the mandatory onboarding sequence: KYC → MPIN → Virtual Account.**
The server drives this via flags (`requires_kyc`, `requires_mpin`, `requires_virtual_account`). Always check these flags after login and route accordingly. Never allow a user to skip a step.

**DO apply platform fee calculation before showing the final payment amount.**
Fee tiers (₹4 / ₹8 / ₹20 / ₹30 based on amount range) must be calculated and displayed before the user confirms payment. This logic belongs in a ViewModel, not an Activity.

**DO handle 401 responses globally via an OkHttp interceptor.**
A `SessionInterceptor` (or equivalent) must catch any 401 response, clear all SharedPreferences, and launch LoginActivity with a cleared back stack. This must not be implemented per-ViewModel.

**DO validate inputs before any API call.**
| Field | Rule |
|---|---|
| Mobile | Exactly 10 digits |
| PAN card | Regex `[A-Z]{5}[0-9]{4}[A-Z]{1}` |
| Aadhaar | Exactly 12 digits |
| Password | Minimum 8 characters |
| MPIN | Exactly 4 digits; confirmation must match |
| Email | Standard email format |

**DO use the `when` block + `Utility.stopClick()` guard for all click handling.**
One centralized `onClickListener()` per Activity that delegates to a `when` block. Every case must call `Utility.stopClick()` before any navigation, API call, or state change.

**DO extend `BaseActivity` for every screen.**
All Activities must extend `BaseActivity`. Never extend `AppCompatActivity` directly.

**DO use `ToastUtil` for all user-facing messages.**
Never call `Toast.makeText()` directly in any Activity, ViewModel, or utility class.

**DO use `ApiHelper.parseErrorMessage()` to extract error text from failed API responses.**
Never show a raw server error string or exception message to the user. Always parse it through `ApiHelper`.

**DO show loading state inside the button that triggered the API call — never use a full-screen loader or alpha-only dimming.**
Every button that triggers a network call must contain both its label/icon and a `ProgressBar` child. Use one `ObservableBoolean` per button declared in the layout `<data>` block and toggled in Kotlin. Set it `true` before the call and `false` in both `onResponse` and `onFailure`. Guard the click handler with `if (showProgressXxx.get()) return`. For dropdown/list fields, show a small spinner inside the field slot (replacing the arrow icon) — never affect the rest of the screen. See the **Button & List Loading State Rule** section in `CLAUDE.md` for the full pattern with XML and Kotlin examples.

**DO use a date picker for all date input fields.**
For DOB, date range filters, and any other date field — use a picker dialog, never a free-text input.

**DO auto-calculate age from the selected date of birth.**
Never make users manually enter their age. Calculate it from the DOB picker result.

**DO use `notifyItemChanged(position)` for single-item RecyclerView updates.**
Never call `notifyDataSetChanged()` for a single item change. Use targeted updates.

**DO store every payment attempt in Room DB immediately after attempting.**
Write to Room on every payment attempt — success or failure — so history is always available offline.

**DO play audio feedback after every payment.**
`payment_success.mp3` and `payment_failed.mp3` must play after every transaction result. Clean up the MediaPlayer in `onDestroy()`.

**DO use `ActivityResultLauncher` for all activity results and file picking.**
The `startActivityForResult()` / `onActivityResult()` APIs are deprecated. Use `registerForActivityResult()` everywhere.

**DO prefer Data Binding (`android:onClickListener="@{onClickListener}"`) over programmatic `setOnClickListener()`.**
Data Binding is the project's established pattern. Use it consistently.

---

## DON'Ts — Anti-Patterns to Avoid

### Architecture

**DON'T put network calls in Activities.**
All network calls must go in a ViewModel. The ViewModel calls `ApiClient.apiService` directly and surfaces results via `onLoading`/`onSuccess`/`onError` callbacks. Activities are pure UI responders.

**DON'T use `suspend fun` or coroutines for Retrofit API calls.**
All Retrofit endpoint declarations must return `Call<T>` (not `Response<T>`) and must be invoked with `.enqueue()`. Never use `suspend fun` on an `ApiService` method, and never wrap a Retrofit call in `viewModelScope.launch`. The `.enqueue()` callback already executes on the main thread — no dispatcher needed.

```kotlin
// Correct
fun someEndpoint(...): Call<SomeItem>

// Wrong — do not use
suspend fun someEndpoint(...): Response<SomeItem>
```

**Repository, LiveData, and StateFlow are NOT required for standard one-shot API calls.** Use them only when:
- Data must outlive the screen (shared across multiple destinations)
- Data is heavy and needs caching or pagination
- Data represents a continuous stream of updates (WebSocket, polling)
- Multiple screens must observe the same data simultaneously

**DON'T create multiple SharedPreferences stores for the same user.**
Use one store, accessed through `SharedPreferenceHelper`. Do not call `getSharedPreferences()` with different store names for the same user's session data.

**DON'T create multiple Retrofit instances for the same base URL.**
One `ApiClient` per distinct base URL. If a second distinct backend is needed (e.g., VPS admin), create exactly one named client for it — not a new instance for each screen.

**DON'T hardcode any active URL, key, or token in Activity/ViewModel code.**
All constants belong in `Constant.kt`. Commented-out development reference URLs (e.g. ngrok, localhost) inside `Constant.kt` are acceptable and must not be removed — they serve as reference during development.

**DON'T store a static mutable field for a token or shared state.**
Storing a dynamic token in a `companion object` or `static` field is a race condition anti-pattern. Use `SharedPreferenceHelper` or a Repository singleton.

**DON'T put business logic in Adapters or Activities.**
Fee calculation, status routing, and validation belong in a ViewModel or dedicated utility. Activities respond to UI events and update views — nothing more.

**DON'T store Activity or Context references as fields inside ViewModels.**
ViewModels must not hold `Activity`, `Fragment`, or `Context` as a class-level property — this causes memory leaks. Passing `context` as a function parameter for a single operation is acceptable.

**DON'T skip the internet check before any API call.**
`Utility.isInternetAvailable()` must be called every time, no exceptions.

**DON'T call `notifyDataSetChanged()` on a RecyclerView for a single-item update.**
This causes full list re-renders and visual flicker. Use `notifyItemChanged(position)`.

**DON'T use raw `Toast.makeText()` directly.**
All messages go through `ToastUtil`. This is non-negotiable.

**DON'T store user-facing strings in Kotlin/Java code.**
All user-visible messages must be in `strings.xml`.

**DON'T load images with Glide without specifying a placeholder and error drawable.**
`Glide.with(context).load(url).into(view)` without `.placeholder()` and `.error()` leaves blank space during load and on failure.

**DON'T use `startActivity()` without calling `Utility.stopClick()` first.**
Rapid taps can launch duplicate Activities. Every click handler must be guarded before any navigation.

**DON'T name adapters with the suffix `Adapter` — use `Adp`.**
Convention: `TransactionAdp`, not `TransactionAdapter`. Consistent naming makes the codebase scannable.

**DON'T name models with suffixes like `Response` or `Model` — use `Item`.**
Convention: `TransactionItem`, not `TransactionResponse` or `TransactionModel`.

**DON'T access Room DB on the main thread.**
Room must always be accessed via coroutines (`Dispatchers.IO`) or RxJava. Direct access from Activity callbacks causes `IllegalStateException` and ANRs.

**DON'T mix multiple API backends for the same feature.**
Each feature must use exactly one base URL for all its calls. Do not call `paytouch.in` for step A and `admin.paytouch.in` for step B of the same payment flow.

**DON'T suppress VPS sync failures silently.**
VPS sync calls are non-blocking, but failures must be logged so issues can be detected and debugged.

**DON'T use a full-screen progress dialog, overlay, or pure alpha dimming for button-triggered API calls.**
Full-screen loaders block the entire UI unnecessarily. Alpha-only dimming gives no clear feedback that a network call is in flight. Always show a `ProgressBar` inside the specific button that was tapped. For field-level list loading (dropdown, selector), show a localized spinner inside that field only — never a full-screen indicator. One `ObservableBoolean` per button; never a single shared `isLoading` flag driving multiple buttons' states.

---

## Category Module Output Rules

**All bill payment modules share the same design. When generating code for any new module (Water, Broadband, DTH, Cable, FASTag, Loans, Taxes, etc.) apply every rule in this section without exception.**

---

### Files to Generate Per Module

Create exactly these files — no more, no less:

| File | Copy From |
|---|---|
| `{Category}Activity` + `activity_{category}.xml` | `ElectricityActivity` / `activity_electricity.xml` |
| `{Category}ViewModel` | `ElectricityViewModel` (extends `BaseBillViewModel`) |
| `{Category}RecentTransactionActivity` + `activity_{category}_recent_transaction.xml` | Electricity equivalent |
| `{Category}RecentTransactionViewModel` | Electricity equivalent |
| `{Category}TransactionReportActivity` + `activity_{category}_transaction_report.xml` | Electricity equivalent |
| `{Category}TransactionReportViewModel` | Electricity equivalent |
| `{Category}TransactionStatusActivity` + `activity_{category}_transaction_status.xml` | Electricity equivalent |
| `{Category}TransactionStatusViewModel` | Electricity equivalent |
| `{Category}SmsReceiptActivity` + `activity_{category}_sms_receipt.xml` | Electricity equivalent |
| `{Category}SmsReceiptViewModel` | Electricity equivalent (standalone `AndroidViewModel`) |
| `retrofit/model/{category}/` DTOs | Match actual API contract — never guess field names |
| `ApiService.kt` entries under `// ── {Category} ──` block | One declaration per endpoint |

Electricity is the canonical template. Copy it; swap names and endpoints only. Do not redesign.

---

### What Is Shared — Never Duplicate Per Module

| Component | Location |
|---|---|
| `TransactionDetailActivity` | `transactions/TransactionDetailActivity.kt` |
| `TransactionAdp` | `adapter/TransactionAdp.kt` |
| `RecentTransactionAdp` | `adapter/RecentTransactionAdp.kt` |
| `TransactionItem` | `transactions/model/TransactionItem.kt` |
| `RecentTransactionItem` | `transactions/model/RecentTransactionItem.kt` |
| `TransactionFilterHelper` | `utill/TransactionFilterHelper.kt` |
| `ReceiptHelper` | `utill/ReceiptHelper.kt` |
| `item_transaction.xml` | `res/layout/item_transaction.xml` |
| `lyt_shimmer_transaction_item.xml` | `res/layout/lyt_shimmer_transaction_item.xml` |
| `sheet_filter.xml` | `res/layout/sheet_filter.xml` |

If one of these is missing a field required by a new module, add it to the existing shared class (nullable, with a sensible default at the call site). Never fork a separate copy.

---

### ViewModel Base Class

| ViewModel | Base Class |
|---|---|
| `{Category}ViewModel` (main payment screen) | `BaseBillViewModel` |
| `{Category}RecentTransactionViewModel` | `AndroidViewModel` |
| `{Category}TransactionReportViewModel` | `AndroidViewModel` |
| `{Category}TransactionStatusViewModel` | `AndroidViewModel` |
| `{Category}SmsReceiptViewModel` | `AndroidViewModel` — **not** `BaseBillViewModel`; receipt only needs `bearerToken()`, not balance checks |

---

### Extension Functions — Never Re-implement

| Function | Declared In | Replaces |
|---|---|---|
| `bearerToken()` | `utill/ViewModelExt.kt` | Any local `"Bearer $token"` construction |
| `getString(@StringRes id)` | `utill/ViewModelExt.kt` | Any local string-resource lookup |
| `Utility.formatAmount(value)` | `utill/Utility.kt` | Any local `NumberFormat` / currency formatting — never use raw string template `"₹${item.amount}"` |
| `Utility.formatDate(raw, "dd MMM yyyy")` | `utill/Utility.kt` | Recent transaction list date display |
| `Utility.formatDate(raw, "dd/MM/yyyy")` | `utill/Utility.kt` | Transaction detail date-only display |
| `Utility.formatDate(raw)` | `utill/Utility.kt` | Full datetime display (default `"dd/MM/yyyy hh:mm a"`) |

If a ViewModel declares a private copy of any of the above, delete it and use the shared function.

#### Date format by screen — use exactly the format below, never the wrong one

| Screen | Format string | Output example |
|---|---|---|
| Recent transaction list (`{Category}RecentTransactionViewModel`) | `"dd MMM yyyy"` | `11 Jul 2026` |
| Transaction detail (`TransactionDetailActivity`) | `"dd/MM/yyyy"` | `11/07/2026` |
| Any other full datetime display | `"dd/MM/yyyy hh:mm a"` | `11/07/2026 03:45 PM` |

#### Amount formatting — never use raw string templates

Always use `Utility.formatAmount(raw: String?)` for every currency field. It normalises decimal places, applies Indian number formatting, and prepends `₹`. Returning `"-"` on null/blank is intentional.

```kotlin
// Correct
amount      = Utility.formatAmount(item.billAmount)
platformFee = Utility.formatAmount(item.platformFee)
totalPayable = Utility.formatAmount(item.totalPayable)

// Wrong — raw string template bypasses normalisation; "100.0" displays as ₹100.0
amount = "₹${item.billAmount ?: "0.00"}"
```

**Exception — platform fee is genuinely zero:** When the DTO has no `platformFee` field (e.g. Prepaid), hardcode `"₹0.00"` explicitly. Do **not** pass `null` to `Utility.formatAmount()` — it returns `"-"`, which is wrong for a zero fee.

---

### `mapToTransactionItem()` Rules

Apply to every `mapToTransactionItem()` in every module's report/status ViewModel:

| Field | Value |
|---|---|
| `userId` | `item.id?.toString() ?: "--"` — **never** use loop index or `(index + 1).toString()` |
| `date` | `item.createdAt ?: "--"` — **raw ISO string, do not pre-format here**; `TransactionDetailActivity` formats it with `"dd/MM/yyyy"` at display time |
| `amount` | `Utility.formatAmount(item.amount)` — never a string template |
| `platformFee` | `Utility.formatAmount(item.platformFee)` — or `"₹0.00"` if the DTO has no fee field |
| `totalPayable` | `Utility.formatAmount(item.totalPayable)` |
| `categoryIconRes` | `R.drawable.ic_{category}` — the only visual difference between modules |

Apply to every `mapToDisplayItem()` in every module's recent transaction ViewModel:

| Field | Value |
|---|---|
| `date` | `Utility.formatDate(item.createdAt, "dd MMM yyyy")` — **pre-format here**; the list card displays it directly, there is no detail formatter for recent items |
| `amount` | `Utility.formatAmount(item.amount ?: "--")` |

---

### Progress State Pattern

Every module must follow the project-wide `ObservableBoolean` + DataBinding pattern. Per-module checklist:

- Main payment screen: one `ObservableBoolean` per button (`showProgressFetch`, `showProgressPay`)
- Operator/field loading: spinner inside the field slot (`pbCompanyLoading` + `ivCompanyArrow` swap)
- SMS receipt download/share: `ObservableBoolean showProgressReceipt`, wired to both `pbReceiptLoading` and `pbDisplayLoading` via DataBinding
- All `ObservableBoolean` variables set `false` in **both** `onResponse` and `onFailure` — no exceptions
- Click guard: `if (showProgressXxx.get()) return@OnClickListener` before every API call

#### Transaction Status Screen — Search Loading

Every `{Category}TransactionStatusActivity` uses a single `ObservableBoolean showProgressSearch` for the search button:

```kotlin
private val showProgressSearch = ObservableBoolean(false)

// in onCreate():
binding.showProgressSearch = showProgressSearch

// showShimmer() drives it — true on page-1 load, false when done:
private fun showShimmer(show: Boolean) {
    showProgressSearch.set(show)
    // ... shimmer visibility toggle
}

// Click guard — always use showProgressSearch, never viewModel.isLoading:
binding.llSearch -> {
    if (Utility.stopClick()) return@OnClickListener
    if (showProgressSearch.get()) return@OnClickListener
    onSearch()
}
```

**Do not guard the search button with `viewModel.isLoading` directly** — the `ObservableBoolean` is already kept in sync via `showShimmer()` and is the canonical guard.

#### List Loading Pattern — Shimmer vs Footer

| Situation | What to show |
|---|---|
| Page 1 initial load (or filter/search reset) | Hide `rvTransactions`, show `shimmerLayout` + `startShimmer()` |
| Page 2+ pagination load | Keep `rvTransactions` visible, show `pbLoadMore` footer spinner |
| Load complete (any page) | Stop/hide shimmer or `pbLoadMore`; show `tvEmpty` if list is empty |

```kotlin
// Page 1
if (page == 1) showShimmer(true) else showFooterLoader(true)

// onSuccess
if (page == 1) {
    showShimmer(false)
    mArrayList.clear(); mArrayList.addAll(list)
    adapter.notifyDataSetChanged()
} else {
    showFooterLoader(false)
    val start = mArrayList.size
    mArrayList.addAll(list)
    adapter.notifyItemRangeInserted(start, list.size)
}

// onError — always hide the loader that was shown
if (page == 1) showShimmer(false) else showFooterLoader(false)
```

Pagination trigger — attach once in `setupRecyclerView()`:

```kotlin
recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
        if (dy <= 0) return
        val lm = recyclerView.layoutManager as LinearLayoutManager
        if (lm.findLastVisibleItemPosition() >= lm.itemCount - 3 && viewModel.canLoadMore()) {
            loadPage(viewModel.nextPage())
        }
    }
})
```

**Never call `showShimmer(true)` for pagination pages** — it blanks the existing list. Footer spinner only.

---

### Layout Rules

- Transaction status/report/recent layouts: copy the electricity equivalent XML, change only the `<string>` title and the toolbar title string resource. Do not alter the view hierarchy.
- SMS receipt layout: copy `activity_electricity_sms_receipt.xml` or `activity_gas_sms_receipt.xml`, change only the title. Do not add or remove views.
- All transaction list layouts must include `lyt_shimmer_transaction_item.xml` as the loading placeholder.
- Operator dropdown field must include the `pbCompanyLoading` + `ivCompanyArrow` inside a `FrameLayout` slot — matching `activity_electricity.xml` exactly.

---

### ApiService Declaration

- Add all endpoints for a module under a single comment block: `// ── {Category} ──`
- Never scatter a module's endpoints across other modules' sections
- Never add an endpoint inside a ViewModel, Activity, or anywhere other than `ApiService.kt` (or `ApiAdminService.kt` for `admin.paytouch.in` endpoints)

---

### Transaction Type Parameter

The `type` query parameter for `GET /api/transactions` must match the server contract:

| Module | `type` value |
|---|---|
| Electricity | `"electricity"` |
| Gas | `"gas"` |
| Prepaid / Mobile Recharge | `"mobile_recharge"` |
| Any new module | Check the API contract — never guess |

---

### SMS Receipt Entry Modes

Every `{Category}SmsReceiptActivity` has two modes, controlled by `EXTRA_FROM_PAYMENT: Boolean`:

| Mode | `fromPayment` | Title row | Data |
|---|---|---|---|
| After payment | `true` | Hidden | `GET /api/{category}/latest-payment` |
| From recent transactions | `false` | Visible (tab bar) | Same `GET /api/{category}/latest-payment` |

Both modes call `getLatestPayments()` unconditionally in `onCreate()`. There is no separate per-transaction receipt endpoint — the latest-payment endpoint always returns the most recent record for the authenticated user.
