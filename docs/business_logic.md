# PayTouch Consumer — Business Logic Reference

> **Current phase:** API wiring in progress. Core modules have live API integration. Field names and endpoint paths listed here are authoritative; do not invent your own.

---

## 1. Authentication

### What It Does
Users register with phone/email/password, then log in via password or MPIN. A Bearer token is issued on success and saved locally. The token is sent with every subsequent API request.

### Rules and Constraints
- Mobile number must be exactly 10 digits
- Password must be at least 8 characters
- Email must pass standard email format validation
- Both password and MPIN login share the same `/api/login` endpoint; the payload differs
- On login, the server response includes two boolean flags that drive routing:
  - `requires_kyc = true` → send to `KycActivity`
  - `requires_mpin = true` → send to `ResetMpinActivity` (create-mode, via `buildCreateIntent()`)
  - Both false → send to `HomeActivity`
  - **Note:** `requires_virtual_account` is permanently retired. Virtual account creation is now handled server-side after KYC approval. Do not add routing for this flag.

### API Endpoints
| Method | Path | Purpose |
|---|---|---|
| POST | `api/login` | Login with mobile + password or MPIN |
| POST | `api/register` | Create new account |
| GET | `api/user` | Fetch current user profile and onboarding flags |
| POST | `api/logout` | Invalidate session on server |

### Request Fields (Login)
- `mobile` (String, required)
- `password` (String, required when using password mode)
- `mpin` (String, required when using MPIN mode)

### Request Fields (Register)
- `mobile` (String, required, 10 digits)
- `email` (String, required, valid format)
- `password` (String, required, min 8 chars)
- `password_confirmation` (String, required, must match password)
- `referral_code` (String, optional)

### Response Fields (Login / Register)
- `token` (String) — Bearer token
- `token_type` (String) — always "Bearer"
- `user.id` (Int)
- `user.mobile` (String)
- `user.email` (String)
- `user.wallet_balance` (Decimal)
- `user.requires_kyc` (Boolean)
- `user.requires_mpin` (Boolean)

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

**Screens:** `onboarding/kyc/KycActivity` (hub) → `onboarding/kyc/identity/IdentityVerificationActivity` + `onboarding/kyc/bank/BankDetailsActivity`

### What It Does
KYC is split into two independently-completable sections, tracked by a 0/2 progress card on the hub:
1. **Identity Verification** — a 4-step flow (Details → Aadhaar → PAN → Selfie) collecting identity documents and a selfie.
2. **Bank Details** — 1 to 4 bank accounts with proof-of-account documents.

Both sections must be completed before KYC can be submitted. Once both are done, the hub auto-agrees and navigates to `KycStatusActivity`.

### Rules and Constraints
- PAN card must match regex: `[A-Z]{5}[0-9]{4}[A-Z]{1}`
- Aadhaar number must be exactly 12 digits
- Selfie is captured via the system camera (no CameraX dependency); a captured selfie is required to submit step 4
- Bank Details supports 1-4 accounts; the delete icon on a card is hidden when only one account remains
- Terms & Conditions checkbox is mandatory before Bank Details submission
- Section completion (`identityDone` / `bankDone`) is driven by the API via `KycSectionStatus.UNDER_REVIEW` returned in the KYC status response
- After both sections are under review, `KycActivity` calls the agree endpoint and navigates to `KycStatusActivity` — never directly to Home

### API Endpoints
| Method | Path | Purpose |
|---|---|---|
| GET  | `api/dashboard/kyc` | Fetch current KYC status and section states |
| POST | `api/dashboard/kyc/initiate` | Initiate KYC session |
| POST | `api/dashboard/kyc/section-b` | Submit identity verification (multipart) |
| POST | `api/dashboard/kyc/section-c` | Submit bank details (multipart, 1-4 accounts) |
| POST | `api/dashboard/kyc/agree` | Mark KYC as agreed after all sections submitted |

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
- If either section was already submitted, its hub row should show a completed state (pre-fill pending API wiring)

---

## 3. MPIN (Mobile PIN)

### What It Does
A 4-digit PIN used as an alternative to password login. Created during onboarding, reset via OTP.

### Rules and Constraints
- MPIN must be exactly 4 digits (numeric only)
- MPIN and confirmation must match before submission
- After successful creation, route to `HomeActivity`
- MPIN is never stored locally — only a boolean flag (`mpin_created`) is stored

### Reset Flow
1. Request OTP to registered mobile
2. User enters OTP → OTP verified by server
3. User sets new 4-digit MPIN → submitted to server

### API Endpoints
| Method | Path | Purpose |
|---|---|---|
| POST | `api/mpin/create` | Create new MPIN during onboarding |
| POST | `api/mpin/verify` | Verify MPIN during login |
| POST | `api/mpin/send-otp` | Send OTP to begin MPIN reset |
| POST | `api/mpin/verify-otp` | Verify reset OTP |
| POST | `api/mpin/reset` | Set new MPIN after OTP verified |

