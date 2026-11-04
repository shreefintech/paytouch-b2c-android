# PayTouch Consumer — DOs and DON'Ts

Rules **not** already stated in `CLAUDE.md` (core architecture, naming, network, loading-state, click, RecyclerView rules live there — not repeated here), plus the full Bill Pay rules. UI code snippets: `docs/code_patterns.md`.

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

**DO enforce the mandatory onboarding sequence: KYC → MPIN.** Server fields `next_step` + `mpin_required` drive routing after login, only via `NextStepRouter`; never allow skipping. `requires_virtual_account` is permanently retired — no client routing for it.

**DO show the platform fee from `GET billpay/fee-preview` before the user confirms a bill payment** (`business_logic.md` §7) — never a client-side fee table.

**DO handle 401 globally via an OkHttp interceptor** (`SessionInterceptor` or equivalent): clear all SharedPreferences, launch LoginActivity with cleared back stack. Never per-ViewModel.

**DO use a date picker for every date field** (DOB, date-range filters) — never free text. **DO auto-calculate age** from the DOB picker result.

**DO play audio feedback after every payment** — `payment_success.mp3` / `payment_failed.mp3`; release the MediaPlayer in `onDestroy()`.

**DO rely on the API for transaction history** — `GET api/mobile/transactions?category=` for every bill category (Recent, Status, Report, Receipt).

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

## Bill Pay Rules (every bill category)

**All bill categories share ONE flow (iOS "Bill Pay Module" doc): one screen, one set of APIs, one set of transaction
screens. A new category is a new `CategoryType` entry — never a new Activity / ViewModel / layout / endpoint set.**
API contract: `docs/business_logic.md` §7.

---

### Adding a Category

1. Add an entry to `enums/CategoryType.kt`: backend `value`, `titleRes`, `fieldLabelRes`, `iconRes` (`R.drawable.img_{category}`),
   `smsBodyRes`, `receiptTitleRes`, `defaultParamName` (only if the iOS/backend doc names one), and the
   `isPlanBased` / `needsCircle` / `isMobileCategory` / `isVehicleCategory` flags.
2. Add its tile to `activity_home.xml` and to the `tiles` list in `HomeActivity.layoutCategoryTiles()`; tap →
   `BillPayActivity.start(mActivity, CategoryType.X)` (or a `TODO(PAYTOUCH-xxx)` + "Coming soon" if the backend has no route).
3. Nothing else. If the category needs something `BillPayActivity` can't do, ask first.

---

### Files — all shared, never duplicate per category

| Component | Location |
|---|---|
| `BillPayActivity` + `activity_bill_pay.xml` + `item_bill_pay_field.xml` | `billpay/` — form for every category |
| `BillPayViewModel` | `billpay/viewmodel/` — billers, circles, fetch bill, fee preview, pay |
| `BillPayPlanSelectionActivity` + `BillPayPlanSelectionViewModel` + `BillPayPlanAdp` | Prepaid / DTH plans (`item_prepaid_plan.xml`) |
| `BillPaymentStatusActivity` + `BillPayStatusViewModel` | `transactions/` + `billpay/viewmodel/` — result screen, polls `billpay/status` |
| `RecentTransactionActivity`, `TransactionStatusActivity`, `TransactionReportActivity`, `SmsReceiptActivity` | `billpay/transactions/` — each takes the category (`start(context, CategoryType)`) |
| `BillPayTransactionsViewModel` | `billpay/viewmodel/` — `GET transactions` paging + `mapToTransactionItem()` / `mapToDisplayItem()` |
| `retrofit/model/billpay/*Item` | DTOs — match the API contract, never guess |
| `TransactionDetailActivity`, `TransactionAdp`, `RecentTransactionAdp`, `TransactionItem`, `RecentTransactionItem` | `transactions/`, `adapter/` |
| `TransactionFilterHelper`, `ReceiptHelper`, `MpinDialogExt`, `MoneyRequestId`, `BankDetailsDialogHelper` | `utill/` |
| `item_transaction.xml`, `lyt_shimmer_transaction_item.xml`, `sheet_filter.xml`, `dialog_mpin.xml` | `res/layout/` |

If a shared class is missing a field a category needs, add it to the shared class (nullable, default at the call
site). Never fork a copy.

---

> ViewModel base class: see `CLAUDE.md` rule 13.


### Extension Functions — Never Re-implement

| Function | Declared In | Replaces |
|---|---|---|
| `bearerToken()` | `utill/ViewModelExt.kt` | Any local `"Bearer $token"` construction |
| `getString(@StringRes id)` | `utill/ViewModelExt.kt` | Any local string-resource lookup |
| `Utility.formatAmount(value)` | `utill/Utility.kt` | Any local `NumberFormat` / currency formatting — never use raw string template `"₹${item.amount}"` |
| `Utility.formatAmount(raw: Double?)` | `utill/Utility.kt` | Bill Pay DTOs (`retrofit/model/billpay/`) use `Double?` for amounts — use this overload; other modules use `String?` and the standard overload |
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

