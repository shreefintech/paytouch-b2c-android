# Gas Module

Handles the full gas bill payment flow: operator selection, bill fetch, payment processing, and all transaction history screens.

**Gas mirrors Electricity exactly** (`electricity/` is the canonical reference for this pattern — see `electricity/README.md`). Only the operator/consumer terminology, the category icon (`R.drawable.ic_gas`), and the `api/gas/*` endpoints differ. Read this file for what's Gas-specific; read the Electricity README for the parts that are identical everywhere.

---

## Screens & ViewModels

| Activity | ViewModel | Purpose |
|---|---|---|
| `GasActivity` | `GasViewModel` | Operator dropdown, consumer number entry, bill fetch, proceed to pay |
| `GasRecentTransactionActivity` | `GasRecentTransactionViewModel` | Paginated gas transaction history |
| `GasTransactionReportActivity` | `GasTransactionReportViewModel` | Filtered report with date range / status / consumer number filter sheet |
| `GasTransactionStatusActivity` | `GasTransactionStatusViewModel` | Search transactions by transaction ID |
| `TransactionDetailActivity` | *(none)* | **Shared** — same activity used by every module, in `transactions/`, not duplicated here |
| `GasSmsReceiptActivity` | `GasSmsReceiptViewModel` | Receipt display, image download, share — used from payment completion and from transaction detail |

All Activities live in `gas/` (root screen) and `gas/transactions/` (history screens). All ViewModels live in `gas/viewmodel/`.

---

## Pay Bill Flow

```
GasActivity
    │
    ├── onCreate ──────────────────────────────────► GET api/gas (operators)
    │                                                 └── populates operator dropdown
    │
    ├── llFetchBill (consumer number entered)
    │       └── POST api/gas fetch-bill
    │               └── onSuccess ──────────────────► shows bill details card (cvBillDetails)
    │
    └── llProceed (bill fetched + terms checked)
            └── verifyBalanceAndProcessPayment()
                    │
                    ├── BaseBillViewModel.checkVpsBalance() (ApiAdminClient)
                    │       ├── balance sufficient ──► processPayment()
                    │       └── insufficient / fail ─► BaseBillViewModel.checkWalletBalance()
                    │
                    ├── GET api/wallet/user-data
                    │       ├── balance sufficient ──► processPayment()
                    │       └── insufficient ────────► onError (msgInsufficientBalance)
                    │
                    └── POST api/gas process-payment
                            └── onSuccess ──────────► GasSmsReceiptActivity (fromPayment=true)
```

`GasActivity` guards `onFetchBill()` / `onProceedToPay()` with the same validation order as Electricity: operator selected → consumer number present (≥10 digits) → bill fetched → terms accepted → amount > 0. If the user taps "Proceed" before a bill is fetched, it silently calls `fetchBill()` first instead of showing an error.

`circleId` is hardcoded to `"0"` for every Gas request (`GasFetchBillRequest`, `GasProcessPaymentRequest`) — Electricity uses `"00"`. This is a server contract difference, not a bug; don't "fix" it to match Electricity.

---

## Balance Check Strategy — Shared with Electricity

`GasViewModel` extends **`BaseBillViewModel`** (`app/src/main/java/.../BaseBillViewModel.kt`), the same base class `ElectricityViewModel` extends. `checkVpsBalance()`, `checkWalletBalance()`, and `bearerToken()` are **not** duplicated per module — they live once in `BaseBillViewModel` and every bill-payment ViewModel inherits them.

`GasViewModel.verifyAndPay()` always checks the VPS admin balance first via the inherited `checkVpsBalance()`. The wallet balance is only checked via `checkWalletBalance()` if the VPS check fails or returns insufficient funds. Both checks are transparent to the user — only `onError` or `onSuccess` surfaces.

> If you're adding a new bill-payment module, extend `BaseBillViewModel` rather than re-implementing balance checks — see `GasViewModel` / `ElectricityViewModel` as the two existing examples.

---

## Transaction ID

