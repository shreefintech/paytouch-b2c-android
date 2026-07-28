# PayTouch Consumer — API Reference

> **Current phase:** UI implementation. APIs are not yet wired. This document is the authoritative reference for field names, endpoint paths, and response structures. Use it when wiring up any API call — do not invent field names or paths.

---

## Base URLs

| Client Name | Base URL | Purpose |
|---|---|---|
| Main API | `https://www.paytouch.in/` | All core features (auth, KYC, payments, wallet) |
| VPS Admin | `https://admin.paytouch.in/` | Parallel user/transaction tracking |
| Legacy | `https://dashboard.shreefintechsolutions.com/api/mobikwik/` | Old MobiKwik endpoint — audit before using |
| QR Payment | *(stable URL TBD — current dev URL is a ngrok tunnel, must be replaced before release)* | Dynamic QR payment |

## Authentication Header
All Main API and VPS Admin requests require:
```
Authorization: Bearer {token}
```
Token is obtained from the login/register response and stored in SharedPreferences via `SharedPreferenceHelper`.

## Standard Response Wrapper
All endpoints return:
```json
{
  "success": true,
  "message": "Human-readable result",
  "data": { /* endpoint-specific payload */ }
}
```
HTTP status codes:
- `200` — Success
- `401` — Unauthorized → forced logout
- `422` — Validation error
- `500` — Server error

---

## AUTHENTICATION & USER MANAGEMENT

### POST `api/login`
**Called from:** `LoginActivity`

**Request:**
| Field | Type | Required | Notes |
|---|---|---|---|
| mobile | String | Yes | 10-digit mobile number |
| password | String | Conditional | Password login mode |
| mpin | String | Conditional | MPIN login mode |

**Response:**
| Field | Type | Notes |
|---|---|---|
| token | String | Bearer token |
| token_type | String | Always "Bearer" |
| user.id | Int | |
| user.mobile | String | |
| user.email | String | |
| user.wallet_balance | Decimal | |
| user.requires_kyc | Boolean | true → route to KYC |
| user.requires_mpin | Boolean | true → route to MPIN creation |
| user.requires_virtual_account | Boolean | true → route to Virtual Account |

---

### POST `api/register`
**Called from:** `CreateAccountActivity`

**Request:**
| Field | Type | Required | Notes |
|---|---|---|---|
| name | String | Yes | Full name |
| mobile | String | Yes | 10-digit |
| email | String | Yes | Valid email |
| password | String | Yes | Min 8 chars |
| password_confirmation | String | Yes | Must match |
| referral_code | String | No | Optional |

**Response:** Same structure as login.

---

### GET `api/user`
**Called from:** Splash screen (session check), Home screen

**Response:** `{ id, mobile, email, wallet_balance, requires_kyc, requires_mpin, requires_virtual_account }`

---

### POST `api/logout`
**Called from:** Home screen (logout action)

**Request:** No body (token in header).
**Response:** `{ success: Boolean, message: String }`

---

## KYC

### GET `api/kyc/account-info`
**Called from:** `UploadKycActivity` (on load, to pre-fill)

**Response:** All KYC fields; may be null if not yet submitted.

---

### POST `api/kyc/submit`
**Called from:** `UploadKycActivity` (on submit)

| Field | Type | Required | Validation |
|---|---|---|---|
| mobile_no | String | Yes | 10 digits |
| member_name | String | Yes | |
| birth_date | String | Yes | From date picker |
| age | Int | Yes | Auto-calculated |
| home_address | String | Yes | |
| city_name | String | Yes | |
| email | String | Yes | Valid email |
| pan_card_no | String | Yes | `[A-Z]{5}[0-9]{4}[A-Z]{1}` |
| aadhaar_no | String | Yes | Exactly 12 digits |
| gst_no | String | No | GST format if provided |

**Response:** `{ success: Boolean, message: String, data: {} }`

---

## MPIN

### POST `api/mpin/create`
| Field | Type | Required |
|---|---|---|
| mpin | String | Yes — 4 digits |
| mpin_confirmation | String | Yes — must match |

---

### POST `api/mpin/verify`
| Field | Type | Required |
|---|---|---|
| mpin | String | Yes |

---

### POST `api/mpin/send-otp`
**Request:** No body required (user identified by Bearer token).

---

