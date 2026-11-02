# PayTouch Consumer — DOs and DON'Ts

Rules **not** already stated in `CLAUDE.md` (core architecture, naming, network, loading-state, click, RecyclerView rules live there — not repeated here), plus the full Category Module rules. UI code snippets: `docs/code_patterns.md`.

---

## DOs

**DO validate inputs before any API call.**
| Field | Rule |
|---|---|
| Mobile | Exactly 10 digits |
| PAN card | Regex `[A-Z]{5}[0-9]{4}[A-Z]{1}` |
| Aadhaar | Exactly 12 digits |
| Password | Minimum 8 characters |
| MPIN | Exactly 4 digits; confirmation must match |
| Email | Standard email format |

**DO enforce the mandatory onboarding sequence: KYC → MPIN.** Server flags `requires_kyc`, `requires_mpin` drive routing after login; never allow skipping. `requires_virtual_account` is permanently retired — no client routing for it.

**DO calculate the platform fee (tiers in `business_logic.md` §7) in a ViewModel and show it before the user confirms payment.**

**DO handle 401 globally via an OkHttp interceptor** (`SessionInterceptor` or equivalent): clear all SharedPreferences, launch LoginActivity with cleared back stack. Never per-ViewModel.

**DO use a date picker for every date field** (DOB, date-range filters) — never free text. **DO auto-calculate age** from the DOB picker result.

**DO play audio feedback after every payment** — `payment_success.mp3` / `payment_failed.mp3`; release the MediaPlayer in `onDestroy()`.

**DO rely on API endpoints for transaction history** — `api/{category}/payment-history` (recent) and `api/{category}/payment-reports` (filtered).

---

## DON'Ts

**DON'T put network calls in Activities.** ViewModels call `ApiClient.apiService` and surface results via `onLoading`/`onSuccess`/`onError` callbacks.

**DON'T wrap Retrofit calls in `viewModelScope.launch`.** Repository, LiveData, StateFlow are NOT required for one-shot API calls. Use them only when data must outlive the screen, needs caching/pagination of heavy data, is a continuous stream (WebSocket, polling), or is observed by multiple screens.

**DON'T create multiple SharedPreferences stores** for the same user's session data.

**DON'T create multiple Retrofit instances for the same base URL** — exactly one named client per distinct backend.

**DON'T hardcode active URLs/keys/tokens in Activity/ViewModel code.** Commented-out dev reference URLs (ngrok, localhost) inside `Constant.kt` are acceptable and must not be removed.

**DON'T store a token or shared state in a static/`companion object` mutable field** (race condition). Use `SharedPreferenceHelper` or a Repository singleton.

**DON'T put business logic (fees, status routing, validation) in Adapters or Activities.** Passing `context` as a function parameter to a ViewModel for a single operation is acceptable; storing it is not.

**DON'T store user-facing strings in Kotlin/Java** — all go in `strings.xml`.

**DON'T load images with Glide without `.placeholder()` and `.error()`.**

**DON'T add Room/SQLite/any local persistence for payment records** — the server is the source of truth.

**DON'T mix backends within one feature** — e.g. never `paytouch.in` for step A and `admin.paytouch.in` for step B of the same payment flow.

**DON'T suppress VPS sync failures silently** — they're non-blocking but must be logged.

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

> ViewModel base class: see `CLAUDE.md` rule 13.


### Extension Functions — Never Re-implement

| Function | Declared In | Replaces |
|---|---|---|
| `bearerToken()` | `utill/ViewModelExt.kt` | Any local `"Bearer $token"` construction |
| `getString(@StringRes id)` | `utill/ViewModelExt.kt` | Any local string-resource lookup |
| `Utility.formatAmount(value)` | `utill/Utility.kt` | Any local `NumberFormat` / currency formatting — never use raw string template `"₹${item.amount}"` |
| `Utility.formatAmount(raw: Double?)` | `utill/Utility.kt` | Electricity DTOs use `Double?` for amount fields — use this overload; all other modules use `String?` and the standard overload |
| `Utility.formatDate(raw, "dd MMM yyyy")` | `utill/Utility.kt` | Recent transaction list date display |
| `Utility.formatDate(raw, "dd/MM/yyyy")` | `utill/Utility.kt` | Transaction detail date-only display |
| `Utility.formatDate(raw)` | `utill/Utility.kt` | Full datetime display (default `"dd/MM/yyyy hh:mm a"`) |
| `Utility.maskNumber(number)` | `utill/Utility.kt` | Masked account/mobile display in list rows — format `9876*****0` (first 4 + `*****` + last 1) |

