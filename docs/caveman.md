# PayTouch Consumer — Plain English Overview

> **Current status:** All core modules (Auth, Electricity, Gas, Prepaid, Postpaid, DTH, FASTag, Loan, Municipal Tax, My Account, Load Wallet) are fully implemented with live API integration. Cable TV is the only remaining planned module. This document describes the app as it stands today.

---

## What This App Is

PayTouch Consumer is a utility bill payment and digital wallet app for Indian consumers. It lets users pay electricity bills, recharge mobile phones, pay for DTH, gas, broadband, FASTag, loans, municipal taxes, cable TV, and more — all from one place. The app is built by Shree Fintech Solutions and runs on top of the PayTouch platform (`paytouch.in`). Users can also maintain a digital wallet and track every payment they have ever made through the app.

---

## Who Uses It

**Regular Consumers — single user role.**

Every user of this app is a verified Indian consumer. There are no admin or agent roles in the consumer app — every logged-in person has the same set of features. However, a user must complete a mandatory onboarding sequence before they can access any bill payment feature:

1. Register an account
2. Complete KYC (Know Your Customer) verification
3. Create a 4-digit MPIN (used for secure login)

Until all three steps are complete, the user cannot reach the main dashboard. Virtual account creation is handled server-side automatically after KYC approval — there is no client-side virtual account screen.

---

## The Core Problem It Solves

People in India pay 10+ different bills every month — electricity, phone, gas, cable, water — to 10+ different service providers through 10+ different channels. PayTouch Consumer centralises all of these payments in one app, with a single wallet, a single transaction history, and consistent receipts.

---

## High-Level Feature List

- **Electricity bill payment** — fetch outstanding bill, pay it, get receipt
- **Gas bill payment** — same flow as electricity
- **Mobile prepaid recharge** — direct top-up for any operator and circle
- **Mobile postpaid bill payment** — fetch and pay monthly bill
- **DTH recharge** — direct recharge with plan selection
- **Cable TV bill payment** — fetch and pay cable bill
- **Broadband bill payment** — fetch and pay broadband bill
- **FASTag recharge** — top up FASTag account
- **Loan repayment** — pay EMIs for registered loan accounts
- **Municipal tax payment** — pay property/municipal taxes
- **Wallet** — view balance; wallet is the funding source for all payments
- **Load Wallet** — top up the digital wallet
- **Transaction history** — full history with category filters, date range, status filters
- **KYC onboarding** — submit identity documents to unlock the account
- **MPIN** — create and manage a 4-digit secure PIN for authentication
- **Virtual account** — register bank account details for money movement
- **Payment receipts** — SMS-style receipt display for every completed transaction
- **Sound feedback** — audio cues on payment success or failure

---

## Key Screens

Full screen list, per-module class/package tables, navigation tree and intent extras: `docs/screens_and_navigation.md`. Cable TV is the only planned module.

---

## How Authentication Works

**Registration:**
1. User enters phone number, email, full name, password, confirm password, and optional referral code
2. App calls the register API and receives a Bearer token
3. Token is saved — user is now logged in
4. App checks whether KYC and MPIN steps are complete and routes accordingly

**Login (two modes):**
- **Password mode:** Enter mobile + password
- **MPIN mode:** Enter mobile + 4-digit MPIN
- Both modes call the same login API
- On success, the Bearer token is saved in SharedPreferences
- App checks response flags (`requires_kyc`, `requires_mpin`) to decide where to route next (`requires_virtual_account` is permanently retired — handled server-side)

**Session:**
- Every API call sends the Bearer token in the `Authorization` header
- If the server returns a 401 at any point, the app immediately clears all stored data and returns to LoginActivity
- The user stays logged in until they explicitly log out or the token is invalidated

**Logout:**
- Calls a logout API endpoint
- Clears all SharedPreferences (token, user data, etc.)
- Navigates back to Login with the back stack cleared

---

## Mandatory Onboarding Sequence

```
Register / Login
      ↓
KYC (requires_kyc = true) → KycStatusActivity (pending/approved/rejected)
      ↓ approved
MPIN creation (requires_mpin = true)
      ↓
HomeActivity (all steps complete)
```

A user who skips any step cannot access payment features. The server drives this via flags on the login/register response.