---

## 4. Virtual Account

**Handled server-side.** `CreateVirtualAccountActivity` has been removed. Virtual account creation now happens automatically on the backend after KYC is approved. There is no client-side virtual account screen. Do not add routing for `requires_virtual_account`.

---

## 5. Password Management

### Reset Flow
1. User taps "Forgot Password" on Login screen
2. Enter registered mobile → request OTP
3. Enter OTP → verify
4. Set new password

### API Endpoints
| Method | Path | Purpose |
|---|---|---|
| POST | `api/password/send-otp` | Send OTP to registered mobile |
| POST | `api/password/verify-otp` | Verify OTP |
| POST | `api/password/reset` | Set new password |

### Rules
- New password and confirmation must match
- Password must be at least 8 characters, include one uppercase letter, one number, and one special character

---

## 6. Home / Dashboard

**Screen:** `HomeActivity`

### What It Does
The main menu of the app. Displays category tiles that navigate to each payment flow.

### Categories Displayed
1. Electricity → `ElectricityActivity`
2. Gas → `GasActivity`
3. Prepaid (Mobile) → `PrepaidActivity`
4. TV Cable → 📋 Planned
5. DTH → `DthActivity`
6. FASTag → `FastagActivity`
7. Loan → `LoanActivity`
8. My Account → `MyAccountActivity`
9. Municipal Tax → `MunicipalTaxActivity`
10. Load Wallet (bottom action) → `LoadWalletActivity`

### Rules
- On Home load, fetch dynamic Shreefintech token from server → store in `Constant.TOKEN` or SharedPreferences
- Fetch and display current wallet balance
- Logout calls the logout API, clears SharedPreferences, returns to `LoginActivity` with cleared stack

### API Endpoints
| Method | Path | Purpose |
|---|---|---|
| GET | `api/shreefintech-token` | Fetch dynamic payment token |
| GET | `api/wallet/balance` | Fetch current wallet balance |
| POST | `api/logout` | Log out user |

---

## 7. Standard Bill Payment Flow

This flow applies to: Electricity, Gas, Mobile Postpaid, Cable TV, Broadband, FASTag, Loan, Municipal Tax.

### Steps
1. User selects operator from dropdown (fetched from API)
2. User enters consumer/account number
3. App calls `fetch-bill` API → displays outstanding amount, due date, consumer name
4. User confirms and taps Pay
5. Platform fee is calculated and shown
6. Payment processed via `process-payment` API
7. App calls `transaction-status` to get final result
8. Success/failure sound plays
9. Receipt is displayed

> Transaction ID is generated and returned by the backend in the `process-payment` response — never generate it client-side.

### Platform Fee Calculation
```
amount < 1000            → fee = ₹4
1000 ≤ amount ≤ 5000     → fee = ₹8
5001 ≤ amount ≤ 40000    → fee = ₹20
amount > 40000           → fee = ₹30
```

### Electricity API Endpoints (same pattern, replace `electricity` with other category names)
| Method | Path | Purpose |
|---|---|---|
| GET | `api/electricity/operators` | Get operator list |
| POST | `api/electricity/fetch-bill` | Fetch outstanding bill |
| POST | `api/electricity/process-payment` | Process payment |
| POST | `api/electricity/verify` | Verify payment result |
| POST | `api/electricity/transaction-status` | Check transaction status by ID |
| GET | `api/electricity/payment-reports` | Filtered payment reports |
| GET | `api/electricity/latest-payment` | Most recent payment |
| GET | `api/electricity/payment-history` | Full payment history |

### Key Request Fields (Fetch Bill)
- `operator` (String, required)
- `consumer_number` (String, required)

### Key Request Fields (Process Payment)
- `operator` (String, required)
- `consumer_number` (String, required)
- `amount` (Decimal, required)
### Edge Cases
- If `fetch-bill` fails, the Pay button stays disabled
- If `process-payment` returns pending status, show "processing" state and allow re-check

---

## 8. Mobile Prepaid Recharge

### How It Differs from Standard Bill Payment
- **No `fetch-bill` step** — user enters amount directly (or selects a plan)
- Circle (telecom zone) must also be selected
- Plan selection is available (fetches from API after operator + circle selected)

### API Endpoints
| Method | Path | Purpose |
|---|---|---|
| GET | `api/recharge/operators` | Get prepaid operators |
| GET | `api/recharge/plans/{operatorId}/{circleId}` | Get plans for selected operator + circle |
| POST | `api/recharge/process-direct` | Process recharge |

