# PayTouch Consumer — Screens & Navigation

---

## Screen Inventory

### ONBOARDING FLOW

---

#### SplashActivity → "Splash / Launch Screen"
**Purpose:** App entry point. Plays a branding video, determines where to route the user.

**Entry points:** Android launcher (first screen of the app)

**Exit points:**
- No saved token → `LoginActivity`
- Token exists + `requires_kyc = true` → `KYCActivity`
- Token exists + `requires_mpin = true` → `MpinActivity`
- Token exists + `requires_virtual_account = true` → `VirtualAccountActivity`
- Token exists + all flags false → `DashboardCategoryActivity`

**Data received:** None (reads SharedPreferences internally)

**Key UI elements:**
- Full-screen video playback (MediaPlayer)
- Auto-routing after video ends or a timeout

---

#### LoginActivity → "Login Screen"
**Purpose:** Authenticate the user with mobile number and password or MPIN.

**Entry points:**
- SplashActivity (no token)
- Any screen (forced logout after 401)
- DashboardCategoryActivity (explicit logout)

**Exit points:**
- Successful login, `requires_kyc = true` → `KYCActivity`
- Successful login, `requires_mpin = true` → `MpinActivity`
- Successful login, `requires_virtual_account = true` → `VirtualAccountActivity`
- Successful login, all clear → `DashboardCategoryActivity`
- "Register" tap → `CreateAccountActivity`
- "Forgot Password" tap → Password reset flow

**Data received:** None (reads LOGIN_PREF for remember-me pre-fill)

**Key UI elements:**
- Mobile number field (pre-filled if remember-me was set)
- Password field / MPIN field (toggle between modes)
- Login mode toggle (Password / MPIN)
- "Remember me" checkbox
- "Register" button
- "Forgot Password" link

---

#### CreateAccountActivity → "Registration Screen"
**Purpose:** Create a new account.

**Entry points:** `LoginActivity`

**Exit points:**
- Successful registration → same routing logic as login (KYC / MPIN / VA / Dashboard)

**Data received:** None

**Key UI elements:**
- Name, mobile, email, password, confirm password fields
- Optional referral code field
- Submit button

---

#### KYCActivity → "KYC Verification Screen"
**Purpose:** Collect identity information to verify the user.

**Entry points:**
- SplashActivity (if `requires_kyc = true`)
- LoginActivity (post-login routing)

**Exit points:**
- Successful KYC submission → `MpinActivity`

**Data received:** Reads existing KYC data from `api/kyc/account-info` to pre-fill

**Key UI elements:**
- Mobile number (pre-filled, possibly read-only)
- Full name, address, city fields
- Date of birth (date picker — no text input)
- Age (auto-calculated, read-only display)
- Email field
- PAN card number field (validated before submit)
- Aadhaar number field (validated before submit)
- Optional GST field
- Submit button

---

#### MpinActivity → "MPIN Creation Screen"
**Purpose:** Create a 4-digit security PIN.

**Entry points:**
- KYCActivity (post-KYC routing)
- SplashActivity (if `requires_mpin = true`)

**Exit points:**
- Successful MPIN creation → `VirtualAccountActivity`

**Data received:** None

**Key UI elements:**
- 4-digit PIN entry (numeric only)
- 4-digit PIN confirmation entry
- Submit button

---

#### VirtualAccountActivity → "Virtual Account Setup Screen"
**Purpose:** Register banking details to complete onboarding.

**Entry points:**
- MpinActivity (post-MPIN routing)
- SplashActivity (if `requires_virtual_account = true`)

**Exit points:**
- Successful VA creation → `DashboardCategoryActivity` (onboarding complete)

**Data received:** None

**Key UI elements:**
- Name, mobile, city fields
- State spinner (Indian states list)
- Aadhaar number, PAN number fields
- Bank account number, IFSC code, UPI ID, branch name fields
- Four file upload buttons (Aadhaar front, Aadhaar back, PAN image, bank proof)
- File previews after selection
- Submit button

---

### MAIN APP

---

#### DashboardCategoryActivity → "Main Dashboard / Home Screen"
**Purpose:** Central menu — shows all available bill payment categories.

**Entry points:**
- VirtualAccountActivity (onboarding complete)
- SplashActivity (already onboarded)

