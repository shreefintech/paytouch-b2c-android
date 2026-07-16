# PayTouch Consumer — Plain English Overview

## What This App Is

PayTouch Consumer is a utility bill payment and digital wallet app for Indian consumers. It lets users pay their electricity bills, recharge their mobile phones, pay for DTH, gas, broadband, FASTag, loans, municipal taxes, cable TV, and more — all from one place. The app is built by Shree Fintech Solutions and runs on top of the PayTouch platform (`paytouch.in`). Users can also maintain a digital wallet and track every payment they have ever made through the app.

---

## Who Uses It

**Regular Consumers (single user role)**

Every user of this app is a verified Indian consumer. There are no admin or agent roles in the consumer app — every person who logs in has the same set of features. However, a user must complete a mandatory onboarding sequence before they can access any bill payment feature:

1. Register an account
2. Complete KYC (Know Your Customer) verification
3. Create a 4-digit MPIN (a PIN used for secure login)
4. Set up a Virtual Account (banking details for receiving/sending money)

Until all four steps are complete, the user cannot reach the main dashboard.

---

## The Core Problem It Solves

People in India pay 10+ different bills every month — electricity, phone, gas, cable, water — to 10+ different service providers through 10+ different channels. PayTouch Consumer centralizes all of these payments in one app, with a single wallet, a single transaction history, and consistent receipts.

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
- **Wallet** — view balance; wallet is used as a funding source for payments
- **Transaction history** — full history with category filters, date range, status filters
- **Transaction search** — look up any past transaction by ID or consumer number
- **KYC onboarding** — submit identity documents to unlock the account
- **MPIN** — create and manage a 4-digit secure PIN for authentication
- **Virtual account** — register bank account details for money movement
- **Sound feedback** — audio cues on payment success or failure
- **Payment receipts** — SMS-style receipt display for every completed transaction

---

## Key Screens and Their Purpose

| Screen | Purpose |
|---|---|
| Splash | App entry point — plays a video, then routes to login or dashboard |
| Login | Sign in with mobile number and password or MPIN |
| Register | Create a new account with name, phone, email, password, referral code |
| KYC | Submit personal and document details (PAN, Aadhaar, etc.) |
| Create MPIN | Set a 4-digit PIN for future logins |
| Virtual Account | Register bank account details to complete setup |
| Dashboard | Main menu — 9 category tiles the user taps to start a payment |
| Category Home | Entry screen for each bill type (e.g., Electricity home) |
| Pay Bill | The actual payment form: operator → consumer number → fetch bill → pay |
| Transaction Status | Look up the status of a specific payment by transaction ID |
| Recent Transactions | Scrollable list of recent payments for a category |
| Transaction Report | Filtered report with date range, status, and consumer number filters |
| SMS Receipt | View the confirmation message/receipt for a completed payment |

---

## How Authentication Works

**Registration:**
1. User enters phone number, email, full name, password, confirm password, and optional referral code
2. App calls the register API and receives a Bearer token
3. Token is saved immediately — user is now logged in
4. App checks whether KYC, MPIN, and Virtual Account steps are complete

**Login (two modes):**
- **Password mode:** Enter mobile + password
- **MPIN mode:** Enter mobile + 4-digit MPIN
- Both modes call the same login API
- On success, the Bearer token is saved in SharedPreferences
- App checks the response flags (`requires_kyc`, `requires_mpin`, `requires_virtual_account`) to decide where to send the user next

**Session:**
- Every API call sends the Bearer token in the `Authorization` header
- If the server returns a 401 (Unauthorized) response at any point, the app immediately clears all stored data and sends the user back to the login screen
- The user stays logged in until they explicitly log out or the server invalidates their token

**Logout:**
- Calls a logout API endpoint
- Clears all SharedPreferences (token, user ID, KYC data, etc.)
- Navigates back to Login with the back stack cleared (user cannot go back)

**MPIN Reset Flow:**
1. Request OTP to registered mobile
2. Enter OTP to verify identity
3. Set new 4-digit MPIN

**Password Reset Flow:**
1. Request OTP to registered mobile
2. Enter OTP to verify identity
3. Set new password

---

## Important Business Rules (App-Wide)

1. **Mandatory onboarding:** A user who skips KYC, MPIN, or Virtual Account creation cannot access any payment feature. Each step must be completed in order.

2. **Platform fees:** Every payment carries a service fee added on top of the bill amount:
   - Bill amount under ₹1,000 → flat ₹4 fee
   - ₹1,000 – ₹5,000 → flat ₹8 fee
   - ₹5,001 – ₹40,000 → flat ₹20 fee
   - Above ₹40,000 → flat ₹30 fee

3. **Internet check:** The app checks for internet connectivity before making any network call. If there is no connection, a message is shown and the API call is not attempted.

4. **Transaction ID format:** Every transaction gets a locally-generated ID before the API call, in the format `PYTCH[DDMMYYYYHHMMSS]M` (e.g., `PYTCH19012026091530M`). This is submitted to the server.

5. **Local transaction storage:** Every completed (or attempted) transaction is stored locally in the device database so the user can view history even without internet.

6. **Payment sound feedback:** A success or failure sound plays after every payment attempt.

7. **Single payment gateway:** All payments are routed through the HDFC SmartGateway. Dynamic QR payment is also supported as an alternative payment method.

8. **Referral code:** New users can enter a referral code during registration (optional).

---

## External Systems the App Talks To

| System | What It Does |
|---|---|
| PayTouch Main API (`paytouch.in`) | All core features: auth, KYC, MPIN, bill payments, wallet |
| VPS Admin Backend (`admin.paytouch.in`) | Parallel tracking of users, transactions, KYC, and balance |
| Legacy MobiKwik API (`dashboard.shreefintechsolutions.com`) | Old payment processing endpoint (some flows still use this) |
| HDFC SmartGateway | Payment processing for bill payments |
| Dynamic QR Server (ngrok tunnel) | QR-based payment flow |
| Firebase Cloud Messaging (FCM) | Push notifications |
| Firebase Analytics | User behaviour analytics |
| Firebase Crashlytics | Crash reporting |
| UPI apps | App-to-app UPI payment intents |
