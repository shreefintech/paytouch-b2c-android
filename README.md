# PayTouch Consumer

Utility bill payment and digital wallet app for Indian consumers. Built by Shree Fintech Solutions.
Single user role — every verified user has the same feature set.

---

## Prerequisites

| Tool | Version |
|---|---|
| Android Studio | Hedgehog (2023.1.1) or newer |
| JDK | 11 (bundled with Android Studio) |
| Android SDK | API 36 (install via SDK Manager) |
| Kotlin | 1.9+ (bundled with the Gradle plugin) |
| Min device / emulator | API 24 (Android 7.0 Nougat) |

---

## Getting Started

```bash
# 1. Clone
git clone <repo-url>
cd PaytouchConsumer1

# 2. Open in Android Studio → File → Open → select this folder
# 3. Let Gradle sync complete (downloads dependencies automatically)
# 4. Run on emulator or device: Run → Run 'app'
```

**First launch:** The app opens at `SplashActivity` → routes to `LoginActivity` (no session yet). Use a test account or register a new one.

**Debug network calls:** All HTTP requests are logged in Logcat. Filter by tag `CURL` to see curl-formatted requests. Filter by tag `OkHttp` for raw logs.

---

## First Day Reading Order

Read these in order — each one builds on the previous:

| # | What | Why |
|---|---|---|
| 1 | `docs/caveman.md` | Plain-English system overview — how the backend works, what the app does |
| 2 | `docs/dos_and_donts.md` | Hard constraints you must not violate — read before touching any code |
| 3 | This file (root `README.md`) | Architecture, module map, and all cross-cutting rules |
| 4 | `CLAUDE.md` | Strict code-generation rules (naming, patterns, API conventions) — repeat violators become review blockers |
| 5 | `electricity/README.md` | Canonical bill-payment module — read before touching any payment flow |
| 6 | The README for the module you're working on | Module-specific flows, gotchas, and API models |

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin (JVM 11) |
| UI | Views + ViewBinding + DataBinding |
| Networking | Retrofit 2.11 + OkHttp 4 + Gson 2.13 |
| Architecture | MVVM — ViewModel + `.enqueue()` callback pattern (no coroutines) |
| Image loading | Glide 4.16 |
| Auth storage | SharedPreferences via `SharedPreferenceHelper` |
| Loading shimmer | Facebook Shimmer 0.5.0 |
| Min / Target SDK | 24 / 36 |

---

## Package Structure

