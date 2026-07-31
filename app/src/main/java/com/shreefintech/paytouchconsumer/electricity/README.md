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
| `TransactionDetailActivity` | *(none)* | Detail view for a single transaction — receives `TransactionItem` via intent JSON |
| `SmsReceiptActivity` | `SmsReceiptViewModel` | Receipt display, image download, share — used from payment completion and from transaction detail |

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
                    ├── POST /admin: getVpsBalance
                    │       ├── balance sufficient ──► processPayment()
                    │       └── insufficient / fail ─► checkWalletBalance()
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

`ElectricityViewModel.verifyAndPay()` always checks the VPS admin balance first. The wallet balance is only checked if the VPS check fails or returns insufficient funds. Both checks are transparent to the user — only `onError` or `onSuccess` surfaces.

- VPS check: `ApiAdminClient` → `GET /admin` balance endpoint (fire without internet guard — fallback handles failure)
- Wallet check: `ApiClient` → `GET /api/wallet/user-data` (guarded with `isInternetAvailable`)

---

## Transaction ID

Generated client-side via `Utility.generateTransactionId()` immediately before `processElectricityPayment` is called. Format: `PYTCH[DDMMyyyyHHmmss]M`. Sent as `transaction_id` in the request body.

---

## Transaction History Screens

Three separate entry points, all launched from the tab bar at the top of `ElectricityActivity`:

| Tab | Screen | Data source |
|---|---|---|
| Recent | `RecentTransactionActivity` | `GET /api/transactions?type=electricity` (paginated 20/page) |
| Report | `TransactionReportActivity` | `POST /api/electricity/payment-report` (filtered) |
| Status | `ElectricityTransactionStatusActivity` | `POST /api/electricity/transaction-status` (by transaction ID) |

`RecentTransactionActivity` loads operators first (to resolve operator IDs to names), then fetches transactions. Operator fetch failure is non-fatal — transactions still load with raw operator IDs.

Pagination in `RecentTransactionActivity` is scroll-triggered: fires `loadNextPage()` when the last visible item is within 3 of the end. `RecentTransactionViewModel` tracks `currentPage`, `hasMore`, and `loading` to prevent duplicate calls.

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
| From payment | `true` | Hidden | Server confirmation via `getLatestPayments()` |
| From transaction detail | `false` | Visible (tab bar) | `GET /api/electricity/latest-payment` |

The receipt card (`cvReceiptCard`) is captured as a `Bitmap` for download and share. Download uses `MediaStore` on API 29+, `FileProvider` on older. Share always uses `FileProvider` via cache dir.

---

## Intent Data Passing

`SmsReceiptActivity` is started with only `context` and `fromPayment`:

```kotlin
SmsReceiptActivity.start(context, fromPayment = true)
```

`TransactionDetailActivity` uses a generic `TransactionItem`. It serializes to JSON via `Gson().toJson()` in the companion `start()` function and deserializes lazily in the receiving activity.

---

## Shared Adapters

| Adapter | Screen(s) | Item model |
|---|---|---|
| `RecentTransactionAdp` | `RecentTransactionActivity` | `RecentTransactionItem` |
| `TransactionAdp` | `TransactionReportActivity`, `ElectricityTransactionStatusActivity` | `TransactionItem` |

Status color in both adapters uses `.lowercase()` comparison — API may return `"success"`, `"Success"`, or `"SUCCESS"`.

---

## API Models

**Feature-local display models (`electricity/model/`):**

| Class | Used by |
|---|---|
| `RecentTransactionItem` | `RecentTransactionAdp` |
| `TransactionItem` | `TransactionAdp`, `TransactionDetailActivity` |

**Retrofit request/response models (`retrofit/model/electricity/`):**

| Class | Endpoint |
|---|---|
| `ElectricityOperatorItem` | GET /api/electricity/operators |
| `ElectricityFetchBillRequest` / `ElectricityBillItem` | POST /api/electricity/fetch-bill |
| `ElectricityProcessPaymentRequest` / `ElectricityPaymentItem` | POST /api/electricity/process-payment |
| `ElectricityTransactionReportRequest` / `ElectricityTransactionReportDataItem` | POST /api/electricity/payment-report |
| `ElectricityTransactionStatusRequest` | POST /api/electricity/transaction-status |
| `ElectricityVerifyPaymentRequest` / `ElectricityVerifyPaymentDataItem` | POST /api/electricity/verify-payment, GET /api/electricity/latest-payment |
| `UnifiedTransactionItem` + `UnifiedTransactionExtraItem` | GET /api/transactions |

`ElectricityPaymentItem` is a flat (unwrapped) response — success check is `response.isSuccessful && body?.success == true`.
All other electricity endpoints use the `General<T>` wrapper — success check is `response.isSuccessful && response.body()?.data != null`.

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