---

## Important Business Rules (App-Wide)

Internet check, transaction-ID ownership → `CLAUDE.md`. Platform fee tiers, transaction history endpoints, payment sounds → `docs/business_logic.md`.

- **Payment gateways:** Bill payments use the PayTouch platform. Wallet top-up uses the HDFC SmartGateway (WebView flow via `LoadWalletActivity` → `HdfcWebViewActivity`).
- **Referral code:** New users can enter a referral code during registration (optional).

---

## External Systems the App Talks To

| System | What It Does |
|---|---|
| PayTouch Main API (`paytouch.in`) | All core features: auth, KYC, MPIN, bill payments, wallet |
| VPS Admin Backend (`admin.paytouch.in`) | Parallel tracking of users, transactions, KYC, and balance |
| Legacy MobiKwik API (`dashboard.shreefintechsolutions.com`) | Old payment processing — some flows may still route here; audit before any new endpoint wiring |
| HDFC SmartGateway | Payment processing for bill payments |
| Dynamic QR Server | QR-based payment flow (URL must be stable before release) |
| Firebase Cloud Messaging (FCM) | Push notifications |
| Firebase Analytics | User behaviour analytics |
| Firebase Crashlytics | Crash reporting |
| UPI apps | App-to-app UPI payment intents |

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin (JVM 11) |
| UI | Views with ViewBinding + DataBinding; Jetpack Compose for new isolated components only |
| Networking | Retrofit 2.11 + OkHttp 4 + Gson 2.13 |
| Architecture | Hybrid MVVM (some screens are Activity-driven, some use ViewModel) |
| Image loading | Glide 4.16 |
| Auth | Bearer token stored in SharedPreferences |
| Push | Firebase Cloud Messaging 25.0 (planned) |
| Analytics | Firebase Analytics + Crashlytics |
| Min/Target SDK | 24 / 37 |

---

## Package Structure (top-level)

```
com.shreefintech.paytouchconsumer/
├── auth/           # Login, OTP, password/MPIN flows, create-account
├── applock/        # AppLockHelper (lifecycle-based lock rules) + AppLockActivity (phone screen lock prompt)
├── kyc/            # KYC hub (KycActivity), kyc/identity/IdentityVerificationActivity, kyc/bank/BankDetailsActivity, KycStatusActivity; virtual account is server-side — no client Activity
├── home/           # Home/Dashboard (HomeActivity — currently at root level, will move here)
├── electricity/    # Electricity bill payment + transaction history (canonical module template)
├── gas/            # Gas bill payment + transaction history
├── prepaid/        # Mobile prepaid recharge — operator + circle + plan selection
├── postpaid/       # Mobile postpaid bill payment
├── dth/            # DTH recharge — operator + plan selection
├── fastag/         # FASTag recharge — vehicle number + amount, real-time fee, no bill-fetch
├── loan/           # Loan repayment — bill-fetch pattern; circleId = "0"
├── municipaltax/   # Municipal tax payment — bill-fetch pattern
├── loadwallet/     # Wallet top-up via HDFC gateway (LoadWallet, HdfcWebView, PaymentStatus, WalletTransactions)
├── myaccount/      # My Account — two-tab profile viewer (Account Info + Refer & Earn), KycDetails, DocPreview
├── transactions/   # Shared TransactionDetailActivity + TransactionItem model (never duplicated per module)
├── adapter/        # Shared adapters: TransactionAdp, RecentTransactionAdp, PrepaidPlanAdp, DthPlanAdp
├── fcm/            # Push notifications — MyFirebaseMessagingService + NotificationHelper (channel, token register)
├── location/       # LocationPermissionHelper — disclosure dialog → permission → one foreground fix for payment risk checks
├── enums/          # Project-wide enums (LoginMode, etc.)
├── glass/          # LiquidGlassEffect custom blur UI components
├── retrofit/       # All networking (ApiService, ApiClient, ApiHelper, models)
├── utill/          # Shared utilities — spelling "utill" is intentional, never rename
├── widget/         # Reusable custom views (LiquidGlassButton, CustomDropdown, etc.)
├── BaseActivity.kt
├── BaseBillViewModel.kt
├── HomeActivity.kt
└── Constant.kt
```
