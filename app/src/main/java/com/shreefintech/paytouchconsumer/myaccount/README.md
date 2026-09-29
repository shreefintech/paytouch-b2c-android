# My Account Module

User profile viewer and referral/earn screen. Two tabs: **Account Info** (KYC-derived profile data) and **Refer & Earn** (referral code, link, and share buttons).

---

## Getting Oriented

**Package:** `com.shreefintech.paytouchconsumer.myaccount`

**First files to open:**
1. `MyAccountActivity.kt` — two-tab screen; both API calls triggered in `onCreate()` (no manual refresh)
2. `viewmodel/MyAccountViewModel.kt` — extends `AndroidViewModel` (not `BaseBillViewModel`); two independent API calls
3. `KycDetailsActivity.kt` — in this same package; opened from "View KYC Details" button; shows submitted KYC documents
4. `retrofit/model/myaccount/` folder — `AccountInfoItem`, `ReferralInfoItem` and their data classes

**Launched from:** `HomeActivity` → `binding.llMyAccount` click handler

**ViewModel base class:** `AndroidViewModel` — this module has no bill-payment flow, so `BaseBillViewModel` is not used

**Key things to know:**
- Both tabs load their data up-front in `onCreate()` — tab switching is purely visual (no re-fetch)
- Balance (`wallet_balance`) is shown via `Utility.formatAmount()`; `"--"` when null
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
| `AccountInfoItem.kt` | Flat response for `GET /api/dashboard-kyc/account-overview`; also declares nested `AccountInfoContactItem` and `AccountInfoMembershipItem` |
| `AccountInfoMemberItem.kt` | Nested `member` object (`memberCode`, `city`, `status`) |
| `ReferralInfoItem.kt` | Response wrapper for `GET /api/referral-info` |
| `ReferralDataItem.kt` | Referral code, link, total earnings, earning potential |

---

## Screens

### Account Info Tab

Displays KYC-verified profile data. Shows a shimmer placeholder while the API call is in-flight.

| Field | Source field |
|---|---|
| Member Name | `AccountInfoItem.name` |
| Status | `AccountInfoItem.member?.status` |
| Member Code | `AccountInfoItem.member?.memberCode` |
| Mobile No | `AccountInfoItem.contact?.mobile` |
| Email | `AccountInfoItem.contact?.email` |
| Home Address | `AccountInfoItem.homeAddress` |
| Registration Date | `AccountInfoItem.membership?.registeredOn` — `Utility.formatDate(…, "dd-MM-yyyy")` |
| Activation Date | `AccountInfoItem.membership?.activatedOn` — `Utility.formatDate(…, "dd-MM-yyyy")` |
| Balance | `AccountInfoItem.walletBalance` — `Utility.formatAmount(…)`, `"--"` when null |

Text fields fall back to `"--"` when null.

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
| GET | `api/dashboard-kyc/account-overview` | `getAccountOverview()` | `getAccountInfo()` | `AccountInfoItem` |
| GET | `api/referral-info` | `getReferralInfo()` | `getReferralInfo()` | `ReferralInfoItem` |

**Success check (both endpoints):** `response.isSuccessful && response.body()?.success == true`

**Parameters:**
- `getAccountInfo` — `Authorization: Bearer <token>` header only
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
