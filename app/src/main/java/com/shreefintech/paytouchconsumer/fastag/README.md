# FASTag Module

Handles FASTag recharge: operator selection, vehicle number entry, amount entry, payment processing, and all transaction history screens.

**FASTag has no bill-fetch step.** Unlike Electricity and Gas, the user enters the recharge amount directly — there is no verify-before-pay handshake. Platform fee and total update in real time via a `TextWatcher`. For the transaction history screens FASTag mirrors Gas exactly.

---

## Screens & ViewModels

| Activity | ViewModel | Purpose |
|---|---|---|
| `FastagActivity` | `FastagViewModel` | Operator dropdown, vehicle number entry, amount + real-time fee display, proceed to pay |
| `FastagRecentTransactionActivity` | `FastagRecentTransactionViewModel` | Paginated FASTag transaction history |
| `FastagTransactionReportActivity` | `FastagTransactionReportViewModel` | Filtered report with date range / status / vehicle number filter sheet |
| `FastagTransactionStatusActivity` | `FastagTransactionStatusViewModel` | Search transactions by transaction ID |
| `TransactionDetailActivity` | *(none)* | **Shared** — same activity used by every module, in `transactions/`, not duplicated here |
| `FastagSmsReceiptActivity` | `FastagSmsReceiptViewModel` | Receipt display, image download, share — used from payment completion and from recent transactions |

All Activities live in `fastag/` (root screen) and `fastag/transactions/` (history screens). All ViewModels live in `fastag/viewmodel/`.

---

## Pay Recharge Flow

```
FastagActivity
    │
    ├── onCreate ──────────────────────────────────► GET /api/fastag/operators
    │                                                 └── populates operator dropdown
    │
    └── llProceed (all fields valid)
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
                    └── POST /api/fastag
                            └── onSuccess ──────────► FastagSmsReceiptActivity (fromPayment=true)
```

Validation order in `onProceedToPay()`:
1. Vehicle number not empty
2. Operator selected (`selectedOperatorBillerId` not null/empty)
3. Amount > 0
4. Terms checkbox checked

The `circleId` is taken from the selected `FastagOperatorItem.circleId` — it is not hardcoded. This differs from Gas (`"0"`) and Electricity (`"00"`), which hardcode it per their server contracts.

---

## FASTag-Specific Differences from Gas / Electricity

### No Bill Fetch

`FastagActivity` has no `llFetchBill` button and no `cvBillDetails` card. The user sets the amount themselves. `Utility.calculatePlatformFee(amount)` drives the platform fee and total payable fields via `TextWatcher` — no server round-trip for fee calculation.

### Vehicle Number vs Consumer Number

Every field labelled "consumer number" in Gas/Electricity is "vehicle number" in FASTag. The shared adapters detect this via `isVehicleCategory = true` set in every FASTag ViewModel mapping function:

```kotlin
// Set in every mapToTransactionItem() and mapToDisplayItem() for FASTag
isMobileCategory  = false
isVehicleCategory = true
```

`RecentTransactionAdp` and `TransactionAdp` switch the label accordingly:

| Flag | Label shown |
|---|---|
| `isVehicleCategory = true` | "Vehicle No - XXXX" (`R.string.labelDetailVehicleNo`) |
| `isMobileCategory = true` | "Mobile No - XXXX" |
| Both `false` | "Consumer No - XXXX" |

### SMS Receipt Consumer Label

`FastagSmsReceiptActivity.populateReceiptFromApi()` sets the label dynamically. Never rely on the XML default:

```kotlin
binding.tvConsumerNoLabel.text = getString(R.string.labelVehicleNumber)
```

### No Operator Pre-load in Recent Transactions

`FastagRecentTransactionViewModel` calls `getTransactions(type = "fastag")` directly — it does **not** pre-load operators before fetching. Gas pre-loads operators to build an in-memory `operatorMap` for ID-to-name resolution. FASTag skips this step because `UnifiedTransactionExtraItem.operatorName` already carries the resolved operator name from the shared transactions endpoint.

```kotlin
// FASTag mapToDisplayItem() — operatorName is already resolved
categoryName = item.extra?.operatorName ?: "--"
```

---

## Balance Check Strategy — Shared with All Bill-Payment Modules

`FastagViewModel` extends **`BaseBillViewModel`** (`app/src/main/java/.../BaseBillViewModel.kt`). `checkVpsBalance()`, `checkWalletBalance()`, and `bearerToken()` live once in `BaseBillViewModel` and are inherited — never duplicated per module.

`FastagViewModel.verifyAndPay()` always checks the VPS admin balance first. Wallet balance is only checked if the VPS check fails or returns insufficient funds. Both checks are transparent to the user.

---

## Transaction History Screens

Three separate entry points, launched from the tab bar at the top of `FastagActivity`:

| Tab | Screen | Data source |
|---|---|---|
| Recent | `FastagRecentTransactionActivity` | `GET /api/transactions?type=fastag` (paginated, 20/page) |
| Report | `FastagTransactionReportActivity` | `GET /api/fastag` with `from_date`, `to_date`, `status`, `vehicle_number`, `page`, `per_page` |
| Status | `FastagTransactionStatusActivity` | `POST /api/fastag/transaction/status` (search by transaction ID) |

**Report** — The filter identifier field is `vehicleNumber`, not `consumerNumber`. `TransactionFilterHelper` passes it through `onApply` as the fourth parameter (the same slot Gas uses for `consumerNo`).

**Status** — `FastagTransactionStatusRequest` sends `vehicleNumber = null, transactionId = query`. Only transaction ID is used for search; vehicle number is always null in the status request regardless of what the user typed.

