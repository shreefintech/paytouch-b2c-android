# My Account Module

User profile viewer and referral/earn screen. Two tabs: **Account Info** (KYC-derived profile data) and **Refer & Earn** (referral code, link, and share buttons).

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
| `AccountInfoItem.kt` | Response wrapper for `GET /api/kyc/account-info` |
| `AccountInfoDataItem.kt` | Account profile fields (member ID, name, balance, dates, etc.) |
| `ReferralInfoItem.kt` | Response wrapper for `GET /api/referral` |
| `ReferralDataItem.kt` | Referral code, link, total earnings, earning potential |

---

## Screens

### Account Info Tab

Displays KYC-verified profile data. Shows a shimmer placeholder while the API call is in-flight.

| Field | Source field |
|---|---|
| Member ID | `AccountInfoDataItem.memberId` |
| Member No | `AccountInfoDataItem.memberNo` |
| Member Code | `AccountInfoDataItem.memberCode` |
| Member Name | `AccountInfoDataItem.memberName` |
| Mobile No | `AccountInfoDataItem.mobileNo` |
| Email | `AccountInfoDataItem.email` |
| Status | `AccountInfoDataItem.status` |
| City | `AccountInfoDataItem.cityName` |
| Home Address | `AccountInfoDataItem.homeAddress` |
| Registration Date | `AccountInfoDataItem.registrationDate` — formatted `dd-MM-yyyy` |
| Activation Date | `AccountInfoDataItem.activationDate` — formatted `dd-MM-yyyy` |
| Balance | `AccountInfoDataItem.balance` — server format: `"100.00 [ Rupees One Hundred Only ]"`. Amount (before `[`) and words (inside `[ ]`) are split and displayed separately. |

**"View KYC Details" button:** `TODO(B2C-81)` — navigates to `KycDetailsActivity` when implemented.

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

| Method | Endpoint | ViewModel method | Response model |
|---|---|---|---|
| GET | `api/kyc/account-info` | `getAccountInfo()` | `AccountInfoItem` |
| GET | `api/referral` | `getReferralInfo()` | `ReferralInfoItem` |

**Success check (both endpoints):** `response.isSuccessful && response.body()?.success == true`

**Parameters:**
- `getAccountInfo` — `Authorization: Bearer <token>` header + `userId` from `SharedPreferenceHelper`
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
| `TODO(B2C-81)` | "View KYC Details" button should navigate to `KycDetailsActivity` (not yet built) |
| `TODO(PAYTOUCH-523)` | Expose `totalEarnings` and `earningPotential` fields on Refer & Earn tab |
