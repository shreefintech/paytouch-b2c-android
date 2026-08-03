# PayTouch Consumer — Screens & Navigation

> **Status legend:** ✅ Implemented (UI) | 🔧 UI pending | 📋 Planned (not started)

---

## Implemented Screens

---

### ✅ LoginActivity — "Login Screen"

**Purpose:** Authenticate the user with mobile number and password or MPIN.

**Entry points:**
- App launch (no saved token)
- Any screen (forced logout after 401)
- Any screen (explicit logout)

**Exit points:**
- Login success, `requires_kyc = true` → `UploadKycActivity`
- Login success, `requires_mpin = true` → MPIN creation screen (planned)
- Login success, `requires_virtual_account = true` → `CreateVirtualAccountActivity`
- Login success, all clear → `HomeActivity`
- "Register" tap → `CreateAccountActivity`
- "Forgot Password" tap → `OtpVerificationActivity` (password reset)

**Key UI elements:**
- Mobile number field (pre-filled if remember-me was set)
- Password field / MPIN field (toggle between modes)
- Login mode toggle (Password / MPIN)
- "Remember me" checkbox
- "Forgot Password" link
- "Register" button
- Sign In button

---

### ✅ CreateAccountActivity — "Registration Screen"

**Purpose:** Create a new account.

**Entry points:** `LoginActivity`

**Exit points:**
- Successful registration → same routing logic as login

**Key UI elements:**
- Name, mobile, email, password, confirm password fields
- Optional referral code field
- Submit button

---

### ✅ OtpVerificationActivity — "OTP Verification Screen"

**Purpose:** Verify a one-time password sent to the registered mobile.

**Entry points:**
- `LoginActivity` (forgot password / forgot MPIN flow)
- `CreateAccountActivity` (account creation OTP)

**Exit points:**
- OTP verified (password reset) → `ResetPasswordActivity`
- OTP verified (MPIN reset) → `ResetMpinActivity`

**Key UI elements:**
- 6-digit OTP entry boxes
- Resend OTP with countdown timer
- Submit OTP button

---

### ✅ ResetPasswordActivity — "Reset Password Screen"

**Purpose:** Set a new password after OTP verification.

**Entry points:** `OtpVerificationActivity` (password reset flow)

**Exit points:**
- Successful reset → `LoginActivity`

**Key UI elements:**
- New password field
- Confirm new password field
- Change Password button

---

### ✅ ResetMpinActivity — "Reset MPIN Screen"

**Purpose:** Set a new 4-digit MPIN after OTP verification.

**Entry points:** `OtpVerificationActivity` (MPIN reset flow)

**Exit points:**
- Successful reset → `LoginActivity`

**Key UI elements:**
- New MPIN field (4-digit)
- Confirm MPIN field
- Change MPIN button

---

### ✅ UploadKycActivity — "KYC Verification Screen"

**Purpose:** Collect identity information to verify the user.

**Entry points:**
- `LoginActivity` (post-login routing, `requires_kyc = true`)
- App launch (token exists, `requires_kyc = true`)

**Exit points:**
- Successful KYC submission → MPIN creation (planned)

**Key UI elements:**
- Mobile number, full name, address, city fields
- Date of birth (date picker)
- Age (auto-calculated, read-only)
- Email field
- PAN card number (validated)
- Aadhaar number (validated, 12 digits)
- Optional GST field
- Submit button

---

### ✅ CreateVirtualAccountActivity — "Virtual Account Setup"

**Purpose:** Register banking details and upload documents to complete onboarding.

**Entry points:**
- Onboarding flow (`requires_virtual_account = true`)

**Exit points:**
- Successful submission → `HomeActivity`

**Key UI elements:**
- Name, mobile, state (dropdown), city, district (dropdowns)
- Aadhaar number, PAN number
- IFSC code, bank account number, UPI ID, branch name
- Four file upload slots (Aadhaar front, Aadhaar back, PAN, bank proof)
- Create Virtual Account button

---

### ✅ HomeActivity — "Home / Dashboard Screen"

**Purpose:** Central menu — shows all available bill payment categories.

**Entry points:**
- `CreateVirtualAccountActivity` (onboarding complete)
- App launch (already onboarded)

**Exit points:**
- Category tile tap → respective category screen (planned)
- "Load Wallet" tap → Load Wallet screen (planned)

**Key UI elements:**
- Toolbar with PayTouch logo and back button
- White card with "Categories" title
- 3×3 grid of category tiles (Electricity, Gas, Prepaid, TV Cable, DTH, Fastag, Loan, My Account, Tax)
- Load Wallet button at the bottom of the card