**Report pagination note** — The report endpoint returns `General<FastagTransactionPageItem>`. The page item has its own `data: List<FastagTransactionReportDataItem>?` field. Double-unwrap required:

```kotlin
response.body()?.data?.data   // General → FastagTransactionPageItem → List<FastagTransactionReportDataItem>
```

Status uses `General<List<FastagTransactionReportDataItem>>` — single unwrap, same as Gas status.

---

## Filter Sheet (FastagTransactionReportActivity)

Reuses `TransactionFilterHelper` (in `utill/`) with no FASTag-specific filter logic:

```kotlin
filterHelper = TransactionFilterHelper(
    activity     = mActivity,
    sheetBinding = binding.incFilterSheet,
    bgOverlay    = binding.viewBg,
    onApply      = { fromDate, toDate, status, vehicleNumber -> callReport(...) },
    onClear      = { ... callReport(null, null, null, null) }
)
filterHelper.setup()
```

Back press is handled by `onBack()` — closes the sheet before finishing the activity.

`FastagTransactionReportActivity` layers a local text-search filter (`etSearch`) on top of the server-side filter. `mAllList` holds all fetched pages; `mDisplayList` is the on-screen filtered subset re-derived by `filterList(query)` whenever the search box changes or a new page arrives.

---

## SMS Receipt

`FastagSmsReceiptActivity` has two entry modes controlled by `EXTRA_FROM_PAYMENT`:

| Mode | `fromPayment` | Title row | Data source |
|---|---|---|---|
| From payment | `true` | Hidden | `GET /api/fastag/latest-payment` via `getLatestPayments()`, called unconditionally in `onCreate()` |
| From recent transactions | `false` | Visible (tab bar) | Same `getLatestPayments()` call — no per-transaction receipt endpoint for FASTag |

The receipt card (`cvReceiptCard`) is captured via the shared `ReceiptHelper` (in `utill/`) for download and share.

- Download: `MediaStore` on API 29+, `FileProvider` on older (behind runtime storage-permission check on API < 29).
- Share: Always uses `FileProvider` via cache dir.

`FastagLatestPaymentDataItem` has no separate `ccf` field. The `platformFee` field is used directly for the CCF row. This differs from Gas which falls back: `item.ccf ?: item.platformFee ?: "--"`.

---

## Intent Data Passing

`FastagSmsReceiptActivity` is started with only `context` and `fromPayment`:

```kotlin
FastagSmsReceiptActivity.start(context, fromPayment = true)
```

`FastagTransactionReportActivity` / `FastagTransactionStatusActivity` navigate to the shared `TransactionDetailActivity`:

```kotlin
TransactionDetailActivity.start(mActivity, item)
```

---

## Shared Models — FASTag Does Not Define Its Own UI Display Models

Display models live in the shared `transactions/model/` package, not in `fastag/`:

| Class | Declared in | Used by |
|---|---|---|
| `RecentTransactionItem` | `transactions/model/RecentTransactionItem.kt` | `RecentTransactionAdp`, `FastagRecentTransactionViewModel` |
| `TransactionItem` | `transactions/model/TransactionItem.kt` | `TransactionAdp`, `FastagTransactionReportViewModel`, `FastagTransactionStatusViewModel`, `TransactionDetailActivity` |

**Do not create a FASTag-local copy of either class.** If a field is missing, add it to the shared model (nullable with a sensible default at the call site) rather than forking it.

`RecentTransactionItem` gained `isVehicleCategory: Boolean = false` for FASTag. All other modules that do not pass this field receive `false` automatically — fully backward compatible.

Shared adapters:

| Adapter | Screen(s) | Item model |
|---|---|---|
| `RecentTransactionAdp` | `FastagRecentTransactionActivity` | `RecentTransactionItem` |
| `TransactionAdp` | `FastagTransactionReportActivity`, `FastagTransactionStatusActivity` | `TransactionItem` |

---

## API Models (`retrofit/model/fastag/`)

FASTag-specific Retrofit request/response DTOs:

| Class | Endpoint | Wrapper |
|---|---|---|
| `FastagOperatorItem` | `GET /api/fastag/operators` | `General<List<FastagOperatorItem>>` |
| `FastagProcessPaymentRequest` / `FastagPaymentItem` | `POST /api/fastag` | Flat — `FastagPaymentItem` directly |
| `FastagTransactionReportDataItem` + `FastagTransactionPageItem` | `GET /api/fastag` | `General<FastagTransactionPageItem>` → `data.data` for the row list |
| `FastagTransactionStatusRequest` | `POST /api/fastag/transaction/status` | `General<List<FastagTransactionReportDataItem>>` |
| `FastagLatestPaymentDataItem` | `GET /api/fastag/latest-payment` | `General<FastagLatestPaymentDataItem>` |

Success check by type:

- `FastagPaymentItem` (flat): `response.isSuccessful && body?.success == true`
- All others (`General<T>`): `response.isSuccessful && response.body()?.data != null`

All FASTag endpoints are declared in `ApiService.kt` under the `// ── FASTag ──` section — never add a FASTag endpoint anywhere else.

---

## Operator Loading State

Identical to Gas and Electricity — spinner inside the dropdown slot, anchor disabled while loading:

```kotlin
private fun setOperatorLoading(loading: Boolean) {
    binding.pbCompanyLoading.visibility = if (loading) View.VISIBLE else View.GONE
    binding.ivCompanyArrow.visibility   = if (loading) View.GONE else View.VISIBLE
    binding.flCompanyAnchor.isClickable = !loading
    binding.flCompanyAnchor.isFocusable = !loading
}
```

If the user taps the dropdown while it is empty (load failed), `loadOperators()` is retried and a warning toast (`msgLoadingOperators`) is shown.
