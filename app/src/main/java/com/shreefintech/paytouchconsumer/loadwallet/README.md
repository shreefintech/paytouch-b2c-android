# Load Wallet Module

Handles wallet top-up via the HDFC payment gateway (WebView-based), displays wallet balance and virtual account details, and shows a paginated wallet transaction history.

---

## Screens & ViewModels

| Activity | ViewModel | Purpose |
|---|---|---|
| `LoadWalletActivity` | `LoadWalletViewModel` | Wallet balance, virtual account info, recent history preview, HDFC payment sheet |
| `WalletTransactionsActivity` | `WalletTransactionsViewModel` | Full paginated wallet transaction history |
| `HdfcWebViewActivity` | *(none)* | Loads the HDFC-issued payment URL in a WebView; intercepts the return URL to finish |
| `PaymentStatusActivity` | *(none)* | Displays payment result with GIF animation; auto-navigates back after 5 s |

`HdfcPaymentHelper` — singleton (`object`) that holds pending order state across the Activity boundary. Set before launching `HdfcWebViewActivity`; cleared on `LoadWalletActivity.onResume()`.

---

## Load Wallet Flow

```
LoadWalletActivity.onCreate()
    ├── GET /api/wallet/user-data       → populateWalletData()
    └── GET /api/wallet/combined-wallet-history (page=1, perPage=2)
                                        → shows last 2 transactions

"Make Payment" button
    └── sheet_make_payment slides up (BottomSheetBehavior)
            │
            User enters amount (required) + description (optional)
            │
            "Proceed" → validateAndShowConfirmDialog()
                    │
                    showConfirmDialog()
                            │
                    "Pay Securely" → LoadWalletViewModel.createHdfcOrder()
                            │         POST /api/hdfc/orders
                            │
                            ├── status is FAILED ──────────► PaymentStatusActivity (failed)
                            │
                            ├── paymentLinks.web is empty ─► toast error
                            │
                            └── success + payUrl present
                                    │
                                HdfcPaymentHelper.launchPayment()
                                    ├── stores pendingOrderId + pendingAmount
                                    └── starts HdfcWebViewActivity

HdfcWebViewActivity
    └── loads payUrl
            │
            Return URL intercepted (shouldInterceptRequest / shouldOverrideUrlLoading)
                    └── finish()

LoadWalletActivity.onResume()
    └── pendingOrderId != null
            │
            HdfcPaymentHelper.clearPendingState()
            showLoading()
            LoadWalletViewModel.checkOrderStatus()
                    │   GET /api/hdfc/orders/{order_id}/status
                    │
                    ├── onSuccess ──► PaymentStatusActivity (result status from API)
                    └── onError   ──► PaymentStatusActivity (status = HDFC_STATUS_NEW)

PaymentStatusActivity
    ├── shows GIF + order ID (copyable) + amount
    ├── auto-finish after 5 s ──► LoadWalletActivity (FLAG_ACTIVITY_CLEAR_TOP)
    └── back press             ──► LoadWalletActivity (FLAG_ACTIVITY_CLEAR_TOP)

LoadWalletActivity.onNewIntent()  (triggered by CLEAR_TOP re-entry)
    └── resets fields + re-fetches wallet data + history
```

---

## HDFC Payment Status Codes

All status constants live in `Constant.kt`.

| Status | Meaning | Screen label |
|---|---|---|
| `CHARGED` / `AUTHORIZED` | Payment successful | Success (green) |
| `NEW` | Order created, payment not attempted | Initiated (orange) |
| `PENDING_VBV` / `AUTHORIZING` / `STARTED` | Payment in progress | Processing (orange) |
| `JUSPAY_DECLINED` / `AUTHENTICATION_FAILED` / `AUTHORIZATION_FAILED` | Payment failed | Failed (red) |
| `AUTO_REFUNDED` | Payment failed and refunded | Failed/Refunded (red) |

`HdfcPaymentHelper.isFailedStatus(status)` checks against the failed set — used to short-circuit directly to `PaymentStatusActivity` without launching the WebView if the order is already failed at creation time.

---

## Return URL Interception

`HdfcWebViewActivity` uses strict scheme + host + path-prefix matching (`isReturnUrl()`). The return URL comes from `HdfcOrderItem.returnUrl` and is passed in the intent.