```
com.shreefintech.paytouchconsumer/
|
+-- auth/               Login, OTP, password/MPIN reset, register, splash
|   \-- viewmodel/
|
+-- kyc/                KYC hub, identity verification, bank details, KYC status (post-login gate)
|   +-- identity/
|   \-- bank/
|
+-- electricity/        Electricity bill payment + transaction history (canonical module template)
|   \-- transactions/   RecentTransaction, TransactionReport, TransactionStatus, SmsReceipt
|
+-- gas/                Gas bill payment + transaction history (mirrors electricity/ exactly)
|   \-- transactions/
|
+-- prepaid/            Mobile prepaid recharge -- operator + circle + plan selection, no bill fetch
|   +-- viewmodel/
|   \-- transactions/
|
+-- postpaid/           Mobile postpaid bill payment -- bill-fetch + circle selection
|   +-- viewmodel/
|   \-- transactions/
|
+-- dth/                DTH recharge -- operator + plan selection (mirrors Prepaid flow)
|   +-- viewmodel/
|   \-- transactions/
|
+-- fastag/             FASTag recharge -- no bill-fetch, vehicle number + amount, real-time fee
|   +-- viewmodel/
|   \-- transactions/
|
+-- loan/               Loan repayment -- bill-fetch pattern; circleId = "0"
|   +-- viewmodel/
|   \-- transactions/
|
+-- municipaltax/       Municipal tax -- bill-fetch; transaction-status routes via mobile-recharge endpoint
|   +-- viewmodel/
|   \-- transactions/
|
+-- myaccount/          My Account -- two-tab profile viewer (Account Info + Refer & Earn)
|   \-- viewmodel/
|
+-- loadwallet/         Wallet top-up via HDFC payment gateway; balance, virtual account, history
|   +-- model/          WalletTransactionItem (display), PaymentStatusItem (local DTO)
|   \-- viewmodel/
|
+-- transactions/       Shared across ALL bill-payment modules -- never duplicate per module
|   +-- model/
|   |   +-- TransactionItem.kt          Category-agnostic report/status row model
|   |   \-- RecentTransactionItem.kt    Recent transaction row model
|   +-- TransactionDetailActivity.kt    Single detail screen reused by every module
|   \-- TransactionHistoryDetailActivity.kt  Wallet transaction detail (separate flow)
|
+-- adapter/            Shared adapters used across modules
|   +-- TransactionAdp.kt           Report + status lists (all modules)
|   +-- RecentTransactionAdp.kt     Recent transaction lists (all modules)
|   +-- PrepaidPlanAdp.kt           Prepaid plan selection
|   +-- DthPlanAdp.kt               DTH plan selection
|   +-- OperatorSelectionAdp.kt     Operator picker
|   \-- WalletTransactionAdp.kt     Wallet history
|
+-- retrofit/           All networking
|   +-- model/
|   |   +-- General.kt              Universal response wrapper { data, success, meta, message }
|   |   +-- electricity/            Electricity request/response DTOs
|   |   +-- gas/                    Gas request/response DTOs
|   |   +-- prepaid/                Prepaid request/response DTOs
|   |   +-- postpaid/               Postpaid request/response DTOs
|   |   +-- dth/                    DTH request/response DTOs
|   |   +-- fastag/                 FASTag request/response DTOs
|   |   +-- loan/                   Loan request/response DTOs
|   |   +-- municipaltax/           Municipal tax request/response DTOs
|   |   +-- kyc/                    KYC request/response DTOs (15+ files)
|   |   +-- myaccount/              AccountInfoItem, ReferralInfoItem
|   |   +-- hdfc/                   HdfcCreateOrderRequest, HdfcOrderItem, etc.
|   |   +-- wallet/                 WalletHistoryItem, WalletHistoryPageItem
|   |   \-- auth/                   LoginItem, RegisterItem, MessageItem
|   +-- ApiClient.kt                Main Retrofit singleton (paytouch.in)
|   +-- ApiAdminClient.kt           VPS Retrofit singleton (admin.paytouch.in)
|   +-- ApiService.kt               All endpoint declarations for paytouch.in
|   +-- ApiAdminService.kt          Endpoint declarations for admin.paytouch.in
|   +-- ApiHelper.kt                Error body parsing
|   +-- SessionInterceptor.kt       Global 401 handler -- clears session + relaunches LoginActivity
|   \-- CurlInterceptor.kt          Debug curl logger (Logcat tag: CURL)
|
+-- glass/              LiquidGlassEffect blur UI system
+-- widget/             Reusable custom views (LiquidGlassButton, CustomDropdown, OutlineTextView)
+-- enums/              Project-wide enums (LoginMode, KycSubmissionStatus, ProofType, etc.)
+-- utill/              Shared utilities -- double-l spelling is intentional, never rename
|                       (TransactionFilterHelper, ReceiptHelper, ToastUtil, Utility, SharedPreferenceHelper)
|
+-- BaseActivity.kt        All Activities extend this -- never AppCompatActivity
+-- BaseBillViewModel.kt   Shared VPS/wallet balance-check logic -- every bill-payment ViewModel extends this
+-- HomeActivity.kt        Main dashboard (will move to home/ package)
\-- Constant.kt            All URLs, SharedPrefs keys, and intent extra names
```

---

## App Launch Flow

```
SplashActivity  (2s logo -> GET /api/user)
    |
    +-- Not logged in ----------------------------------------> LoginActivity
    +-- No internet (session exists) -------------------------> LoginActivity
    |
    \-- Logged in + internet
            |
            +-- requires_kyc = true -------------------------> KycActivity
            \-- all flags clear -----------------------------> HomeActivity
```

> `requires_mpin = true` routes to `ResetMpinActivity` as a placeholder. A dedicated CreateMpinActivity should replace this once built.

---

## Mandatory Onboarding Sequence

Server-driven via flags on the login/session response. Users cannot skip any step.

```
Register / Login
    |
    +-- requires_kyc = true   --> KycActivity --> KycStatusActivity (pending/approved/rejected)
    |                                                       |
    |                                         approved --> ResetMpinActivity (create mode)
    +-- requires_mpin = true  --> ResetMpinActivity
    \-- all clear             --> HomeActivity
```

---

## Module Directory

