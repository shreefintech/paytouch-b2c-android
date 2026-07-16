# PayTouch Consumer — API Reference

All endpoints grouped by domain area. The new project must use the PayTouch Main API (`https://www.paytouch.in/`) as the canonical source. Legacy endpoints are documented for completeness but should be audited and removed where replaced.

---

## Base URLs

| Client Name | Base URL | Purpose |
|---|---|---|
| Main API | `https://www.paytouch.in/` | All core features |
| VPS Admin | `https://admin.paytouch.in/` | Parallel user/transaction tracking |
| Legacy | `https://dashboard.shreefintechsolutions.com/api/mobikwik/` | Old MobiKwik endpoint (audit for removal) |
| QR Payment | `https://tablet-frying-shy.ngrok-free.dev/` | Dynamic QR (REPLACE with stable URL) |

## Authentication Headers
All Main API and VPS Admin requests require:
```
Authorization: Bearer {token}
```
Token is obtained from login/register response and stored in SharedPreferences.

---

## AUTHENTICATION & USER MANAGEMENT

### POST `api/login`
**Purpose:** Authenticate user with mobile + password or MPIN.
**Called from:** LoginActivity

**Request Body:**
| Field | Type | Required | Notes |
|---|---|---|---|
| mobile | String | Yes | 10-digit mobile number |
| password | String | Conditional | Required for password login mode |
| mpin | String | Conditional | Required for MPIN login mode |

**Response:**
| Field | Type | Notes |
|---|---|---|
| token | String | Bearer token for subsequent requests |
| token_type | String | Always "Bearer" |
| user.id | Int | User ID |
| user.mobile | String | Registered mobile |
| user.email | String | Registered email |
| user.wallet_balance | Decimal | Current wallet balance |
| user.requires_kyc | Boolean | True → route to KYC screen |
| user.requires_mpin | Boolean | True → route to MPIN creation |
| user.requires_virtual_account | Boolean | True → route to Virtual Account |

---

### POST `api/register`
**Purpose:** Create a new user account.
**Called from:** CreateAccountActivity

**Request Body:**
| Field | Type | Required | Notes |
|---|---|---|---|
| name | String | Yes | Full name |
| mobile | String | Yes | 10-digit mobile |
| email | String | Yes | Valid email format |
| password | String | Yes | Min 8 characters |
| password_confirmation | String | Yes | Must match password |
| referral_code | String | No | Optional referral code |

**Response:** Same structure as login response.

---

### GET `api/user`
**Purpose:** Fetch current authenticated user's profile and onboarding flags.
**Called from:** SplashActivity (session check), DashboardCategoryActivity

**Response:**
| Field | Type | Notes |
|---|---|---|
| id | Int | User ID |
| mobile | String | |
| email | String | |
| wallet_balance | Decimal | |
| requires_kyc | Boolean | |
| requires_mpin | Boolean | |
| requires_virtual_account | Boolean | |

---

### POST `api/logout`
**Purpose:** Invalidate the server-side session.
**Called from:** DashboardCategoryActivity (menu → logout)

**Request:** No body required (token in header is sufficient).
**Response:** `{ success: true, message: String }`

---

## KYC

### GET `api/kyc/account-info`
**Purpose:** Fetch existing KYC data (if any) to pre-fill the KYC form.
**Called from:** KYCActivity (on load)

**Response:** All KYC fields listed below, possibly null if not yet submitted.

---

### POST `api/kyc/submit`
**Purpose:** Submit KYC identity information.
**Called from:** KYCActivity (on submit)

**Request Body:**
| Field | Type | Required | Validation |
|---|---|---|---|
| mobile_no | String | Yes | 10 digits |
| member_name | String | Yes | |
| birth_date | String | Yes | Date format (from date picker) |
| age | Int | Yes | Auto-calculated from DOB |
| home_address | String | Yes | |
| city_name | String | Yes | |
| email | String | Yes | Valid email |
| pan_card_no | String | Yes | Regex: `[A-Z]{5}[0-9]{4}[A-Z]{1}` |
| aadhaar_no | String | Yes | Exactly 12 digits |
| gst_no | String | No | GST format if provided |

**Response:** `{ success: Boolean, message: String, data: {} }`

---

## MPIN

### POST `api/mpin/create`
**Purpose:** Create a new 4-digit MPIN during onboarding.
**Called from:** MpinActivity

| Field | Type | Required | Notes |
|---|---|---|---|
| mpin | String | Yes | Exactly 4 digits |
| mpin_confirmation | String | Yes | Must match mpin |

**Response:** `{ success: Boolean, message: String }`

---

### POST `api/mpin/verify`
**Purpose:** Verify MPIN during MPIN-mode login.
**Called from:** LoginActivity (MPIN mode)

