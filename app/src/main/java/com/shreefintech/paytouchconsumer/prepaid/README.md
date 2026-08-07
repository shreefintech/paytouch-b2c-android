# Prepaid (Mobile Recharge) Module

Handles mobile prepaid recharge: operator selection, circle selection, plan browsing, amount entry, payment processing, and all transaction history screens.

**Prepaid follows the same overall structure as Gas and Electricity** but has meaningful differences — a plan selection screen, no server-fetched bill step, and a circle (telecom region) picker. Read the Gas README for the identical parts; read this file for what's Prepaid-specific.

---

## Screens & ViewModels

| Activity | ViewModel | Purpose |
|---|---|---|
| `PrepaidActivity` | `PrepaidViewModel` | Operator dropdown, circle picker, mobile number entry, amount / plan, proceed to pay |
| `PrepaidPlanSelectionActivity` | `PrepaidPlanSelectionViewModel` | Browse and select a recharge plan for a chosen operator + circle |
| `PrepaidRecentTransactionActivity` | `PrepaidRecentTransactionViewModel` | Paginated prepaid transaction history |
| `PrepaidTransactionReportActivity` | `PrepaidTransactionReportViewModel` | Filtered report with date range / status / mobile number filter sheet |
| `PrepaidTransactionStatusActivity` | `PrepaidTransactionStatusViewModel` | Search transactions by mobile number |
| `TransactionDetailActivity` | *(none)* | **Shared** — same activity used by every module, in `transactions/`, not duplicated here |
| `PrepaidSmsReceiptActivity` | `PrepaidSmsReceiptViewModel` | Receipt display, image download, share — used from payment completion and from recent transactions |

All Activities live in `prepaid/` (root screens) and `prepaid/transactions/` (history screens). All ViewModels live in `prepaid/viewmodel/`.

---

## Pay Flow — Two Entry Paths

Prepaid has no bill-fetch step. The user either selects a plan (which fills the amount) or types an amount directly.

```
PrepaidActivity
    │
    ├── onCreate ──────────────────────────────────► GET /api/recharge/operators
    │                                                 └── populates operator dropdown
    │
    ├── [Browse Plans] button
    │       └── PrepaidPlanSelectionActivity  (operator + circle required)
    │               └── GET /api/recharge/plans/{operatorId}/{circleId}
    │                       └── user selects plan ──► back to PrepaidActivity
    │                               └── amount auto-filled from plan
    │
    └── llProceed (mobile + operator + circle + amount all valid)
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
                    └── POST /api/recharge/process-direct
                            └── onSuccess ──────────► PrepaidSmsReceiptActivity (fromPayment=true)
```

---

## Circle Selection — Local, No API Call

The telecom circle list is a hardcoded `STATE_LIST` in `PrepaidActivity.Companion` (24 entries, e.g. `"05" to "Delhi & NCR"`). There is no API call for circles — the user picks from this static list. `circleId` is the numeric string key (e.g. `"05"`); `circleCode` passed to the API is the same value.

---

## Plan Selection (PrepaidPlanSelectionActivity)

Launched via `PrepaidPlanSelectionActivity.start(activity, launcher, operatorId, circleId)` using an `ActivityResultLauncher`. On plan selection, the activity calls `setResult(RESULT_OK, intent)` with the selected `PrepaidPlanItem` serialized as JSON under `EXTRA_SELECTED_PLAN`. `PrepaidActivity` reads it in `planSelectionLauncher` and calls `onPlanSelected(plan)` to auto-fill the amount field.

`PrepaidPlanSelectionViewModel` extends `BaseBillViewModel`. The plans response is a flat `PrepaidPlansListItem` (not `General<T>`) — success check is `response.isSuccessful && body?.success == true`.

---

## Balance Check Strategy — Inherited from BaseBillViewModel

`PrepaidViewModel` extends **`BaseBillViewModel`** exactly like `GasViewModel` and `ElectricityViewModel`. `checkVpsBalance()`, `checkWalletBalance()`, and `bearerToken()` are not duplicated — they're inherited. The payment flow checks VPS balance first, falls back to wallet balance on failure.

---

## Transaction History Screens

Three separate entry points, launched from the tab bar at the top of `PrepaidActivity`:

| Tab | Screen | Data source |
|---|---|---|
| Recent | `PrepaidRecentTransactionActivity` | `GET /api/transactions?type=mobile_recharge` (paginated 20/page) |
| Report | `PrepaidTransactionReportActivity` | `POST /api/utility/payment-report` (filtered) |
| Status | `PrepaidTransactionStatusActivity` | `POST /api/mobile-recharge/transaction-status` (by **mobile number**, not transaction ID) |

> **Important:** The `type` query parameter for the unified transactions endpoint is `"mobile_recharge"`, not `"prepaid"`. Do not change it.

> **Important:** `PrepaidTransactionStatusActivity` searches by mobile number (`mobileNo`), unlike Electricity/Gas which search by transaction ID.

`PrepaidRecentTransactionActivity` loads operators first (to resolve operator codes/IDs to names via an in-memory `operatorMap` keyed by both `code` and `id`), then fetches transactions. Operator fetch failure is non-fatal.

Pagination in `PrepaidRecentTransactionActivity` is scroll-triggered: fires `loadNextPage()` when the last visible item is within 3 of the end. `PrepaidRecentTransactionViewModel` tracks `currentPage`, `hasMore`, and `isLoading` to prevent duplicate calls.

