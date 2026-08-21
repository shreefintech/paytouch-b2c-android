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
- Login success, `requires_kyc = true` → `KycActivity`
- Login success, `requires_mpin = true` → MPIN creation screen (planned)
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

### ✅ KycActivity — "KYC Verification Hub"

**Purpose:** Entry point for KYC; shows a 0/2 document-progress card and launches the two required sections.

**Entry points:**
- `LoginActivity` (post-login routing, `requires_kyc = true`)
- App launch (token exists, `requires_kyc = true`)

**Exit points:**
- "Identity Verification" tap → `IdentityVerificationActivity` (for result)
- "Bank Details" tap → `BankDetailsActivity` (for result)
- Both sections under review → `KycStatusActivity` (KycActivity finishes, no back stack)

**Key UI elements:**
- Document Progress card (count + horizontal progress bar)
- Identity Verification row
- Bank Details row

---

### ✅ IdentityVerificationActivity — "Identity Verification (4-step)"

**Purpose:** Collect identity information across 4 steps: Details (mobile/email), Aadhaar upload, PAN upload, selfie capture.

**Entry points:**
- `KycActivity` ("Identity Verification" row)

**Exit points:**
- Previous on step 0 → back to `KycActivity` (no changes)
- Submit on step 4 → `setResult(1)` → back to `KycActivity`

**Key UI elements:**
- Dot step indicator (4 dots) + step title
- Step 1: Mobile Number, Email Address
- Step 2: Aadhaar number, front/back upload slots
- Step 3: PAN number, front upload slot
- Step 4: Selfie capture (system camera) with circular guide
- Previous / Continue (Submit on last step) buttons

---

### ✅ BankDetailsActivity — "Bank Details"

**Purpose:** Collect 1-4 bank accounts (account number, bank name, IFSC, branch, proof type + proof upload).

**Entry points:**
- `KycActivity` ("Bank Details" row)

**Exit points:**
- Submit → `setResult(1)` → back to `KycActivity`

**Key UI elements:**
- Dynamically added bank account cards (max 4, min 1, delete per card when > 1)
- Terms & Conditions checkbox
- "+ Add Another Bank Account" button
- Submit button

---

### ✅ KycStatusActivity — "KYC Status"

**Purpose:** Shows KYC submission result (pending / approved / rejected). On approval, routes to MPIN creation.

**Entry points:**
- `KycActivity` (after both sections submitted)

**Exit points:**
- `KYC_APPROVED` → `ResetMpinActivity` (create mode, stack cleared)
- `KYC_REJECTED` → retry button → `KycActivity`
- `PENDING_KYC` → `KycActivity`
- Pull-to-refresh re-fetches status from API

**Key UI elements:**
- Animated GIF (pending / rejected)
- Status title and description
- Retry button (visible on rejection only)
- Pull-to-refresh

---

### ✅ HomeActivity — "Home / Dashboard Screen"

**Purpose:** Central menu — shows all available bill payment categories.

**Entry points:**
- `ResetMpinActivity` (onboarding complete, after MPIN creation)
- App launch (already onboarded)

**Exit points:**
- Category tile tap → respective category screen (planned)
- "Load Wallet" tap → `LoadWalletActivity`

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

### ✅ FASTag Module

No bill-fetch step — user enters vehicle number and amount directly. Platform fee calculated in real time.

| Screen | Class | Package |
|---|---|---|
| Recharge | `FastagActivity` | `fastag/` |
| Recent Transactions | `FastagRecentTransactionActivity` | `fastag/transactions/` |
| Transaction Report | `FastagTransactionReportActivity` | `fastag/transactions/` |
| Transaction Status | `FastagTransactionStatusActivity` | `fastag/transactions/` |
| SMS Receipt | `FastagSmsReceiptActivity` | `fastag/transactions/` |

---

### ✅ Loan Repayment Module

Bill-fetch pattern (mirrors Gas). `circleId = "0"` hardcoded. Flat payment response (no `ccf` field).

| Screen | Class | Package |
|---|---|---|
| Pay Bill | `LoanActivity` | `loan/` |
| Recent Transactions | `LoanRecentTransactionActivity` | `loan/transactions/` |
| Transaction Report | `LoanTransactionReportActivity` | `loan/transactions/` |
| Transaction Status | `LoanTransactionStatusActivity` | `loan/transactions/` |
| SMS Receipt | `LoanSmsReceiptActivity` | `loan/transactions/` |