**Exit points:**
- Category tile tap → respective Category Home Activity
- Logout → `LoginActivity` (stack cleared)

**Data received:** None (reads user from SharedPreferences)

**Key UI elements:**
- Grid of 9 category tiles:
  1. Electricity
  2. Gas
  3. Mobile Prepaid
  4. Mobile Postpaid
  5. DTH
  6. FASTag
  7. Loan Repayment
  8. Municipal Taxes
  9. My Account
  10. Load Wallet
- User name / wallet balance display (top)
- Logout option (menu or icon)

---

### ELECTRICITY SCREENS (same pattern repeats for Gas, Postpaid, Cable, Broadband, Loan, Municipal)

---

#### ElectricityActivity → "Electricity Home"
**Purpose:** Entry point for electricity bill payment. Shows sub-options.

**Entry points:** `DashboardCategoryActivity`

**Exit points:**
- "Pay Bill" → `ElectricityPayBillActivity`
- "Recent Transactions" → `ElectricityRecentTransactionActivity`
- "Transaction Report" → `ElectricityTransactionReportActivity`
- "Transaction Status" → `ElectricityTransactionStatusActivity`

---

#### ElectricityPayBillActivity → "Pay Electricity Bill"
**Purpose:** Full bill fetch-and-pay flow.

**Entry points:** `ElectricityActivity`

**Exit points:**
- After payment → `ElectricityTransactionStatusActivity` or receipt screen

**Data received:** None (user selects operator + consumer number on screen)

**Key UI elements:**
- Operator spinner (loaded from API)
- Consumer number text field
- "Fetch Bill" button
- Bill details card (outstanding amount, due date, consumer name) — shown after fetch
- Platform fee display
- "Pay" button (enabled only after successful bill fetch)
- Loading indicator during fetch and payment

---

#### ElectricityRecentTransactionActivity → "Recent Electricity Transactions"
**Purpose:** Display list of recent electricity payments from local Room DB.

**Entry points:** `ElectricityActivity`

**Exit points:** Back → `ElectricityActivity`

**Key UI elements:**
- RecyclerView of transactions (txnId, consumer number, amount, status, date)
- Pull-to-refresh or auto-load on entry

---

#### ElectricityTransactionReportActivity → "Electricity Transaction Report"
**Purpose:** Filtered view of electricity transactions.

**Entry points:** `ElectricityActivity`

**Key UI elements:**
- From-date / to-date pickers
- Status filter (All / Success / Failed / Processing)
- Consumer number search field
- "Search" button
- Results RecyclerView

---

#### ElectricityTransactionStatusActivity → "Electricity Transaction Status"
**Purpose:** Look up a specific transaction by ID.

**Entry points:**
- `ElectricityActivity` (direct navigation)
- `ElectricityPayBillActivity` (post-payment redirect)

**Data received:** Optional `transaction_id` (Intent extra — pre-fills the search field if coming from payment flow)

**Key UI elements:**
- Transaction ID field
- "Check Status" button
- Status result card

---

#### ElectricitySMSReceiptActivity → "Electricity SMS Receipt"
**Purpose:** Display the confirmation/receipt message for a completed payment.

**Entry points:** Post-payment flow

**Data received:** `transaction_id` (Intent extra)

---

### MOBILE PREPAID SCREENS

---

#### MobilePrepaidActivity → "Mobile Prepaid Home"
**Entry points:** `DashboardCategoryActivity`
**Exit points:** PayBill, Recent, Report, Status screens

#### MobilePrepaidPayBillActivity → "Prepaid Recharge"
**Key difference from bill payment:** No fetch-bill step. User picks operator, circle, enters mobile and amount (or selects a plan).

**Key UI elements:**
- Operator spinner
- Circle (telecom zone) spinner
- Mobile number to recharge
- Plan selection (optional — launches plan list)
- Amount field (or auto-filled from plan)
- "Recharge" button

---

### DTH SCREENS

---

#### DTHActivity → "DTH Home"
#### DTHPayBillActivity → "DTH Recharge"
**Key difference:** Has plan selection step. Plans fetched from `api/dth/plans` after operator is selected.

---

### FASTAG SCREENS

---

#### FasTagActivity → "FASTag Home"
#### FasTagPayBillActivity → "FASTag Recharge"
**Flow:** fetch-bill (account lookup) → confirm amount → process-recharge

