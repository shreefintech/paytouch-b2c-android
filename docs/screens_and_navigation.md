# PayTouch Consumer — Screens & Navigation

> **Status legend:** ✅ Implemented (UI + API) | 🔧 UI only (API pending) | 📋 Planned (not started)
> All core modules are fully implemented with live API integration. Cable TV is the only remaining planned module.

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
- Login success, `next_step` is a KYC step (`NextStep.isKycStep`) → `KycActivity`
- Login success, `mpin_required = true` → `ResetMpinActivity` (create mode, via `buildCreateIntent()`)
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
- `LoginActivity` (post-login routing, KYC `next_step`)
- App launch (token exists, `auth/me` returns a KYC `next_step`)

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
- Step 4: Selfie capture — "Capture" / edit opens `SelfieCaptureActivity` (live liveness check, for result)
- Previous / Continue (Submit on last step) buttons

---

### ✅ SelfieCaptureActivity — "Live Selfie"

**Purpose:** Capture a live selfie only after an on-device liveness check (position → blink twice → hold still).

**Entry points:**
- `IdentityVerificationActivity.captureSelfie()` (step 4 Capture / edit) — for result

**Intent extras:** `extra_output_path` (String — absolute path of the JPEG file to write; primitive-only exception)

**Exit points:**
- Liveness passed + photo saved → `RESULT_OK` → back to step 4 (host compresses and shows preview)
- Back / camera permission denied / no front camera → `RESULT_CANCELED`

**Key UI elements:**
- Full-screen front camera, dimmed outside a centred circle with a 1dp stroke (`FaceCircleOverlayView`); stroke turns green after the blink
- Instruction text (16sp) below the circle + "taken automatically" hint

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
- Bill tile tap → `BillPayActivity` for that `CategoryType` (FASTag → "Coming soon" toast, no route yet)
- "Load Wallet" tap → `LoadWalletActivity`
- "My Account" tile → `MyAccountActivity`

**Key UI elements:**
- Toolbar with PayTouch logo and back button
- White card with "Categories" title
- Category tiles from `GET billpay/categories` (only categories with billers), re-flowed in rows of three; My Account always shown; a Retry row replaces the bill tiles when the first load fails
- Load Wallet button at the bottom of the card
- **Aadhaar dialog (blocking):** on every create, `GET mobile/auth/me` is checked. When `kyc_rejection.items` contains both `aadhaar_number` and `aadhaar_photo` (`MeDataItem.canResubmitAadhaar`), a non-cancelable dialog asks for the Aadhaar number + document and submits to `POST mobile/kyc/individual/resubmit`. When `next_step` is `aadhaar_required`, the same dialog opens in add mode with State + City dropdowns and submits to `POST mobile/kyc/individual/aadhaar`. Only a successful submit dismisses it, then `auth/me` is re-checked. If `auth/me` fails, Home stays usable and the check repeats on the next launch.

---

---

## Implemented Category Screens

---

### ✅ Bill Pay (all bill categories — one shared flow)

Every bill category opens the same screens with its `CategoryType` (`extra_category` = `CategoryType.value`).
Flow and APIs: `docs/business_logic.md` §7.

| Screen | Class | Package |
|---|---|---|
| Pay Bill (form: company, circle, account fields, fetch bill / browse plan, fee, MPIN) | `BillPayActivity` | `billpay/` |
| Plan selection (Prepaid, DTH) | `BillPayPlanSelectionActivity` | `billpay/` |
| Payment result (polls status) | `BillPaymentStatusActivity` | `transactions/` |
| Recent Transactions | `RecentTransactionActivity` | `billpay/transactions/` |
| Transaction Report | `TransactionReportActivity` | `billpay/transactions/` |
| Transaction Status (All / Success / Failed / Pending tabs) | `TransactionStatusActivity` | `billpay/transactions/` |
| SMS Receipt | `SmsReceiptActivity` | `billpay/transactions/` |

Categories: Electricity, Gas, Postpaid, Loan, Municipal Tax (fetch bill) · Prepaid (circle + plans) · DTH (plans).
FASTag tile shows "Coming soon" (matches iOS).

