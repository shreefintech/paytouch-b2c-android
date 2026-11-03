# PayTouch Consumer — Business Logic Reference

> All core modules are fully implemented with live API integration. Field names and endpoint paths listed here are authoritative; do not invent your own.

---

## 1. Authentication

### What It Does
Users register with phone/email/password, then log in via password or MPIN. A Bearer token is issued on success and saved locally. The token is sent with every subsequent API request.

### Rules and Constraints
- Mobile number must be exactly 10 digits
- Password must be at least 8 characters
- Email must pass standard email format validation
- Auth + onboarding endpoints are declared in `ApiService` with the `MOBILE` prefix (`https://backend-b2c.paytouch.in/api/mobile/`) and called via `ApiClient.apiService`. Other modules still use the old `api/` prefix — pending migration to new routes.
- Both password and MPIN login share `mobile/auth/login`; `type` is `Constant.CREDENTIAL_TYPE_PASSWORD` or `Constant.CREDENTIAL_TYPE_MPIN`
- All auth responses use the `General<T>` envelope (`{success, message, data}`) — check `isSuccessful && body?.data != null`
- Routing after login / Splash / MPIN creation goes through **`NextStepRouter.intentFor(context, next_step, mpin_required)`** — never route inline:
  - `kyc_required` → `KycActivity` (full KYC flow). Home's `auth/me` check also redirects here if it sees this step.
  - `pending_approval`, `registration_rejected` → `KycStatusActivity`
  - `aadhaar_required` → `HomeActivity`, whose `auth/me` check shows the blocking Aadhaar dialog in **add mode** (Aadhaar number + photo + state + city → `kyc/individual/aadhaar`; afterwards `next_step` is `bank_details_required`)
  - `kyc_resubmit_required` → `HomeActivity`; the dialog opens in **resubmit mode** (`kyc/individual/resubmit`) only when `kyc_rejection.items` has both `aadhaar_number` and `aadhaar_photo` (`MeDataItem.canResubmitAadhaar`). Resubmit 409s with no open rejection and 422s on a field that was not rejected.
  - A 403 whose `error_code` is one of these steps (`NextStep.isKycStep`) is routed the same way by `ApiHelper.onForbidden()`
  - else `mpin_required = true` → `ResetMpinActivity` (create-mode, via `buildCreateIntent()`)
  - else (`bank_details_required`, `complete`, null or unknown value) → `HomeActivity`. Unknown values fall through to Home **deliberately**.
  - `bank_details_required` also forces `KEY_BANK_DETAILS_ADDED = false` (see `saveOnboardingFlags()`), so the Add Bank Details popup shows before the first money action.
  - **Note:** `requires_virtual_account` is permanently retired. Virtual account creation is now handled server-side after KYC approval. Do not add routing for this flag.

### API Endpoints (`ApiService`, prefix `api/mobile/`)
| Method | Path | Purpose |
|---|---|---|
| POST | `auth/login` | Login with mobile + password or MPIN → `General<LoginDataItem>` |
| POST | `auth/register` | Create new account → `General<LoginDataItem>` |
| POST | `auth/register/individual` | V2 multipart registration (Aadhaar + address) → `MessageItem` |
| GET | `auth/me` | Current user + onboarding state → `General<MeDataItem>` (Splash session check, Home Aadhaar check, My Account profile) |
| PUT | `auth/me` | Edit Profile (My Account) — `EditProfileRequest`, MPIN required → `MessageItem` (`isSuccessful && success == true`) |
| GET | `wallet/balance` | All wallet buckets → `General<WalletBalanceItem>`; My Account shows `main_wallet` |
| GET | `rewards/referral` | Referral history (`data`, not shown yet) + totals in `meta.summary` (`ReferralDataItem`) → flat `ReferralRewardsItem` (`isSuccessful && success == true`) |
| POST | `auth/logout` | Invalidate session on server |
| POST | `device-token`, `location` | FCM token / user location |
| GET | `address/states`, `address/states/{state_id}/cities` | Registration address pickers |
| POST | `kyc/individual/resubmit` | Aadhaar re-upload after rejection (rejected items only) |
| POST | `kyc/individual/aadhaar` | `aadhaar_required` only — multipart `aadhaar_number`, `aadhaar_photo`, `state_id`, `city_id` → `MessageItem`. 409 Aadhaar already on file / KYC approved; 422 not linked to Dashboard or Aadhaar used by another account; 429 double tap |
| POST | `kyc/individual/bank-details`, `kyc/individual/pan` | Bank / PAN onboarding dialogs |

