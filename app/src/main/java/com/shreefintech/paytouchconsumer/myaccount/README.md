# My Account Module

User profile viewer and referral/earn screen. Two tabs: **Account Info** (KYC-derived profile data) and **Refer & Earn** (referral code, link, and share buttons).

---

## Getting Oriented

**Package:** `com.shreefintech.paytouchconsumer.myaccount`

**First files to open:**
1. `MyAccountActivity.kt` — two-tab screen; both API calls triggered in `onCreate()` (no manual refresh)
2. `viewmodel/MyAccountViewModel.kt` — extends `AndroidViewModel` (not `BaseBillViewModel`); two independent API calls
3. `KycDetailsActivity.kt` — in this same package; opened from "View KYC Details" button; shows submitted KYC documents
4. `retrofit/model/myaccount/` folder — `ReferralRewardsItem`, `ReferralDataItem`, `EditProfileRequest`; the profile itself is `AuthUserItem` (`retrofit/model/auth/`)

**Launched from:** `HomeActivity` → `binding.llMyAccount` click handler

**ViewModel base class:** `AndroidViewModel` — this module has no bill-payment flow, so `BaseBillViewModel` is not used

**Key things to know:**
- Both tabs load their data up-front in `onCreate()` — tab switching is purely visual (no re-fetch)
- Balance (`main_wallet` from `GET api/mobile/wallet/balance`) is shown via `Utility.formatAmount()`; `"--"` when null
- `KycDetailsActivity` is in this package but calls `GET /api/dashboard-kyc/my-account` (same endpoint family as the KYC module)
- `DocPreviewActivity` is also in this package — handles full-screen image/PDF preview of KYC documents
- Clipboard copy uses `ClipboardManager` directly; share uses `Intent.ACTION_SEND` — no third-party libraries needed

---

## Entry Point

`MyAccountActivity` — launched from `HomeActivity` via `binding.llMyAccount`.

```kotlin
// HomeActivity.kt
binding.llMyAccount -> {
    if (Utility.stopClick()) return@OnClickListener
    MyAccountActivity.start(mActivity)
}
```

No data is passed on entry — all content is fetched from the API.

---

## Package

`com.shreefintech.paytouchconsumer.myaccount`

---

## Files

| File | Purpose |
|---|---|
| `MyAccountActivity.kt` | Two-tab screen: Account Info and Refer & Earn |
| `viewmodel/MyAccountViewModel.kt` | API calls for account info and referral data |

### Retrofit models — `retrofit/model/myaccount/`

| File | Purpose |
|---|---|
| `ReferralRewardsItem.kt` | Response of `GET api/mobile/rewards/referral`; totals in `meta.summary` (`ReferralDataItem`) |
| `EditProfileRequest.kt` | Body of `PUT api/mobile/auth/me` — name, email, state_id, city_id, mpin |
| `ReferralDataItem.kt` | Referral code, link, total earnings, earning potential |

---

## Screens

### Account Info Tab

Displays KYC-verified profile data. Shows a shimmer placeholder while the API call is in-flight.

| Field | Source field |
|---|---|
| Member Name | `AuthUserItem.name` (`GET api/mobile/auth/me` → `data.user`) |
| Status | `AuthUserItem.status` (first letter capitalised) |
| Member Code | Always `"--"` — not available in B2C |
| Mobile No | `AuthUserItem.mobile` |
| Email | `AuthUserItem.email` |
| Home Address | `"city, state"` from `AuthUserItem.city` / `.state` — no street address in B2C |
| Registration Date | `AuthUserItem.createdAt` — `Utility.formatDate(…, "dd-MM-yyyy")` |
| Activation Date | `AuthUserItem.activatedAt` — `Utility.formatDate(…, "dd-MM-yyyy")` |
| Balance | `WalletBalanceItem.mainWallet` — `Utility.formatAmount(…)`, `"--"` when null |

Text fields fall back to `"--"` when null.

**Edit profile (pencil next to Member Code):** Opens `dialog_edit_profile` — name, email, state, city prefilled from `AuthUserItem`. `auth/me` sends state/city as names only, so they are matched by name against the `api/mobile/address/states` / `address/states/{state_id}/cities` lists to get ids. Submit validates, then opens `dialog_mpin`; the MPIN submit calls `PUT api/mobile/auth/me`. On success both dialogs close, stored email is updated and `auth/me` is re-fetched. Ignored until account info has loaded.

**"View KYC Details" button:** Navigates to `KycDetailsActivity` (in this package) — fetches `GET /api/dashboard-kyc/my-account` via `KycDetailsViewModel`. Document taps open `DocPreviewActivity` with `extra_file_url` + `extra_file_title`.

---

### Refer & Earn Tab

Displays referral code and link with copy and share actions. Shows a shimmer placeholder while loading.

| Element | Behaviour |
|---|---|
| Referral Code | Displayed in a read-only field; **Copy** button copies to clipboard |
| Referral Link | Displayed in a read-only field; **Copy** button copies to clipboard |
| WhatsApp share | Opens WhatsApp intent with referral link pre-filled |
| Facebook share | Opens Facebook share intent with referral link |
| Email share | Opens email chooser with referral link in body |
| Generic share | Opens Android system share sheet |

> `TODO(PAYTOUCH-523)`: expose `totalEarnings` and `earningPotential` from `ReferralDataItem` once the UI area is finalised.

---

## API Endpoints

| Method | Endpoint | `ApiService` function | ViewModel method | Response model |
|---|---|---|---|---|
| GET | `api/mobile/auth/me` | `getMe()` | `getProfile()` | `General<MeDataItem>` |
| GET | `api/mobile/wallet/balance` | `getWalletBalance()` | `getWalletBalance()` | `General<WalletBalanceItem>` |
| GET | `api/mobile/rewards/referral` | `getReferralRewards()` | `getReferralInfo()` | `ReferralRewardsItem` |
| PUT | `api/mobile/auth/me` | `updateMe()` | `updateProfile()` | `General<EditProfileDataItem>` |

**Success check:** `General<…>` calls — `isSuccessful && body?.data != null` (`updateMe`: `success == true`, `data` optional); referral — `isSuccessful && success == true`

**Parameters:**
- `getProfile` / `getWalletBalance` — `Authorization: Bearer <token>` header only
- `getReferralInfo` — `Authorization: Bearer <token>` header only

**ViewModel base class:** `AndroidViewModel` — My Account has no bill-payment flow, so `BaseBillViewModel` is not used here.

---

## Patterns Used

- Both API calls are triggered in `onCreate()` — page loads content immediately, no manual refresh trigger.
- Shimmer loading state per tab — shimmer shows until the respective API call completes or fails.
- Tab switch is purely visual (no API re-fetch) — both data loads happen up front.
- Clipboard copy uses `ClipboardManager` directly — no third-party library needed.
- Share intents use `Intent.ACTION_SEND` — no extra dependency.

---

## Pending

| Ticket | What is missing |
|---|---|
| `TODO(PAYTOUCH-523)` | Expose `totalEarnings` and `earningPotential` fields on Refer & Earn tab |
