# PayTouch Consumer

Utility bill payment and digital wallet app for Indian consumers. Built by Shree Fintech Solutions.
Single user role -- every verified user has the same feature set.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin (JVM 11) |
| UI | Views + ViewBinding + DataBinding |
| Networking | Retrofit 2 + OkHttp 4 + Gson |
| Architecture | MVVM -- ViewModel + `.enqueue()` callback pattern |
| Image loading | Glide 4 |
| Auth storage | SharedPreferences via `SharedPreferenceHelper` |
| Min / Target SDK | 24 / 36 |

---

## Package Structure

```
com.shreefintech.paytouchconsumer/
|
+-- auth/               Login, OTP, password/MPIN reset, register, splash
|   \-- viewmodel/
|
+-- onboarding/         KYC upload, Virtual Account creation (post-login gates)
|   \-- viewmodel/
|
+-- electricity/        Electricity bill payment + transaction history (canonical module template)
|   \-- transactions/   RecentTransaction, TransactionReport, TransactionStatus, SmsReceipt
|
+-- gas/                Gas bill payment + transaction history (mirrors electricity/ exactly)
|   \-- transactions/   RecentTransaction, TransactionReport, TransactionStatus, SmsReceipt
|
+-- prepaid/            Mobile prepaid recharge -- operator + circle + plan selection, no bill fetch
|   +-- viewmodel/
|   \-- transactions/   RecentTransaction, TransactionReport, TransactionStatus, SmsReceipt
|
+-- postpaid/           Mobile postpaid bill payment -- shares PrepaidPlanSelectionActivity
|   +-- viewmodel/
|   \-- transactions/   RecentTransaction, TransactionReport, TransactionStatus, SmsReceipt
|
+-- transactions/       Shared across ALL bill-payment modules -- never duplicate per module
|   +-- model/
|   |   \-- TransactionItem.kt         Category-agnostic report/status row model
|   \-- TransactionDetailActivity.kt   Single detail screen reused by every module
|
+-- home/               (planned -- HomeActivity currently lives at root)
|
+-- adapter/            Shared adapters used across modules (TransactionAdp, RecentTransactionAdp, PrepaidPlanAdp)
|
+-- retrofit/           All networking
|   +-- model/
|   |   +-- General.kt            Universal response wrapper for most endpoints
|   |   +-- UserProfileItem.kt    GET /api/user response
|   |   +-- electricity/          Electricity request/response DTOs
|   |   +-- gas/                  Gas request/response DTOs
|   |   +-- prepaid/              Prepaid request/response DTOs
|   |   +-- postpaid/             Postpaid request/response DTOs
|   |   \-- auth/
|   |       +-- LoginItem.kt
|   |       +-- RegisterItem.kt
|   |       \-- MessageItem.kt
|   +-- ApiClient.kt              Main Retrofit singleton (paytouch.in)
|   +-- ApiAdminClient.kt         VPS Retrofit singleton (admin.paytouch.in)
|   +-- ApiService.kt             All endpoint declarations
|   +-- ApiAdminService.kt
|   +-- ApiHelper.kt              Error body parsing
|   +-- SessionInterceptor.kt     Global 401 handler
|   \-- CurlInterceptor.kt        Debug curl logger (tag: CURL)
|
+-- glass/              LiquidGlassEffect blur UI system
+-- widget/             Reusable custom views (LiquidGlassButton, CustomDropdown, OutlineTextView)
+-- enums/              LoginMode
+-- utill/              Shared utilities -- double-l spelling is intentional, never rename
|                       (TransactionFilterHelper, ReceiptHelper, ToastUtil, Utility, SharedPreferenceHelper)
|
+-- BaseActivity.kt        All Activities extend this -- never AppCompatActivity
+-- BaseBillViewModel.kt   Shared VPS/wallet balance-check logic -- every bill-payment ViewModel extends this
+-- HomeActivity.kt        Main dashboard (will move to home/ package)
\-- Constant.kt            All URLs, keys, and intent extra names
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
            +-- requires_kyc = true -------------------------> UploadKycActivity
            +-- requires_virtual_account = true -------------> CreateVirtualAccountActivity
            \-- all flags clear -----------------------------> HomeActivity
```