---

### ✅ Municipal Tax Module

Bill-fetch pattern. Transaction-status endpoint intentionally routes via `mobile-recharge/transaction-status` (backend-side routing — do not change the URL).

| Screen | Class | Package |
|---|---|---|
| Pay Bill | `MunicipalTaxActivity` | `municipaltax/` |
| Recent Transactions | `MunicipalTaxRecentTransactionActivity` | `municipaltax/transactions/` |
| Transaction Report | `MunicipalTaxTransactionReportActivity` | `municipaltax/transactions/` |
| Transaction Status | `MunicipalTaxTransactionStatusActivity` | `municipaltax/transactions/` |
| SMS Receipt | `MunicipalTaxSmsReceiptActivity` | `municipaltax/transactions/` |

---

### ✅ Load Wallet Module

Wallet top-up via HDFC payment gateway. Distinct from all bill-payment modules — no bill-fetch, no operator selection, no `BaseBillViewModel`. See `loadwallet/README.md` for the full payment flow and return-URL interception detail.

| Screen | Class | Package |
|---|---|---|
| Wallet Balance & Top-up | `LoadWalletActivity` | `loadwallet/` |
| Full Transaction History | `WalletTransactionsActivity` | `loadwallet/` |
| HDFC Payment WebView | `HdfcWebViewActivity` | `loadwallet/` |
| Payment Status | `PaymentStatusActivity` | `loadwallet/` |

**Entry points:** `HomeActivity` ("Load Wallet" button)

**Exit points (from `PaymentStatusActivity`):** auto-navigates back to `LoadWalletActivity` after 5 s, or immediately on back press.

---

### ✅ MyAccountActivity — "My Account"

**Purpose:** View KYC-derived user profile and referral/earn information.

**Entry points:** `HomeActivity` ("My Account" tile)

**Exit points:** Back → `HomeActivity`

**Tabs:**

| Tab | Content |
|---|---|
| Account Info | Member ID/No/Code/Name, Mobile, Email, Status, City, Address, Registration Date, Activation Date, Balance |
| Refer & Earn | Referral Code (copyable), Referral Link (copyable + shareable via WhatsApp / Facebook / Email / Share) |

**API calls (both triggered on `onCreate()`):**

| Endpoint | Purpose |
|---|---|
| `GET api/kyc/account-info` | Fetch account profile data |
| `GET api/referral` | Fetch referral code and link |

**Pending:** `TODO(B2C-81)` — "View KYC Details" navigates to `KycDetailsActivity` when built. `TODO(PAYTOUCH-523)` — expose `totalEarnings` and `earningPotential` on Refer & Earn tab.

---

## Planned Screens

The following screens are defined in the navigation plan but not yet implemented.

---

### 📋 Cable TV Module

Bill-fetch pattern (mirrors Gas/Electricity).

---

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
                                       requires_kyc ────────────► KycActivity ✅
                                                                     │
                                                                     └── Both sections done ──► KycStatusActivity ✅
                                                                                                        │
                                                                                          KYC_APPROVED ──► ResetMpinActivity ✅
                                                                     │
                                       requires_mpin ───────────────────────────────────────────────────►┐
                                                                     │                                   │
                                                                     │                   MPIN created ───►┤
                                                                     │
                                       All flags false ───────────────────────────────────────────────────►┤
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
               PrepaidActivity ✅              PostpaidActivity ✅       FastagActivity ✅
                           │                              │                    │
               Plans  Recent Report Status   Recent Report Status    Recent Report Status Receipt
                Sel    Txns        Receipt    Txns        Receipt      Txns
                ✅      ✅    ✅    ✅   ✅     ✅    ✅    ✅    ✅         ✅    ✅    ✅    ✅

                           │                              │
               LoanActivity ✅             MunicipalTaxActivity ✅    MyAccountActivity ✅
                           │                              │
               Recent Report Status Receipt  Recent Report Status Receipt  (Account Info + Refer & Earn tabs)
                Txns                          Txns
                ✅    ✅    ✅    ✅             ✅    ✅    ✅    ✅

               ... (Cable TV 📋)

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
