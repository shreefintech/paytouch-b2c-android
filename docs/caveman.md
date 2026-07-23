# PayTouch Consumer — Plain English Overview

> **Current phase:** UI implementation. Most screens are being built without live API calls. API wiring and business logic finalization happen in a later phase. Everything in this document describes the intended final behaviour of the app.

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
4. Set up a Virtual Account (banking details for receiving/sending money)

Until all four steps are complete, the user cannot reach the main dashboard.

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

## Key Screens and Their Purpose

| Screen | Class | Status |
|---|---|---|
| Login | `LoginActivity` | Implemented (UI) |
| Register | `CreateAccountActivity` | Implemented (UI) |
| OTP Verification | `OtpVerificationActivity` | Implemented (UI) |
| Reset Password | `ResetPasswordActivity` | Implemented (UI) |
| Reset MPIN | `ResetMpinActivity` | Implemented (UI) |
| KYC | `UploadKycActivity` | Implemented (UI) |
| Virtual Account | `CreateVirtualAccountActivity` | Implemented (UI) |
| Home / Dashboard | `HomeActivity` | Implemented (UI) |
| Category screens | TBD | Planned |
| Pay Bill screens | TBD | Planned |
| Transaction screens | TBD | Planned |
| My Account | TBD | Planned |
| Load Wallet | TBD | Planned |

---

## How Authentication Works

**Registration:**
1. User enters phone number, email, full name, password, confirm password, and optional referral code
2. App calls the register API and receives a Bearer token
3. Token is saved — user is now logged in
4. App checks whether KYC, MPIN, and Virtual Account steps are complete and routes accordingly

**Login (two modes):**
- **Password mode:** Enter mobile + password
- **MPIN mode:** Enter mobile + 4-digit MPIN
- Both modes call the same login API
- On success, the Bearer token is saved in SharedPreferences
- App checks response flags (`requires_kyc`, `requires_mpin`, `requires_virtual_account`) to decide where to route next

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
KYC (requires_kyc = true)
      ↓
MPIN creation (requires_mpin = true)
      ↓
Virtual Account (requires_virtual_account = true)
      ↓
HomeActivity (all steps complete)
```

A user who skips any step cannot access payment features. The server drives this via flags on the login/register response.

---

## Important Business Rules (App-Wide)

1. **Internet check first:** Always call `Utility.isInternetAvailable()` before any network request. If no connection, show message and abort.

2. **Platform fees:** Every payment carries a service fee:
   - Under ₹1,000 → ₹4
   - ₹1,000–₹5,000 → ₹8
   - ₹5,001–₹40,000 → ₹20
   - Above ₹40,000 → ₹30

3. **Transaction ID format:** `PYTCH[DDMMYYYYHHMMSS]M` — generated client-side before the API call (e.g., `PYTCH19012026091530M`).

4. **Local transaction storage:** Every completed or attempted transaction is stored locally so the user can view history even without internet.

5. **Payment sound feedback:** A success or failure sound plays after every payment attempt.

6. **Single payment gateway:** All payments are routed through the HDFC SmartGateway. Dynamic QR payment is also supported as an alternative.

7. **Referral code:** New users can enter a referral code during registration (optional).

---

## External Systems the App Talks To

| System | What It Does |
|---|---|
| PayTouch Main API (`paytouch.in`) | All core features: auth, KYC, MPIN, bill payments, wallet |
| VPS Admin Backend (`admin.paytouch.in`) | Parallel tracking of users, transactions, KYC, and balance |
| Legacy MobiKwik API (`dashboard.shreefintechsolutions.com`) | Old payment processing — audit which endpoints are still in use |
| HDFC SmartGateway | Payment processing for bill payments |
| Dynamic QR Server | QR-based payment flow (URL must be stable before release) |
| Firebase Cloud Messaging (FCM) | Push notifications |
| Firebase Analytics | User behaviour analytics |
| Firebase Crashlytics | Crash reporting |
| UPI apps | App-to-app UPI payment intents |