### Request Fields (Login — `LoginRequest`, JSON)
- `mobile` (String, required)
- `credential` (String, required — password or MPIN)
- `type` (String, required — `password` | `mpin`)

### Request Fields (Register — `RegisterRequest`, JSON)
- `name` (String, required)
- `mobile` (String, required, 10 digits)
- `email` (String, required, valid format)
- `password` (String, required, min 8 chars)
- `password_confirmation` (String, required, must match password)
- `referral_code` (String, optional)
- `platform` (String — `Constant.FCM_PLATFORM_ANDROID`)

### Response Fields (Login / Register — `data`)
- `token` (String) — Bearer token
- `token_type` (String) — always "Bearer"
- `next_step` (String) — see routing above
- `mpin_required` (Boolean)
- `bank_details_added` (Boolean)
- `pan_added` (Boolean)
- `user` (`AuthUserItem`): `id`, `name`, `mobile`, `email`, `referral_code`, `wallet_balance`, `aadhaar_masked` (short-signup users only)

### Response Fields (`auth/me` and `auth/mpin` — `data`, `MeDataItem`)
- `user` (`AuthUserItem`), `next_step`, `mpin_required`, `bank_details_added`, `aadhaar_added`, `pan_added`, `kyc_rejection` (`{ items: [String], reason }`, kept untyped as `Any?` and read via `MeDataItem.kycRejectedItems`; items are `aadhaar_number` / `aadhaar_photo` / `bank_details` → Home Aadhaar re-upload dialog only when both Aadhaar items are rejected — `MeDataItem.canResubmitAadhaar`; `isAadhaarRejected` (either item) only drives the KYC Details badge)

- `GET auth/me` only, on `user`: `status`, `created_at`, `activated_at`, `state`, `city` — `state`/`city` are plain names (not `{id, name}`), so Edit Profile matches them by name (case-insensitive) against the `address/states` / `cities` lists to get ids; no match → the field resets to its hint

### Request Fields (Edit Profile — `PUT auth/me`, `EditProfileRequest`, JSON)
- `name` (String, required — letters and spaces only)
- `email` (String, required, valid format)
- `state_id` (Int, required), `city_id` (Int, required)
- `mpin` (String, required, 4 digits — collected in `dialog_mpin` on every update; never stored)
- Response `data` is ignored (typed as flat `MessageItem`); on success the screen refetches `auth/me` and stores `email` in `KEY_EMAIL`

### Response Fields (`wallet/balance` — `data`, `WalletBalanceItem`)
- `has_wallet`, `main_wallet`, `qr_load_wallet`, `upi_intent_wallet`, `lock_wallet`, `upi_settlement_wallet`, `aeps_wallet`, `referral_wallet`, `bonus_wallet`, `earning_wallet_principal`, `earning_wallet_interest_accrued`, `updated_at`, `live_balance` (amounts as String)

### Response Fields (`rewards/referral` — `meta.summary`, `ReferralDataItem`)
- `referral_code`, `referral_link`, `total_earnings`, `referral_count`, `earning_potential`

### State Transitions
```
Not Registered → Registered (after register API)
Logged Out → Logged In (after login API + token saved)
Logged In → Logged Out (after logout API + clearAll())
Logged In → Logged Out (automatic, on any 401 response)
```

### Edge Cases
- 401 from any API endpoint → immediate forced logout + LoginActivity (stack cleared)
- If token exists in SharedPreferences at app start, Splash skips login and routes based on onboarding flags

---

## 2. KYC (Know Your Customer)