> `transaction-status` and `payment-reports` for Prepaid are not implemented yet — planned as a follow-up ticket, same sequencing as Gas (pay module first, transaction status/report second).

### Response Fields (Operators — `General<T>` wrapped)
- `data[].id` (String), `data[].name` (String), `data[].code` (String)

### Response Fields (Plans — flat)
- `success` (Boolean)
- `plans[].plan_type` (Int), `plans[].amount` (Int), `plans[].description` (String), `plans[].validity` (String), `plans[].talktime` (Double), `plans[].data` (String)
- `operator_id` (String), `circle_id` (String)

### Request Fields (Process Recharge)
- `mobile_no` (String, required)
- `operator_code` (String, required)
- `circle_code` (String, required) — circle value from the fixed circle list, not a server-fetched dropdown
- `amount` (Decimal, required)
- `platform_fee` (Decimal, required)
- `total_payable` (Decimal, required)

### Response Fields (Process Recharge — flat)
- `success` (Boolean), `message` (String), `req_id` (String), `amount` (Decimal), `platform_fee` (Decimal), `total_payable` (Decimal), `status` (String), `transaction_id` (String)

---

## 9. DTH Recharge

### How It Differs
- Has explicit plan selection after operator is chosen
- Uses `process-direct` endpoint (not `process-payment`)

### API Endpoints
| Method | Path | Purpose |
|---|---|---|
| GET | `api/dth/operators` | Get DTH operators |
| GET | `api/dth/plans` | Get plans for selected operator |
| POST | `api/dth/process-direct` | Process recharge |
| POST | `api/dth/transaction-status` | Check status |

---

## 10. FASTag Recharge

### How It Differs
- **No `fetch-bill` step** — user enters vehicle number, operator, and amount directly (same style as Prepaid/DTH)
- Uses `api/fastag` (not `process-payment`) to process the recharge

### API Endpoints
| Method | Path | Purpose |
|---|---|---|
| GET | `api/fastag/operators` | Get FASTag operators |
| POST | `api/fastag` | Process recharge |

> `transaction-status`, `payment-report`, and SMS receipt for FASTag are not implemented yet — planned as a follow-up ticket.

### Request Fields (Process Recharge)
- `vehicle_number` (String, required)
- `operator` (String, required), `operator_name` (String, required)
- `circle` (String, required)
- `amount` (Decimal, required), `platform_fee` (Decimal, required), `total_payable` (Decimal, required)

### Response Fields (Process Recharge — flat)
- `success` (Boolean), `message` (String), `req_id` (String), `status` (String), `transaction_id` (String)

---

## 11. Wallet

### Rules
- Balance is fetched fresh from the API on every Home/dashboard load
- `Load Wallet` opens `LoadWalletActivity` which routes to the HDFC payment WebView

### API Endpoints
| Method | Path | Purpose |
|---|---|---|
| GET | `api/wallet/balance` | Get current wallet balance |
| GET | `api/transactions` | Get unified paginated transaction history |
| POST | `api/hdfc/create-order` | Create HDFC payment order for wallet top-up |
| GET | `api/wallet/transactions` | Paginated wallet transaction history (Load Wallet screen) |
| GET | `api/hdfc/order-status/{orderId}` | Check HDFC order status after returning from WebView |
| GET | `api/kyc/my-account` | Fetch full KYC details for `KycDetailsActivity` |

### Response Fields (Balance)
- `data.balance` (Decimal) — current wallet balance

---

## 11a. Load Wallet (HDFC Gateway Flow)

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

## 12. Transaction History & Reporting

### Rules
- Transaction history is fetched from the API — no local database is used
- Each module has its own paginated report endpoint (`api/{category}/payment-reports`)
- Recent transactions use `api/{category}/payment-history` (most recent records)
- Search can be done by transaction ID or consumer number
- Report filtering: category, consumer number, status (Success/Failed/Processing), from-date, to-date
- `TransactionDetailActivity` is shared across all modules; detail is passed via `Gson().toJson(TransactionItem)`

---

## 13. Transaction ID

### Ownership
Transaction IDs are **generated and returned by the backend** in the `process-payment` (or `process-direct`) response. The client must never generate a transaction ID before or during the API call.

### Rules
- Do not pass a `transaction_id` field in any payment request DTO
- Read the transaction ID from the API response and store it for status lookup and receipts
- `Utility.generateTransactionId()` was removed — do not recreate it
- The ID format (`PYTCH…`) is set by the server; the client treats it as an opaque string

---

## 14. Session Management

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
- `isLoggedIn()` = token is not null AND userId > 0

---

## 15. VPS Backend Synchronization

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

## 16. Internet Connectivity

### Rules
- `Utility.isInternetAvailable(context)` must be called before every API request
- If no internet: show message via `ToastUtil`, do not make the API call

---

## 17. Payment Sound Feedback

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
