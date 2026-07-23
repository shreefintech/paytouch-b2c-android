# PayTouch Consumer — Business Logic Reference

This document is the authoritative source of truth for every domain rule, flow, and constraint extracted from the existing codebase. Use this before implementing any feature.

---

## 1. Authentication

### What It Does
Users register with phone/email/password, then log in via password or MPIN. A Bearer token is issued on success and saved locally. The token is sent with every subsequent API request.

### Rules and Constraints
- Mobile number must be exactly 10 digits
- Password must be at least 8 characters
- Email must pass standard email format validation
- Both password and MPIN login share the same `/api/login` endpoint; the payload differs
- On login, the server response includes three boolean flags that drive routing:
  - `requires_kyc` → send to KYCActivity
  - `requires_mpin` → send to MpinActivity
  - `requires_virtual_account` → send to VirtualAccountActivity
  - All false → send to DashboardCategoryActivity
- "Remember Me" saves the mobile number to LoginPreferences so it pre-fills on next open
- Login type preference (password vs. MPIN) is also persisted between sessions
- The app also registers the user on the VPS backend (`admin.paytouch.in`) immediately after login/registration to keep both systems in sync

### API Endpoints
| Method | Path | Purpose |
|---|---|---|
| POST | `api/login` | Login with mobile, password or mpin |
| POST | `api/register` | Create new account |
| GET | `api/user` | Fetch current user profile and flags |
| POST | `api/logout` | Invalidate session on server |

### Request Fields (Login)
- `mobile` (String, required)
- `password` (String, required when using password mode)
- `mpin` (String, required when using MPIN mode)

### Request Fields (Register)
- `name` (String, required)
- `mobile` (String, required, 10 digits)
- `email` (String, required, valid format)
- `password` (String, required, min 8 chars)
- `password_confirmation` (String, required, must match password)
- `referral_code` (String, optional)

### Response Fields (Login)
- `token` (String) — Bearer token
- `token_type` (String) — always "Bearer"
- `user.id` (Int) — user ID
- `user.mobile` (String)
- `user.email` (String)
- `user.requires_kyc` (Boolean)
- `user.requires_mpin` (Boolean)
- `user.requires_virtual_account` (Boolean)

### State Transitions
```
Not Registered → Registered (after register API)
Logged Out → Logged In (after login API + token saved)
Logged In → Logged Out (after logout API + clearAll())
Logged In → Logged Out (automatic, on any 401 response from server)
```

### Edge Cases
- 401 from any API endpoint at any time → immediate forced logout + LoginActivity
- If token exists in SharedPreferences at app start, the Splash screen skips login and routes based on onboarding flags
- If the VPS registration call fails, the main login still proceeds (non-blocking)

---

## 2. KYC (Know Your Customer)

**Screen:** `onboarding/UploadKycActivity` + `onboarding/viewmodel/UploadKycViewModel`

### What It Does
Collects personal identity information and submits it to the server for verification. This unlocks the full account.

### Rules and Constraints
- PAN card must match the regex pattern: `[A-Z]{5}[0-9]{4}[A-Z]{1}` (5 uppercase letters, 4 digits, 1 uppercase letter)
- Aadhaar number must be exactly 12 digits
- GST number is **optional** — validate format only when the field is non-empty; never block submission if blank
- Date of birth is selected via a date picker (not typed)
- Age is auto-calculated from the selected DOB
- KYC data is also synced to the VPS backend after successful submission to the main API
- After successful KYC, the app routes to MPIN creation (never skips to dashboard)
- KYC data (name, city, PAN, Aadhaar, referral code) is cached in SharedPreferences under the key `AUTH` for use elsewhere in the app

### API Endpoints
| Method | Path | Purpose |
|---|---|---|
| GET | `api/kyc/account-info` | Pre-fill form with existing KYC data if any |
| POST | `api/kyc/submit` | Submit KYC data |

### Request Fields (Submit)
- `mobile_no` (String, required)
- `member_name` (String, required)
- `birth_date` (String, required, date format)
- `age` (Int, required, auto-calculated)
- `home_address` (String, required)
- `city_name` (String, required)
- `email` (String, required)
- `pan_card_no` (String, required, format validated)
- `aadhaar_no` (String, required, 12 digits)
- `gst_no` (String, optional)