**Screens:** `kyc/KycActivity` (hub) → `kyc/identity/IdentityVerificationActivity` + `kyc/bank/BankDetailsActivity`

### What It Does
KYC is split into two independently-completable sections, tracked by a 0/2 progress card on the hub:
1. **Identity Verification** — a 4-step flow (Details → Aadhaar → PAN → Selfie) collecting identity documents and a selfie.
2. **Bank Details** — 1 to 4 bank accounts with proof-of-account documents.

Both sections must be completed before KYC can be submitted. Once both are done, the hub auto-agrees and navigates to `KycStatusActivity`.

### Rules and Constraints
- PAN card must match regex: `[A-Z]{5}[0-9]{4}[A-Z]{1}`
- Aadhaar number must be exactly 12 digits
- Selfie is captured in-app by `SelfieCaptureActivity` (CameraX front camera + bundled on-device ML Kit face detection) only after an active liveness check passes: face centred in the circle and looking straight → blink twice (each blink: both eyes open → closed → open within 1.5 s) → hold still, centred, eyes open. If `ImageCapture` fails it retries once, then saves the current live camera frame instead. A second face, lost face or face tracking-id change restarts the check; the whole challenge times out after 20 s. The photo is auto-captured (no manual shutter). A captured selfie is required to submit step 4
- Camera permission is requested at runtime by `SelfieCaptureActivity`; denial or dismissing the popup shows a toast and returns to step 4 with no selfie. Only a denial after an earlier explicit "Deny" (`KEY_CAMERA_DENIED`, survives logout) is treated as "Don't ask again" and opens app settings. If the screen is restored without the permission (e.g. revoked in Settings while backgrounded), it asks again
- The on-device liveness check is a UX gate only — it can be defeated by a replayed video. The backend must still perform its own face match of the selfie against the Aadhaar/PAN photo; never treat the client check as the sole anti-spoofing control
- Bank Details supports 1-4 accounts; the delete icon on a card is hidden when only one account remains
- Terms & Conditions checkbox is mandatory before Bank Details submission
- Section completion (`identityDone` / `bankDone`) is driven by the API via `KycSectionStatus.UNDER_REVIEW` returned in the KYC status response
- After both sections are under review, `KycActivity` calls the agree endpoint and navigates to `KycStatusActivity` — never directly to Home

### API Endpoints
| Method | Path | Purpose |
|---|---|---|
| GET  | `api/dashboard-kyc/status` | Fetch current KYC status and section states |
| POST | `api/dashboard-kyc/initiate` | Initiate KYC session (entity_type = "individual") |
| POST | `api/dashboard-kyc/sections/a` | Submit section A placeholder (has_gst = "0") |
| POST | `api/dashboard-kyc/sections/b/signatory` | Submit identity documents (PAN, Aadhaar ×2, selfie — multipart) |
| POST | `api/dashboard-kyc/sections/c` | Submit bank details (multipart, 1–4 accounts) |
| POST | `api/dashboard-kyc/agree` | Mark KYC as agreed after all sections submitted |

### Request Fields — Identity Verification
- `mobile` (String, required, 10 digits)
- `email` (String, required, email format)
- `aadhaar_number` (String, required, 12 digits)
- `aadhaar_front` / `aadhaar_back` (File, required)
- `pan_number` (String, required, regex validated)
- `pan_front` (File, required)
- `selfie` (File, required)

### Request Fields — Bank Details (per account, 1-4 accounts)
- `account_number` (String, required, 9-18 digits)
- `bank_name` (String, required)
- `ifsc_code` (String, required, regex validated)
- `branch_name` (String, required)
- `proof_type` (String, required)
- `bank_proof` (File, required)

### Edge Cases
- If a section was already submitted, its hub row shows a completed state (derived from `api/dashboard-kyc/status` response)

---

## 3. MPIN (Mobile PIN)

### What It Does
A 4-digit PIN used as an alternative to password login. Created during onboarding, reset via OTP.