`PrepaidTransactionReportActivity` and `PrepaidTransactionStatusActivity` track pagination via `currentPage` / `isLastPage` / `isLoading` on their respective ViewModels, exposed through `canLoadMore()` / `nextPage()`.

---

## Filter Sheet (PrepaidTransactionReportActivity)

Reuses `TransactionFilterHelper` (in `utill/`) — no prepaid-specific filter logic:

```kotlin
filterHelper = TransactionFilterHelper(
    activity     = mActivity,
    sheetBinding = binding.incFilterSheet,
    bgOverlay    = binding.viewBg,
    onApply      = { fromDate, toDate, status, mobileNo -> callReport(...) },
    onClear      = { ... callReport(null, null, null, null) }
)
filterHelper.setup()
```

Back press closes the sheet before finishing the activity.

---

## SMS Receipt

`PrepaidSmsReceiptActivity` has two entry modes controlled by `EXTRA_FROM_PAYMENT`, identical to Gas and Electricity:

| Mode | `fromPayment` | Title row | Data source |
|---|---|---|---|
| From payment | `true` | Hidden | `GET /api/recharge/latest-payment` via `getLatestPayments()`, called unconditionally in `onCreate()` |
| From recent transactions | `false` | Visible (tab bar) | Same `getLatestPayments()` call |

`PrepaidSmsReceiptViewModel` is a standalone `AndroidViewModel` (not a `BaseBillViewModel` subclass) — it only needs the latest-payment fetch, not balance checks. It uses the `bearerToken()` and `getString()` extension functions from `ViewModelExt.kt`.

The receipt card is captured via the shared `ReceiptHelper` for download and share — no prepaid-specific rendering. Loading state uses `ObservableBoolean showProgressReceipt` wired via DataBinding, matching the Gas and Electricity receipt screens.

`platformFee` is used directly for the CCF row (Gas has `ccf ?: platformFee`; Prepaid only exposes `platformFee`).

---

## Intent Data Passing

`PrepaidSmsReceiptActivity` is started with only `context` and `fromPayment`:

```kotlin
PrepaidSmsReceiptActivity.start(context, fromPayment = true)
```

`PrepaidTransactionReportActivity` / `PrepaidTransactionStatusActivity` navigate to the **shared** `TransactionDetailActivity`:

```kotlin
TransactionDetailActivity.start(mActivity, item)
```

---

## Shared Models — Prepaid Does Not Define Its Own UI Display Models

Display models live in the shared `transactions/model/` package:

| Class | Declared in | Used by |
|---|---|---|
| `RecentTransactionItem` | `transactions/model/RecentTransactionItem.kt` | `RecentTransactionAdp`, `PrepaidRecentTransactionViewModel` |
| `TransactionItem` | `transactions/model/TransactionItem.kt` | `TransactionAdp`, `PrepaidTransactionReportViewModel`, `PrepaidTransactionStatusViewModel`, `TransactionDetailActivity` |

**Do not create a Prepaid-local copy of either class.**

Shared adapters (not duplicated):

| Adapter | Screen(s) | Item model |
|---|---|---|
| `RecentTransactionAdp` | `PrepaidRecentTransactionActivity` | `RecentTransactionItem` |
| `TransactionAdp` | `PrepaidTransactionReportActivity`, `PrepaidTransactionStatusActivity` | `TransactionItem` |

`PrepaidPlanAdp` (`adapter/PrepaidPlanAdp.kt`) is Prepaid-specific — it serves only `PrepaidPlanSelectionActivity` and is not shared.

---

## API Models (`retrofit/model/prepaid/`)

Prepaid-specific Retrofit request/response DTOs:

| Class | Endpoint |
|---|---|
| `PrepaidOperatorItem` | GET /api/recharge/operators |
| `PrepaidPlansListItem` / `PrepaidPlanItem` | GET /api/recharge/plans/{operatorId}/{circleId} |
| `PrepaidProcessDirectRequest` / `PrepaidPaymentItem` | POST /api/recharge/process-direct |
| `PrepaidTransactionStatusRequest` / `PrepaidTransactionDataItem` | POST /api/mobile-recharge/transaction-status |
| `PrepaidTransactionReportRequest` / `PrepaidTransactionDataItem` | POST /api/utility/payment-report |
| `PrepaidVerifyPaymentDataItem` | GET /api/recharge/latest-payment |

`PrepaidPaymentItem` and `PrepaidPlansListItem` are flat (unwrapped) responses — success check is `response.isSuccessful && body?.success == true`.
`PrepaidTransactionDataItem` is **reused** for both Status and Report responses.
All other Prepaid endpoints use the `General<T>` wrapper — success check is `response.isSuccessful && response.body()?.data != null`.

All endpoints are declared in `ApiService.kt` under the `// ── Mobile Prepaid ──` section.

---

## Key Differences vs Gas / Electricity

| Aspect | Gas / Electricity | Prepaid |
|---|---|---|
| Bill fetch step | Required (server-fetched amount) | None — user enters amount or selects a plan |
| Circle / region | Hardcoded `"0"` or `"00"` per request | User picks from local `STATE_LIST` in `PrepaidActivity` |
| Plan selection | N/A | `PrepaidPlanSelectionActivity` (separate screen) |
| Transactions type param | `"electricity"` / `"gas"` | `"mobile_recharge"` |
| Status search field | Transaction ID | Mobile number |
| SMS ViewModel base | `BaseBillViewModel` subclass | Standalone `AndroidViewModel` |
| CCF field | `ccf ?: platformFee` | `platformFee` only |