### Edge Cases
- If KYC was already partially submitted, `account-info` pre-fills the form
- The app submits to two backends (PayTouch + VPS); if either fails, an error is shown

---

## 3. MPIN (Mobile PIN)

### What It Does
A 4-digit PIN that can be used as an alternative to password login. Created once during onboarding and can be reset via OTP.

### Rules and Constraints
- MPIN must be exactly 4 digits (numeric only)
- MPIN and MPIN confirmation must match before submission
- After successful creation, `mpin_created = true` is saved in SharedPreferences (`app_prefs`)
- After MPIN creation, the app routes to Virtual Account creation (never to dashboard directly)

### Reset Flow
1. User requests OTP → OTP sent to registered mobile
2. User enters OTP → OTP verified by server
3. User sets new 4-digit MPIN → submitted to server

### API Endpoints
| Method | Path | Purpose |
|---|---|---|
| POST | `api/mpin/create` | Create new 4-digit MPIN |
| POST | `api/mpin/verify` | Verify MPIN during login |
| POST | `api/mpin/send-otp` | Send OTP to reset MPIN |
| POST | `api/mpin/verify-otp` | Verify reset OTP |
| POST | `api/mpin/reset` | Set new MPIN after OTP verified |

### Request Fields (Create)
- `mpin` (String, 4 digits)
- `mpin_confirmation` (String, must match mpin)

### Edge Cases
- If MPIN creation API fails, the user stays on the MPIN screen; they cannot proceed
- MPIN is never stored locally — only a boolean flag indicating it was created is stored

---

## 4. Virtual Account

**Screen:** `onboarding/CreateVirtualAccountActivity` + `onboarding/viewmodel/CreateVirtualAccountViewModel`

### What It Does
Collects the user's banking details and supporting documents. This completes the onboarding sequence and unlocks all payment features.

### Rules and Constraints
- Required text fields: name, mobile, city, aadhaar, PAN, bank account number, IFSC code, UPI ID, branch name
- Required file uploads: Aadhaar front image, Aadhaar back image, PAN image, bank proof image
- State is selected from a dropdown (fixed list of Indian states)
- All four file uploads are required; submission is blocked if any is missing
- After successful submission, `virtual_account = true` is saved in SharedPreferences (`app_prefs`)
- After Virtual Account creation, the app routes to DashboardCategoryActivity (onboarding complete)

### API Endpoints
| Method | Path | Purpose |
|---|---|---|
| POST | `api/virtual-account/create` | Submit VA details as multipart form |

### Request Fields (Multipart)
- `name` (String, required)
- `mobile` (String, required)
- `city` (String, required)
- `state` (String, required, from spinner)
- `aadhaar` (String, required)
- `pan` (String, required)
- `bank_acc` (String, required)
- `ifsc` (String, required)
- `upi` (String, required)
- `branch` (String, required)
- `aadhaar_front` (File, required)
- `aadhaar_back` (File, required)
- `pan_image` (File, required)
- `bank_proof` (File, required)

### Edge Cases
- Files are selected via system file picker (GetContent ActivityResultLauncher)
- File URIs must be converted to multipart parts before submission
- The VPS backend also receives the virtual account data

---

## 5. Password Management

### What It Does
Allows users who forgot their password to reset it via OTP sent to their registered mobile number.

### API Endpoints
| Method | Path | Purpose |
|---|---|---|
| POST | `api/password/send-otp` | Send OTP to registered mobile |
| POST | `api/password/verify-otp` | Verify OTP entered by user |
| POST | `api/password/reset` | Set new password after OTP verified |

### Rules
- New password and confirmation must match
- Password must meet minimum length (8 characters)

---

## 6. Dashboard

### What It Does
The main menu of the app. Displays 9 bill category tiles that navigate to each payment flow.

### Categories Displayed
1. Electricity
2. Gas
3. Mobile Prepaid
4. Mobile Postpaid
5. DTH
6. FASTag
7. Loan Repayment
8. Municipal Taxes
9. My Account (account management)
10. Load Wallet

### Rules
- On dashboard load, the app fetches a dynamic Shreefintech token from the server and stores it in a static field (`Constant.TOKEN`). This token is used for certain payment API calls.
- The app also refreshes the user profile from the VPS backend on every dashboard open.
- Logout from the dashboard calls the logout API, clears SharedPreferences, and returns to LoginActivity with a cleared back stack.