---

### LOAN REPAYMENT SCREENS

---

#### LoanRepaymentActivity → "Loan Repayment Home"
#### LoanRepaymentPayBillActivity → "Pay Loan EMI"
**Flow:** Select lender → enter loan account number → fetch-bill → pay

---

### MUNICIPAL TAX SCREENS

---

#### MunicipalTaxActivity → "Municipal Tax Home"
#### MunicipalTaxPayBillActivity → "Pay Municipal Tax"
**Flow:** Select municipality → enter consumer number → fetch-bill → pay

---

### ACCOUNT MANAGEMENT

---

#### MyAccountActivity → "My Account"
**Purpose:** View user profile, manage MPIN, view KYC status.

**Entry points:** `DashboardCategoryActivity` ("My Account" tile)

---

### WALLET

---

#### LoadWalletActivity → "Load Wallet"
**Purpose:** Top up the user's digital wallet.

**Entry points:** `DashboardCategoryActivity` ("Load Wallet" tile)

---

## Full Navigation Tree

```
[App Launch]
     │
     ▼
SplashActivity (video + session check)
     │
     ├── No token ──────────────────────────────────────► LoginActivity
     │                                                          │
     │                                                          ├── "Register" ──► CreateAccountActivity
     │                                                          │                          │
     │                                                          │                          └── (same routing as login ↓)
     │                                                          │
     │                                                          └── Success ──┐
     │                                                                        │
     └── Token exists ────────────────────────────────────────────────────────┤
                                                                              │
                                                              Check onboarding flags:
                                                                              │
                                                    requires_kyc ────────────► KYCActivity
                                                                              │      │
                                                                              │      └── Success ──► MpinActivity
                                                                              │                          │
                                                    requires_mpin ───────────►──────────────────────────┘
                                                                              │      │
                                                                              │      └── Success ──► VirtualAccountActivity
                                                                              │                             │
                                                    requires_virtual_account ►──────────────────────────────┘
                                                                              │      │
                                                                              │      └── Success ──┐
                                                                              │                    │
                                                    All flags false ──────────────────────────────►│
                                                                                                   ▼
                                                                                    DashboardCategoryActivity
                                                                                          │
                                     ┌────────────────────────────────────────────────────┼──────────────────────────┐
                                     │                           │                         │                          │
                                     ▼                           ▼                         ▼                          ▼
                              ElectricityActivity         GasActivity            MobilePrepaidActivity         PostpaidActivity
                                     │                           │                         │                          │
                          ┌──────────┤               ┌──────────┤            ┌────────────┤             ┌────────────┤
                          │          │               │          │            │            │             │            │
                         Pay      Recent            Pay      Recent         Recharge   Recent          Pay        Recent
                         Bill      Txns             Bill      Txns                      Txns           Bill        Txns
                          │          │               │                                                  │
                        Report    Status           Report                                             Report     Status
                          │
                        SMS Receipt

                                     │                           │                         │                          │
                                     ▼                           ▼                         ▼                          ▼
                               DTHActivity              FasTagActivity          LoanRepaymentActivity     MunicipalTaxActivity
                                     │                           │                         │                          │
                                (same sub-screens per category)

                                     │                           │
                                     ▼                           ▼
                             MyAccountActivity           LoadWalletActivity


[Session expires / 401 at any point]
     │
     └──────────────────────────────────────────────────────────► LoginActivity (stack cleared)
```

---

## Intent Extras Reference

| From Screen | To Screen | Extra Key | Type | Purpose |
|---|---|---|---|---|
| PayBillActivity | TransactionStatusActivity | `transaction_id` | String | Pre-fill status lookup after payment |
| PayBillActivity | SMSReceiptActivity | `transaction_id` | String | Load receipt for completed transaction |
| Any | Any | (none standard) | | Most screens re-fetch their own data on load |

---

## Back Stack Rules

- All onboarding screens use `FLAG_ACTIVITY_CLEAR_TOP` or similar so the user cannot navigate back to a completed onboarding step
- After login, the back stack is cleared — the user cannot press Back to reach the login screen from the Dashboard
- After forced logout (401), the stack is completely cleared with `FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_CLEAR_TASK`
- Within a bill category, Back navigation returns to the Category Home screen