| Module | Entry Point | Pattern | Screens | Status |
|---|---|---|---|---|
| Auth | `SplashActivity` → `LoginActivity` | — | 6 | Complete |
| KYC (Onboarding) | `KycActivity` | — | 4 + 4 fragments | Complete |
| Home | `HomeActivity` | — | 1 | Complete |
| Electricity | `ElectricityActivity` | Bill-fetch | 5 | Complete |
| Gas | `GasActivity` | Bill-fetch | 5 | Complete |
| Mobile Prepaid | `PrepaidActivity` | Plan-select | 6 | Complete |
| Mobile Postpaid | `PostpaidActivity` | Bill-fetch + circle | 5 | Complete |
| DTH | `DthActivity` | Plan-select | 6 | Complete |
| FASTag | `FastagActivity` | Amount-entry | 5 | Complete |
| Loan Repayment | `LoanActivity` | Bill-fetch | 5 | Complete |
| Municipal Tax | `MunicipalTaxActivity` | Bill-fetch | 5 | Complete |
| My Account | `MyAccountActivity` | Profile | 3 | Complete |
| Load Wallet | `LoadWalletActivity` | HDFC gateway | 4 | Complete |
| TV Cable | — | — | — | Not started |
| Broadband | — | — | — | Not started |

**Bill-fetch pattern:** operator → consumer/account number → server fetches bill → show amount → pay (Electricity, Gas, Postpaid, Loan, Municipal Tax)

**Plan-select pattern:** operator → select plan → amount auto-filled → pay (Prepaid, DTH)

**Amount-entry pattern:** operator → vehicle number → user enters amount → real-time fee → pay (FASTag)

---

## Architecture Rules

| Rule | Detail |
|---|---|
| `ApiClient.apiService` only | Never construct Retrofit directly; `ApiAdminClient` is the only valid second instance |
| `SharedPreferenceHelper` only | Never call `getSharedPreferences()` directly |
| `Constant.kt` only | No inline URL strings, key strings, or extra names elsewhere |
| `ToastUtil` only | Never `Toast.makeText()` |
| `ApiHelper.parseErrorMessage()` | Never write custom error string logic |
| `Utility.isInternetAvailable()` first | Call before every network request |
| Extend `BaseActivity` | All Activities extend `BaseActivity`, never `AppCompatActivity` |
| No Context in ViewModel field | Pass context as a lambda parameter — never store it as a field |
| No network in Adapters | All API calls belong in a ViewModel |
| `Utility.stopClick()` guard | Every click that triggers navigation, API call, or form submit must call this first |
| Extend `BaseBillViewModel` | Every main bill-payment ViewModel extends this; transaction/receipt ViewModels extend `AndroidViewModel` directly |
| Reuse `transactions/` package | `TransactionItem`, `TransactionDetailActivity`, `TransactionAdp`, `RecentTransactionAdp` are shared by every module — never fork per module |
| `Utility.maskNumber()` | Always mask the account/mobile number in list rows |
| `Utility.formatAmount()` | Always use this for currency — never `"₹%.2f".format(value)` |
| `Utility.formatDate()` | Always use this for dates — never `SimpleDateFormat` directly in a ViewModel |
| Never generate `transaction_id` client-side | Backend owns transaction ID generation |

---

## Networking

### ApiClient — `paytouch.in`

Lazy singleton. OkHttp chain (in order):

1. Header interceptor — adds `Accept: application/json`
2. **SessionInterceptor** — on 401: clears SharedPreferences, relaunches `LoginActivity` (clear back stack)
3. `HttpLoggingInterceptor` — DEBUG builds only
4. `CurlInterceptor` — DEBUG builds only (Logcat tag: `CURL`)

Timeouts: 30s connect / read / write.

### ApiAdminClient — `admin.paytouch.in`

Separate singleton. Used only for VPS user registration (fire-and-forget after login success). No interceptors.

### Endpoint Pattern

All endpoints return `Call<T>`, never `suspend fun`. Always invoked with `.enqueue()`.

```kotlin
// ApiService declaration
@FormUrlEncoded
@POST("api/some-endpoint")
fun doThing(@Field("param") param: String): Call<SomeItem>

// ViewModel call
if (!Utility.isInternetAvailable(context)) { onError(...); return }
onLoading()
ApiClient.apiService.doThing(param).enqueue(object : Callback<SomeItem> {
    override fun onResponse(call: Call<SomeItem>, response: Response<SomeItem>) {
        if (response.isSuccessful) onSuccess(response.body())
        else onError(ApiHelper.parseErrorMessage(context, response.code(), response.errorBody()?.string()))
    }
    override fun onFailure(call: Call<SomeItem>, t: Throwable) {
        onError(t.localizedMessage ?: context.getString(R.string.errGeneric))
    }
})
```

### Response Models