If a ViewModel declares a private copy of any of the above, delete it and use the shared function.

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
| `categoryIconRes` | `R.drawable.img_{category}` — the only visual difference between modules |
| `isMobileCategory` | `true` for Mobile Prepaid, Postpaid, DTH; `false` for all others — **never omit this field** |
| `isVehicleCategory` | `true` for FASTag only; `false` for all others — drives "Vehicle No" label in adapters |

Apply to every `mapToDisplayItem()` in every module's recent transaction ViewModel:

| Field | Value |
|---|---|
| `date` | `Utility.formatDate(item.createdAt, "dd MMM yyyy")` — **pre-format here**; the list card displays it directly, there is no detail formatter for recent items |
| `amount` | `Utility.formatAmount(item.totalPayable ?: item.amount)` — prefer `totalPayable`; fall back to `amount` only if `totalPayable` is absent |
| `isMobileCategory` | Same rule as `mapToTransactionItem()` above |
| `isVehicleCategory` | Same rule as `mapToTransactionItem()` above |

**`isMobileCategory` and `isVehicleCategory` drive adapter label behaviour:**
- `isMobileCategory = true` → "Mobile No - %s"
- `isVehicleCategory = true` → "Vehicle No - %s"
- Both `false` → "Consumer No - %s"
- **Masked display** (`TransactionAdp.tvMobile`): shows `Utility.maskNumber(item.mobileNumber)` — the `mobileNumber` field holds whichever identifier applies (mobile number, consumer number, or vehicle number)

**SMS Receipt label** — set `tvConsumerNoLabel` dynamically in `populateReceiptFromApi()`, never rely on the static XML default alone:

| Module | Label string |
|---|---|
| Electricity, Gas, Loan, Municipal Tax | `R.string.labelConsumerNo` — "Consumer No" |
| Mobile Prepaid, Mobile Postpaid, DTH | `R.string.labelMobileNo` — "Mobile No." |
| FASTag | `R.string.labelVehicleNumber` — "Vehicle Number" |

```kotlin
// In populateReceiptFromApi() — consumer-number category (Electricity, Gas, Loan, Municipal Tax)
binding.tvConsumerNoLabel.text = getString(R.string.labelConsumerNo)

// In populateReceiptFromApi() — mobile-number category (Prepaid, Postpaid, DTH)
binding.tvConsumerNoLabel.text = getString(R.string.labelMobileNo)

// In populateReceiptFromApi() — vehicle-number category (FASTag)
binding.tvConsumerNoLabel.text = getString(R.string.labelVehicleNumber)
```

The XML default (`android:text="@string/labelConsumerNo"`) is a layout-editor placeholder only; every Activity must set the label explicitly at runtime so the correct string is guaranteed regardless of the XML default.

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
| Mobile Prepaid | `"mobile_recharge"` |
| Mobile Postpaid | `"mobile_postpaid"` |
| DTH | `"dth"` |
| FASTag | `"fastag"` |
| Loan Repayment | `"loan_repayment"` |
| Municipal Tax | `"municipal_tax"` |
| Any new module | Check the API contract — never guess |

---

### SMS Receipt Entry Modes

Every `{Category}SmsReceiptActivity` has two modes, controlled by `EXTRA_FROM_PAYMENT: Boolean`:

| Mode | `fromPayment` | Title row | Data |
|---|---|---|---|
| After payment | `true` | Hidden | `GET /api/{category}/latest-payment` |
| From recent transactions | `false` | Visible (tab bar) | Same `GET /api/{category}/latest-payment` |

Both modes call `getLatestPayments()` unconditionally in `onCreate()`. There is no separate per-transaction receipt endpoint — the latest-payment endpoint always returns the most recent record for the authenticated user.