### Rules and Constraints
- MPIN must be exactly 4 digits (numeric only)
- MPIN and confirmation must match before submission
- After successful creation, route again via `NextStepRouter` using the response's `next_step` (the response has the `auth/me` shape, with `mpin_required = false`)
- MPIN is never stored locally — only a boolean flag (`mpin_created`) is stored

### Reset Flow (shared with password — see §5)
1. `auth/forgot-credential` with `type = mpin` → OTP sent
2. `auth/forgot-credential/verify-otp` → server returns `data.reset_token`
3. `OtpVerificationActivity` passes the token to `ResetMpinActivity` as `Constant.EXTRA_RESET_TOKEN` (the mobile number is **not** passed on)
4. `auth/forgot-credential/reset` with `reset_token`, `type = mpin`, `credential` = new MPIN

### API Endpoints (`ApiService`, prefix `api/mobile/`)
| Method | Path | Purpose |
|---|---|---|
| POST | `auth/mpin` | Create new MPIN during onboarding → `General<MeDataItem>`; 422 if already set |
| POST | `auth/forgot-credential` | Send reset OTP (`mobile`, `type`) → `MessageItem` |
| POST | `auth/forgot-credential/verify-otp` | Verify OTP (`mobile`, `otp`) → `General<ResetTokenDataItem>` |
| POST | `auth/forgot-credential/reset` | Set new credential (`reset_token`, `type`, `credential`) → `MessageItem` |

---

## 4. Virtual Account

**Handled server-side.** `CreateVirtualAccountActivity` has been removed. Virtual account creation now happens automatically on the backend after KYC is approved. There is no client-side virtual account screen. Do not add routing for `requires_virtual_account`.

---

## 5. Password Management

### Reset Flow
1. User taps "Forgot Password" on Login screen
2. Enter registered mobile → request OTP
3. Enter OTP → verify → server returns `reset_token`
4. Set new password (sent with `reset_token`, received via `Constant.EXTRA_RESET_TOKEN`)

### API Endpoints
Same unified `auth/forgot-credential` endpoints as MPIN reset (§3), with `type = password`.

### Rules
- New password and confirmation must match
- Password must be at least 8 characters, include one uppercase letter, one number, and one special character

---

## 6. Home / Dashboard

**Screen:** `HomeActivity`

### What It Does
The main menu. Bill tiles come from `GET api/mobile/billpay/categories` — a tile shows only when its category is listed
(only categories with at least one active biller are listed). My Account and Load Wallet always show.

### Tiles → `CategoryType` (`enums/CategoryType.kt`, `value` = backend `category`)
| Tile | `category` | Opens |
|---|---|---|
| Electricity | `electricity` | `BillPayActivity` (fetch bill) |
| Gas | `gas` | `BillPayActivity` (fetch bill) |
| Prepaid | `prepaid` | `BillPayActivity` (plans, with circle) |
| Postpaid | `mobile_postpaid` | `BillPayActivity` (fetch bill) |
| DTH | `dth` | `BillPayActivity` (plans, no circle) |
| FASTag | `fastag` | "Coming soon" toast — no route yet (same as iOS) |
| Loan | `loan_repayment` | `BillPayActivity` (fetch bill) |
| Municipal Tax | `municipal_taxes` | `BillPayActivity` (fetch bill) |
| My Account | — | `MyAccountActivity` (always shown) |

### Rules
- Listed categories with no tile (`broadband_postpaid`, `water`, `insurance`, …) are ignored.
- Visible tiles are re-flowed into rows of three (`HomeActivity.layoutCategoryTiles()`), in the layout's order.
- Bill tiles stay hidden until the first response. If the first load fails, a Retry row replaces them. The error toast
  shows only on the first load and on Retry; the silent refresh on every `onResume()` keeps the last good tiles.
- `POST api/mobile/location` is sent on Home open (`LocationPermissionHelper` → `sendLocation()` in `ViewModelExt.kt`).
- Logout calls the logout API, clears SharedPreferences, returns to `LoginActivity` with cleared stack.

