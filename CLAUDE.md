# PayTouch Consumer — Claude Context

Fintech Android app (Kotlin, MVVM hybrid) by Shreefintech for Indian consumers: utility bill payments (electricity, gas, mobile, DTH, cable, broadband, FASTag, loans, taxes) + digital wallet. Single user role.

> **Current state:** API wiring is **in progress**. Core modules (Auth, KYC, Bill Pay — one shared flow for Electricity, Gas, Prepaid, Postpaid, DTH, Loan, Municipal Tax —, Load Wallet, My Account) have live API integration. Bill Pay follows the iOS "Bill Pay Module — Dashboard to Receipt" doc (`docs/business_logic.md` §7); `billpay/pay` is not completable on staging yet. Newly added modules may begin with UI stubs marked `TODO(PAYTOUCH-xxx):` — do not treat pending stubs or relaxed validations as bugs.

## Docs — read only when the task touches that area

| File | Read when |
|---|---|
| `docs/caveman.md` | Need app overview, tech stack, package structure, external systems |
| `docs/business_logic.md` | Feature/data work: fees, onboarding, auth/session, endpoints, request/response fields, validation |
| `docs/dos_and_donts.md` | Bill Pay work: adding a category, transaction screens, mapping rules, list loading |
| `docs/code_patterns.md` | Need full reference code: click handling, button/list loading, LiquidGlass, keyboard, back/result, intent passing, bottom sheet |
| `docs/screens_and_navigation.md` | New screen, navigation flow, back-stack, intent extras |
| `docs/no_internet_handling.md` | Offline/connectivity handling |

If a task touches something no doc covers, **ask before proceeding**.

---

## Ask, Don't Assume (Mandatory)

If at any point during a session — while reading code, planning, or generating output — something is **missing, ambiguous, or unclear**, stop and ask before proceeding. This applies even when auto-permission is enabled.

**Never assume an answer and move on.** Examples of when to stop and ask:

- An API endpoint, request/response field, or contract detail is not documented and cannot be inferred from existing code.
- A UI requirement (layout, behavior, copy) is unspecified and multiple valid interpretations exist.
- A referenced file, class, drawable, or string resource does not exist and its intended form is unclear.
- Business logic (fee, routing, validation rule) is not covered in `docs/` and cannot be derived from analogous modules.
- The correct base class, pattern, or architecture decision is genuinely ambiguous for the new case.

**One blocking question is better than one wrong assumption baked into generated code.** Ask the question in plain language, state what you were about to assume, and wait for the answer before writing any code that depends on it.

---
## Final Verification (Mandatory)

Before presenting code, self-review and confirm: no compile errors · no logic bugs/wrong assumptions · correct nullability (`?.`, `?:`, `!!`) · no lifecycle issues (Context leaks, callbacks after destroy) · UI on main thread, background work off it · no memory leaks (anonymous inner classes holding Activity) · edge cases (empty list, null response, out-of-bounds) · no perf regressions (full redraws, redundant API calls, main-thread work) · project naming · no unused imports/variables/dead code · integrates with existing patterns · all rules in this file satisfied · no deprecated APIs · works on API 24–37. Only present the solution after this passes.

## Feature & Dependency Removal Rule (Mandatory — No Exceptions)

**Never remove a feature, dependency, module, screen, API endpoint, or any existing functionality without explicit user approval — even in auto mode, even mid-implementation, even if it looks unused/dead, even as a side-effect of another task.** Covers: Gradle deps, `ApiService`/`ApiAdminService` endpoints, Activities/Fragments/ViewModels, UI elements, business logic/validations/flows, utilities/helpers, any reachable code path. If a removal is tempting: stop, describe what and why, wait for explicit yes.

## Code Generation & Implementation Rules

- Production-ready Kotlin/MVVM: readable, performant, testable, SOLID; consider lifecycle and threading always. Never sacrifice quality for brevity.
- Analyze existing code first; follow existing architecture, naming, folder structure. Find and reuse similar Activities/ViewModels/Adapters/utils/layouts.
- Don't refactor unrelated code. Minimal, focused changes; new files only when necessary.