### POST `api/mpin/verify-otp`
| Field | Type | Required |
|---|---|---|
| otp | String | Yes |

---

### POST `api/mpin/reset`
| Field | Type | Required |
|---|---|---|
| mpin | String | Yes |
| mpin_confirmation | String | Yes |

---

## PASSWORD MANAGEMENT

### POST `api/password/send-otp`
| Field | Type | Required |
|---|---|---|
| mobile | String | Yes |

---

### POST `api/password/verify-otp`
| Field | Type | Required |
|---|---|---|
| otp | String | Yes |

---

### POST `api/password/reset`
| Field | Type | Required |
|---|---|---|
| password | String | Yes — min 8 chars |
| password_confirmation | String | Yes — must match |

---

## VIRTUAL ACCOUNT

### POST `api/virtual-account/create`
**Called from:** `CreateVirtualAccountActivity`
**Content-Type:** `multipart/form-data`

| Field | Type | Required |
|---|---|---|
| name | String | Yes |
| mobile | String | Yes |
| city | String | Yes |
| state | String | Yes |
| district | String | Yes |
| aadhaar | String | Yes |
| pan | String | Yes |
| bank_acc | String | Yes |
| ifsc | String | Yes |
| upi | String | Yes |
| branch | String | Yes |
| aadhaar_front | File | Yes |
| aadhaar_back | File | Yes |
| pan_image | File | Yes |
| bank_proof | File | Yes |

**Response:** `{ success: Boolean, message: String, data: {} }`

---

## WALLET & TRANSACTIONS

### GET `api/wallet/balance`
**Response:**
| Field | Type |
|---|---|
| success | Boolean |
| data.balance | Decimal |

---

### GET `api/transactions`
**Query Params:** `page` (Int), `category` (String, optional)

**Response:** Paginated list of transaction objects.

---

## HOME / DASHBOARD TOKEN

### GET `api/shreefintech-token`
**Called from:** `HomeActivity` (on load)

**Response:**
| Field | Type |
|---|---|
| success | Boolean |
| data.token | String |

---

## HDFC SMART GATEWAY

### POST `api/hdfc/orders`
| Field | Type | Required |
|---|---|---|
| amount | Decimal | Yes |
| transaction_id | String | Yes — PYTCH-format |
| category | String | Yes |
| consumer_number | String | Yes |

**Response:** `{ success: Boolean, data.order_id: String }`

---

### GET `api/hdfc/orders/{order_id}/status`
**Response:** `{ success: Boolean, status: String, message: String }`

---

## ELECTRICITY BILL PAYMENT

### GET/POST `api/electricity/operators`
**Response:** `{ success: Boolean, data: [{ id, name, code }] }`

### POST `api/electricity/fetch-bill`
| Field | Type | Required |
|---|---|---|
| operator | String | Yes |
| consumer_number | String | Yes |

**Response:**
| Field | Type |
|---|---|
| data.outstanding_amount | Decimal |
| data.due_date | String |
| data.consumer_name | String |

### POST `api/electricity/process-payment`
| Field | Type | Required |
|---|---|---|
| operator | String | Yes |
| consumer_number | String | Yes |
| amount | Decimal | Yes |
| transaction_id | String | Yes — PYTCH-format |

### POST `api/electricity/verify`
| Field | Type |
|---|---|
| transaction_id | String |

### POST `api/electricity/transaction-status`
| Field | Type |
|---|---|
| transaction_id | String |

### GET `api/electricity/payment-reports`
**Query Params:** `page`, `consumer_number`, `status`, `from_date`, `to_date`

### GET `api/electricity/latest-payment`
### GET `api/electricity/payment-history`

---

## GAS BILL PAYMENT
*Same endpoint pattern as Electricity — replace `electricity` with `gas` in all paths.*

---

## MOBILE PREPAID RECHARGE

### GET `api/prepaid/operators`

### POST `api/prepaid/recharge`
| Field | Type | Required | Notes |
|---|---|---|---|
| operator | String | Yes | |
| mobile | String | Yes | Mobile number to recharge |
| circle | String | Yes | Telecom circle/zone |
| amount | Decimal | Yes | |
| plan | String | No | Optional plan code |
| transaction_id | String | Yes | PYTCH-format |

### POST `api/prepaid/transaction-status`
### GET `api/prepaid/payment-reports`
### GET `api/prepaid/latest-payment`
### GET `api/prepaid/recent-transactions`