### API Endpoints
| Method | Path | Purpose |
|---|---|---|
| GET | `api/mobile/billpay/categories` | Which bill tiles to show — `General<List<BillPayCategoryItem>>` |
| POST | `api/mobile/location` | Location for payment risk checks (required before pay) |
| POST | `api/mobile/auth/logout` | Log out user |

---

## 7. Bill Pay (every bill category — one shared flow)

Source: the iOS "Bill Pay Module — Dashboard to Receipt" doc. Every category uses **one screen** (`billpay/BillPayActivity`,
`activity_bill_pay.xml`) and the same APIs; the only branch is how the amount is found:
**bill categories fetch a bill, prepaid and DTH pick a plan** (`CategoryType.isPlanBased`, `needsCircle`).

All routes are on `backend-b2c.paytouch.in/api/mobile/` (`ApiService`, `MOBILE` prefix), with `Authorization: Bearer`.
Envelope `{success, message, meta, data}` → `General<T>`, success = `isSuccessful && data != null`
(except `transactions`, see below). Models: `retrofit/model/billpay/`.

### Steps
1. **Company** — `GET billpay/utility-billers?category=X` → `List<BillPayBillerItem>`. Picked via `OperatorSelectionActivity`.
   `id` is sent later as `biller_id` (as a string).
2. **Account fields** — one field per `input_params` entry, keyed by `param_name` (else `name` / `key`), labelled by its
   `label` (else `CategoryType.fieldLabelRes`). `input_params: []` → one field keyed by `CategoryType.defaultParamName`
   (`consumer_number` electricity/gas; `mobile_number` prepaid/postpaid/DTH). No default (loan, municipal tax, FASTag)
   → "This company's details couldn't be loaded" and the biller can't be paid. `mobile_number` = 10 digits.
3. **Amount**
   - Bill categories: `POST billpay/fetch-bill` `{biller_id, input_params}` → `BillPayBillItem`. `has_bill: false` →
     "No bill found", no bill card. Found → bill card (customer_name, due_date, bill_date, amount) and the Amount field.
     `enquiry_ref` is not shown. Editing an account field clears the fetched bill.
   - Prepaid: `GET billpay/circles` → `List<BillPayCircleItem>`; `code` is the 2-digit `circle_code`.
   - Prepaid / DTH: `GET billpay/utility-billers/{id}/plans?circle_code=05` (circle_code left out for DTH) →
     `List<BillPayPlanItem>` in `BillPayPlanSelectionActivity`. Picking a plan fills the Amount field.
4. **Fee** — `GET billpay/fee-preview?amount=500` → `BillPayFeePreviewItem`. Platform Fee row = `fee + gst_amount`,
   Total Payable row = `total`. Re-fetched (400 ms debounce) when the amount changes, kept only for the amount it was
   asked for. Proceed is blocked until the preview matches the current amount. No client-side fee table.
5. **Pay** — Proceed checks bank details (`BankDetailsDialogHelper.runIfBankAdded`), terms, then the MPIN popup
   (`dialog_mpin.xml`, `MpinDialogExt.kt`). Submit → `POST billpay/pay` (HTTP 201):
   `{biller_id, input_params (+ circle_code for prepaid), amount (string), mpin, request_id}`. No separate MPIN verify,
   no client wallet/VPS balance check — the server decides.
   - `request_id` is an idempotency key (`MoneyRequestId`), new per attempt, reused while the same attempt
     (biller + params + amount) is retried after an unknown outcome. It is **not** a transaction id.
   - Response = same shape as `billpay/status` → read `client_reference` and open `BillPaymentStatusActivity`.
6. **Status** — `BillPaymentStatusActivity` polls `GET billpay/status/{client_reference}` every 2.5 s
   (`Constant.BILLPAY_POLL_INTERVAL_MS`) until `is_final`, capped at `BILLPAY_POLL_MAX_ATTEMPTS` (~60 s, then it stays on
   "Payment Processing"). 5 s after a final status it opens the receipt.