### API Endpoints
| Method | Path | Purpose |
|---|---|---|
| GET | `api/shreefintech-token` | Fetch dynamic payment token |
| GET | `users.php` (VPS) | Refresh user profile |
| POST | `api/logout` | Log out user |

---

## 7. Electricity Bill Payment

### What It Does
Fetches an outstanding electricity bill for a given consumer number and operator, then processes payment.

### Standard Bill Payment Flow (applies to Electricity, Gas, Postpaid, Cable, Broadband, FASTag, Loan, Municipal)
1. User selects operator from dropdown (fetched from API)
2. User enters consumer/account number
3. App calls `fetch-bill` API → displays outstanding amount, due date, bill details
4. User confirms and taps Pay
5. Platform fee is calculated and displayed
6. App generates a local transaction ID (`PYTCH...`)
7. Payment is processed via `process-payment` API
8. App polls/calls `transaction-status` to get final result
9. Transaction is saved to Room database
10. Success/failure sound plays
11. Receipt is displayed

### Platform Fee Calculation
```
amount < 1000      → fee = ₹4
1000 ≤ amount ≤ 5000  → fee = ₹8
5001 ≤ amount ≤ 40000 → fee = ₹20
amount > 40000     → fee = ₹30
```

### API Endpoints (Electricity — same pattern for Gas, Postpaid, Cable, Broadband, Loan, Municipal)
| Method | Path | Purpose |
|---|---|---|
| GET/POST | `api/electricity/operators` | Get list of electricity operators |
| POST | `api/electricity/fetch-bill` | Fetch outstanding bill |
| POST | `api/electricity/process-payment` | Process the payment |
| POST | `api/electricity/verify` | Verify payment result |
| POST | `api/electricity/transaction-status` | Check transaction status by ID |
| GET | `api/electricity/payment-reports` | Get payment reports |
| GET | `api/electricity/latest-payment` | Get most recent payment |
| GET | `api/electricity/payment-history` | Get full payment history |

### Key Request Fields (Fetch Bill)
- `operator` (String, required)
- `consumer_number` (String, required)

### Key Request Fields (Process Payment)
- `operator` (String, required)
- `consumer_number` (String, required)
- `amount` (Decimal, required)
- `transaction_id` (String, locally generated)

### Key Response Fields (Fetch Bill)
- `outstanding_amount` (Decimal)
- `due_date` (String)
- `consumer_name` (String)
- Bill-specific details vary by operator

### State Transitions
```
Pending → Processing → Success
Pending → Processing → Failed
Success/Failed → (stored in Room DB with timestamp)
```

### Edge Cases
- If `fetch-bill` fails, payment cannot proceed (button disabled)
- If `process-payment` is called but `transaction-status` returns pending, the app shows a "processing" state and allows re-check

---

## 8. Mobile Prepaid Recharge

### What It Does
Direct mobile top-up without needing to fetch a bill first. User picks operator, circle, plan (optional), and enters amount.

### How It Differs from Bill Payment
- No `fetch-bill` step — user enters amount directly
- Plan selection is available (fetches available plans from API)
- Circle (telecom zone) must be selected in addition to operator

### API Endpoints
| Method | Path | Purpose |
|---|---|---|
| GET | `api/prepaid/operators` | Get list of prepaid operators |
| POST | `api/prepaid/recharge` | Process direct recharge |
| POST | `api/prepaid/transaction-status` | Check recharge status |
| GET | `api/prepaid/payment-reports` | Get recharge reports |
| GET | `api/prepaid/latest-payment` | Most recent recharge |
| GET | `api/prepaid/recent-transactions` | Recent recharge list |

### Additional Fields (vs. bill payment)
- `circle` (String, required)
- `plan` (String, optional — from plan selection)

---

## 9. DTH Recharge

### What It Does
Direct recharge for DTH (satellite TV) subscribers. Supports operator plan selection.

### How It Differs
- Has explicit plan selection from the API (not just free-text amount)
- Uses `process-direct` endpoint (not `process-payment`)

### API Endpoints
| Method | Path | Purpose |
|---|---|---|
| GET | `api/dth/operators` | Get DTH operators |
| GET | `api/dth/plans` | Get plans for selected operator |
| POST | `api/dth/process-direct` | Process recharge |
| POST | `api/dth/transaction-status` | Check status |
| GET | `api/dth/payment-report` | Reports |
| GET | `api/dth/sms-receipt` | SMS-style receipt |
| GET | `api/dth/recent` | Recent DTH transactions |