> `requires_mpin = true` routes to `ResetMpinActivity` as a placeholder. A dedicated CreateMpinActivity should replace this once built.

---

## Mandatory Onboarding Sequence

Server-driven via flags on the login / session response. Users cannot skip any step.

```
Register / Login
    |
    +-- requires_kyc = true             --> UploadKycActivity
    +-- requires_mpin = true            --> ResetMpinActivity (placeholder -- CreateMpinActivity not yet built)
    \-- requires_virtual_account = true --> CreateVirtualAccountActivity
                                                |
                                                \-- all done --> HomeActivity
```

---

## Modules

### Implemented

| Module | Entry Point | Sub-screens |
|---|---|---|
| Auth | `SplashActivity` -> `LoginActivity` | `CreateAccountActivity`, `OtpVerificationActivity`, `ResetPasswordActivity`, `ResetMpinActivity` |
| Onboarding | `UploadKycActivity` | `CreateVirtualAccountActivity` |
| Home | `HomeActivity` | Category grid -- routes to bill payment screens |
| Electricity | `ElectricityActivity` | `RecentTransactionActivity`, `TransactionReportActivity`, `ElectricityTransactionStatusActivity`, `TransactionDetailActivity` (shared), `SmsReceiptActivity` |
| Gas | `GasActivity` | `GasRecentTransactionActivity`, `GasTransactionReportActivity`, `GasTransactionStatusActivity`, `TransactionDetailActivity` (shared), `GasSmsReceiptActivity` |
| Mobile Prepaid | `PrepaidActivity` | `PrepaidPlanSelectionActivity`, `PrepaidRecentTransactionActivity`, `PrepaidTransactionReportActivity`, `PrepaidTransactionStatusActivity`, `TransactionDetailActivity` (shared), `PrepaidSmsReceiptActivity` |
| Mobile Postpaid | `PostpaidActivity` | `PrepaidPlanSelectionActivity` (shared from prepaid), `PostpaidRecentTransactionActivity`, `PostpaidTransactionReportActivity`, `PostpaidTransactionStatusActivity`, `TransactionDetailActivity` (shared), `PostpaidSmsReceiptActivity` |

### Planned (stubs in HomeActivity)

| Module | Status |
|---|---|
| DTH recharge | Not started |
| TV Cable payment | Not started |
| FASTag recharge | Not started |
| Loan repayment | Not started |
| Tax payment | Not started |
| My Account | Not started |
| Load Wallet | Not started |
| Broadband | Not started |

---

## Architecture Rules

| Rule | Detail |
|---|---|
| `ApiClient.apiService` only | Never construct Retrofit directly -- `ApiAdminClient` is the only valid second instance |
| `SharedPreferenceHelper` only | Never call `getSharedPreferences()` directly |
| `Constant.kt` only | No inline URL strings, key strings, or extra names elsewhere |
| `ToastUtil` only | Never `Toast.makeText()` |
| `ApiHelper.parseErrorMessage()` | Never write custom error string logic |
| `Utility.isInternetAvailable()` first | Call before every network request |
| Extend `BaseActivity` | All Activities extend `BaseActivity`, never `AppCompatActivity` |
| No Context in ViewModel field | Pass context as a lambda parameter -- never store it as a field |
| No network in Adapters | All API calls belong in a ViewModel |
| `Utility.stopClick()` guard | Every click that triggers navigation, API call, or form submit must call this first |
| Extend `BaseBillViewModel` | Every main bill-payment ViewModel (Electricity, Gas, Prepaid, Postpaid) extends this for shared `checkVpsBalance()` / `checkWalletBalance()` / `bearerToken()` -- never reimplement balance checks per module. Transaction history / receipt ViewModels extend `AndroidViewModel` directly. |
| Reuse `transactions/` package | `TransactionItem`, `TransactionDetailActivity`, `TransactionAdp`, `RecentTransactionAdp` are category-agnostic and shared by every module -- never fork per module |
| Reuse `PrepaidPlanSelectionActivity` | Postpaid shares this screen from `prepaid/` -- do not create a `PostpaidPlanSelectionActivity` |