| `status` | Final? | Result screen |
|---|---|---|
| `success` | Yes | Payment Successful → receipt |
| `failed`, `expired` | Yes | Payment Failed, with `failure_reason` when sent |
| `pending`, `processing`, `unconfirmed` | No | Keep polling; "Payment Processing" if it stays open |

### Status / transaction row fields (`BillPayTransactionItem`)
Read: `client_reference`, `status`, `is_final`, `amount`, `charge + gst_amount` (platform fee), `biller_name`,
`input_params` (account = first value other than `circle_code`), `identifier_label` (account label; `null` for imported
old payments), `provider_ref`, `txn_reference` (reference fallback), `failure_reason`, `created_at`.
Not read yet: `payment_breakdown`, `level`, `tds`. No customer name and no numeric id in this shape.

### Receipt, Recent, Transaction Status, Report — `GET transactions`
`GET transactions?category=X&page=1&per_page=20` (optional `status`; `per_page` 1–50). Bills and recharges only,
newest first. Flat body → `BillPayTransactionPageItem` (`success`, `data` rows, `meta.current_page / last_page`).
Shared VM: `BillPayTransactionsViewModel` (paging + `mapToTransactionItem()` / `mapToDisplayItem()`).

| Screen (`billpay/transactions/`) | Calls | Notes |
|---|---|---|
| `SmsReceiptActivity` (form tab, no ref) | `transactions?category=X&per_page=1` → first row's `client_reference` → `billpay/status/{ref}` | Latest payment |
| `SmsReceiptActivity` (row / result) | `billpay/status/{client_reference}` | Reference row = `provider_ref`, else `client_reference` |
| `RecentTransactionActivity` | `transactions?category=X` | Paged by `meta.current_page < last_page` |
| `TransactionStatusActivity` | `transactions?category=X` | Tabs All / Success / Failed / Pending: success / failed sent as `status`; pending (pending, processing, unconfirmed) filtered in the app |
| `TransactionReportActivity` | `transactions?category=X` | Success / Failed sent as `status`; Pending, date range and account number filtered in the app |

`GET transactions/pending` exists on the backend but no screen uses it yet.

### Errors
403 has a top-level `error_code`; 422 has messages under `data.<field>` (`ApiHelper.parseErrorMessage()` shows the
first one). `ApiHelper.parseErrorCode()` reads `error_code`.

| Error | When | What the app does |
|---|---|---|
| 403 `BANK_DETAILS_REQUIRED` | Pay with no bank details | Clears `KEY_BANK_DETAILS_ADDED`, opens the Add Bank Details popup; Save retries Proceed to Pay |
| 403 `LOCATION_REQUIRED` | Pay with no location this login | Toast, resends location (`LocationPermissionHelper`); the user taps Pay again |
| 403 `AADHAAR_REQUIRED` / `KYC_RESUBMIT_REQUIRED` | KYC missing / rejected | `ApiHelper` opens the KYC step via `NextStepRouter` |
| 403 `USER_BLOCKED` | Account blocked | `ApiHelper` signs out to Login |
| 401 | Session expired | `SessionInterceptor` signs out to Login |
| 422 | Bad input (amount, params, MPIN) | Toast with the message; form and MPIN popup stay |
| Timeout / 5xx on pay | Outcome unknown | "Not confirmed" toast pointing to Recent Transactions, form resets; never shown as failed |
| No internet | Before sending | No-internet toast or screen; nothing sent |

### Open backend questions (from the iOS doc, 2026-11-03)
- `billpay/pay` doesn't complete on staging yet; staging electricity billers (BESCOM 33, PGVCL 34) send `input_params: []`.
- Gas, prepaid, postpaid, DTH, loan and municipal tax have no staging billers yet → their tiles are hidden.
- No real `billpay/status` sample after a completed payment yet (with `payment_breakdown`, `level`). Sample values in
  the iOS doc are illustrative.
- Live API (`www.paytouch.in/api/mobile/`) answers 404 today.

---

## 8. Wallet

