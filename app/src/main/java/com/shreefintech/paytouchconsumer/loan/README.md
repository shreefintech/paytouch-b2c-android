# Loan Repayment Module

Handles the full loan repayment flow: operator selection, account number entry, bill fetch, payment processing, and all transaction history screens.

**Loan mirrors Gas / Electricity** (bill-fetch-before-pay pattern). The key differences from Gas are the `api/loanrepayment/*` endpoints, the category icon (`R.drawable.ic_loan`), and the absence of a `ccf` field (the receipt uses `platformFee` directly). Read the Gas README for patterns that are identical across both modules.

---

## Screens & ViewModels

| Activity | ViewModel | Purpose |
|---|---|---|
| `LoanActivity` | `LoanViewModel` | Operator dropdown, account number entry, bill fetch, proceed to pay |
| `LoanRecentTransactionActivity` | `LoanRecentTransactionViewModel` | Paginated loan transaction history |
| `LoanTransactionReportActivity` | `LoanTransactionReportViewModel` | Filtered report with date range / status / consumer number filter sheet |
| `LoanTransactionStatusActivity` | `LoanTransactionStatusViewModel` | Search transactions by transaction ID |
| `TransactionDetailActivity` | *(none)* | **Shared** — same activity used by every module, in `transactions/`, not duplicated here |
| `LoanSmsReceiptActivity` | `LoanSmsReceiptViewModel` | Receipt display, image download, share — used from payment completion and from recent transactions |

All Activities live in `loan/` (root screen) and `loan/transactions/` (history screens). All ViewModels live in `loan/viewmodel/`.

---

## Pay Bill Flow

```
LoanActivity
    │
    ├── onCreate ──────────────────────────────────► GET /api/loanrepayment/operators
    │                                                 └── populates operator dropdown
    │
    ├── llFetchBill (account number entered)
    │       └── POST /api/loanrepayment/fetch-bill
    │               └── onSuccess ──────────────────► shows bill details card (cvBillDetails)
    │
    └── llProceed (bill fetched + terms checked)
            └── verifyAndPay()
                    │
                    ├── BaseBillViewModel.checkVpsBalance() (ApiAdminClient)
                    │       ├── balance sufficient ──► processPayment()
                    │       └── insufficient / fail ─► BaseBillViewModel.checkWalletBalance()
                    │
                    ├── GET /api/wallet/user-data
                    │       ├── balance sufficient ──► processPayment()
                    │       └── insufficient ────────► onError (msgInsufficientBalance)
                    │
                    └── POST /api/loanrepayment/process-payment
                            └── onSuccess ──────────► LoanSmsReceiptActivity (fromPayment=true)
                                                       onReset()
```

Validation order in `onProceedToPay()`:
1. Consumer/account number not empty
2. Consumer/account number ≥ 10 digits
3. Operator selected (`selectedOperatorId` not null/empty)
4. Bill fetched (`isBillFetched = true`) — if not, silently calls `fetchBill()` first
5. Terms checkbox checked
6. Amount > 0

`circleId` is hardcoded to `Constant.LOAN_CIRCLE_ID = "0"` for every Loan request — same value as Gas, different from Electricity (`"00"`). This is a server contract difference; never change it to match Electricity.

---

## Balance Check Strategy — Shared with All Bill-Payment Modules

`LoanViewModel` extends **`BaseBillViewModel`**. `checkVpsBalance()`, `checkWalletBalance()`, and `bearerToken()` are inherited — never duplicated here.

`verifyAndPay()` always checks VPS balance first. Wallet balance is only checked if VPS check fails or returns insufficient funds. Both checks are transparent to the user.

---

## Transaction History Screens

Three separate entry points, launched from the tab bar at the top of `LoanActivity`:

| Tab | Screen | Data source |
|---|---|---|
| Recent | `LoanRecentTransactionActivity` | `GET /api/transactions?type=loan_repayment` (paginated, 20/page) |
| Report | `LoanTransactionReportActivity` | `POST /api/loanrepayment/payment-report` (filtered) |
| Status | `LoanTransactionStatusActivity` | `POST /api/loanrepayment/transaction-status` (by transaction ID) |

`LoanRecentTransactionActivity` / `LoanRecentTransactionViewModel.loadOperatorsThenData()` loads operators first (to resolve operator IDs to names via an in-memory `operatorMap`), then fetches transactions via the shared `GET /api/transactions` endpoint with `type = "loan_repayment"`. Operator fetch failure is non-fatal — `fetchTransactions()` still runs and transactions display with the raw operator ID as the fallback name.

Pagination in `LoanRecentTransactionActivity` is scroll-triggered: fires `loadNextPage()` when the last visible item is within 3 of the end. `LoanRecentTransactionViewModel` tracks `currentPage`, `hasMore`, and `isLoading` to prevent duplicate calls — same pattern as Gas.

---

## Operator Operators Data Shape

The `/api/loanrepayment/operators` response is wrapped in `General<LoanOperatorsDataItem>`. The `operators` field is a `Map<String, String>` (id → name), not a list:

```kotlin
// LoanOperatorsDataItem
data class LoanOperatorsDataItem(
    @field:SerializedName("operators") val operators: Map<String, String>?
)

// LoanViewModel.loadOperators() converts map to LoanOperatorItem list for the dropdown
val operators = operatorMap.map { (id, name) -> LoanOperatorItem(id = id, name = name) }
```

`LoanRecentTransactionViewModel` stores the map directly as `operatorMap: HashMap<String, String>` for O(1) name lookup during transaction list mapping.

---

## Filter Sheet (LoanTransactionReportActivity)

Reuses `TransactionFilterHelper` (in `utill/`) exactly as Gas does — no Loan-specific filter logic:

```kotlin
filterHelper = TransactionFilterHelper(
    activity     = mActivity,
    sheetBinding = binding.incFilterSheet,
    bgOverlay    = binding.viewBg,
    onApply      = { fromDate, toDate, status, consumerNo -> callReport(...) },
    onClear      = { ... callReport(null, null, null, null) }
)
filterHelper.setup()
```

Back press is handled by `onBack()` — closes the filter sheet before finishing the activity.

`LoanTransactionReportActivity` layers a local text-search filter (`etSearch`) on top of the server-side filter: `mAllList` holds every fetched page; `mDisplayList` is the on-screen filtered subset re-derived by `filterList(query)` whenever the search box changes or a new page arrives.

---

## SMS Receipt

`LoanSmsReceiptActivity` has two entry modes controlled by `EXTRA_FROM_PAYMENT`:

| Mode | `fromPayment` | Title row | Data source |
|---|---|---|---|
| From payment | `true` | Hidden | `GET /api/loanrepayment/latest-payment` via `getLatestPayment()`, called unconditionally in `onCreate()` |
| From recent transactions | `false` | Visible (tab bar) | Same `getLatestPayment()` call — no per-transaction receipt endpoint for Loan |

The receipt card (`cvReceiptCard`) is captured via the shared `ReceiptHelper` (in `utill/`) for download and share — no Loan-specific receipt rendering code. Download uses `MediaStore` on API 29+, `FileProvider` on older (behind a runtime storage-permission check on API < 29). Share always uses `FileProvider` via cache dir.

`LoanLatestPaymentDataItem` has no `ccf` field. The `platformFee` field is used directly for the CCF row — no fallback needed. This differs from Gas which falls back: `item.ccf ?: item.platformFee`.

The consumer label is always `R.string.labelConsumerNo` — set dynamically in `populateReceiptFromApi()`, never rely on the XML default:

```kotlin
binding.tvConsumerNoLabel.text = getString(R.string.labelConsumerNo)
```

---

## Intent Data Passing

`LoanSmsReceiptActivity` is started with only `context` and `fromPayment`:

```kotlin
LoanSmsReceiptActivity.start(context, fromPayment = true)   // after payment
LoanSmsReceiptActivity.start(context)                       // from recent transactions tab
```

`LoanTransactionReportActivity` / `LoanTransactionStatusActivity` navigate to the shared `TransactionDetailActivity`:

```kotlin
TransactionDetailActivity.start(mActivity, item)
```

---

## Shared Models — Loan Does Not Define Its Own UI Display Models

Display models live in the shared `transactions/model/` package, not in `loan/`:

| Class | Declared in | Used by |
|---|---|---|
| `RecentTransactionItem` | `transactions/model/RecentTransactionItem.kt` | `RecentTransactionAdp`, `LoanRecentTransactionViewModel` |
| `TransactionItem` | `transactions/model/TransactionItem.kt` | `TransactionAdp`, `LoanTransactionReportViewModel`, `LoanTransactionStatusViewModel`, `TransactionDetailActivity` |

**Do not create a Loan-local copy of either class.** If a field is missing, add it to the shared model (nullable with a sensible default at the call site) rather than forking it.

Shared adapters:

| Adapter | Screen(s) | Item model |
|---|---|---|
| `RecentTransactionAdp` | `LoanRecentTransactionActivity` | `RecentTransactionItem` |
| `TransactionAdp` | `LoanTransactionReportActivity`, `LoanTransactionStatusActivity` | `TransactionItem` |

All `mapToTransactionItem()` and `mapToDisplayItem()` calls set `isMobileCategory = false` — Loan uses consumer number, not mobile number.

---

## API Models (`retrofit/model/loan/`)

Loan-specific Retrofit request/response DTOs:

| Class | Endpoint | Wrapper |
|---|---|---|
| `LoanOperatorsDataItem` | `GET /api/loanrepayment/operators` | `General<LoanOperatorsDataItem>` |
| `LoanFetchBillRequest` / `LoanBillItem` | `POST /api/loanrepayment/fetch-bill` | `General<List<LoanBillItem>>` — `.firstOrNull()` for the single result |
| `LoanProcessPaymentRequest` / `LoanPaymentItem` | `POST /api/loanrepayment/process-payment` | Flat — success check: `response.isSuccessful && body?.success == true` |
| `LoanTransactionReportRequest` / `LoanTransactionReportDataItem` | `POST /api/loanrepayment/payment-report` | `General<List<LoanTransactionReportDataItem>>` |
| `LoanTransactionStatusRequest` | `POST /api/loanrepayment/transaction-status` | `General<List<LoanTransactionReportDataItem>>` |
| `LoanLatestPaymentDataItem` | `GET /api/loanrepayment/latest-payment` | `General<LoanLatestPaymentDataItem>` |

`LoanPaymentItem` is the only flat (unwrapped) response. All other Loan endpoints use `General<T>` — success check is `response.isSuccessful && response.body()?.data != null`.

All Loan endpoints are declared in `ApiService.kt` under the `// ── Loan ──` section — never add a Loan endpoint anywhere else.

---

## Operator Loading State

Identical to Gas and Electricity — spinner inside the dropdown field slot, anchor disabled while loading:

```kotlin
private fun setOperatorLoading(loading: Boolean) {
    binding.pbCompanyLoading.visibility = if (loading) View.VISIBLE else View.GONE
    binding.ivCompanyArrow.visibility   = if (loading) View.GONE else View.VISIBLE
    binding.flCompanyAnchor.isClickable = !loading
    binding.flCompanyAnchor.isFocusable = !loading
}
```

If the user taps the dropdown while it is empty (load failed), `loadOperators()` is retried and a warning toast (`msgLoadingOperators`) is shown.