---

### ✅ Shared Transaction Detail

`TransactionDetailActivity` (`transactions/TransactionDetailActivity.kt`) — shared by all categories. Never duplicated.
View Receipt → `SmsReceiptActivity` with the row's `client_reference`.

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

**Exit points:**
- Back → `HomeActivity`
- "View KYC Details" → `KycDetailsActivity`
- Pencil icon next to Member Code → Edit Profile dialog (name, email, state, city — stays on this screen)

**Tabs:**

| Tab | Content |
|---|---|
| Account Info | Member ID/No/Code/Name, Mobile, Email, Status, City, Address, Registration Date, Activation Date, Balance |
| Refer & Earn | Referral Code (copyable), Referral Link (copyable + shareable via WhatsApp / Facebook / Email / Share) |

**API calls (both triggered on `onCreate()`):**

| Endpoint | Purpose |
|---|---|
| `GET api/mobile/auth/me` | Fetch account profile data |
| `GET api/mobile/wallet/balance` | Fetch wallet balance (main wallet) |
| `GET api/mobile/rewards/referral` | Fetch referral code, link and totals |

**Pending:** `TODO(PAYTOUCH-523)` — expose `totalEarnings` and `earningPotential` on Refer & Earn tab.

---

### ✅ KycDetailsActivity — "KYC Details"

**Purpose:** Show the submitted KYC details (identity, bank, documents) with a ViewPager2 document slider.

**Entry points:** `MyAccountActivity` ("View KYC Details" button)

**Exit points:**
- Document tap / "Preview" button → `DocPreviewActivity`
- Back → `MyAccountActivity`

**Package:** `myaccount/`

**ViewModel:** `KycDetailsViewModel` — calls `api/dashboard-kyc/my-account` (flat response, `KycMyAccountItem`)

**Key UI elements:**
- Identity section: avatar, mobile, Aadhaar, PAN, email, verification status chip
- Bank section: account number, bank name, IFSC, branch, status chip
- Documents section: ViewPager2 slider with dot indicator, document title, "Preview" button; empty state if no documents

---

### ✅ DocPreviewActivity — "Document Preview"

**Purpose:** Preview KYC documents (PDF or image) fetched from a URL. PDFs render page-by-page via `PdfRenderer` with pinch-to-zoom; images load via Glide; unknown types fall back to a Google Docs WebView.

**Entry points:** `KycDetailsActivity` (document tap or "Preview" button)

**Exit points:** Back → `KycDetailsActivity`

**Package:** `myaccount/`

**ViewModel:** `DocPreviewViewModel` — downloads PDFs to cache via `PdfThumbnailRepository` on `Dispatchers.IO`, returns a `File` to the Activity for rendering. Images are loaded directly by Glide without going through the ViewModel.

**Key UI elements:**
- Full-screen loading spinner while downloading
- `ImageView` for PDF pages and image files (pinch-to-zoom via `ScaleGestureDetector`)
- `WebView` fallback for unknown file types
- PDF navigation bar (Prev / Next / page indicator) — visible only when rendering a PDF

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
                                                     NextStepRouter:
                                                                     │
                                       KYC next_step ───────────► KycActivity ✅
                                                                     │
                                                                     └── Both sections done ──► KycStatusActivity ✅
                                                                                                        │
                                                                                          KYC_APPROVED ──► ResetMpinActivity ✅
                                                                     │
                                       mpin_required ───────────────────────────────────────────────────►┐
                                                                     │                                   │
                                                                     │                   MPIN created ───►┤
                                                                     │
                                       Otherwise ─────────────────────────────────────────────────────────►┤
                                                                                           ▼
                                                                                    HomeActivity ✅
                                                                                          │
                                                                                          │
                     Bill tiles from billpay/categories (Electricity, Gas, Prepaid, Postpaid, DTH, Loan, Municipal Tax)
                                                                                          │
                                                                              BillPayActivity ✅ (one screen, CategoryType)
                                                                                          │
                     ┌──────────────┬──────────────┬───────────────┬──────────────┬───────┴────────────┐
               Browse Plan    Report tab      Status tab      Receipt tab     Recent Txns       Pay (MPIN)
               (Prepaid/DTH)       │               │               │               │                 │
     BillPayPlanSelectionActivity ✅  TransactionReport  TransactionStatus  SmsReceipt  RecentTransaction  BillPaymentStatusActivity ✅
                                    Activity ✅      Activity ✅       Activity ✅  Activity ✅         (polls) → SmsReceiptActivity ✅

               FASTag tile → "Coming soon" toast
               MyAccountActivity ✅ (tile, always shown) → Account Info + Refer & Earn, KycDetailsActivity ✅

               LoadWalletActivity ✅ ("Load Wallet" button)
                    │
               HdfcWebViewActivity ✅ → PaymentStatusActivity ✅
               WalletTransactionsActivity ✅ ("View All" transactions)

               MyAccountActivity ✅
                    │
               KycDetailsActivity ✅ → DocPreviewActivity ✅

               ... (Cable TV 📋)