### Rules
- Balance is fetched fresh from the API on every Home/dashboard load
- `Load Wallet` opens `LoadWalletActivity` which routes to the HDFC payment WebView
- Earning Wallet lock: minimum `Constant.EARNING_MIN_LOCK_AMOUNT` (₹100), maximum = main wallet balance. Below-minimum is rejected on tap of Lock (toast), not shown live while typing; over-balance shows an inline error and disables the button.
- Earning Wallet lock requires PAN: when `KEY_PAN_ADDED` is false (or unset), tapping Lock Money shows the PAN dialog first — for opted-in users too. Submitting PAN continues to the opt-in or lock sheet; closing the dialog opens nothing.
- Bank/PAN onboarding flags (`KEY_BANK_DETAILS_ADDED`, `KEY_PAN_ADDED`) are set via `saveOnboardingFlags()` from `auth/login` (bank only), `auth/me` (Splash + Home) and `auth/mpin`, and read with default `false` — if none has answered yet, the bank/PAN dialog is shown rather than skipped. `next_step = bank_details_required` forces the bank flag to `false`.

### API Endpoints
| Method | Path | Purpose |
|---|---|---|
| GET | `api/wallet/balance` | Get current wallet balance |
| GET | `api/transactions` | Get unified paginated transaction history |
| POST | `api/hdfc/orders` | Create HDFC payment order for wallet top-up |
| GET | `api/wallet/transactions` | Paginated wallet transaction history (Load Wallet screen) |
| GET | `api/hdfc/orders/{order_id}/status` | Check HDFC order status after returning from WebView |
| GET | `api/dashboard-kyc/my-account` | Fetch full KYC details for `KycDetailsActivity` |

### Response Fields (Balance)
- `data.balance` (Decimal) — current wallet balance

---

## 8a. Load Wallet (HDFC Gateway Flow)

### What It Does
User enters a top-up amount, the app creates an HDFC payment order, then opens `HdfcWebViewActivity` with the payment URL. On payment completion, HDFC redirects to a return URL which the WebView intercepts — routing to `PaymentStatusActivity`.

### Screens
| Screen | Class | Purpose |
|---|---|---|
| Wallet Balance + Top-up Form | `LoadWalletActivity` | Shows balance, enter amount, create order |
| HDFC Payment WebView | `HdfcWebViewActivity` | Renders HDFC payment page |
| Payment Status | `PaymentStatusActivity` | Shows success/failure, auto-navigates back |
| Full Transaction History | `WalletTransactionsActivity` | Paginated list of all wallet transactions |

### Rules
- `LoadWalletActivity` is `singleTop` (prevents duplicate stacking from HDFC redirect)
- `HdfcWebViewActivity` intercepts the return URL and finishes, handing control back
- `PaymentStatusActivity` auto-navigates to `LoadWalletActivity` after 5 s or on back press
- Wallet transaction history is paginated via `api/wallet/transactions`

---

## 9. Transaction History & Reporting

### Rules
- Transaction history is fetched from the API — no local database is used
- Bill payments: one endpoint for every category — `GET api/mobile/transactions?category=` (see §7)
- `TransactionDetailActivity` is shared across all categories; detail is passed via `Gson().toJson(TransactionItem)`;
  its View Receipt opens `billpay/transactions/SmsReceiptActivity` with the row's `client_reference`
- Wallet history (`TransactionHistoryDetailActivity`) is separate — see §8 / §8a

---

## 10. Transaction ID / client_reference

Bill payments are identified by the server's `client_reference`, returned by `billpay/pay` and used for
`billpay/status/{client_reference}` and receipts. Never generated client-side (`Utility.generateTransactionId()` was
removed — do not recreate). The only client-made id is `request_id` — an idempotency key (`MoneyRequestId`), not a
transaction id. Treat every server id as an opaque string.

---

## 11. Session Management