| Field | Type | Required |
|---|---|---|
| mpin | String | Yes |

**Response:** `{ success: Boolean, message: String }`

---

### POST `api/mpin/send-otp`
**Purpose:** Send OTP to registered mobile to begin MPIN reset.
**Called from:** MPIN reset flow

**Request:** Mobile number or no body (uses token to identify user).
**Response:** `{ success: Boolean, message: String }`

---

### POST `api/mpin/verify-otp`
**Purpose:** Verify OTP for MPIN reset.

| Field | Type | Required |
|---|---|---|
| otp | String | Yes |

**Response:** `{ success: Boolean, message: String }`

---

### POST `api/mpin/reset`
**Purpose:** Set new MPIN after OTP verified.

| Field | Type | Required |
|---|---|---|
| mpin | String | Yes |
| mpin_confirmation | String | Yes |

**Response:** `{ success: Boolean, message: String }`

---

## PASSWORD MANAGEMENT

### POST `api/password/send-otp`
**Purpose:** Send OTP for password reset.
**Called from:** Forgot Password flow

| Field | Type | Required |
|---|---|---|
| mobile | String | Yes |

**Response:** `{ success: Boolean, message: String }`

---

### POST `api/password/verify-otp`
**Purpose:** Verify OTP for password reset.

| Field | Type | Required |
|---|---|---|
| otp | String | Yes |

**Response:** `{ success: Boolean, message: String }`

---

### POST `api/password/reset`
**Purpose:** Set new password after OTP verified.

| Field | Type | Required |
|---|---|---|
| password | String | Yes | Min 8 chars |
| password_confirmation | String | Yes | Must match |

**Response:** `{ success: Boolean, message: String }`

---

## VIRTUAL ACCOUNT

### POST `api/virtual-account/create`
**Purpose:** Submit banking details and KYC documents to create a virtual account. This completes onboarding.
**Called from:** VirtualAccountActivity
**Content-Type:** `multipart/form-data`

| Field | Type | Required | Notes |
|---|---|---|---|
| name | String | Yes | |
| mobile | String | Yes | |
| city | String | Yes | |
| state | String | Yes | From Indian states list |
| aadhaar | String | Yes | |
| pan | String | Yes | |
| bank_acc | String | Yes | Bank account number |
| ifsc | String | Yes | IFSC code |
| upi | String | Yes | UPI ID |
| branch | String | Yes | Bank branch name |
| aadhaar_front | File | Yes | Image file |
| aadhaar_back | File | Yes | Image file |
| pan_image | File | Yes | Image file |
| bank_proof | File | Yes | Image file |

**Response:** `{ success: Boolean, message: String, data: {} }`

---

## WALLET & TRANSACTIONS

### GET `api/wallet/balance`
**Purpose:** Fetch current wallet balance.
**Called from:** Dashboard, before payments

**Response:**
| Field | Type |
|---|---|
| success | Boolean |
| data.balance | Decimal |

---

### GET `api/transactions`
**Purpose:** Get unified paginated transaction history.
**Called from:** Transaction history screens

**Query Params:**
| Param | Type | Notes |
|---|---|---|
| page | Int | Pagination page number |
| category | String | Optional filter |

**Response:** Paginated list of transaction objects.

---

## TOKEN

### GET `api/shreefintech-token`
**Purpose:** Fetch a dynamic token used for certain payment operations. Fetched on every dashboard load.
**Called from:** DashboardCategoryActivity

**Response:**
| Field | Type |
|---|---|
| success | Boolean |
| data.token | String |

---

## HDFC SMART GATEWAY

### POST `api/hdfc/orders`
**Purpose:** Create a new payment order on the HDFC gateway.
**Called from:** Payment confirmation screens

| Field | Type | Required |
|---|---|---|
| amount | Decimal | Yes |
| transaction_id | String | Yes | PYTCH-format locally-generated ID |
| category | String | Yes | Bill category |
| consumer_number | String | Yes | |

**Response:**
| Field | Type |
|---|---|
| success | Boolean |
| data.order_id | String |

---

### GET `api/hdfc/orders/{order_id}/status`
**Purpose:** Poll for the completion status of an HDFC order.
**Called from:** Payment status screens

**Path Param:** `order_id` — returned from create order call
**Response:** `{ success: Boolean, status: String, message: String }`

---

## ELECTRICITY BILL PAYMENT

### GET/POST `api/electricity/operators`
**Purpose:** Get list of available electricity operators.
**Called from:** Electricity PayBill screen (operator dropdown)

**Response:** `{ success: Boolean, data: [{ id, name, code }] }`

---

### POST `api/electricity/fetch-bill`
**Purpose:** Fetch outstanding electricity bill for a consumer number.
**Called from:** Electricity PayBill screen (after operator + consumer number entered)

