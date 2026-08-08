# Postpaid (Mobile Postpaid Bill Payment) Module

Handles mobile postpaid bill payment: operator selection, circle selection, plan browsing, amount entry, payment processing, and all transaction history screens.

**Postpaid follows the same overall structure as Prepaid** — it reuses `PrepaidPlanSelectionActivity` and `PrepaidPlanItem` directly. Read the Prepaid README for the identical parts; read this file for what's Postpaid-specific.

---

## Screens & ViewModels

| Activity | ViewModel | Purpose |
|---|---|---|
| `PostpaidActivity` | `PostpaidViewModel` | Operator dropdown, circle picker, mobile number entry, plan selection, amount entry, proceed to pay |
| `PostpaidRecentTransactionActivity` | `PostpaidRecentTransactionViewModel` | Paginated postpaid transaction history |
| `PostpaidTransactionReportActivity` | `PostpaidTransactionReportViewModel` | Filtered report with date range / status / connection number filter sheet |
| `PostpaidTransactionStatusActivity` | `PostpaidTransactionStatusViewModel` | Search transactions by transaction ID |
| `TransactionDetailActivity` | *(none)* | **Shared** — same activity used by every module, in `transactions/`, not duplicated here |
| `PostpaidSmsReceiptActivity` | `PostpaidSmsReceiptViewModel` | Receipt display (Receipt + Display tabs), image download, share |

**Shared screen (not duplicated):** `PrepaidPlanSelectionActivity` from the `prepaid/` module — Postpaid launches it with the same `ActivityResultLauncher` pattern.

All Activities live in `postpaid/` (root screen) and `postpaid/transactions/` (history screens). All ViewModels live in `postpaid/viewmodel/`.

---

## Pay Flow

Postpaid has no server bill-fetch step. The user must select a plan (or type an amount), but plan selection is mandatory — `onProceedToPay()` calls `onBrowsePlan()` if no plan is selected.

```
PostpaidActivity
    │
    ├── onCreate ──────────────────────────────────► GET /api/mobile-postpaid/operators
    │                                                 └── populates operator dropdown
    │
    ├── [Browse Plans] button
    │       └── PrepaidPlanSelectionActivity  (operator + circle required, shared from prepaid/)
    │               └── GET /api/recharge/plans/{operatorId}/{circleId}
    │                       └── user selects plan ──► back to PostpaidActivity
    │                               └── amount auto-filled from plan
    │
    └── llProceed (mobile + operator + circle + plan selected + terms checked + amount > 0)
            └── verifyBalanceAndProcessPayment()
                    │
                    ├── BaseBillViewModel.checkVpsBalance() (ApiAdminClient)
                    │       ├── balance sufficient ──► processPayment()
                    │       └── insufficient / fail ─► BaseBillViewModel.checkWalletBalance()
                    │
                    ├── GET /api/wallet/user-data
                    │       ├── balance sufficient ──► processPayment()
                    │       └── insufficient ────────► onError (msgInsufficientBalance)
                    │
                    └── POST /api/mobile-postpaid/process-payment
                            └── onSuccess ──────────► PostpaidSmsReceiptActivity (fromPayment=true)
```

`PostpaidActivity` validates in this order: mobile number present → mobile number 10 digits → operator selected → circle selected → plan selected (auto-launches plan picker) → terms accepted → amount > 0.

---

## Circle Selection — Shared Static List

The circle list is `Utility.STATE_LIST` (24 entries), the same static list used by Prepaid. There is no API call for circles — the user picks from this list. `circleId` is the numeric string key (e.g. `"05"`).

---

## Plan Selection — Shared from Prepaid

`PostpaidActivity` launches `PrepaidPlanSelectionActivity.start(activity, launcher, operatorId, circleId)` using an `ActivityResultLauncher`. The plan endpoint (`GET /api/recharge/plans/{operatorId}/{circleId}`) is shared — the same plans API used by Prepaid. On selection, `PostpaidActivity` receives the `PrepaidPlanItem` via `EXTRA_SELECTED_PLAN` and auto-fills the amount field.