---

## 10. FASTag Recharge

### What It Does
Recharges the user's FASTag (highway toll) account by consumer/vehicle number.

### How It Differs
- Uses `fetch-bill` to look up the account
- Uses `process-recharge` (not `process-payment`)
- Has a separate `transaction-report` endpoint (not `payment-reports`)

### API Endpoints
| Method | Path | Purpose |
|---|---|---|
| POST | `api/fastag/fetch-bill` | Look up FASTag account |
| POST | `api/fastag/process-recharge` | Process recharge |
| POST | `api/fastag/transaction-status` | Check status |
| GET | `api/fastag/transaction-report` | Reports |
| GET | `api/fastag/latest-payment` | Latest payment |

---

## 11. Wallet

### What It Does
Displays the user's current wallet balance. The wallet is the funding source for all payments.

### Rules
- Balance is fetched fresh from the API (not cached)
- Balance is also available from the VPS backend as a fallback
- The `Load Wallet` option on the dashboard routes to a wallet top-up flow

### API Endpoints
| Method | Path | Purpose |
|---|---|---|
| GET | `api/wallet/balance` | Get current wallet balance |
| GET | `api/transactions` | Get unified transaction history (paginated) |
| GET | `balance.php` (VPS) | Get VPS-side wallet balance |

### Response Fields (Balance)
- `data.balance` (Decimal) — current wallet balance

---

## 12. Transaction History & Reporting

### What It Does
Shows all past transactions. Supports filtering by category, date range, status, and consumer number. Data is fetched from the local Room database.

### Rules
- Transactions are stored in Room DB immediately after any payment attempt
- The local DB is the primary source for history and reports — not the server
- Search can be done by transaction ID or consumer number
- Report filtering uses: category, consumer number, status (Success/Failed/Processing), from-date, to-date

### Room DB Schema
**Table: `recent_transactions`**
| Column | Type | Notes |
|---|---|---|
| id | Int (PK, auto) | Auto-generated |
| txnId | String | Transaction ID (PYTCH format) |
| consumerNumber | String | Customer's account/consumer number |
| consumerName | String | Name of the bill holder |
| providerName | String | Operator/provider name |
| category | String | e.g., "Electricity", "DTH" |
| amount | Double | Payment amount |
| status | String | "Success", "Failed", "Processing" |
| timestamp | Long | Unix timestamp |

### Queries Supported
- Get all transactions
- Get by category
- Get all distinct categories
- Search by txnId + consumerNumber
- Filtered report (category + consumer + status + date range)
- Last transaction by category
- Total transaction count

---

## 13. HDFC SmartGateway Integration

### What It Does
Routes payment through the HDFC payment gateway for processing.

### API Endpoints
| Method | Path | Purpose |
|---|---|---|
| POST | `api/hdfc/orders` | Create a new order on HDFC gateway |
| GET | `api/hdfc/orders/{order_id}/status` | Poll for order completion status |

### Flow
1. App creates an order with amount and metadata
2. HDFC returns an order ID
3. App polls status until terminal state (success/failed)

---

## 14. Dynamic QR Payment

### What It Does
Generates a QR code the user can scan to complete a payment via any UPI app.

### Rules
- The QR server runs on a dynamic ngrok URL (not a stable domain) — **CONFLICT: this is production code with a dev tunnel URL**
- The token for this flow is fetched from `api/shreefintech-token` on dashboard load

### API Endpoints
| Method | Path | Purpose |
|---|---|---|
| POST | (ngrok URL) | Generate dynamic QR code |

### Response Fields
- `data.qr_url` (String) — URL of the QR image
- `data.amount` (Decimal) — confirmed payment amount
- `data.order_id` (String) — order reference

---

## 15. Transaction ID Generation

### What It Does
Generates a unique transaction ID locally before submitting a payment. This ID is sent to the server and stored in the local DB.

### Format
```
PYTCH[DDMMYYYYHHMMSS]M
Example: PYTCH19012026091530M
```

- Prefix: `PYTCH` (always)
- Date-time: current device date and time
- Suffix: `M` (default; other suffixes used for specific flows)
- Total length: 20 characters

