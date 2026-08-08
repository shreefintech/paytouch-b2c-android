# DTH Module

Handles DTH (Direct-to-Home) recharge: operator selection, plan browsing, mobile number entry, payment processing, and all transaction history screens.

**DTH follows the same overall structure as Prepaid** — plan selection screen, no server-fetched bill step, and `isMobileCategory = true` throughout. Read the Prepaid README for the identical parts; read this file for what's DTH-specific.

---

## Screens & ViewModels

| Activity | ViewModel | Purpose |
|---|---|---|
| `DthActivity` | `DthViewModel` | Operator dropdown, mobile number entry, plan selection, proceed to pay |
| `DthPlanSelectionActivity` | `DthPlanSelectionViewModel` | Browse and select a recharge plan for a chosen operator |
| `DthRecentTransactionActivity` | `DthRecentTransactionViewModel` | Paginated DTH transaction history |
| `DthTransactionReportActivity` | `DthTransactionReportViewModel` | Filtered report with date range / status / subscriber number filter sheet |
| `DthTransactionStatusActivity` | `DthTransactionStatusViewModel` | Search transactions by transaction ID |
| `TransactionDetailActivity` | *(none)* | **Shared** — same activity used by every module, in `transactions/`, not duplicated here |
| `DthSmsReceiptActivity` | `DthSmsReceiptViewModel` | Receipt display, image download, share — used from payment completion and from recent transactions |

All Activities live in `dth/` (root screens) and `dth/transactions/` (history screens). All ViewModels live in `dth/viewmodel/`.

---

## Pay Flow

DTH has no bill-fetch step. The user selects an operator, browses plans, and the amount is auto-filled from the selected plan.

```
DthActivity
    │
    ├── onCreate ──────────────────────────────────► GET /api/dth/operators
    │                                                 └── populates operator dropdown
    │
    ├── [Browse Plans] button (operator required)
    │       └── DthPlanSelectionActivity
    │               └── GET /api/dth/plans?operator_id=
    │                       └── user selects plan ──► back to DthActivity (ActivityResult)
    │                               └── amount auto-filled from DthPlanItem.amount
    │                               └── plan details card shown (description, validity, talktime, data)
    │
    └── llProceed (mobile + operator + plan selected + terms accepted + amount > 0)
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
                    └── POST /api/dth/process-direct
                            └── onSuccess ──────────► DthSmsReceiptActivity (fromPayment=true) + finish()
```

**Validation order in `onProceedToPay()`:** mobile present (10-digit) → operator selected → plan selected (if not, triggers Browse Plans instead of showing error) → terms accepted → amount > 0.

**Operator change clears plan:** Picking a different operator calls `clearSelectedPlan()` — resets `selectedPlan`, `isPlanSelected`, hides the plan card, clears the amount field. Prevents a stale plan from a previous operator being submitted.

**`requiresManualAmount`:** `DthPlansListItem` includes this `Boolean?` field from the server. Currently unused — amount is always filled from the plan. Future: when `true`, allow manual override after plan selection.

---

## Balance Check Strategy — Inherited from BaseBillViewModel

`DthViewModel` extends **`BaseBillViewModel`** exactly like `GasViewModel`, `ElectricityViewModel`, and `PrepaidViewModel`. `checkVpsBalance()`, `checkWalletBalance()`, and `bearerToken()` are not duplicated — they're inherited. Always checks VPS balance first, falls back to wallet balance on failure.

---

## Plan Selection (DthPlanSelectionActivity)

Launched via `DthPlanSelectionActivity.start(activity, launcher, operatorId)` using an `ActivityResultLauncher`. On plan tap, sets `RESULT_OK` with the selected `DthPlanItem` serialized as JSON under `EXTRA_SELECTED_PLAN`. `DthActivity` reads it in `planSelectionLauncher` and calls `onPlanSelected(plan)` to populate the plan card and auto-fill the amount.