| Field | Type | Required |
|---|---|---|
| operator | String | Yes |
| consumer_number | String | Yes |

**Response:**
| Field | Type |
|---|---|
| success | Boolean |
| data.outstanding_amount | Decimal |
| data.due_date | String |
| data.consumer_name | String |
| data.bill_details | Object (varies by operator) |

---

### POST `api/electricity/process-payment`
**Purpose:** Process electricity bill payment.
**Called from:** Electricity PayBill screen (after user confirms)

| Field | Type | Required |
|---|---|---|
| operator | String | Yes |
| consumer_number | String | Yes |
| amount | Decimal | Yes |
| transaction_id | String | Yes | PYTCH-format ID |

**Response:** `{ success: Boolean, message: String, data.transaction_id: String }`

---

### POST `api/electricity/verify`
**Purpose:** Verify payment result after processing.

| Field | Type | Required |
|---|---|---|
| transaction_id | String | Yes |

**Response:** `{ success: Boolean, status: String, message: String }`

---

### POST `api/electricity/transaction-status`
**Purpose:** Check the status of a specific electricity transaction by ID.
**Called from:** Transaction Status screen

| Field | Type | Required |
|---|---|---|
| transaction_id | String | Yes |

**Response:** `{ success: Boolean, status: String (Success/Failed/Processing), message: String }`

---

### GET `api/electricity/payment-reports`
**Purpose:** Get paginated electricity payment reports (filtered).
**Called from:** Transaction Report screen

**Query Params:** `page`, `consumer_number`, `status`, `from_date`, `to_date`

---

### GET `api/electricity/latest-payment`
**Purpose:** Get the most recent electricity payment.

---

### GET `api/electricity/payment-history`
**Purpose:** Get full electricity payment history.

---

## GAS BILL PAYMENT
*Same endpoint pattern as Electricity — replace `electricity` with `gas` in all paths.*

| Endpoint | Same as Electricity? |
|---|---|
| `api/gas/operators` | Yes |
| `api/gas/fetch-bill` | Yes |
| `api/gas/process-payment` | Yes |
| `api/gas/verify` | Yes |
| `api/gas/transaction-status` | Yes |
| `api/gas/payment-reports` | Yes |
| `api/gas/latest-payment` | Yes |
| `api/gas/payment-history` | Yes |

---

## MOBILE PREPAID RECHARGE

### GET `api/prepaid/operators`
**Purpose:** Get list of prepaid mobile operators.

---

### POST `api/prepaid/recharge`
**Purpose:** Process direct mobile recharge.
**Note:** No fetch-bill step — user enters amount directly.

| Field | Type | Required |
|---|---|---|
| operator | String | Yes |
| mobile | String | Yes | The mobile number to recharge |
| circle | String | Yes | Telecom circle/zone |
| amount | Decimal | Yes |
| plan | String | No | Optional plan code |
| transaction_id | String | Yes |

---

### POST `api/prepaid/transaction-status`
### GET `api/prepaid/payment-reports`
### GET `api/prepaid/latest-payment`
### GET `api/prepaid/recent-transactions`
*Same structure as electricity equivalents.*

---

## MOBILE POSTPAID BILL PAYMENT
*Same pattern as electricity (fetch-bill → process-payment → verify → status). Replace `electricity` with `postpaid`.*

| Endpoint |
|---|
| `api/postpaid/fetch-bill` |
| `api/postpaid/process-payment` |
| `api/postpaid/verify` |
| `api/postpaid/transaction-status` |
| `api/postpaid/payment-reports` |

---

## DTH RECHARGE

### GET `api/dth/operators`
### GET `api/dth/plans`
**Purpose:** Get available recharge plans for a selected operator.

| Query Param | Type | Required |
|---|---|---|
| operator | String | Yes |

**Response:** `{ success: Boolean, data: [{ id, name, amount, validity, description }] }`

---

### POST `api/dth/process-direct`
**Purpose:** Process DTH recharge directly (no fetch-bill step).

| Field | Type | Required |
|---|---|---|
| operator | String | Yes |
| consumer_number | String | Yes | DTH subscriber ID |
| amount | Decimal | Yes |
| transaction_id | String | Yes |

---

### POST `api/dth/transaction-status`
### GET `api/dth/payment-report`
### GET `api/dth/sms-receipt`
### GET `api/dth/recent`

---

## CABLE TV BILL PAYMENT

### GET `api/cable/operators`
### POST `api/cable/fetch-bill`
### POST `api/cable/process-direct`
### POST `api/cable/transaction-status`
### GET `api/cable/payment-report`
### GET `api/cable/sms-receipt`

---

## BROADBAND BILL PAYMENT