## Output Rules

After every task show: 1. Files Changed 2. Reason For Each Change 3. Diff Summary. Plus **Missing Assets** if any placeholders were used.

---

## Tech Stack (essentials)

Kotlin (JVM 11) · Views + ViewBinding/DataBinding (Compose only for new isolated components) · Retrofit 2.11 + OkHttp 4 + Gson 2.13 · Glide 4.16 · Bearer token in SharedPreferences · Firebase Analytics/Crashlytics/FCM · minSdk 24 / targetSdk 37. Full stack + package tree: `docs/caveman.md`.

Notes: package `utill/` spelling is intentional — never rename. FCM notification icon = `img_paytouch` (intentional — don't flag).

## Architecture Rules

1. **ApiClient** is the only source for Retrofit — never construct Retrofit directly.
2. **SharedPreferenceHelper** is the only way to read/write SharedPreferences.
3. **Constant.kt** holds all hardcoded URLs and keys — no inline strings.
4. **ToastUtil** is the only way to show user messages — never raw `Toast.makeText`.
5. **Utility.isInternetAvailable()** before every network request.
6. All Activities extend **BaseActivity** — never `AppCompatActivity` directly.
7. Network errors parsed via **ApiHelper.parseErrorMessage()**.
8. Adapters contain no business logic or network calls.
9. ViewModels must not store Activity/Context references.
10. All endpoints declared in **`ApiService`** (`backend-b2c.paytouch.in` = `Constant.BASE_URL`, via `ApiClient` — auth/onboarding under `api/mobile/`, legacy routes under `api/`) or **`ApiAdminService`** (`admin.paytouch.in`, via `ApiAdminClient`) — never construct `OkHttpClient`/`Retrofit` in a ViewModel or Activity. One client per backend URL.
11. `mapToTransactionItem()` uses `item.clientReference ?: "--"` for `userId` (Bill Pay rows have no numeric id; other lists keep `item.id?.toString() ?: "--"`) — never a loop index (duplicates on paginated appends, loses server identity).
12. **Never generate `transaction_id` / `client_reference` client-side** — no `Utility.generateTransactionId()`, no transaction id in payment request DTOs. Read `client_reference` from the `billpay/pay` response. The only client-made id is `request_id` — an idempotency key from `MoneyRequestId` (new per attempt, reused on retry of the same attempt), never a transaction id.
13. **ViewModel base class:** every ViewModel extends `AndroidViewModel` (use `bearerToken()` / `getString()` / `sendLocation()` from `ViewModelExt.kt`). `BaseBillViewModel` was removed with the per-module flows — Bill Pay has no client wallet/VPS balance check (the server decides on `billpay/pay`). Bill Pay ViewModels live in `billpay/viewmodel/` and are shared by every category — never add a per-category ViewModel.
14. `isMobileCategory` (Prepaid, Postpaid, DTH) and `isVehicleCategory` (FASTag) come from `CategoryType` in `mapToTransactionItem()` / `mapToDisplayItem()` (`BillPayTransactionsViewModel`) — never hard-code in the adapter. SMS Receipt: `binding.tvConsumerNoLabel.text` set in `SmsReceiptActivity.populateReceipt()` from `identifier_label`, else `labelConsumerNo` / `labelMobileNo` / `labelVehicleNumber`.
15. List rows show `Utility.maskNumber(item.mobileNumber)` (`9876*****0`); raw number only in `TransactionDetailActivity`.
16. Currency always via `Utility.formatAmount()` — `(Double?)` overload for Bill Pay DTOs (`retrofit/model/billpay/`), `(String?)` overload elsewhere. Never `"₹%.2f".format()` or string templates.
17. Dates only via `Utility.formatDate()` — never `SimpleDateFormat` in a ViewModel. `"dd MMM yyyy"` in `mapToDisplayItem()`; `"dd/MM/yyyy"` in `TransactionDetailActivity`; default (`"dd/MM/yyyy hh:mm a"`) for full datetime. `mapToTransactionItem()` passes `item.createdAt ?: "--"` raw.
18. **Confirmed backend quirks — do NOT "fix":**

    | Location | Apparent anomaly | Confirmed behaviour |
    |---|---|---|
    | `KycViewModel.callInitiate()` — HTTP 422 | Treated as success (proceeds to `submitSectionAPlaceholder`) | **Intentional.** 422 = KYC already initiated. Do not treat as error. |
    | `KycViewModel.agreeKyc()` — any error/failure | Both `onResponse` (any code) and `onFailure` proceed to `fetchFinalStatus()` | **Intentional.** `GET /kyc/status` is source of truth. Do not add error handling. |

19. Never use `getApplication()`/`applicationContext` for anything involving a View (no Material theme → `InflateException`), and never show a toast from a ViewModel — pass the message to the Activity via callback and show with `mActivity`.
20. **Never throw or rethrow** — log and return a fallback (`null` / early `return`). Unexpected failures (image compression, PDF render, camera bind, receipt save/share) → `Utility.logError(e)` (Crashlytics non-fatal). Expected failures (`ActivityNotFoundException`, network `onFailure`, `CancellationException`) and `OutOfMemoryError` → plain `e.printStackTrace()`.
21. **`CurlInterceptor` is local-only — intentional.** `retrofit/CurlInterceptor.kt` is gitignored (it uses `Log.d`, which the pre-commit hook blocks) and is referenced from `ApiClient` and `ApiAdminClient` under `BuildConfig.DEBUG`. All builds are made locally, so this is not a blocker. Do not flag it as a missing/untracked class, do not commit it, do not move it to a debug source set, and do not remove it from `ApiClient` / `ApiAdminClient` or `.gitignore`.

## Naming (hard rules)

| Thing | Rule | Example |
|---|---|---|
| Activity | ends `Activity` (never Screen/Page/View) | `HomeActivity` |
| Adapter | ends `Adp` (never Adapter) | `TransactionAdp` |
| Model/DTO | ends `Item` (never Response/Model/Dto/Data/Entity); qualify clashes (`UserProfileItem` vs `UserItem`) | `TransactionItem` |
| ViewModel | ends `ViewModel` | `LoginViewModel` |
| Layout | `activity_` `item_` `lyt_` `sheet_` `dialog_` | `item_transaction` |
| Drawable | `ic_` `bg_` `img_` only | `ic_wallet` |
| String | camelCase prefix: `msg` `title` `label` `hint` `btn` `err` `category` | `msgNoInternet` |

## Network Call Pattern

Endpoints return `Call<T>` — **never** `suspend fun` / `Response<T>`. Invoke with `.enqueue()` (callback already on main thread).

```kotlin
if (!Utility.isInternetAvailable(mActivity)) return
ApiClient.apiService.someEndpoint(body).enqueue(object : Callback<SomeItem> {
    override fun onResponse(call: Call<SomeItem>, response: Response<SomeItem>) {
        if (response.isSuccessful) { /* handle */ }
        else { ToastUtil.show(mActivity, ApiHelper.parseErrorMessage(mActivity, response.code(), response.errorBody()?.string())) }
    }
    override fun onFailure(call: Call<SomeItem>, t: Throwable) { ToastUtil.show(mActivity, t.localizedMessage) }
})
```

## API Response Models

- Match the real API — **never guess `General<T>`**.
  - Envelope `{data, success, meta, message}` → `Call<General<YourItem?>>`, check `response.isSuccessful && body?.data != null`.
  - Flat → `Call<YourItem>`, check `isSuccessful && body?.success == true` if it has `success`, else `isSuccessful`.
- Auth + onboarding endpoints are in **`ApiService`** under `MOBILE` (`backend-b2c.paytouch.in/api/mobile/`, via `ApiClient`) and use the envelope: `GET auth/me` → `General<MeDataItem>`; `POST auth/login` / `auth/register` → `General<LoginDataItem>`; `POST auth/mpin` → `General<MeDataItem>`; `POST auth/forgot-credential/verify-otp` → `General<ResetTokenDataItem>` (all: `isSuccessful && body?.data != null`). `POST auth/forgot-credential` / `auth/forgot-credential/reset` → `MessageItem` (`isSuccessful && success == true`). Route post-auth screens only via `NextStepRouter`.
- **API response DTOs:** every field nullable (incl. `Int?`/`Double?`/`Long?`), every field `@field:SerializedName("key")`; apply `?:` fallbacks at call site.
- **Local activity-to-activity DTOs:** nullable only if genuinely optional; no `@SerializedName` needed.

## Deprecated API & API Level Rules

Never use deprecated APIs. Common replacements: `Handler()` → `Handler(Looper.getMainLooper())` · `AsyncTask` → coroutines + `Dispatchers.IO` · `startActivityForResult`/`onActivityResult` and `requestPermissions` → `ActivityResultLauncher` · `NetworkInfo` → `NetworkCapabilities` · `Context.getColor/getDrawable` → `ContextCompat` · `versionCode` → `longVersionCode` · `SYSTEM_UI_FLAG_*` → `WindowInsetsControllerCompat`. `@Suppress("OVERRIDE_DEPRECATION")` only for intentional overrides with an explanatory comment.

Must work on API 24–37. Any API > 24 needs `if (Build.VERSION.SDK_INT >= ...)` guard + fallback (never suppress without fallback). Prefer `*Compat` helpers, `WindowInsetsControllerCompat`, `EdgeToEdgeCompat`. XML attrs newer than 24 → `layout-v{N}/` or compat equivalent. Ask: "Would this crash on Android 7?"

---

## UI Rules (full code: `docs/code_patterns.md`)

- **Clicks:** single centralized `onClickListener()` with `when (it)`; prefer DataBinding `android:onClickListener="@{onClickListener}"`; every branch starts with `if (Utility.stopClick()) return@OnClickListener`.
- **Hub/dashboard taps:** implemented module → navigate; pending → empty handler with `// TODO(PAYTOUCH-xxx): navigate to XxxActivity when implemented`. A silent empty handler without TODO is a review blocker.
- **A bill tile opens only its own category** — `BillPayActivity.start(mActivity, CategoryType.X)` with the tile's own `CategoryType` (never Electricity from the Gas tile). Non-bill modules never open each other's Activities.
- **Loading state (hard rule):** every button that triggers an API call shows a `ProgressBar` *inside the button*, driven by one `ObservableBoolean` per button (layout `<data>` variable), set `true` before the call and `false` in both `onResponse` and `onFailure`; click guard `if (showProgressX.get()) return@OnClickListener`. Dropdown/list fields: small spinner replaces the arrow icon in the field slot. Never full-screen loaders, never alpha-only dimming, never one shared `isLoading` for multiple buttons.
- **LiquidGlassButton:** call `.attach(binding.clRoot as ViewGroup)` on each one in `onCreate()` after `setContentView()` — never `LiquidGlassEffect.attach()`.
- **Keyboard (screens with EditText):** manifest `adjustResize`; `ViewCompat.setOnApplyWindowInsetsListener` padding with `maxOf(ime.bottom, systemBars.bottom)`; `Utility.hideKeyboard(mActivity)` before button actions.
- **Back:** `onBackPressedDispatcher` callback, close topmost dialog/sheet/overlay first; call `onBack()` in `onCreate()`.
- **Result:** child sets `resultCode` (0 none / 1 modified) via `setResult()` on back; parent uses `registerForActivityResult(StartActivityForResult())` and refreshes on 1.
- **Intent passing:** one domain object = one `EXTRA_ITEM` (`private const` in companion) holding `Gson().toJson(item)`; receiver `by lazy` + `val item = myItem ?: return` guards. Exception: 1–2 plain primitives (URL, title) may use individual `putExtra` keys.
- **Dialog vs bottom sheet:** `Dialog` for confirmations; `BottomSheetBehavior` (`sheet_*.xml` in `CoordinatorLayout` + `viewBg` overlay) only for UI designed as a sheet. Reference: `sheet_filter.xml`.
- **Cards:** use `MaterialCardView` for solid/rounded/stroked/pill/elevated backgrounds — no new shape drawables for those. Shape drawables only for gradients, multi-layer/vector shapes, and small status badge chips.
- **Assets:** search for existing drawables/colors/styles/dimens first; if missing, use a placeholder name (`ic_edit_placeholder`) and list under **Missing Assets**.
- **RecyclerView:** single change → update `mArrayList` + `notifyItemChanged(pos)`; delete → `removeAt` + `notifyItemRemoved`; full reload (filter/search/page 1) → `clear()` + `notifyDataSetChanged()`; pagination → `notifyItemRangeInserted`. Never reload the API to reflect a single-item change.

## Bill Pay (all bill categories — one shared flow, follows the iOS doc)

- **One flow for every bill category** (iOS "Bill Pay Module — Dashboard to Receipt"; contract in `docs/business_logic.md` §7). Screens: `billpay/BillPayActivity` (form), `BillPayPlanSelectionActivity` (Prepaid/DTH), `transactions/BillPaymentStatusActivity` (polls `billpay/status`), `billpay/transactions/` Recent / Report / Status / SmsReceipt. Each takes a `CategoryType` (`enums/CategoryType.kt`).
- **Never add a per-category Activity, ViewModel, layout, DTO set or endpoint.** A new category = a new `CategoryType` entry + its Home tile (`docs/dos_and_donts.md` → Adding a Category). The old per-module packages (`electricity/`, `gas/`, `prepaid/`, …) and their `api/<module>/…` endpoints were removed with approval — do not recreate them.
- Endpoints: only the `// ── Bill Pay ──` block in `ApiService` (`api/mobile/billpay/…`, `api/mobile/transactions`). Home tiles come from `billpay/categories` (only categories with billers).
- Amount: bill categories → `fetch-bill` (`has_bill: false` = "No bill found"); Prepaid/DTH → plans (Prepaid needs a circle). Fee from `fee-preview` only — no client fee table. Pay = MPIN popup → one `billpay/pay` with a `MoneyRequestId` `request_id`; timeout/5xx = "not confirmed" (never shown as failed), form resets.
- Pay 403s: `BANK_DETAILS_REQUIRED` → bank popup then retry, `LOCATION_REQUIRED` → resend location (`ApiHelper.parseErrorCode()`); `AADHAAR_REQUIRED` / `KYC_RESUBMIT_REQUIRED` / `USER_BLOCKED` handled in `ApiHelper`.
- Shared, never duplicated: `TransactionAdp`, `RecentTransactionAdp`, `BillPayPlanAdp`, `TransactionItem`, `RecentTransactionItem`, `TransactionDetailActivity`, `TransactionFilterHelper`, `ReceiptHelper`, `item_transaction.xml`, `item_bill_pay_field.xml`, `item_prepaid_plan.xml`, `lyt_shimmer_transaction_item.xml`, `sheet_filter.xml`, `dialog_mpin.xml`.
- `TransactionDetailActivity` has no SMS Receipt button — do not add it back.
- Category icon = `CategoryType.iconRes` (`R.drawable.img_{category}`).
- `TransactionHistoryDetailActivity` (wallet history) and `TransactionReportDetailActivity` (bill reports) are **intentionally separate** — do not merge or flag as duplication.
- FASTag tile shows "Coming soon" (iOS has no route yet) — `CategoryType.FASTAG` exists for when the backend routes it.
- Add `{module}/README.md` only when a non-bill module is fully complete (Bill Pay is documented in `docs/`); root `README.md` module table may be updated once `HomeActivity` wires it.
