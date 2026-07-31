# Auth Module

Handles all pre-login screens: splash routing, login, account creation, OTP verification, and password/MPIN reset.

---

## Screens & ViewModels

| Activity | ViewModel | Purpose |
|---|---|---|
| `SplashActivity` | `SplashViewModel` | Entry point — validates saved session, routes to first required screen |
| `LoginActivity` | `LoginViewModel` | Password or MPIN login |
| `CreateAccountActivity` | `CreateAccountViewModel` | New user registration |
| `OtpVerificationActivity` | `OtpVerificationViewModel` | OTP entry for both forgot-password and forgot-MPIN flows |
| `ResetPasswordActivity` | `ResetPasswordViewModel` | Set new password after OTP verified |
| `ResetMpinActivity` | `ResetMpinViewModel` | Set new MPIN after OTP verified |

---

## App Launch Flow

```
SplashActivity (2s logo delay)
    │
    ├── Not logged in  ──────────────────────────────► LoginActivity
    │
    ├── No internet (but session exists) ────────────► LoginActivity
    │
    └── Logged in + internet ─► GET /api/user
            │
            ├── requires_kyc = true    ──────────────► UploadKycActivity
            ├── requires_virtual_account = true ─────► CreateVirtualAccountActivity
            └── (none) ──────────────────────────────► HomeActivity
```

> `requires_mpin = true` routes to `ResetMpinActivity` as a placeholder. A dedicated CreateMpinActivity should replace this once built.

---

## Login Flow

```
LoginActivity
    │
    ├── PASSWORD tab ─► POST /api/login  {mobile, password}
    │                       └── onSuccess ─► navigateAfterLogin()
    │
    └── MPIN tab ─────► POST /api/login  {mobile, mpin}
                            └── onSuccess ─► navigateAfterLogin()
```

`navigateAfterLogin()` applies the same `requires_kyc / requires_mpin / requires_virtual_account` routing as SplashActivity.

After a successful login, `LoginViewModel` also fires a **non-blocking** VPS registration call to `ApiAdminClient` (separate base URL). Failure is logged but never shown to the user.

---

## Forgot Password Flow

```
LoginActivity (Password tab)
    └── tvForgotPassword (mobile must be valid first)
            └── OtpVerificationActivity  flowType=RESET_PASSWORD
                    │── sendOtp on launch  ─► POST /api/password/send-otp
                    │── verifyOtp          ─► POST /api/password/verify-otp
                    └── onSuccess
                            └── ResetPasswordActivity
                                    └── POST /api/password/reset
                                            └── onSuccess ─► LoginActivity (clear top)
```

---

## Forgot MPIN Flow

```
LoginActivity (MPIN tab)
    └── tvForgotPassword
            └── OtpVerificationActivity  flowType=RESET_MPIN
                    │── sendOtp on launch  ─► POST /api/mpin/send-otp
                    │── verifyOtp          ─► POST /api/mpin/verify-otp
                    └── onSuccess
                            └── ResetMpinActivity
                                    └── POST /api/mpin/reset
                                            └── onSuccess ─► LoginActivity (clear top)
```

---

## Register Flow

```
LoginActivity
    └── llCreateAccount
            └── CreateAccountActivity
                    └── POST /api/register  {mobile, email, password, password_confirmation, referral_code}
                            └── onSuccess ─► token saved ─► LoginActivity (FLAG_CLEAR_TOP)
```

---

## Mobile Number Propagation

`mobile` travels through OTP screens via `Constant.EXTRA_MOBILE` intent extra. Always pass it forward — never re-read from SharedPreferences mid-flow.

```
LoginActivity  ──EXTRA_MOBILE──►  OtpVerificationActivity  ──EXTRA_MOBILE──►  ResetPasswordActivity
                                                                             └──► ResetMpinActivity
```

`OtpVerificationActivity.newIntent()` is the only entry point — always call it with `(context, flowType, mobile)`.

---

## OtpVerificationViewModel — flowType routing

`flowType` is either `Constant.FLOW_RESET_PASSWORD` or `Constant.FLOW_RESET_MPIN`. All three methods (`sendOtp`, `verifyOtp`, `resendOtp`) branch on this value to hit the correct endpoint.

---

## Session Storage (SharedPreferences keys)

| Key | Set by | Used by |
|---|---|---|
| `KEY_TOKEN` | `LoginViewModel`, `CreateAccountViewModel` | `SplashActivity`, `SessionInterceptor` |
| `KEY_TOKEN_TYPE` | `LoginViewModel`, `CreateAccountViewModel` | `SplashActivity` |
| `KEY_USER_ID` | `LoginViewModel`, `CreateAccountViewModel` | `SharedPreferenceHelper.isLoggedIn()` |
| `KEY_MOBILE` | `LoginViewModel` | Any screen needing the logged-in mobile |
| `KEY_EMAIL` | `LoginViewModel` | Profile screens |
| `KEY_WALLET_BALANCE` | `LoginViewModel` | Home screen |
| `KEY_REFERRAL_CODE` | `LoginViewModel`, `CreateAccountViewModel` | Profile / referral screens |

`SharedPreferenceHelper.isLoggedIn()` returns `true` only when both `KEY_TOKEN` and `KEY_USER_ID` are non-empty.

---

## API Models (retrofit/model/auth/)

| Class | File | Endpoint |
|---|---|---|
| `LoginItem` + `UserItem` | `LoginItem.kt` | POST /api/login |
| `RegisterItem` + `RegisterUserItem` | `RegisterItem.kt` | POST /api/register |
| `MessageItem` | `MessageItem.kt` | All OTP + reset endpoints |
| `UserProfileItem` | `../UserProfileItem.kt` | GET /api/user |

`MessageItem` responses must check **both** `response.isSuccessful && response.body()?.success == true`. HTTP 200 with `success = false` is a valid server error (e.g. expired OTP).

---

## 401 Handling

`SessionInterceptor` (in `retrofit/`) intercepts every response. On 401 it clears all SharedPreferences and launches `LoginActivity` with `FLAG_ACTIVITY_NEW_TASK or FLAG_ACTIVITY_CLEAR_TASK`. No per-screen handling needed.
