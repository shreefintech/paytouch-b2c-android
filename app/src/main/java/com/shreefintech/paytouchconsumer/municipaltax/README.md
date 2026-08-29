# Municipal Tax Module

Handles municipal (property/local body) tax bill payment: operator selection, house number entry, bill fetch, payment processing, and all transaction history screens.

**Municipal Tax mirrors the Gas / Electricity fetch-bill pattern exactly** — the same operator dropdown, bill-fetch card, and two-button (Fetch Bill / Proceed) layout. Read the Gas README for the identical parts; read this file for what's Municipal Tax-specific.

---

## Screens & ViewModels

| Activity | ViewModel | Purpose |
|---|---|---|
| `MunicipalTaxActivity` | `MunicipalTaxViewModel` | Operator dropdown, house number entry, bill fetch, proceed to pay |
| `MunicipalTaxRecentTransactionActivity` | `MunicipalTaxRecentTransactionViewModel` | Paginated municipal tax transaction history |
| `MunicipalTaxTransactionReportActivity` | `MunicipalTaxTransactionReportViewModel` | Filtered report with date range / status / consumer number filter sheet |
| `MunicipalTaxTransactionStatusActivity` | `MunicipalTaxTransactionStatusViewModel` | Search transactions by transaction ID |
| `TransactionDetailActivity` | *(none)* | **Shared** — same activity used by every module, in `transactions/`, not duplicated here |
| `MunicipalTaxSmsReceiptActivity` | `MunicipalTaxSmsReceiptViewModel` | Receipt display, image download, share — used from payment completion and from recent transactions |

All Activities live in `municipaltax/` (root screen) and `municipaltax/transactions/` (history screens). All ViewModels live in `municipaltax/viewmodel/`.

---

## Pay Bill Flow

```
MunicipalTaxActivity
    │
    ├── onCreate ──────────────────────────────────► GET /api/municipal-taxes/operators
    │                                                 └── populates operator dropdown
    │                                                     operator object carries both id AND circleId
    │
    ├── llFetchBill (house number entered)
    │       └── POST /api/municipal-taxes/fetch-bill
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
                    └── POST /api/municipal-taxes/process-payment
                            └── onSuccess ──────────► MunicipalTaxSmsReceiptActivity (fromPayment=true)
```

`MunicipalTaxActivity` validates in this order: house number present (≥10 digits) → operator selected → bill fetched. If "Proceed" is tapped without a fetched bill, `fetchBill()` is called automatically instead of showing an error.

`circleId` for each operator comes from `MunicipalTaxOperatorItem.circleId` on the selected operator — it is NOT a static value or user-selected. Unlike Gas (hardcoded `"0"`), each Municipal Tax operator carries its own `circleId` in the response.

---

## Balance Check Strategy

`MunicipalTaxViewModel` extends **`BaseBillViewModel`**. The VPS admin balance is checked first; the wallet balance is the fallback. Both checks are transparent to the user — only `onError` or `onSuccess` surfaces.

---

## Transaction History Screens

Three separate entry points, launched from the tab bar at the top of `MunicipalTaxActivity`:

| Tab | Screen | Data source |
|---|---|---|
| Recent | `MunicipalTaxRecentTransactionActivity` | `GET /api/transactions?type=municipal_tax` (paginated 20/page) |
| Report | `MunicipalTaxTransactionReportActivity` | `POST /api/municipal-taxes/payment-report` (filtered) |
| Status | `MunicipalTaxTransactionStatusActivity` | `POST /api/mobile-recharge/transaction-status` (⚠️ see quirk below) |

---

## Confirmed Backend Quirks — Do NOT "Fix"

| Location | Apparent Anomaly | Confirmed Behaviour |
|---|---|---|
| `getMunicipalTaxTransactionStatus` in `ApiService.kt` | Uses `@POST("mobile-recharge/transaction-status")` instead of `municipal-taxes/...` | **Backend-side intentional routing.** The mobile-recharge transaction-status endpoint serves municipal tax queries too. Do not change this URL. |
| `MunicipalTaxLatestPaymentDataItem.subService` | `@field:SerializedName("subservice")` — no underscore | **Confirmed from API contract.** The backend sends `subservice`, not `sub_service`. Do not rename. |

---

## Filter Sheet (MunicipalTaxTransactionReportActivity)

Reuses `TransactionFilterHelper` (in `utill/`) — no municipal-tax-specific filter logic:

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