---

## Networking

### ApiClient -- `paytouch.in`

Lazy singleton. OkHttp chain (in order):

1. Header interceptor -- adds `Accept: application/json`
2. **SessionInterceptor** -- on 401: clears SharedPreferences, relaunches `LoginActivity` (clear back stack)
3. `HttpLoggingInterceptor` -- DEBUG builds only
4. `CurlInterceptor` -- DEBUG builds only (Logcat tag: `CURL`)

Timeouts: 30s connect / read / write.

### ApiAdminClient -- `admin.paytouch.in`

Separate singleton. Used only for VPS user registration (fire-and-forget after login success). No interceptors.

### Endpoint Pattern

All endpoints return `Call<T>`, never `suspend fun`. Always invoked with `.enqueue()`.

```kotlin
// ApiService declaration
@FormUrlEncoded
@POST("api/some-endpoint")
fun doThing(@Field("param") param: String): Call<SomeItem>

// ViewModel
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

`MessageItem` requires both checks -- HTTP 200 with `success = false` is a valid server error (e.g. expired OTP).

---

## BaseActivity

All Activities extend `BaseActivity`. It provides:

- `mActivity: Activity` -- stable Activity reference for use inside lambdas and callbacks
- `betterActivityResult` -- pre-registered `ActivityResultLauncher`
- Transparent status + navigation bars
- Forced `fontScale = 1.0f` and `densityDpi = DENSITY_DEVICE_STABLE` (prevents system accessibility overrides from breaking layouts)

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

> Double-l spelling is intentional -- 30+ imports reference it. Never rename.

| Utility | Purpose |
|---|---|
| `Utility.isInternetAvailable(context)` | Active network check -- call before every API call |
| `Utility.stopClick()` | 800ms debounce guard -- call at the top of every click handler |
| `Utility.hideKeyboard(activity)` | Dismisses soft keyboard |
| `Utility.calculatePlatformFee(amount)` | Returns platform fee for the amount (see Business Rules) |
| `Utility.EmojiExcludeFilter()` | InputFilter that strips emoji |
| `Utility.digitFilter()` | InputFilter that allows digits only |
| `Utility.alphaSpaceFilter()` | InputFilter that allows letters and spaces only |
| `View.visible()` / `.gone()` / `.invisible()` | Visibility extension functions |
| `SharedPreferenceHelper` | Only way to read/write SharedPreferences |
| `FilePickerUtil` | File + image picking helpers |
| `TransactionFilterHelper` | Filter state for transaction report screens |

---

## LiquidGlass UI System (`glass/`)

Custom blur-glass cards and buttons used throughout the app.

**Card / overlay blur:**
```kotlin
LiquidGlassEffect.attach(
    targetView   = binding.flCard,
    rootView     = binding.clRoot as ViewGroup,
    cornerRadius = resources.getDimensionPixelSize(R.dimen.glass_frem_radius),
    distortion   = 0f,
    blur         = resources.getDimensionPixelSize(R.dimen.glass_frem_blur)
)
```

**LiquidGlassButton:** Call `.attach(root as ViewGroup)` in `onCreate()` after `setContentView()` for every button.
Without it the button renders with no background.

```kotlin
binding.flSubmit.attach(binding.clRoot as ViewGroup)
```

---

## Naming Conventions

| Thing | Pattern | Example |
|---|---|---|
| Activity | PascalCase + `Activity` | `ElectricityActivity` |
| Adapter | PascalCase + `Adp` -- never `Adapter` | `TransactionAdp` |
| Model / DTO | PascalCase + `Item` -- never `Response`, `Model`, `Dto` | `TransactionItem` |
| ViewModel | PascalCase + `ViewModel` | `LoginViewModel` |
| Layout | prefix + snake_case | `activity_home`, `item_transaction`, `sheet_filter` |
| Drawable | `ic_` icons, `bg_` shapes, `img_` raster | `ic_wallet`, `bg_otp_box` |
| String IDs | camelCase with context prefix | `msgNoInternet`, `titleHome`, `hintConsumerNumber`, `errGeneric` |
| ViewBinding field | always `binding` | -- |
| Activity ref inside callbacks | always `mActivity` | -- |
| Booleans | `is` / `has` / `can` prefix | `isShowPwd`, `isLastPage` |
| Constants / enum entries | `UPPER_SNAKE_CASE` | `FLOW_RESET_MPIN` |

---

## Business Rules

### Platform Fee

Applied before every payment. Use `Utility.calculatePlatformFee(amount: Double)` -- never inline.

| Bill amount | Fee |
|---|---|
| < Rs.1,000 | Rs.4 |
| Rs.1,000 - Rs.5,000 | Rs.8 |
| Rs.5,001 - Rs.40,000 | Rs.20 |
| > Rs.40,000 | Rs.30 |

### Field Validation

| Field | Rule |
|---|---|
| Mobile | Exactly 10 digits, starts with 6-9 |
| Password | Minimum 8 characters |
| MPIN | Exactly 4 digits |
| PAN | [A-Z]{5}[0-9]{4}[A-Z] |
| Aadhaar | Exactly 12 digits |
| Email | Standard email format |

---

## Constant.kt -- Key Reference

| Constant | Purpose |
|---|---|
| `BASE_URL` | `https://www.paytouch.in/` |
| `BASE_URL_ADMIN` | `https://admin.paytouch.in/` |
| `KEY_TOKEN` | SharedPrefs -- Bearer token |
| `KEY_TOKEN_TYPE` | SharedPrefs -- token type (e.g. "Bearer") |
| `KEY_USER_ID` | SharedPrefs -- user ID (non-empty = logged in) |
| `KEY_MOBILE` | SharedPrefs -- logged-in user's mobile |
| `KEY_EMAIL` | SharedPrefs -- logged-in user's email |
| `KEY_WALLET_BALANCE` | SharedPrefs -- last known wallet balance |
| `KEY_REFERRAL_CODE` | SharedPrefs -- user's referral code |
| `EXTRA_FLOW_TYPE` | Intent extra -- OTP screen routing (RESET_PASSWORD or RESET_MPIN) |
| `EXTRA_MOBILE` | Intent extra -- mobile number propagated through OTP and reset screens |
| `FLOW_RESET_PASSWORD` | "RESET_PASSWORD" |
| `FLOW_RESET_MPIN` | "RESET_MPIN" |

---

## Module READMEs

| Module | README |
|---|---|
| Auth | `app/src/main/java/.../auth/README.md` |
| Electricity | `app/src/main/java/.../electricity/README.md` (canonical bill-payment module reference) |
| Gas | `app/src/main/java/.../gas/README.md` (mirrors Electricity -- read Electricity's README first) |
| Mobile Prepaid | `app/src/main/java/.../prepaid/README.md` (adds plan selection + circle picker vs Gas/Electricity) |
| Mobile Postpaid | `app/src/main/java/.../postpaid/README.md` (shares `PrepaidPlanSelectionActivity`; status searches by transaction ID) |

---

## Docs

| File | What it contains |
|---|---|
| `docs/caveman.md` | Plain-English system overview -- read first on any new task |
| `docs/business_logic.md` | Domain rules, fee tiers, routing flags, field validation |
| `docs/dos_and_donts.md` | Hard architecture and coding constraints |
| `docs/screens_and_navigation.md` | Navigation graph, back-stack rules, intent extras |
| `docs/api_reference.md` | All endpoint signatures and response shapes |
| `docs/api_call_guide.md` | Retrofit call patterns, OkHttp setup, error parsing |