### `mapToTransactionItem()` / `mapToDisplayItem()` Rules

Both live once in `BillPayTransactionsViewModel` and take the `CategoryType`:

| Field | Value |
|---|---|
| `transactionId` | `item.clientReference ?: "--"` |
| `userId` | `item.clientReference ?: "--"` — the API has no numeric id; **never** a loop index |
| `date` (`TransactionItem`) | `item.createdAt ?: "--"` — raw; `TransactionDetailActivity` formats it with `"dd/MM/yyyy"` |
| `date` (`RecentTransactionItem`) | `Utility.formatDate(item.createdAt, "dd MMM yyyy")` — pre-formatted |
| `amount` | `Utility.formatAmount(item.amount)` (`Double?` overload) — never a string template |
| `platformFee` | `Utility.formatAmount(charge + gst_amount)` |
| `totalPayable` | `Utility.formatAmount(amount + charge + gst_amount)` |
| `mobileNumber` / `accountNumber` | First `input_params` value other than `circle_code` (`accountNumber(item)`) |
| `referenceId` / `reference` | `provider_ref`, else `txn_reference`, else `client_reference` (`referenceOf(item)`) |
| `categoryIconRes` | `category.iconRes` |
| `isMobileCategory` / `isVehicleCategory` | From `CategoryType` — never hard-code in the adapter |
| `categoryType` | The `CategoryType` — drives View Receipt in `TransactionDetailActivity` |

**Labels:** `isMobileCategory` → "Mobile No - %s", `isVehicleCategory` → "Vehicle No - %s", else "Consumer No - %s".
List rows mask with `Utility.maskNumber()`; the raw number only in `TransactionDetailActivity`.

**SMS Receipt label** — `SmsReceiptActivity.populateReceipt()` sets `tvConsumerNoLabel` from `identifier_label`, falling
back to `labelVehicleNumber` / `labelMobileNo` / `labelConsumerNo` by category flags. Title = `category.receiptTitleRes`,
SMS body = `category.smsBodyRes`.

---

### Progress State Pattern

- Bill Pay form: `showProgressFetch` (Fetch Bill), `showProgressPay` (shared by Proceed and the MPIN popup's Submit — one
  pay call). Company / circle fields: spinner in the field slot (`pbCompanyLoading` / `pbCircleLoading`).
- SMS receipt: `showProgressReceipt`.
- Home: `showProgressCategories` (Retry button).
- Every `ObservableBoolean` is set `false` in **both** success and failure. Click guard
  `if (showProgressXxx.get()) return@OnClickListener` before every API call.
- Transaction Status has no search button any more — status filter tabs (All / Success / Failed / Pending) reload page 1.

#### List Loading Pattern — Shimmer vs Footer

| Situation | What to show |
|---|---|
| Page 1 initial load (or filter / tab change) | Hide `rvTransactions`, show `shimmerLayout` + `startShimmer()` |
| Page 2+ pagination load | Keep `rvTransactions` visible, show `pbLoadMore` footer spinner |
| Load complete (any page) | Stop/hide shimmer or `pbLoadMore`; show `tvEmpty` if list is empty |

Paging: `BillPayTransactionsViewModel.canLoadMore()` (`meta.current_page < meta.last_page`) + `nextPage()`, triggered
when the last visible row is within 3 of the end. Screens that filter in the app (Status → Pending, Report) keep paging
while a page brings no visible rows. **Never call `showShimmer(true)` for pagination pages.**

---

### ApiService Declaration

- Bill Pay endpoints live in one block: `// ── Bill Pay (all categories share these) ──` in `ApiService.kt`.
- Never add per-category endpoints. Never add an endpoint outside `ApiService.kt` / `ApiAdminService.kt`.

---

### `category` Parameter

`GET billpay/utility-billers` and `GET transactions` take `category = CategoryType.value`:
`electricity`, `gas`, `prepaid`, `mobile_postpaid`, `dth`, `fastag`, `loan_repayment`, `municipal_taxes`.
Any new category: check the API contract (`billpay/categories` lists the 16 backend values) — never guess.

---

### SMS Receipt Entry Modes

`SmsReceiptActivity.start(context, category, clientReference?, fromPayment)`:

| Opened from | `client_reference` | Data |
|---|---|---|
| Result screen (`fromPayment = true`, title row hidden) | Yes | `GET billpay/status/{ref}` |
| Transaction Detail → View Receipt | Yes | `GET billpay/status/{ref}` |
| SMS Receipt tab on the form | No | `GET transactions?category=X&per_page=1` → first row's ref → `GET billpay/status/{ref}` |