Generated client-side via `Utility.generateTransactionId()` immediately before `processGasPayment` is called (inside `GasViewModel.processPayment()`, mirroring Electricity). Format: `PYTCH[DDMMyyyyHHmmss]M`. Sent as `transaction_id` in the request body.

---

## Transaction History Screens

Three separate entry points, launched from the tab bar at the top of `GasActivity`:

| Tab | Screen | Data source |
|---|---|---|
| Recent | `GasRecentTransactionActivity` | `GET api/transactions?type=gas` (paginated 20/page) |
| Report | `GasTransactionReportActivity` | `POST api/gas payment-report` (filtered) |
| Status | `GasTransactionStatusActivity` | `POST api/gas transaction-status` (by transaction ID) |

`GasRecentTransactionActivity` / `GasRecentTransactionViewModel.loadOperatorsThenData()` loads gas operators first (to resolve operator IDs to names via an in-memory `operatorMap`), then fetches transactions via the shared `GET api/transactions` endpoint with `type = "gas"`. Operator fetch failure is non-fatal — `fetchTransactions()` still runs and transactions load with raw operator IDs as the fallback name.

Pagination in `GasRecentTransactionActivity` is scroll-triggered: fires `loadNextPage()` when the last visible item is within 3 of the end. `GasRecentTransactionViewModel` tracks `currentPage`, `hasMore`, and `loading` to prevent duplicate calls (same pattern as Electricity's `RecentTransactionViewModel`).

`GasTransactionReportActivity` and `GasTransactionStatusActivity` instead track pagination via `currentPage` / `isLastPage` / `isLoading` on their respective ViewModels, exposed through `canLoadMore()` / `nextPage()` — a slightly different but equivalent pattern to the Recent screen. Follow whichever of the two your new screen most resembles.

---

## Filter Sheet (GasTransactionReportActivity)

Reuses `TransactionFilterHelper` (in `utill/`) exactly as Electricity does — no gas-specific filter logic exists.

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

`GasTransactionReportActivity` additionally layers a local text-search filter (`etSearch`) on top of the server-side filter: `mAllList` holds every fetched page, `mDisplayList` is the on-screen filtered subset re-derived by `filterList(query)` whenever the search box changes or a new page arrives.

Back press is handled by `onBack()` — closes the filter sheet before finishing the activity.

---

## SMS Receipt

`GasSmsReceiptActivity` has two entry modes controlled by `EXTRA_FROM_PAYMENT`, identical to Electricity's `SmsReceiptActivity`:

| Mode | `fromPayment` | Title row | Data source |
|---|---|---|---|
| From payment | `true` | Hidden | `GET api/gas latest-payment` via `getLatestPayments()`, called unconditionally in `onCreate()` |
| From transaction detail | `false` | Visible (tab bar) | Same `getLatestPayments()` call — Gas has no separate per-transaction receipt endpoint |

The receipt card (`cvReceiptCard`) is captured via the shared `ReceiptHelper` (in `utill/`) for download and share — no gas-specific receipt rendering code. Download uses `MediaStore` on API 29+, `FileProvider` on older (behind a runtime storage-permission check on API < 29). Share always uses `FileProvider` via cache dir.

`ccf` (customer convenience fee) falls back to `platformFee` if the server omits it: `item.ccf ?: item.platformFee ?: "--"`.

---

## Intent Data Passing

`GasSmsReceiptActivity` is started with only `context` and `fromPayment`:

```kotlin
GasSmsReceiptActivity.start(context, fromPayment = true)
```

`GasTransactionReportActivity` / `GasTransactionStatusActivity` navigate to the **shared** `TransactionDetailActivity` (not a gas-specific detail screen) using the generic `TransactionItem`, serialized to JSON exactly as every other module does:

```kotlin
TransactionDetailActivity.start(mActivity, item)
```

---

## Shared Models — Gas Does Not Define Its Own UI Models

Unlike Electricity, the Gas module has **no `gas/model/` package**. It reuses the same category-agnostic display models Electricity introduced:

| Class | Declared in | Used by |
|---|---|---|
| `RecentTransactionItem` | `transactions/model/RecentTransactionItem.kt` | `RecentTransactionAdp`, `RecentTransactionViewModel`, `GasRecentTransactionViewModel` |
| `TransactionItem` | `transactions/model/TransactionItem.kt` | `GasTransactionReportViewModel`, `GasTransactionStatusViewModel`, `TransactionDetailActivity` |

**Do not create a Gas-local copy of either class.** If a field is missing for Gas, add it to the existing shared model (nullable, with a sensible default at the call site) rather than forking it.

Shared adapters (also not duplicated):

| Adapter | Screen(s) | Item model |
|---|---|---|
| `RecentTransactionAdp` | `GasRecentTransactionActivity` | `RecentTransactionItem` |
| `TransactionAdp` | `GasTransactionReportActivity`, `GasTransactionStatusActivity` | `TransactionItem` |

---

## API Models (`retrofit/model/gas/`)

Gas-specific Retrofit request/response DTOs — these *are* per-module, unlike the UI models above:

| Class | Endpoint |
|---|---|
| `GasOperatorItem` | GET api/gas (operators) |
| `GasFetchBillRequest` / `GasBillItem` | POST api/gas fetch-bill |
| `GasProcessPaymentRequest` / `GasPaymentItem` | POST api/gas process-payment |
| `GasTransactionReportRequest` / `GasTransactionReportDataItem` | POST api/gas payment-report |
| `GasTransactionStatusRequest` | POST api/gas transaction-status (reuses `GasTransactionReportDataItem` as the response row) |
| `GasVerifyPaymentDataItem` | GET api/gas latest-payment |

`GasPaymentItem` is a flat (unwrapped) response — success check is `response.isSuccessful && body?.success == true`.
Every other Gas endpoint uses the `General<T>` wrapper — success check is `response.isSuccessful && response.body()?.data != null`.

All endpoints are declared in `ApiService.kt` under the `// ── Gas ──` section — never add a Gas endpoint anywhere else.

---

## Operator Loading State

Identical pattern to Electricity — spinner inside the dropdown field slot, anchor disabled while loading:

```kotlin
private fun setOperatorLoading(loading: Boolean) {
    binding.pbCompanyLoading.visibility = if (loading) View.VISIBLE else View.GONE
    binding.ivCompanyArrow.visibility   = if (loading) View.GONE else View.VISIBLE
    binding.flCompanyAnchor.isClickable = !loading
    binding.flCompanyAnchor.isFocusable = !loading
}
```

If the user taps the dropdown while it is empty (load failed), `loadOperators()` is called again and a warning toast (`msgLoadingOperators`) is shown.

---

## What to Copy When Adding the Next Module (e.g. Water, Broadband)

Gas is itself a copy of Electricity with names swapped — use either as the template. In order of what you'll touch:

1. `{Category}Activity` + `activity_{category}.xml` — copy `GasActivity` / `activity_gas.xml`, rename fields/strings only.
2. `{Category}ViewModel` extending `BaseBillViewModel` — copy `GasViewModel`, swap endpoint names and request/response DTOs.
3. `retrofit/model/{category}/` — new DTOs matching the new endpoint's actual field names (check `docs/api_reference.md` first, never guess).
4. Add the new endpoints to `ApiService.kt` under a new `// ── {Category} ──` section.
5. `{Category}RecentTransactionActivity` + `ViewModel`, `{Category}TransactionReportActivity` + `ViewModel`, `{Category}TransactionStatusActivity` + `ViewModel`, `{Category}SmsReceiptActivity` + `ViewModel` — copy the Gas transaction screens verbatim, reusing the shared `RecentTransactionItem`, `TransactionItem`, `RecentTransactionAdp`, `TransactionAdp`, `TransactionDetailActivity`, `TransactionFilterHelper`, and `ReceiptHelper`. **Never fork these.**
6. Pass `R.drawable.ic_{category}` as `categoryIconRes` in the new ViewModel's mapping functions — that's the only visual difference between modules' transaction rows.

This list matches the "Transaction Screens — Shared Structure Across All Modules" rule in the project's `CLAUDE.md` — read that first if anything here is ambiguous.