> Do not create a `PostpaidPlanSelectionActivity` — the one in `prepaid/` is shared.

---

## Balance Check Strategy — Inherited from BaseBillViewModel

`PostpaidViewModel` extends **`BaseBillViewModel`** exactly like `PrepaidViewModel`, `GasViewModel`, and `ElectricityViewModel`. The payment flow checks VPS balance first, falls back to wallet balance on failure.

---

## Transaction History Screens

Three separate entry points, launched from the tab bar at the top of `PostpaidActivity`:

| Tab | Screen | Data source |
|---|---|---|
| Recent | `PostpaidRecentTransactionActivity` | `GET /api/transactions?type=mobile_postpaid` (paginated 20/page) |
| Report | `PostpaidTransactionReportActivity` | `POST /api/mobile-postpaid/payment-report` (filtered) |
| Status | `PostpaidTransactionStatusActivity` | `POST /api/mobile-postpaid/transaction-status` (by **transaction ID**) |

> **Important:** The `type` query parameter for the unified transactions endpoint is `"mobile_postpaid"`.

> **Important:** `PostpaidTransactionStatusActivity` searches by **transaction ID**, unlike Prepaid which searches by mobile number.

`PostpaidRecentTransactionActivity` loads operators first (to resolve operator IDs to names via an in-memory `operatorMap`), then fetches transactions via the shared `GET /api/transactions` endpoint. Operator fetch failure is non-fatal — transactions still load with raw operator IDs as the fallback.

Pagination in `PostpaidRecentTransactionActivity` is scroll-triggered: fires `loadNextPage()` when the last visible item is within 3 of the end. `PostpaidRecentTransactionViewModel` tracks `currentPage`, `hasMore`, and `isLoading` to prevent duplicate calls.

`PostpaidTransactionReportActivity` and `PostpaidTransactionStatusActivity` track pagination via `currentPage` / `isLastPage` / `isLoading` on their respective ViewModels, exposed through `canLoadMore()` / `nextPage()`.

---

## Filter Sheet (PostpaidTransactionReportActivity)

Reuses `TransactionFilterHelper` (in `utill/`) — no postpaid-specific filter logic. The filter's fourth field is `connectionNumber` (the mobile/subscriber number), matching the `PostpaidTransactionReportRequest` field name:

```kotlin
filterHelper = TransactionFilterHelper(
    activity     = mActivity,
    sheetBinding = binding.incFilterSheet,
    bgOverlay    = binding.viewBg,
    onApply      = { fromDate, toDate, status, connectionNumber -> callReport(...) },
    onClear      = { ... callReport(null, null, null, null) }
)
filterHelper.setup()
```

Back press closes the sheet before finishing the activity.

---

## SMS Receipt

`PostpaidSmsReceiptActivity` has two entry modes controlled by `EXTRA_FROM_PAYMENT`:

| Mode | `fromPayment` | Title row | Data source |
|---|---|---|---|
| From payment | `true` | Hidden | `GET /api/mobile-postpaid/latest-payment` via `getLatestPayments()`, called unconditionally in `onCreate()` |
| From recent transactions | `false` | Visible (two tabs: Receipt / Display) | Same `getLatestPayments()` call |

The receipt card displays two tabs when opened from recent transactions:
- **Receipt tab** — structured receipt card with all payment fields
- **Display tab** — SMS-style summary view with the amount and mobile number highlighted

`tvConsumerNoLabel` is always set to `getString(R.string.labelMobileNo)` in `populateReceiptFromApi()` — Postpaid is a mobile-number module (`isMobileCategory = true`).

Operator name display falls back through: `operatorName → subservice → "--"`.

`PostpaidSmsReceiptViewModel` is a standalone `AndroidViewModel` (not a `BaseBillViewModel` subclass) — it only needs the latest-payment fetch. It uses the `bearerToken()` and `getString()` extension functions from `ViewModelExt.kt`.