---

## MOBILE POSTPAID BILL PAYMENT
*Same pattern as Electricity — replace `electricity` with `postpaid`.*

---

## DTH RECHARGE

### GET `api/dth/operators`

### GET `api/dth/plans`
**Query Param:** `operator` (String, required)
**Response:** `{ data: [{ id, name, amount, validity, description }] }`

### POST `api/dth/process-direct`
| Field | Type | Required |
|---|---|---|
| operator | String | Yes |
| consumer_number | String | Yes |
| amount | Decimal | Yes |
| transaction_id | String | Yes |

### POST `api/dth/transaction-status`
### GET `api/dth/payment-report`
### GET `api/dth/sms-receipt`
### GET `api/dth/recent`

---

## CABLE TV BILL PAYMENT
*Endpoints: `api/cable/operators`, `api/cable/fetch-bill`, `api/cable/process-direct`, `api/cable/transaction-status`, `api/cable/payment-report`, `api/cable/sms-receipt`*

---

## BROADBAND BILL PAYMENT
*Endpoints: `api/broadband/operators`, `api/broadband/fetch-bill`, `api/broadband/process-direct`, `api/broadband/transaction-status`, `api/broadband/payment-report`, `api/broadband/sms-receipt`, `api/broadband/recent`*

---

## FASTAG RECHARGE

### POST `api/fastag/fetch-bill`
| Field | Type | Required |
|---|---|---|
| consumer_number | String | Yes — vehicle registration or FASTag ID |

### POST `api/fastag/process-recharge`
| Field | Type | Required |
|---|---|---|
| consumer_number | String | Yes |
| amount | Decimal | Yes |
| transaction_id | String | Yes |

### POST `api/fastag/transaction-status`
### GET `api/fastag/transaction-report`
### GET `api/fastag/latest-payment`

---

## LOAN REPAYMENT

### GET `api/loan/operators`
### POST `api/loan/fetch-bill`
| Field | Type |
|---|---|
| operator | String |
| consumer_number | String — loan account number |

### POST `api/loan/process-payment`
### GET `api/loan/payment-report`
### GET `api/loan/latest-payment`

---

## MUNICIPAL TAX PAYMENT
*Endpoints: `api/municipal/operators`, `api/municipal/fetch-bill`, `api/municipal/process-payment`, `api/municipal/payment-report`, `api/municipal/transaction-status`, `api/municipal/latest-payment`, `api/municipal/recent-transactions`*

---

## VPS ADMIN BACKEND (`https://admin.paytouch.in/`)

### POST `users.php` — Register/update user
### GET `users.php` — Fetch user profile
### POST `transactions.php` — Log a transaction

| Field | Type | Required |
|---|---|---|
| user_id | Int | Yes |
| category | String | Yes |
| txnId | String | Yes |
| amount | Decimal | Yes |
| status | String | Yes |
| consumer_number | String | Yes |
| provider_name | String | Yes |

### GET `transactions.php` — Fetch transaction history
### GET `balance.php` — Fetch VPS-side wallet balance

> VPS calls are non-blocking. Log failures; do not surface them to the user.

---

## LEGACY API (`https://dashboard.shreefintechsolutions.com/api/mobikwik/`)

> **Audit required before use.** These endpoints are from the old MobiKwik integration. Determine which are still active and which are replaced by the main PayTouch API before wiring any of them.

| Method | Path | Purpose |
|---|---|---|
| POST | `operators` | Get operator list |
| POST | `circles` | Get telecom circles |
| POST | `view-bill` | Fetch bill |
| POST | `payment` | Process payment |
| POST | `status` | Check payment status |
| POST | `plans/{operatorId}/{circleId}` | Get prepaid plans |
| POST | `balance` | Check balance |

---

## DYNAMIC QR PAYMENT

> **WARNING:** Base URL is currently a ngrok development tunnel. Must be replaced with a stable production URL before release.

### POST `{qr-base-url}/`

| Field | Type | Required |
|---|---|---|
| amount | Decimal | Yes |
| transaction_id | String | Yes — PYTCH-format |
| token | String | Yes — from `api/shreefintech-token` |

**Response:**
| Field | Type |
|---|---|
| success | Boolean |
| data.qr_url | String — URL to QR image |
| data.amount | Decimal |
| data.order_id | String |