| Wrapper | When used | Success check |
|---|---|---|
| `General<T>` | Most non-auth endpoints | `response.isSuccessful && response.body()?.data != null` |
| `LoginItem` / `RegisterItem` / `UserProfileItem` | Auth endpoints (no wrapper) | `response.isSuccessful` |
| `MessageItem` | OTP + reset endpoints (no wrapper) | `response.isSuccessful && response.body()?.success == true` |

**DTO nullability rule:** Every field in an API response DTO must be nullable (`String?`, `Int?`, `Double?`). Gson silently sets missing fields to `null` — non-nullable fields crash at runtime.

---

## BaseActivity

All Activities extend `BaseActivity`. It provides:

- `mActivity: Activity` — stable Activity reference for use inside lambdas and callbacks
- `betterActivityResult` — pre-registered `ActivityResultLauncher`
- Transparent status + navigation bars
- Forced `fontScale = 1.0f` and `densityDpi = DENSITY_DEVICE_STABLE` (prevents accessibility overrides from breaking layouts)

---

## Toast System

Never use `Toast.makeText()`. Always use `ToastUtil`:

| Method | Color | Use for |
|---|---|---|
| `ToastUtil.showSuccess()` | Green | Operation completed (login, saved, verified) |
| `ToastUtil.showUpload()` | Blue | File uploaded |
| `ToastUtil.showEdit()` | Yellow | Data updated or edited |
| `ToastUtil.showDelete()` | Red | Error, validation failure, deletion |
| `ToastUtil.showWarning()` | Orange | Pending state or soft warning |
| `ToastUtil.showExpired()` | Gray | Session or token expired |

---

## Shared Utilities (`utill/`)

> Double-l spelling is intentional — 30+ imports reference it. Never rename.

| Utility | Purpose |
|---|---|
| `Utility.isInternetAvailable(context)` | Active network check — call before every API call |
| `Utility.stopClick()` | 800ms debounce guard — call at the top of every click handler |
| `Utility.hideKeyboard(activity)` | Dismisses soft keyboard |
| `Utility.calculatePlatformFee(amount)` | Returns platform fee for the amount (see Business Rules) |
| `Utility.maskNumber(number)` | Masks account/mobile number for display in list rows: `9876*****0` |
| `Utility.formatAmount(raw)` | Formats currency string — two overloads (`String?` and `Double?`) |
| `Utility.formatDate(raw, pattern)` | Formats date string — always use instead of `SimpleDateFormat` |
| `SharedPreferenceHelper` | Only way to read/write SharedPreferences |
| `TransactionFilterHelper` | Filter state and sheet behavior for all transaction report screens |
| `ReceiptHelper` | Receipt card capture, download (MediaStore API 29+), share |
| `FilePickerUtil` | File + image picking helpers |

---

## LiquidGlass UI System (`glass/`)

Custom blur-glass cards and buttons used throughout the app.

**LiquidGlassButton:** Call `.attach(root as ViewGroup)` in `onCreate()` after `setContentView()` for every button.
Without it the button renders with no background.

```kotlin
// In onCreate(), after setContentView():
binding.flSubmit.attach(binding.clRoot as ViewGroup)
binding.flFetchBill.attach(binding.clRoot as ViewGroup)
```

---

## Naming Conventions

| Thing | Pattern | Example |
|---|---|---|
| Activity | PascalCase + `Activity` | `ElectricityActivity` |
| Adapter | PascalCase + `Adp` — never `Adapter` | `TransactionAdp` |
| Model / DTO | PascalCase + `Item` — never `Response`, `Model`, `Dto` | `TransactionItem` |
| ViewModel | PascalCase + `ViewModel` | `LoginViewModel` |
| Layout | prefix + snake_case | `activity_home`, `item_transaction`, `sheet_filter` |
| Drawable | `ic_` icons, `bg_` shapes, `img_` raster | `ic_wallet`, `bg_otp_box` |
| String IDs | camelCase with context prefix | `msgNoInternet`, `titleHome`, `hintConsumerNumber`, `errGeneric` |
| ViewBinding field | always `binding` | — |
| Activity ref inside callbacks | always `mActivity` | — |
| Booleans | `is` / `has` / `can` prefix | `isShowPwd`, `isLastPage` |
| Constants / enum entries | `UPPER_SNAKE_CASE` | `FLOW_RESET_MPIN` |

---

## Business Rules

### Platform Fee

Applied before every payment. Use `Utility.calculatePlatformFee(amount: Double)` — never inline.