The receipt card (`cvReceiptCard`) is captured via the shared `ReceiptHelper` for download and share. Download uses `MediaStore` on API 29+, `FileProvider` on older (behind a runtime storage-permission check on API < 29). Share always uses `FileProvider` via cache dir.

---

## Intent Data Passing

`PostpaidSmsReceiptActivity` is started with only `context` and `fromPayment`:

```kotlin
PostpaidSmsReceiptActivity.start(context, fromPayment = true)
```

`PostpaidTransactionReportActivity` / `PostpaidTransactionStatusActivity` navigate to the **shared** `TransactionDetailActivity`:

```kotlin
TransactionDetailActivity.start(mActivity, item)
```

---

## Shared Models — Postpaid Does Not Define Its Own UI Display Models

Display models live in the shared `transactions/model/` package:

| Class | Declared in | Used by |
|---|---|---|
| `RecentTransactionItem` | `transactions/model/RecentTransactionItem.kt` | `RecentTransactionAdp`, `PostpaidRecentTransactionViewModel` |
| `TransactionItem` | `transactions/model/TransactionItem.kt` | `TransactionAdp`, `PostpaidTransactionReportViewModel`, `PostpaidTransactionStatusViewModel`, `TransactionDetailActivity` |

**Do not create a Postpaid-local copy of either class.**

Shared adapters (not duplicated):

| Adapter | Screen(s) | Item model |
|---|---|---|
| `RecentTransactionAdp` | `PostpaidRecentTransactionActivity` | `RecentTransactionItem` |
| `TransactionAdp` | `PostpaidTransactionReportActivity`, `PostpaidTransactionStatusActivity` | `TransactionItem` |

---

## API Models (`retrofit/model/postpaid/`)

Postpaid-specific Retrofit request/response DTOs:

| Class | Endpoint |
|---|---|
| `PostpaidOperatorItem` | GET /api/mobile-postpaid/operators |
| `PostpaidProcessPaymentRequest` / `PostpaidPaymentItem` | POST /api/mobile-postpaid/process-payment |
| `PostpaidTransactionStatusRequest` / `PostpaidTransactionReportDataItem` | POST /api/mobile-postpaid/transaction-status |
| `PostpaidTransactionReportRequest` / `PostpaidTransactionReportDataItem` | POST /api/mobile-postpaid/payment-report |
| `PostpaidLatestPaymentDataItem` | GET /api/mobile-postpaid/latest-payment |

`PostpaidPaymentItem` is a flat (unwrapped) response — success check is `response.isSuccessful && body?.success == true`.
All other Postpaid endpoints use the `General<T>` wrapper — success check is `response.isSuccessful && response.body()?.data != null`.

`PostpaidTransactionReportDataItem` is **reused** for both Status and Report responses.

All endpoints are declared in `ApiService.kt` under the `// ── Mobile Postpaid ──` section.

---

## Key Differences vs Prepaid / Gas / Electricity

| Aspect | Gas / Electricity | Prepaid | Postpaid |
|---|---|---|---|
| Bill fetch step | Required (server-fetched amount) | None | None |
| Plan selection | N/A | `PrepaidPlanSelectionActivity` (own launcher) | **Shares `PrepaidPlanSelectionActivity`** |
| Circle / region | Hardcoded `"0"` or `"00"` per request | User picks from `Utility.STATE_LIST` | User picks from `Utility.STATE_LIST` |
| Transactions type param | `"electricity"` / `"gas"` | `"mobile_recharge"` | `"mobile_postpaid"` |
| Status search field | Transaction ID | Mobile number | **Transaction ID** |
| SMS ViewModel base | `BaseBillViewModel` subclass | Standalone `AndroidViewModel` | Standalone `AndroidViewModel` |
| `isMobileCategory` | `false` | `true` | `true` |
| Report filter label | Consumer number | Mobile number | Connection number |
| CCF field | `ccf ?: platformFee` | `platformFee` only | `platformFee` only |
