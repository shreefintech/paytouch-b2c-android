# PayTouch Consumer — Business Logic Reference

> **Current phase:** UI implementation. API calls are not yet wired. This document describes the **target behaviour** — implement UI first, then wire each section to the API when that phase begins. Field names and endpoint paths here are authoritative; do not invent your own.

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
  - `requires_kyc = true` → send to `UploadKycActivity`
  - `requires_mpin = true` → send to MPIN creation screen
  - `requires_virtual_account = true` → send to `CreateVirtualAccountActivity`
  - All false → send to `HomeActivity`
- "Remember Me" saves the mobile number to SharedPreferences so it pre-fills on next open
- Login type preference (password vs. MPIN) is also persisted between sessions

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
- `name` (String, required)
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
- `user.requires_virtual_account` (Boolean)

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
- "Remember Me" checked → save mobile in SharedPreferences; pre-fill on next open

---

## 2. KYC (Know Your Customer)

**Screen:** `onboarding/UploadKycActivity`

### What It Does
Collects personal identity information and submits it to the server for verification. This unlocks the full account.

### Rules and Constraints
- PAN card must match regex: `[A-Z]{5}[0-9]{4}[A-Z]{1}`
- Aadhaar number must be exactly 12 digits
- GST number is **optional** — validate format only when non-empty; never block submission if blank
- Date of birth is selected via a date picker (not typed)
- Age is auto-calculated from the selected DOB
- After successful KYC, route to MPIN creation — never skip to Home

### API Endpoints
| Method | Path | Purpose |
|---|---|---|
| GET | `api/kyc/account-info` | Pre-fill form with existing KYC data if any |
| POST | `api/kyc/submit` | Submit KYC data |

### Request Fields (Submit)
- `mobile_no` (String, required)
- `member_name` (String, required)
- `birth_date` (String, required, from date picker)
- `age` (Int, required, auto-calculated)
- `home_address` (String, required)
- `city_name` (String, required)
- `email` (String, required)
- `pan_card_no` (String, required, regex validated)
- `aadhaar_no` (String, required, 12 digits)
- `gst_no` (String, optional)

### Edge Cases
- If KYC was already partially submitted, `account-info` pre-fills the form

---

## 3. MPIN (Mobile PIN)

### What It Does
A 4-digit PIN used as an alternative to password login. Created during onboarding, reset via OTP.

### Rules and Constraints
- MPIN must be exactly 4 digits (numeric only)
- MPIN and confirmation must match before submission
- After successful creation, route to `CreateVirtualAccountActivity`
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

**Screen:** `onboarding/CreateVirtualAccountActivity`

### What It Does
Collects the user's banking details and supporting documents. Completes the onboarding sequence and unlocks all payment features.

### Rules and Constraints
- Required text fields: name, mobile, state, city, district, Aadhaar, PAN, bank account number, IFSC code, UPI ID, branch name
- Required file uploads: Aadhaar front, Aadhaar back, PAN image, bank proof image
- State, city, district are selected from dropdowns
- All four file uploads are mandatory; submission is blocked if any is missing
- After successful submission, route to `HomeActivity`

### API Endpoints
| Method | Path | Purpose |
|---|---|---|
| POST | `api/virtual-account/create` | Submit VA details as multipart form |

### Request Fields (Multipart)
- `name`, `mobile`, `city`, `state`, `district`, `aadhaar`, `pan`, `bank_acc`, `ifsc`, `upi`, `branch` (all String, required)
- `aadhaar_front`, `aadhaar_back`, `pan_image`, `bank_proof` (File, required)

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
1. Electricity
2. Gas
3. Prepaid (Mobile)
4. TV Cable
5. DTH
6. Fastag
7. Loan (TV Cable placeholder)
8. My Account
9. Tax (TV Cable placeholder)
10. Load Wallet (bottom action)

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
6. App generates a local transaction ID (`PYTCH[DDMMYYYYHHMMSS]M`)
7. Payment processed via `process-payment` API
8. App calls `transaction-status` to get final result
9. Transaction saved to Room DB
10. Success/failure sound plays
11. Receipt is displayed

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
- `transaction_id` (String, PYTCH-format, generated client-side)

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
| GET | `api/prepaid/operators` | Get prepaid operators |
| POST | `api/prepaid/recharge` | Process recharge |
| POST | `api/prepaid/transaction-status` | Check status |
| GET | `api/prepaid/payment-reports` | Reports |