---

## Planned Screens

The following screens are defined in the navigation plan but not yet implemented.

---

### 📋 Category Home Screens (one per payment type)

Each category has a home screen with sub-options: Pay Bill, Recent Transactions, Transaction Report, Transaction Status.

| Category | Home Activity | Status |
|---|---|---|
| Electricity | `ElectricityActivity` | Planned |
| Gas | `GasActivity` | Planned |
| Mobile Prepaid | `MobilePrepaidActivity` | Planned |
| Mobile Postpaid | `PostpaidActivity` | Planned |
| DTH | `DthActivity` | Planned |
| FASTag | `FasTagActivity` | Planned |
| Loan Repayment | `LoanRepaymentActivity` | Planned |
| Municipal Tax | `MunicipalTaxActivity` | Planned |

---

### 📋 Pay Bill Screens

One per category, following the same standard flow:
1. Select operator (from API dropdown)
2. Enter consumer/account number
3. Fetch bill → display outstanding amount, due date
4. Calculate and show platform fee
5. User confirms → process payment
6. Show transaction status / receipt

**Key difference for Mobile Prepaid:** No fetch-bill step. User enters amount directly; optional plan selection from API.

**Key difference for DTH:** Plan selection step added after operator selection.

---

### 📋 Transaction Screens (per category)

- `[Category]RecentTransactionActivity` — Recent payments from local Room DB
- `[Category]TransactionReportActivity` — Filtered report (date range, status, consumer number)
- `[Category]TransactionStatusActivity` — Look up a specific transaction by ID

---

### 📋 MyAccountActivity — "My Account"

**Purpose:** View user profile, manage MPIN, view KYC status.

**Entry points:** `HomeActivity` ("My Account" tile)

---

### 📋 LoadWalletActivity — "Load Wallet"

**Purpose:** Top up the user's digital wallet.

**Entry points:** `HomeActivity` ("Load Wallet" button)

---

## Full Navigation Tree

```
[App Launch]
     │
     ▼
Session check (read SharedPreferences)
     │
     ├── No token ──────────────────────────────────────► LoginActivity ✅
     │                                                          │
     │                                                   ┌──────┴──────┐
     │                                                   │             │
     │                                            "Register"    Login success
     │                                                   │             │
     │                                         CreateAccountActivity ✅  │
     │                                                               │
     └── Token exists ─────────────────────────────────────────────►┤
                                                                     │
                                                     Check onboarding flags:
                                                                     │
                                       requires_kyc ────────────► UploadKycActivity ✅
                                                                     │      │
                                                                     │      └── Success ──► MpinActivity 📋
                                                                     │
                                       requires_mpin ───────────────────────────────►────►┐
                                                                     │                    │
                                                                     │      └── Success ──► CreateVirtualAccountActivity ✅
                                                                     │
                                       requires_virtual_account ─────────────────────────►┐
                                                                                          │
                                       All flags false ───────────────────────────────────►┤
                                                                                           ▼
                                                                                    HomeActivity ✅
                                                                                          │
                                              ┌───────────────────────────────────────────┤
                                              │                           │                │
                                              ▼                           ▼                ▼
                                    ElectricityActivity 📋         GasActivity 📋    ... (other categories)
                                              │
                                    ┌─────────┤
                                    │         │
                                   Pay      Recent      Report      Status      Receipt
                                   Bill      Txns

[401 at any point]
     └──────────────────────────────────────────────────────► LoginActivity (stack cleared)
```

---

## Navigation Rules

- **Onboarding back stack:** Users cannot navigate back to a completed onboarding step. Use `FLAG_ACTIVITY_CLEAR_TOP` or equivalent.
- **Post-login stack:** After successful login/registration, the back stack is cleared — the user cannot press Back to reach the login screen from Home.
- **Forced logout (401):** Stack completely cleared with `FLAG_ACTIVITY_NEW_TASK or FLAG_ACTIVITY_CLEAR_TASK`.
- **Within a category:** Back navigation returns to the Category Home screen.
- **Result refresh:** Use `ActivityResultLauncher` when a child screen's data changes should trigger a refresh on the parent screen.

---

## Intent Extras Reference

| From | To | Extra Key | Type | Purpose |
|---|---|---|---|---|
| PayBillActivity | TransactionStatusActivity | `transaction_id` | String | Pre-fill status lookup after payment |
| PayBillActivity | SMSReceiptActivity | `transaction_id` | String | Load receipt for completed payment |