`DthPlanSelectionViewModel` extends `BaseBillViewModel`. The plans response is a flat `DthPlansListItem` (not `General<T>`) — success check is `response.isSuccessful && body?.success == true`.

`DthPlanAdp` (`adapter/DthPlanAdp.kt`) is DTH-specific — it serves only `DthPlanSelectionActivity` and is not shared.

---

## Transaction History Screens

Three separate entry points, launched from the tab bar at the top of `DthActivity`:

| Tab | Screen | Data source |
|---|---|---|
| Recent | `DthRecentTransactionActivity` | `GET /api/transactions?type=dth` (paginated 20/page) |
| Report | `DthTransactionReportActivity` | `POST /api/dth/payment-report` (filtered) |
| Status | `DthTransactionStatusActivity` | `POST /api/dth/transaction-status` (by transaction ID) |

`DthRecentTransactionActivity` loads operators first (to build an in-memory `operatorMap` for display names), then fetches via the shared unified endpoint with `type = "dth"`. Operator fetch failure is non-fatal — transactions still load with raw operator IDs as the fallback name.

Pagination is scroll-triggered in all three screens: fires load when the last visible item is within 3 of the end. `DthRecentTransactionViewModel` tracks `currentPage`, `hasMore`, `isLoading`; Report and Status ViewModels use `currentPage`, `isLastPage`, `isLoading` with `canLoadMore()` / `nextPage()` helpers.

---

## Filter Sheet (DthTransactionReportActivity)

Reuses `TransactionFilterHelper` (in `utill/`) — no DTH-specific filter logic:

```kotlin
filterHelper = TransactionFilterHelper(
    activity     = mActivity,
    sheetBinding = binding.incFilterSheet,
    bgOverlay    = binding.viewBg,
    onApply      = { fromDate, toDate, status, subscriberNo -> callReport(...) },
    onClear      = { ... callReport(null, null, null, null) }
)
filterHelper.setup()
```

`DthTransactionReportActivity` layers a local text-search (`etSearch`) on top of server-side filters: `mAllList` holds every fetched page, `mDisplayList` is the filtered subset re-derived by `filterList(query)` on search changes or new page arrivals. Search matches on `mobileNumber` or `transactionId`.

Back press closes the filter sheet before finishing the activity.

---

## SMS Receipt

`DthSmsReceiptActivity` has two entry modes controlled by `EXTRA_FROM_PAYMENT`, identical to Gas and Electricity:

| Mode | `fromPayment` | Title row | Data source |
|---|---|---|---|
| From payment | `true` | Hidden | `GET /api/dth/latest-payment` via `getLatestPayment()`, called unconditionally in `onCreate()` |
| From recent transactions | `false` | Visible (tab bar) | Same `getLatestPayment()` call |

`DthSmsReceiptViewModel` is a standalone `AndroidViewModel` (not `BaseBillViewModel`) — receipt screens only need `bearerToken()`, not balance checks. Uses `bearerToken()` and `getString()` from `ViewModelExt.kt`.

`tvConsumerNoLabel` is explicitly set to `getString(R.string.labelMobileNo)` in `populateReceiptFromApi()` — DTH is a mobile-number category. Never rely on the XML default.

Mobile number in receipt: `item.mobileNo ?: item.subscriberNo ?: "--"`. Both fields exist in `DthLatestPaymentDataItem`; `mobileNo` is preferred.

Receipt card captured via shared `ReceiptHelper` for download and share.

---

## Intent Data Passing

`DthSmsReceiptActivity` is started with only `context` and `fromPayment`:

```kotlin
DthSmsReceiptActivity.start(context, fromPayment = true)
```

`DthTransactionReportActivity` / `DthTransactionStatusActivity` navigate to the **shared** `TransactionDetailActivity` (not a DTH-specific detail screen):

```kotlin
TransactionDetailActivity.start(mActivity, item)
```

Plan selection uses `ActivityResultLauncher`:

```kotlin
DthPlanSelectionActivity.start(mActivity, planSelectionLauncher, selectedOperatorId!!)
// result: EXTRA_SELECTED_PLAN → JSON of DthPlanItem
```

---

## Shared Models — DTH Does Not Define Its Own UI Display Models

Display models live in the shared `transactions/model/` package:

| Class | Declared in | Used by |
|---|---|---|
| `RecentTransactionItem` | `transactions/model/RecentTransactionItem.kt` | `RecentTransactionAdp`, `DthRecentTransactionViewModel` |
| `TransactionItem` | `transactions/model/TransactionItem.kt` | `TransactionAdp`, `DthTransactionReportViewModel`, `DthTransactionStatusViewModel`, `TransactionDetailActivity` |

**Do not create a DTH-local copy of either class.**

Shared adapters (not duplicated):

| Adapter | Screen(s) | Item model |
|---|---|---|
| `RecentTransactionAdp` | `DthRecentTransactionActivity` | `RecentTransactionItem` |
| `TransactionAdp` | `DthTransactionReportActivity`, `DthTransactionStatusActivity` | `TransactionItem` |

`DthPlanAdp` (`adapter/DthPlanAdp.kt`) is DTH-specific — it serves only `DthPlanSelectionActivity` and is not shared.

---

## API Models (`retrofit/model/dth/`)

DTH-specific Retrofit request/response DTOs:

| Class | Endpoint |
|---|---|
| `DthOperatorItem` | GET /api/dth/operators |
| `DthPlansListItem` / `DthPlanItem` | GET /api/dth/plans?operator_id= |
| `DthProcessPaymentRequest` / `DthPaymentItem` | POST /api/dth/process-direct |
| `DthTransactionReportRequest` / `DthTransactionReportDataItem` | POST /api/dth/payment-report |
| `DthTransactionStatusRequest` | POST /api/dth/transaction-status (reuses `DthTransactionReportDataItem` as response row) |
| `DthLatestPaymentDataItem` | GET /api/dth/latest-payment |

`DthPaymentItem` and `DthPlansListItem` are flat (unwrapped) responses — success check is `response.isSuccessful && body?.success == true`.
`DthTransactionReportDataItem` is reused for both Report and Status responses.
All other DTH endpoints use the `General<T>` wrapper — success check is `response.isSuccessful && response.body()?.data != null`.

Process-direct request fields: `cn` (subscriber/mobile number), `op` (operator ID), `amount` (Int cast to String).

All endpoints declared in `ApiService.kt` under the `// ── DTH ──` section — never add a DTH endpoint anywhere else.

---

## Key Differences vs Other Modules

| Aspect | Gas / Electricity | Prepaid | DTH |
|---|---|---|---|
| Bill fetch step | Required | None | None |
| Circle selection | N/A | User picks from local STATE_LIST | Not applicable |
| Plan selection screen | N/A | `PrepaidPlanSelectionActivity` | `DthPlanSelectionActivity` |
| Transactions type param | `"electricity"` / `"gas"` | `"mobile_recharge"` | `"dth"` |
| Status search field | Transaction ID | Mobile number | Transaction ID |
| `isMobileCategory` | `false` | `true` | `true` |
| Category icon | `ic_electricity` / `ic_gas` | `ic_mobile` | `ic_tv` |
| SMS ViewModel base | `BaseBillViewModel` (Elec/Gas) / `AndroidViewModel` (Prepaid) | `AndroidViewModel` | `AndroidViewModel` |
| Payment endpoint | `process-payment` | `process-direct` | `process-direct` |

---

## Known TODOs

- `TODO(PAYTOUCH-570)` in `DthRecentTransactionActivity`: Add `showNoInternet()` / `hideNoInternet()` / retry callback once the no-internet placeholder design is finalised.
- `DthPlansListItem.requiresManualAmount`: field present in DTO but not yet wired to UI.