### Additional Request Fields
- `circle` (String, required)
- `plan` (String, optional)

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
- Uses `fetch-bill` to look up the account (same as bill payment)
- Uses `process-recharge` (not `process-payment`)

### API Endpoints
| Method | Path | Purpose |
|---|---|---|
| POST | `api/fastag/fetch-bill` | Look up FASTag account |
| POST | `api/fastag/process-recharge` | Process recharge |
| POST | `api/fastag/transaction-status` | Check status |
| GET | `api/fastag/transaction-report` | Reports |

---

## 11. Wallet

### Rules
- Balance is fetched fresh from the API on every Home/dashboard load
- `Load Wallet` routes to a wallet top-up flow

### API Endpoints
| Method | Path | Purpose |
|---|---|---|
| GET | `api/wallet/balance` | Get current wallet balance |
| GET | `api/transactions` | Get unified paginated transaction history |

### Response Fields (Balance)
- `data.balance` (Decimal) — current wallet balance

---

## 12. Transaction History & Reporting

### Rules
- Transactions are stored in Room DB immediately after any payment attempt
- The local DB is the primary source for history and reports
- Search can be done by transaction ID or consumer number
- Report filtering: category, consumer number, status (Success/Failed/Processing), from-date, to-date

### Room DB Schema — `recent_transactions`
| Column | Type | Notes |
|---|---|---|
| id | Int (PK, auto) | Auto-generated |
| txnId | String | PYTCH-format transaction ID |
| consumerNumber | String | Consumer/account number |
| consumerName | String | Name of the bill holder |
| providerName | String | Operator/provider name |
| category | String | e.g. "Electricity", "DTH" |
| amount | Double | Payment amount (before fee) |
| status | String | "Success", "Failed", "Processing" |
| timestamp | Long | Unix timestamp |

---

## 13. Transaction ID Generation

### Format
```
PYTCH[DDMMYYYYHHMMSS]M
Example: PYTCH19012026091530M
```

- Prefix: `PYTCH` (always)
- Date-time: current device date and time in DDMMYYYYHHMMSS format
- Suffix: `M` (default)
- Total length: 20 characters

### Rules
- Generated client-side **before** the API call
- Stored in Room DB with the transaction record
- Used for status lookup and receipts
- Do not use `Math.random()` or `UUID` for this — use the timestamp format above

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
| `AUTH` | `ReferralCode` | String | Referral code |
| `app_prefs` | `mpin_created` | Boolean | MPIN setup complete |
| `app_prefs` | `virtual_account` | Boolean | VA setup complete |
| `LOGIN_PREF` | `REMEMBER` | Boolean | Remember me flag |
| `LOGIN_PREF` | `MOBILE` | String | Saved mobile for auto-fill |
| `LOGIN_PREF` | `LOGIN_TYPE_PASSWORD` | Boolean | true=password, false=MPIN |

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
| Virtual Account creation | Upload VA documents to VPS |

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
The dynamic token from `api/shreefintech-token` is currently stored as a static field (`Constant.TOKEN`). This is a race condition risk in multi-thread scenarios. Move this to `SharedPreferenceHelper` or a Repository before the API wiring phase.

**Issue 3 — Legacy MobiKwik API:**
Some payment flows may route through `dashboard.shreefintechsolutions.com/api/mobikwik/`. Audit each endpoint before wiring — determine which are still active and which are replaced by `paytouch.in` equivalents.