| Bill amount | Fee |
|---|---|
| < Rs.1,000 | Rs.4 |
| Rs.1,000 – Rs.5,000 | Rs.8 |
| Rs.5,001 – Rs.40,000 | Rs.20 |
| > Rs.40,000 | Rs.30 |

### Field Validation

| Field | Rule |
|---|---|
| Mobile | Exactly 10 digits, starts with 6–9 |
| Password | Minimum 8 characters |
| MPIN | Exactly 4 digits |
| PAN | `[A-Z]{5}[0-9]{4}[A-Z]` |
| Aadhaar | Exactly 12 digits |
| Email | Standard email format |

---

## Confirmed Backend Quirks — Do NOT Fix

| Location | Apparent anomaly | Confirmed behaviour |
|---|---|---|
| `getMunicipalTaxTransactionStatus` in `ApiService.kt` | Uses `@POST("mobile-recharge/transaction-status")` (not `municipal-taxes/...`) | Backend-side intentional routing. Do not change this URL. |
| `MunicipalTaxLatestPaymentDataItem.subService` | `@field:SerializedName("subservice")` has no underscore | Backend sends `subservice`, not `sub_service`. Do not rename. |
| `KycViewModel.callInitiate()` — HTTP 422 from `initiateKyc` | 422 treated the same as success | 422 means KYC was already initiated. Proceed as if initiation succeeded. |
| Gas `circleId` | Hardcoded `"0"` | Gas uses `"0"`, Electricity uses `"00"`. Both are correct per their server contracts. |

---

## Constant.kt — Key Reference

| Constant | Purpose |
|---|---|
| `BASE_URL` | `https://www.paytouch.in/` |
| `BASE_URL_ADMIN` | `https://admin.paytouch.in/` |
| `KEY_TOKEN` | SharedPrefs — Bearer token |
| `KEY_TOKEN_TYPE` | SharedPrefs — token type (e.g. "Bearer") |
| `KEY_USER_ID` | SharedPrefs — user ID (non-empty = logged in) |
| `KEY_MOBILE` | SharedPrefs — logged-in user's mobile |
| `KEY_EMAIL` | SharedPrefs — logged-in user's email |
| `KEY_WALLET_BALANCE` | SharedPrefs — last known wallet balance |
| `KEY_REFERRAL_CODE` | SharedPrefs — user's referral code |
| `EXTRA_FLOW_TYPE` | Intent extra — OTP screen routing |
| `EXTRA_MOBILE` | Intent extra — mobile number through OTP + reset screens |
| `FLOW_RESET_PASSWORD` | `"RESET_PASSWORD"` |
| `FLOW_RESET_MPIN` | `"RESET_MPIN"` |
| `EXTRA_FROM_PAYMENT` | Intent extra — `PaymentStatusActivity` → `LoadWalletActivity` (triggers refresh) |
| `HDFC_STATUS_CHARGED` / `HDFC_STATUS_AUTHORIZED` | HDFC success statuses |
| `HDFC_STATUS_NEW` | HDFC order created, payment not yet attempted |

---

## Module READMEs

| Module | README |
|---|---|
| Auth | `app/src/main/java/.../auth/README.md` |
| KYC (Onboarding) | `app/src/main/java/.../kyc/README.md` |
| Electricity | `app/src/main/java/.../electricity/README.md` — **canonical bill-payment template** |
| Gas | `app/src/main/java/.../gas/README.md` (mirrors Electricity) |
| Mobile Prepaid | `app/src/main/java/.../prepaid/README.md` |
| Mobile Postpaid | `app/src/main/java/.../postpaid/README.md` |
| DTH | `app/src/main/java/.../dth/README.md` |
| FASTag | `app/src/main/java/.../fastag/README.md` |
| Loan Repayment | `app/src/main/java/.../loan/README.md` |
| Municipal Tax | `app/src/main/java/.../municipaltax/README.md` |
| My Account | `app/src/main/java/.../myaccount/README.md` |
| Load Wallet | `app/src/main/java/.../loadwallet/README.md` |

---

## Docs

| File | What it contains | Read when |
|---|---|---|
| `docs/caveman.md` | Plain-English system overview — read first on any new task | Always first |
| `docs/business_logic.md` | Domain rules, fee tiers, routing flags, field validation | Before any feature or data-related code |
| `docs/dos_and_donts.md` | Hard architecture and coding constraints | Before any structural or architectural decision |
| `docs/screens_and_navigation.md` | Navigation graph, back-stack rules, intent extras | Before implementing a new screen or navigation flow |
| `docs/no_internet_handling.md` | Offline behavior strategy | Before touching connectivity handling |