### SharedPreferences Store Map
| Store | Key | Type | Purpose |
|---|---|---|---|
| `AUTH` | `TOKEN` | String | Bearer token |
| `AUTH` | `USERID` | Int | User ID |
| `AUTH` | `EMAIL` | String | User email |
| `AUTH` | `MOBILE` | String | Mobile number |
| `AUTH` | `TOKEN_TYPE` | String | Token type |
| `AUTH` | `WALLET_BALANCE` | String | Cached wallet balance |
| `AUTH` | `ReferralCode` | String | Referral code |
| `AUTH` | `PENDING_ORDER_ID` | String | HDFC order ID in progress (cleared after status check) |
| `AUTH` | `PENDING_AMOUNT` | String | HDFC payment amount in progress (cleared after status check) |
| `app_prefs` | `mpin_created` | Boolean | MPIN setup complete |

### Rules
- Token is checked at Splash; no token → go to Login
- Any 401 response → clear ALL SharedPreferences → launch LoginActivity with `FLAG_ACTIVITY_NEW_TASK or FLAG_ACTIVITY_CLEAR_TASK`
- **App lock (replaces the old 10-minute idle logout):** `applock/AppLockHelper` (registered in `MyApp`) shows `AppLockActivity`, which asks for the phone's own screen lock (fingerprint / face / PIN / pattern via `BiometricPrompt`, `BIOMETRIC_WEAK or DEVICE_CREDENTIAL`). The app does **not** log out; only a 401 does.
  - Locks on a cold start with a saved session, and when the app returns from the background after more than `APP_LOCK_GRACE_MS` (1 min).
  - When the app itself opened another app (camera, file picker, UPI app, HDFC gateway, share, browser), the grace period is `APP_LOCK_EXTERNAL_GRACE_MS` (5 min). `BaseActivity.startActivityForResult()` detects these launches, so no per-screen code is needed.
  - Time away is measured with `SystemClock.elapsedRealtime()`, so changing the phone clock can't skip the lock.
  - Exempt screens: Splash and the auth flow (Login, Create Account, OTP, Reset Password, Reset MPIN). A credential login (`LoginViewModel.saveSession()`) counts as unlocked.
  - Lock screen (GPay-style): `AppLockActivity` is a transparent window, so the screen the user left stays visible behind the system prompt but cannot be touched. Cancel dims it and shows a bottom Unlock panel; Back closes the app. It must not set `screenOrientation` (a translucent Activity with a fixed orientation crashes on API 26). If the phone has no screen lock set, the lock is skipped.
- `isLoggedIn()` = token is not null AND userId > 0

---

## 12. VPS Backend Synchronization

### What Gets Synced
| Event | VPS Action |
|---|---|
| User login / register | Register/update user on VPS (`admin.paytouch.in`) |
| KYC submission | Sync KYC fields to VPS |
| Every payment | Log transaction to VPS |
| Home screen open | Refresh user profile from VPS |
| KYC approval | Virtual account created server-side automatically |

### Rules
- VPS sync failures are **non-blocking** — the main flow continues even if VPS call fails
- VPS failures must be **logged** for debugging
- User ID on VPS matches the user ID from the main API

---

## 13. Internet Connectivity

`Utility.isInternetAvailable(context)` before every API request; offline → `ToastUtil` message, no call. Full offline UX: `docs/no_internet_handling.md`.

---

## 14. Payment Sound Feedback

### Rules
- `payment_success.mp3` plays on successful payment
- `payment_failed.mp3` plays on failed payment
- Call `cleanup()` (or release MediaPlayer) in `onDestroy()` to avoid leaks

---

## Known Issues to Resolve Before Release

**Issue 1 — QR payment URL:**
The Dynamic QR payment feature uses a `ngrok-free.dev` tunnel URL. This will break in production. Replace with a stable production URL before any live deployment.

**Issue 2 — Static Shreefintech token field:**
The dynamic token from `api/shreefintech-token` is currently stored as a static field (`Constant.TOKEN`). This is a race condition risk in multi-thread scenarios. Move this to `SharedPreferenceHelper` or a Repository before release.

**Issue 3 — Legacy MobiKwik API:**
Some payment flows may route through `dashboard.shreefintechsolutions.com/api/mobikwik/`. Audit each endpoint before wiring — determine which are still active and which are replaced by `paytouch.in` equivalents.