Three WebView callbacks all guard the same `hasReturned` flag:
- `shouldInterceptRequest` — background thread; posts `finish()` to main thread
- `shouldOverrideUrlLoading` (API 24+ and legacy) — main thread; calls `finish()` directly

Once `hasReturned = true`, all three callbacks are no-ops to prevent double `finish()`.

---

## Wallet Transaction Pagination

`WalletTransactionsActivity` uses `WalletTransactionsViewModel` for state:

| ViewModel field | Purpose |
|---|---|
| `currentPage` | Last successfully loaded page |
| `isLastPage` | True when `page >= pageData.lastPage` |
| `isLoading` | Guards against concurrent loads |

Scroll listener fires `loadPage(nextPage())` when the last visible item is within 3 of the end. Page 1 shows a shimmer; subsequent pages show a footer `ProgressBar` (`pbLoadMore`).

---

## Models

### Retrofit (API) DTOs — `retrofit/model/`

| Class | Endpoint | Notes |
|---|---|---|
| `HdfcCreateOrderRequest` | POST `api/hdfc/orders` | `amount: Double`, `description: String`, `purpose` defaults to `"wallet_topup"` |
| `HdfcOrderResponseItem` | — | Wrapper: `success: Boolean?`, `message: String?`, `data: HdfcOrderItem?` |
| `HdfcOrderItem` | — | `orderId`, `amount`, `status`, `paymentLinks`, `returnUrl`, `walletCreditedAt` |
| `HdfcPaymentLinksItem` | — | `web`, `mobile`, `expiry` |
| `WalletDataItem` | GET `api/wallet/user-data` | Wallet balance, virtual account number, IFSC, live bank balance, embedded `WalletInfoItem` |
| `WalletInfoItem` | — | Nested inside `WalletDataItem`; `balance`, `status`, KYC flag, limits |
| `WalletHistoryPageItem` | GET `api/wallet/combined-wallet-history` | Paginated wrapper: `data: List<WalletHistoryItem>`, `lastPage`, `currentPage` |
| `WalletHistoryItem` | — | `transactionId`, `amount`, `type` (`CREDIT`/`DEBIT`), `serviceName`, `createdAt`, `status` |

### Local Display DTOs — `loadwallet/model/`

| Class | Purpose |
|---|---|
| `WalletTransactionItem` | Display model mapped from `WalletHistoryItem` via `WalletTransactionItem.from(item)` |
| `PaymentStatusItem` | Passed to `PaymentStatusActivity` — `orderId`, `amount`, `status` (non-nullable; local only) |

`WalletTransactionItem` and `PaymentStatusItem` are local activity-to-activity DTOs — no `@field:SerializedName` required.

---

## API Endpoints

All declared in `ApiService.kt` under `// ── Wallet ──` and `// ── HDFC ──` sections.

| Method | Path | Used by |
|---|---|---|
| `GET` | `api/wallet/user-data` | `LoadWalletViewModel.fetchUserWalletData()` |
| `GET` | `api/wallet/combined-wallet-history` | `LoadWalletViewModel.fetchRecentHistory()`, `WalletTransactionsViewModel.loadHistory()` |
| `POST` | `api/hdfc/orders` | `LoadWalletViewModel.createHdfcOrder()` |
| `GET` | `api/hdfc/orders/{order_id}/status` | `LoadWalletViewModel.checkOrderStatus()` |

---

## Key Design Decisions

- **`LoadWalletActivity.launchMode = singleTop`** — required so `PaymentStatusActivity` can use `FLAG_ACTIVITY_CLEAR_TOP or FLAG_ACTIVITY_SINGLE_TOP` to re-enter the wallet screen via `onNewIntent()` rather than creating a new instance.
- **`HdfcPaymentHelper.clearPendingState()` before `checkOrderStatus()`** — ensures that if the Activity is re-entered a second time (e.g. after a rotation in a non-portrait orientation, which is blocked anyway), it won't re-check an already-handled order.
- **`@Volatile hasReturned`** — `shouldInterceptRequest` runs on a background WebView thread; the volatile flag ensures the main thread sees the update immediately before `finish()` is posted.
- **`isNavigating` flag in `PaymentStatusActivity`** — prevents the 5 s auto-finish handler and a back-press arriving in the same frame from both calling `goToWallet()` and double-starting `LoadWalletActivity`.
