# PayTouch Consumer — Screens & Navigation

> **Status legend:** ✅ Implemented (UI + API) | 🔧 UI pending | 📋 Planned (not started)

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

---

## Implemented Category Screens

---

### ✅ Electricity Module

Full module implemented. See Electricity as the canonical reference for all other modules.

| Screen | Class | Package |
|---|---|---|
| Pay Bill | `ElectricityActivity` | `electricity/` |
| Recent Transactions | `RecentTransactionActivity` | `electricity/transactions/` |
| Transaction Report | `TransactionReportActivity` | `electricity/transactions/` |
| Transaction Status | `ElectricityTransactionStatusActivity` | `electricity/transactions/` |
| SMS Receipt | `SmsReceiptActivity` | `electricity/transactions/` |

---

### ✅ Gas Module

Full module implemented. Mirrors Electricity exactly.

| Screen | Class | Package |
|---|---|---|
| Pay Bill | `GasActivity` | `gas/` |
| Recent Transactions | `GasRecentTransactionActivity` | `gas/transactions/` |
| Transaction Report | `GasTransactionReportActivity` | `gas/transactions/` |
| Transaction Status | `GasTransactionStatusActivity` | `gas/transactions/` |
| SMS Receipt | `GasSmsReceiptActivity` | `gas/transactions/` |

---

### ✅ DTH Module

Full module implemented. DTH is a **plan-selection** module (`isMobileCategory = true`) — no fetch-bill step; user selects a plan before paying. See `docs/dth.md` for full detail.

| Screen | Class | Package |
|---|---|---|
| Pay Bill | `DthActivity` | `dth/` |
| Plan Selection | `DthPlanSelectionActivity` | `dth/` |
| Recent Transactions | `DthRecentTransactionActivity` | `dth/transactions/` |
| Transaction Report | `DthTransactionReportActivity` | `dth/transactions/` |
| Transaction Status | `DthTransactionStatusActivity` | `dth/transactions/` |
| SMS Receipt | `DthSmsReceiptActivity` | `dth/transactions/` |

---

### ✅ Mobile Prepaid Module

Full module implemented. Prepaid is also a **plan-selection** module (`isMobileCategory = true`). Circle selection is required in addition to operator. No fetch-bill step.

| Screen | Class | Package |
|---|---|---|
| Pay / Recharge | `PrepaidActivity` | `prepaid/` |
| Plan Selection | `PrepaidPlanSelectionActivity` | `prepaid/` |
| Recent Transactions | `PrepaidRecentTransactionActivity` | `prepaid/transactions/` |
| Transaction Report | `PrepaidTransactionReportActivity` | `prepaid/transactions/` |
| Transaction Status | `PrepaidTransactionStatusActivity` | `prepaid/transactions/` |
| SMS Receipt | `PrepaidSmsReceiptActivity` | `prepaid/transactions/` |

---

### ✅ Mobile Postpaid Module

Full module implemented. Standard bill-payment flow (`isMobileCategory = true`): fetch bill → confirm → pay.

| Screen | Class | Package |
|---|---|---|
| Pay Bill | `PostpaidActivity` | `postpaid/` |
| Recent Transactions | `PostpaidRecentTransactionActivity` | `postpaid/transactions/` |
| Transaction Report | `PostpaidTransactionReportActivity` | `postpaid/transactions/` |
| Transaction Status | `PostpaidTransactionStatusActivity` | `postpaid/transactions/` |
| SMS Receipt | `PostpaidSmsReceiptActivity` | `postpaid/transactions/` |

---

### ✅ Shared Transaction Detail

`TransactionDetailActivity` (`transactions/TransactionDetailActivity.kt`) — shared by all modules. Never duplicated per module.

---

## Planned Screens

The following screens are defined in the navigation plan but not yet implemented.

---

### 📋 Category Home Screens (remaining)

| Category | Home Activity | Status |
|---|---|---|
| Cable TV | `CableTvActivity` | Planned |
| FASTag | `FasTagActivity` | Planned |
| Loan Repayment | `LoanRepaymentActivity` | Planned |
| Municipal Tax | `MunicipalTaxActivity` | Planned |

Each planned module also needs: Recent Transactions, Transaction Report, Transaction Status, SMS Receipt screens following the same pattern as implemented modules.

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
SplashActivity
     │
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
                           ┌──────────────────────────────┬──────────────────────────────┤
                           ▼                              ▼                               ▼
               ElectricityActivity ✅           GasActivity ✅                  DthActivity ✅
                           │                              │                               │
               ┌───────────┤               ┌─────────────┤               ┌───────────────┤
              Pay  Recent Report Status   Pay  Recent Report Status      Pay  Plans Recent Report Status
              Bill  Txns        Receipt  Bill  Txns        Receipt      Bill  Sel   Txns        Receipt
               ✅    ✅    ✅    ✅    ✅    ✅    ✅    ✅    ✅    ✅    ✅    ✅   ✅    ✅    ✅   ✅

                           │                              │
               PrepaidActivity ✅              PostpaidActivity ✅       ... (Cable, FASTag, Loan, Tax 📋)
                           │                              │
               Plans  Recent Report Status   Recent Report Status Receipt
                Sel    Txns        Receipt    Txns
                ✅      ✅    ✅    ✅   ✅     ✅    ✅    ✅    ✅

All detail taps → TransactionDetailActivity ✅ (shared by all modules)

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

All object passing between activities uses `Gson().toJson(item)` into a single `putExtra` key. See the Object Transfer Rule in `CLAUDE.md`.

| From | To | Extra Key | Type | Purpose |
|---|---|---|---|---|
| `DthActivity` | `DthPlanSelectionActivity` | `extra_operator_id` | String | Operator ID to fetch plans for |
| `DthPlanSelectionActivity` | `DthActivity` (ActivityResult) | `extra_selected_plan` | String (JSON `DthPlanItem`) | Selected plan returned to caller |
| `{Category}Activity` | `{Category}SmsReceiptActivity` | `extra_from_payment` | Boolean | `true` = after payment (hides title/tabs); `false` = from tab bar |
| Any module | `TransactionDetailActivity` | `extra_item` | String (JSON `TransactionItem`) | Full transaction detail to display |