### Rules
- Generated client-side before the API call
- Stored in Room DB with the transaction record
- Used for status lookup and receipts

---

## 16. VPS Backend Synchronization

### What It Does
A parallel backend at `admin.paytouch.in` tracks users and transactions independently. The main app syncs data to this system alongside calls to the main API.

### What Gets Synced
| Event | VPS Action |
|---|---|
| User login/register | Register/update user on VPS |
| KYC submission | Sync KYC fields to VPS |
| Every payment | Log transaction to VPS via `addTransaction()` |
| Dashboard open | Refresh user profile from VPS |
| Virtual Account creation | Upload VA documents to VPS |

### Rules
- VPS sync failures are non-blocking (do not prevent the main flow)
- User ID on VPS matches the user ID from the main API
- Transaction records on VPS include: user_id, category, txnId, amount, status

---

## 17. Session Management

### What It Does
Keeps the user logged in across app restarts. Handles forced logout on session expiry.

### SharedPreferences Store Map
| Store Name | Key | Type | Purpose |
|---|---|---|---|
| `AUTH` | `TOKEN` | String | Bearer token |
| `AUTH` | `USERID` | Int | User ID |
| `AUTH` | `EMAIL` | String | User email |
| `AUTH` | `MOBILE` | String | Mobile number |
| `AUTH` | `TOKEN_TYPE` | String | Token type |
| `AUTH` | `ReferralCode` | String | Referral code |
| `app_prefs` | `mpin_created` | Boolean | MPIN setup complete |
| `app_prefs` | `virtual_account` | Boolean | VA setup complete |
| `LOGIN_PREF` | `REMEMBER` | Boolean | Remember me flag |
| `LOGIN_PREF` | `MOBILE` | String | Saved mobile for auto-fill |
| `LOGIN_PREF` | `LOGIN_TYPE_PASSWORD` | Boolean | true=password, false=MPIN |
| `PAYTOUCH_Session` | various | mixed | Legacy session data |

### Rules
- Token is checked at Splash screen; no token → direct to login
- Any 401 response → `SessionInterceptor` fires → clears ALL SharedPreferences → launches LoginActivity with `FLAG_ACTIVITY_NEW_TASK or FLAG_ACTIVITY_CLEAR_TASK`
- `isLoggedIn()` = token is not null AND userId > 0

---

## 18. Internet Connectivity Check

### Rules
- `Utility.isInternetAvailable(context)` must be called before every API request
- If no internet: show user message via `ToastUtil`, do not make API call
- Uses Android `ConnectivityManager` to check active network

---

## 19. Payment Sound Feedback

### Rules
- `PaymentSoundPlayer` plays audio from the app's assets folder
- `payment_success.mp3` plays on successful payment
- `payment_failed.mp3` plays on failed payment
- Must call `cleanup()` when the screen is destroyed to release MediaPlayer

---

## Known Conflicts and Ambiguities

**CONFLICT 1 — ngrok URL in production code**
The `PaymentApiClient` hardcodes a `ngrok-free.dev` URL for the dynamic QR payment feature. This is a development tunnel URL that changes and will break in production. The new project must replace this with a stable, production URL.

**CONFLICT 2 — Multiple SharedPreferences stores for the same user data**
User data (token, user ID, mobile, email) is scattered across at least three separate SharedPreferences stores (`AUTH`, `PAYTOUCH_Session`, legacy SessionManager). Some stores duplicate the same fields. The new project must consolidate into one store.

**CONFLICT 3 — Duplicate Retrofit clients for the same data**
Wallet balance is available from both `api/wallet/balance` (main API) and `balance.php` (VPS). Transaction history is available from both `api/transactions` and `transactions.php` (VPS). It is not clear which is the canonical source. The new project should use the main PayTouch API as the only source of truth.

**CONFLICT 4 — Legacy MobiKwik API (`dashboard.shreefintechsolutions.com`)**
Some payment flows route through the old MobiKwik endpoint while others use the new PayTouch API. The routing logic between old and new APIs is inconsistent. The new project should audit which endpoints are still active and remove the old one entirely.

**CONFLICT 5 — Static token field (`Constant.TOKEN`)**
The dynamic Shreefintech token is stored in a static field. In multi-thread scenarios, this could cause race conditions. The new project should store this in a ViewModel or repository, not a static variable.