All detail taps → TransactionDetailActivity ✅ (shared by all categories)

[401 at any point]
     └──────────────────────────────────────────────────────► LoginActivity (stack cleared)
```

---

## Navigation Rules

- **Onboarding back stack:** Users cannot navigate back to a completed onboarding step. Use `FLAG_ACTIVITY_CLEAR_TOP` or equivalent.
- **Post-login stack:** After successful login/registration, the back stack is cleared — the user cannot press Back to reach the login screen from Home.
- **Forced logout (401):** Stack completely cleared with `FLAG_ACTIVITY_NEW_TASK or FLAG_ACTIVITY_CLEAR_TASK`.
- **App lock (no idle logout):** `AppLockHelper` opens the transparent `AppLockActivity` on top of the current screen (cold start with a saved session, or back from background after the grace period). Unlocking finishes it and the user is on the same screen; Back on the lock closes the app (`finishAffinity()`). Only a 401 logs the user out.
- **Within a category:** Back navigation returns to the Category Home screen.
- **Result refresh:** Use `ActivityResultLauncher` when a child screen's data changes should trigger a refresh on the parent screen.

---

## Intent Extras Reference

All object passing between activities uses `Gson().toJson(item)` into a single `putExtra` key. Rule: `CLAUDE.md` → UI Rules (Intent passing); code: `docs/code_patterns.md`.

| From | To | Extra Key | Type | Purpose |
|---|---|---|---|---|
| `HomeActivity` / `BillPayActivity` | `BillPayActivity` and every `billpay/transactions/` screen | `extra_category` | String (`CategoryType.value`) | Which bill category the screen serves |
| `BillPayActivity` | `BillPayPlanSelectionActivity` | `extra_biller_id` / `extra_circle_code` | String / String? | Plans for the biller (circle prepaid only) |
| `BillPayPlanSelectionActivity` | `BillPayActivity` (ActivityResult) | `extra_selected_plan` | String (JSON `BillPayPlanItem`) | Selected plan returned to caller |
| `BillPayActivity` | `BillPaymentStatusActivity` | `extra_item` | String (JSON `BillPaymentStatusItem`) | client_reference, amount, status, is_final, category from billpay/pay |
| `BillPaymentStatusActivity` / `TransactionDetailActivity` | `SmsReceiptActivity` | `extra_client_reference` + `extra_from_payment` | String + Boolean | Receipt for one payment; `fromPayment` hides title/tabs |
| Any module | `TransactionDetailActivity` | `extra_item` | String (JSON `TransactionItem`) | Full transaction detail to display |
| `LoadWalletActivity` | `PaymentStatusActivity` | `extra_item` | String (JSON `PaymentStatusItem`) | Order ID, amount, status to display |
| `KycDetailsActivity` / `KycDocumentAdp` | `DocPreviewActivity` | `extra_file_url` | String | File URL to preview (PDF or image) |
| `KycDetailsActivity` / `KycDocumentAdp` | `DocPreviewActivity` | `extra_file_title` | String | Document label shown in toolbar |