Back press closes the sheet before finishing the activity.

---

## SMS Receipt

`MunicipalTaxSmsReceiptActivity` has two entry modes controlled by `EXTRA_FROM_PAYMENT`:

| Mode | `fromPayment` | Title row | Data source |
|---|---|---|---|
| From payment | `true` | Hidden | `GET /api/municipal-taxes/latest-payment` via `getLatestPayments()`, called in `onCreate()` |
| From recent transactions | `false` | Visible (tab bar) | Same `getLatestPayments()` call |

`tvConsumerNoLabel` is set to `getString(R.string.labelConsumerNo)` in `populateReceiptFromApi()` — Municipal Tax is a consumer-number module (`isMobileCategory = false`).

The receipt card (`cvReceiptCard`) is captured via the shared `ReceiptHelper` for download and share.

---

## Intent Data Passing

`MunicipalTaxSmsReceiptActivity` is started with only `context` and `fromPayment`:

```kotlin
MunicipalTaxSmsReceiptActivity.start(context, fromPayment = true)
```

`MunicipalTaxTransactionReportActivity` / `MunicipalTaxTransactionStatusActivity` navigate to the **shared** `TransactionDetailActivity`:

```kotlin
TransactionDetailActivity.start(mActivity, item)
```

---

## Shared Models — Municipal Tax Does Not Define Its Own UI Display Models

Display models live in the shared `transactions/model/` package:

| Class | Declared in | Used by |
|---|---|---|
| `RecentTransactionItem` | `transactions/model/RecentTransactionItem.kt` | `RecentTransactionAdp`, `MunicipalTaxRecentTransactionViewModel` |
| `TransactionItem` | `transactions/model/TransactionItem.kt` | `TransactionAdp`, `MunicipalTaxTransactionReportViewModel`, `MunicipalTaxTransactionStatusViewModel`, `TransactionDetailActivity` |

Shared adapters (not duplicated):

| Adapter | Screen(s) | Item model |
|---|---|---|
| `RecentTransactionAdp` | `MunicipalTaxRecentTransactionActivity` | `RecentTransactionItem` |
| `TransactionAdp` | `MunicipalTaxTransactionReportActivity`, `MunicipalTaxTransactionStatusActivity` | `TransactionItem` |

---

## API Models (`retrofit/model/municipaltax/`)

| Class | Endpoint |
|---|---|
| `MunicipalTaxOperatorItem` | GET /api/municipal-taxes/operators |
| `MunicipalTaxFetchBillRequest` / `MunicipalTaxFetchBillDataItem` | POST /api/municipal-taxes/fetch-bill |
| `MunicipalTaxProcessPaymentRequest` / `MunicipalTaxPaymentItem` | POST /api/municipal-taxes/process-payment |
| `MunicipalTaxTransactionReportRequest` / `MunicipalTaxTransactionReportDataItem` | POST /api/municipal-taxes/payment-report |
| `MunicipalTaxTransactionStatusRequest` | POST /api/mobile-recharge/transaction-status (quirk — see above) |
| `MunicipalTaxLatestPaymentDataItem` | GET /api/municipal-taxes/latest-payment |
| `MunicipalTaxRecentPageItem` / `MunicipalTaxRecentDataItem` | GET /api/transactions?type=municipal_tax |

`MunicipalTaxPaymentItem` is a flat (unwrapped) response — success check is `response.isSuccessful && body?.success == true`.
All other Municipal Tax endpoints use the `General<T>` wrapper — success check is `response.isSuccessful && response.body()?.data != null`.

`fetchBill` returns `General<List<MunicipalTaxFetchBillDataItem>>` — the first element of the list is used: `response.body()?.data?.firstOrNull()`.

All endpoints are declared in `ApiService.kt` under the `// ── Municipal Tax ──` section.

---

## Key Differences vs Gas / Electricity

| Aspect | Gas | Electricity | Municipal Tax |
|---|---|---|---|
| Input field label | Consumer number | Consumer number | House number (`houseNumber`) |
| `circleId` source | Hardcoded `"0"` per request | Hardcoded `"00"` per request | From `MunicipalTaxOperatorItem.circleId` |
| Status endpoint | `api/gas/transaction-status` | `api/electricity/transaction-status` | `api/mobile-recharge/transaction-status` (backend quirk) |
| Bill response shape | `General<List<...>>` first item | `General<...>` single object | `General<List<...>>` first item |
| `isMobileCategory` | `false` | `false` | `false` |