### GET `api/broadband/operators`
### POST `api/broadband/fetch-bill`
### POST `api/broadband/process-direct`
### POST `api/broadband/transaction-status`
### GET `api/broadband/payment-report`
### GET `api/broadband/sms-receipt`
### GET `api/broadband/recent`

---

## FASTAG RECHARGE

### POST `api/fastag/fetch-bill`
**Purpose:** Look up FASTag account by vehicle/consumer number.

| Field | Type | Required |
|---|---|---|
| consumer_number | String | Yes | Vehicle registration or FASTag ID |

---

### POST `api/fastag/process-recharge`
**Purpose:** Top up the FASTag account.

| Field | Type | Required |
|---|---|---|
| consumer_number | String | Yes |
| amount | Decimal | Yes |
| transaction_id | String | Yes |

---

### POST `api/fastag/transaction-status`
### GET `api/fastag/transaction-report`
### GET `api/fastag/latest-payment`

---

## LOAN REPAYMENT

### GET `api/loan/operators`
**Purpose:** Get list of loan providers/lenders.

---

### POST `api/loan/fetch-bill`
**Purpose:** Fetch outstanding loan EMI details.

| Field | Type | Required |
|---|---|---|
| operator | String | Yes |
| consumer_number | String | Yes | Loan account number |

---

### POST `api/loan/process-payment`
### POST `api/loan/utility-payment-status-search`
**Purpose:** Search for a loan payment by consumer number (alternative status check).

### GET `api/loan/payment-report`
### GET `api/loan/latest-payment`
### GET `api/loan/payment-history`

---

## MUNICIPAL TAX PAYMENT

### GET `api/municipal/operators`
### POST `api/municipal/fetch-bill`
### POST `api/municipal/process-payment`
### GET `api/municipal/payment-report`
### POST `api/municipal/transaction-status`
### GET `api/municipal/latest-payment`
### GET `api/municipal/recent-transactions`

---

## WATER BILL PAYMENT

### GET `api/water/operator-list`
**Purpose:** Get list of water utility operators.
*Note: Only one endpoint is defined for Water in the current codebase. The payment flow may route through the legacy API.*

---

## VPS ADMIN BACKEND (`https://admin.paytouch.in/`)

### POST `users.php`
**Purpose:** Register or update a user on the VPS backend.
**Called from:** After login/register on main API.

### GET `users.php`
**Purpose:** Fetch VPS-side user profile.
**Called from:** DashboardCategoryActivity (profile sync).

### POST `transactions.php`
**Purpose:** Log a transaction to the VPS backend.
**Called from:** After every payment attempt.

| Field | Type | Required |
|---|---|---|
| user_id | Int | Yes |
| category | String | Yes |
| txnId | String | Yes |
| amount | Decimal | Yes |
| status | String | Yes |
| consumer_number | String | Yes |
| provider_name | String | Yes |

### GET `transactions.php`
**Purpose:** Fetch VPS-side transaction history.

### GET `balance.php`
**Purpose:** Fetch VPS-side wallet balance.

---

## LEGACY API (`https://dashboard.shreefintechsolutions.com/api/mobikwik/`)

> **WARNING:** These endpoints are from the old MobiKwik integration. Audit each one before using in the new project. Some may be replaced by the main PayTouch API.

| Method | Path | Purpose |
|---|---|---|
| POST | `operators` | Get operator list (legacy) |
| POST | `circles` | Get telecom circle list |
| POST | `view-bill` | Fetch bill (legacy) |
| POST | `payment` | Process payment (legacy) |
| POST | `status` | Check payment status (legacy) |
| POST | `plans/{operatorId}/{circleId}` | Get prepaid plans (legacy) |
| POST | `balance` | Check balance (legacy) |

---

## DYNAMIC QR PAYMENT (ngrok — REPLACE URL)

### POST `(ngrok base URL)/`
**Purpose:** Generate a QR code for UPI payment.
**WARNING:** Base URL is `https://tablet-frying-shy.ngrok-free.dev/` — a development tunnel. Must be replaced with a stable production URL before shipping.

**Request:**
| Field | Type | Required |
|---|---|---|
| amount | Decimal | Yes |
| transaction_id | String | Yes |
| token | String | Yes | Shreefintech token from `api/shreefintech-token` |

**Response:**
| Field | Type |
|---|---|
| success | Boolean |
| message | String |
| data.qr_url | String | URL to QR image |
| data.amount | Decimal | |
| data.order_id | String | |

---

## STANDARD RESPONSE WRAPPER

All endpoints return a common wrapper:
```json
{
  "success": true,
  "message": "Human-readable result message",
  "data": { /* endpoint-specific payload */ }
}
```

**Error responses** follow the same structure with `success: false` and an error message. HTTP status codes used:
- `200` — Success
- `401` — Unauthorized (token expired/invalid → forced logout)
- `422` — Validation error (field errors in response body)
- `500` — Server error
