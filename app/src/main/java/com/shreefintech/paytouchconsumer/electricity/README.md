# Electricity Module

Handles the full electricity bill payment flow: operator selection, bill fetch, payment processing, and all transaction history screens.

---

## Screens & ViewModels

| Activity | ViewModel | Purpose |
|---|---|---|
| `ElectricityActivity` | `ElectricityViewModel` | Operator dropdown, consumer number entry, bill fetch, proceed to pay |
| `RecentTransactionActivity` | `RecentTransactionViewModel` | Paginated electricity transaction history |
| `TransactionReportActivity` | `TransactionReportViewModel` | Filtered report with date range / status / consumer number filter sheet |
| `ElectricityTransactionStatusActivity` | `ElectricityTransactionStatusViewModel` | Search transactions by transaction ID |
| `TransactionDetailActivity` | *(none)* | **Shared** — same activity used by every module, in `transactions/`, not duplicated here |
| `SmsReceiptActivity` | `SmsReceiptViewModel` | Receipt display, image download, share — used from payment completion and from recent transactions |

All Activities live in `electricity/` (root screen) and `electricity/transactions/` (history screens). All ViewModels live in `electricity/viewmodel/`.

---

## Pay Bill Flow

```
ElectricityActivity
    │
    ├── onCreate ──────────────────────────────────► GET /api/electricity/operators
    │                                                 └── populates operator dropdown
    │
    ├── llFetchBill (consumer number entered)
    │       └── POST /api/electricity/fetch-bill
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
                    └── POST /api/electricity/process-payment
                            └── onSuccess ──────────► SmsReceiptActivity (fromPayment=true)
```

---

## Balance Check Strategy

`ElectricityViewModel` extends **`BaseBillViewModel`** (`app/src/main/java/.../BaseBillViewModel.kt`). `checkVpsBalance()`, `checkWalletBalance()`, and `bearerToken()` live once in `BaseBillViewModel` and are inherited — never duplicated per module.

`ElectricityViewModel.verifyAndPay()` always checks the VPS admin balance first. The wallet balance is only checked if the VPS check fails or returns insufficient funds. Both checks are transparent to the user — only `onError` or `onSuccess` surfaces.

> If you're adding a new bill-payment module, extend `BaseBillViewModel` rather than re-implementing balance checks.

---

## Transaction History Screens

Three separate entry points, launched from the tab bar at the top of `ElectricityActivity`:

| Tab | Screen | Data source |
|---|---|---|
| Recent | `RecentTransactionActivity` | `GET /api/transactions?type=electricity` (paginated 20/page) |
| Report | `TransactionReportActivity` | `POST /api/electricity/payment-report` (filtered) |
| Status | `ElectricityTransactionStatusActivity` | `POST /api/electricity/transaction-status` (by transaction ID) |

`RecentTransactionActivity` loads operators first (to resolve operator IDs to names via an in-memory `operatorMap`), then fetches transactions. Operator fetch failure is non-fatal — transactions still load with raw operator IDs as the fallback name.

Pagination in `RecentTransactionActivity` is scroll-triggered: fires `loadNextPage()` when the last visible item is within 3 of the end. `RecentTransactionViewModel` tracks `currentPage`, `hasMore`, and `isLoading` to prevent duplicate calls.

`TransactionReportActivity` and `ElectricityTransactionStatusActivity` track pagination via `currentPage` / `isLastPage` / `isLoading` on their respective ViewModels, exposed through `canLoadMore()` / `nextPage()`.

---

## Filter Sheet (TransactionReportActivity)

`TransactionFilterHelper` (in `utill/`) owns all filter state and sheet behavior. Set it up once in `setupFilterSheet()`:

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

Back press is handled by `onBack()` — closes the sheet before finishing the activity.

---

## SMS Receipt

`SmsReceiptActivity` has two entry modes controlled by `EXTRA_FROM_PAYMENT`:

| Mode | `fromPayment` | Title row | Data source |
|---|---|---|---|
| From payment | `true` | Hidden | `GET /api/electricity/latest-payment` via `getLatestPayments()`, called unconditionally in `onCreate()` |
| From recent transactions | `false` | Visible (tab bar) | Same `getLatestPayments()` call |

The receipt card (`cvReceiptCard`) is captured via the shared `ReceiptHelper` (in `utill/`) for download and share. Download uses `MediaStore` on API 29+, `FileProvider` on older (behind a runtime storage-permission check on API < 29). Share always uses `FileProvider` via cache dir.

`ccf` (customer convenience fee) falls back to `platformFee` if the server omits it: `item.ccf ?: item.platformFee ?: "--"`.

---

## Intent Data Passing

`SmsReceiptActivity` is started with only `context` and `fromPayment`:

```kotlin
SmsReceiptActivity.start(context, fromPayment = true)
```

`TransactionReportActivity` / `ElectricityTransactionStatusActivity` navigate to the **shared** `TransactionDetailActivity` using the generic `TransactionItem`, serialized to JSON:

```kotlin
TransactionDetailActivity.start(mActivity, item)
```

---

## Shared Models — Electricity Does Not Define Its Own UI Display Models

Display models live in the shared `transactions/model/` package, not in `electricity/`:

| Class | Declared in | Used by |
|---|---|---|
| `RecentTransactionItem` | `transactions/model/RecentTransactionItem.kt` | `RecentTransactionAdp`, `RecentTransactionViewModel` |
| `TransactionItem` | `transactions/model/TransactionItem.kt` | `TransactionAdp`, `ElectricityTransactionStatusViewModel`, `TransactionReportViewModel`, `TransactionDetailActivity` |

**Do not create an electricity-local copy of either class.** If a field is missing, add it to the existing shared model (nullable, with a sensible default at the call site) rather than forking it.

Shared adapters (also not duplicated):

| Adapter | Screen(s) | Item model |
|---|---|---|
| `RecentTransactionAdp` | `RecentTransactionActivity` | `RecentTransactionItem` |
| `TransactionAdp` | `TransactionReportActivity`, `ElectricityTransactionStatusActivity` | `TransactionItem` |

Status color in both adapters uses `.lowercase()` comparison — the API may return `"success"`, `"Success"`, or `"SUCCESS"`.

---

## API Models (`retrofit/model/electricity/`)

Electricity-specific Retrofit request/response DTOs:

| Class | Endpoint |
|---|---|
| `ElectricityOperatorItem` | GET /api/electricity/operators |
| `ElectricityFetchBillRequest` / `ElectricityBillItem` | POST /api/electricity/fetch-bill |
| `ElectricityProcessPaymentRequest` / `ElectricityPaymentItem` | POST /api/electricity/process-payment |
| `ElectricityTransactionReportRequest` / `ElectricityTransactionReportDataItem` | POST /api/electricity/payment-report |
| `ElectricityTransactionStatusRequest` | POST /api/electricity/transaction-status |
| `ElectricityVerifyPaymentRequest` / `ElectricityVerifyPaymentDataItem` | POST /api/electricity/verify-payment, GET /api/electricity/latest-payment |
| `UnifiedTransactionItem` + `UnifiedTransactionExtraItem` | GET /api/transactions (shared by all modules) |

`ElectricityPaymentItem` is a flat (unwrapped) response — success check is `response.isSuccessful && body?.success == true`.
All other electricity endpoints use the `General<T>` wrapper — success check is `response.isSuccessful && response.body()?.data != null`.

All endpoints are declared in `ApiService.kt` under the `// ── Electricity ──` section — never add an electricity endpoint anywhere else.

---

## Operator Loading State

When operators are loading, `ElectricityActivity` shows a spinner inside the dropdown field slot and disables the anchor:

```kotlin
private fun setOperatorLoading(loading: Boolean) {
    binding.pbCompanyLoading.visibility = if (loading) View.VISIBLE else View.GONE
    binding.ivCompanyArrow.visibility   = if (loading) View.GONE else View.VISIBLE
    binding.flCompanyAnchor.isClickable = !loading
    binding.flCompanyAnchor.isFocusable = !loading
}
```

If the user taps the dropdown while it is empty (load failed), `loadOperators()` is called again and a warning toast is shown.

---

## What to Copy When Adding the Next Module (e.g. Water, Broadband)

Electricity is the canonical reference for bill-payment modules with a bill-fetch step. For modules with plan selection (no bill-fetch), use Prepaid or Postpaid as the template instead. In order of what you'll touch:

1. `{Category}Activity` + `activity_{category}.xml` — copy `ElectricityActivity` / `activity_electricity.xml`, rename fields/strings only.
2. `{Category}ViewModel` extending `BaseBillViewModel` — copy `ElectricityViewModel`, swap endpoint names and request/response DTOs.
3. `retrofit/model/{category}/` — new DTOs matching the new endpoint's actual field names (check the API contract first, never guess).
4. Add the new endpoints to `ApiService.kt` under a new `// ── {Category} ──` section.
5. `{Category}RecentTransactionActivity` + `ViewModel`, `{Category}TransactionReportActivity` + `ViewModel`, `{Category}TransactionStatusActivity` + `ViewModel`, `{Category}SmsReceiptActivity` + `ViewModel` — copy the Electricity transaction screens verbatim, reusing the shared `RecentTransactionItem`, `TransactionItem`, `RecentTransactionAdp`, `TransactionAdp`, `TransactionDetailActivity`, `TransactionFilterHelper`, and `ReceiptHelper`. **Never fork these.**
6. Pass `R.drawable.ic_{category}` as `categoryIconRes` in the new ViewModel's mapping functions — that's the only visual difference between modules' transaction rows.

This list matches the "Transaction Screens — Shared Structure Across All Modules" rule in the project's `CLAUDE.md`.
