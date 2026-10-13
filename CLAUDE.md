# PayTouch Consumer — Claude Context

Fintech Android app (Kotlin, MVVM hybrid) by Shreefintech for Indian consumers: utility bill payments (electricity, gas, mobile, DTH, cable, broadband, FASTag, loans, taxes) + digital wallet. Single user role.

> **Current state:** API wiring is **in progress**. Core modules (Auth, Electricity, Gas, Prepaid, Postpaid, DTH, FASTag, Loan, Municipal Tax, My Account) have live API integration. Newly added modules may begin with UI stubs marked `TODO(PAYTOUCH-xxx):` — do not treat pending stubs or relaxed validations as bugs.

## Docs — read only when the task touches that area

| File | Read when |
|---|---|
| `docs/caveman.md` | Need app overview, tech stack, package structure, external systems |
| `docs/business_logic.md` | Feature/data work: fees, onboarding, auth/session, endpoints, request/response fields, validation |
| `docs/dos_and_donts.md` | Creating a bill-payment module or transaction screens (per-module files, mapping rules, list loading) |
| `docs/code_patterns.md` | Need full reference code: click handling, button/list loading, LiquidGlass, keyboard, back/result, intent passing, bottom sheet |
| `docs/screens_and_navigation.md` | New screen, navigation flow, back-stack, intent extras |
| `docs/no_internet_handling.md` | Offline/connectivity handling |

If a task touches something no doc covers, **ask before proceeding**.

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
10. All endpoints declared in **`ApiService`** (`paytouch.in`, via `ApiClient`) or **`ApiAdminService`** (`admin.paytouch.in`, via `ApiAdminClient`) — never construct `OkHttpClient`/`Retrofit` in a ViewModel or Activity. One client per backend URL.
11. `mapToTransactionItem()` uses `item.id?.toString() ?: "--"` for `userId` — never a loop index (duplicates on paginated appends, loses server identity).
12. **Never generate `transaction_id` client-side** — no `Utility.generateTransactionId()`, no `transaction_id` in payment request DTOs. Read it from the server response.
13. **ViewModel base class by role:** main payment `{Category}ViewModel` → `BaseBillViewModel` (balance checks). RecentTransaction / TransactionReport / TransactionStatus / SmsReceipt ViewModels → `AndroidViewModel` (use `bearerToken()` / `getString()` from `ViewModelExt.kt`). Do not flag these for extending `AndroidViewModel`.
14. Set `isMobileCategory` (true: Prepaid, Postpaid, DTH) and `isVehicleCategory` (true: FASTag) in every `mapToTransactionItem()` / `mapToDisplayItem()` — never hard-code in the adapter. SMS Receipt: set `binding.tvConsumerNoLabel.text` in `populateReceiptFromApi()` → `labelConsumerNo` / `labelMobileNo` / `labelVehicleNumber`.
15. List rows show `Utility.maskNumber(item.mobileNumber)` (`9876*****0`); raw number only in `TransactionDetailActivity`.
16. Currency always via `Utility.formatAmount()` — `(String?)` overload for all modules, `(Double?)` only for Electricity DTOs. Never `"₹%.2f".format()` or string templates.
17. Dates only via `Utility.formatDate()` — never `SimpleDateFormat` in a ViewModel. `"dd MMM yyyy"` in `mapToDisplayItem()`; `"dd/MM/yyyy"` in `TransactionDetailActivity`; default (`"dd/MM/yyyy hh:mm a"`) for full datetime. `mapToTransactionItem()` passes `item.createdAt ?: "--"` raw.
18. **Confirmed backend quirks — do NOT "fix":**

    | Location | Apparent anomaly | Confirmed behaviour |
    |---|---|---|
    | `ApiService.kt` — `getMunicipalTaxTransactionStatus` | Uses `@POST("${AUTH}mobile-recharge/transaction-status")` | **Intentional backend routing.** Do not change the URL. |
    | `MunicipalTaxLatestPaymentDataItem.subService` | `@field:SerializedName("subservice")` (no underscore) | **API contract.** Backend sends `subservice`. Do not rename. |
    | `KycViewModel.callInitiate()` — HTTP 422 | Treated as success (proceeds to `submitSectionAPlaceholder`) | **Intentional.** 422 = KYC already initiated. Do not treat as error. |
    | `KycViewModel.agreeKyc()` — any error/failure | Both `onResponse` (any code) and `onFailure` proceed to `fetchFinalStatus()` | **Intentional.** `GET /kyc/status` is source of truth. Do not add error handling. |

19. Never use `getApplication()`/`applicationContext` for anything involving a View (no Material theme → `InflateException`), and never show a toast from a ViewModel — pass the message to the Activity via callback and show with `mActivity`.
20. **Never throw or rethrow** — log and return a fallback (`null` / early `return`). Unexpected failures (image compression, PDF render, camera bind, receipt save/share) → `Utility.logError(e)` (Crashlytics non-fatal). Expected failures (`ActivityNotFoundException`, network `onFailure`, `CancellationException`) and `OutOfMemoryError` → plain `e.printStackTrace()`.

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
- Auth endpoints are always flat: `GET /api/user` → `UserProfileItem`; `POST /api/login` → `LoginItem`; `POST /api/register` → `RegisterItem` (all: `isSuccessful`); `POST /api/*/send-otp|verify-otp|reset` → `MessageItem` (`isSuccessful && success == true`).
- **API response DTOs:** every field nullable (incl. `Int?`/`Double?`/`Long?`), every field `@field:SerializedName("key")`; apply `?:` fallbacks at call site.
- **Local activity-to-activity DTOs:** nullable only if genuinely optional; no `@SerializedName` needed.

## Deprecated API & API Level Rules

Never use deprecated APIs. Common replacements: `Handler()` → `Handler(Looper.getMainLooper())` · `AsyncTask` → coroutines + `Dispatchers.IO` · `startActivityForResult`/`onActivityResult` and `requestPermissions` → `ActivityResultLauncher` · `NetworkInfo` → `NetworkCapabilities` · `Context.getColor/getDrawable` → `ContextCompat` · `versionCode` → `longVersionCode` · `SYSTEM_UI_FLAG_*` → `WindowInsetsControllerCompat`. `@Suppress("OVERRIDE_DEPRECATION")` only for intentional overrides with an explanatory comment.

Must work on API 24–37. Any API > 24 needs `if (Build.VERSION.SDK_INT >= ...)` guard + fallback (never suppress without fallback). Prefer `*Compat` helpers, `WindowInsetsControllerCompat`, `EdgeToEdgeCompat`. XML attrs newer than 24 → `layout-v{N}/` or compat equivalent. Ask: "Would this crash on Android 7?"

---

## UI Rules (full code: `docs/code_patterns.md`)

- **Clicks:** single centralized `onClickListener()` with `when (it)`; prefer DataBinding `android:onClickListener="@{onClickListener}"`; every branch starts with `if (Utility.stopClick()) return@OnClickListener`.
- **Hub/dashboard taps:** implemented module → navigate; pending → empty handler with `// TODO(PAYTOUCH-xxx): navigate to XxxActivity when implemented`. A silent empty handler without TODO is a review blocker.
- **No cross-module Activity navigation — ever** (never open `ElectricityActivity` from the Gas tile).
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

## Bill Payment Modules

- Electricity is the canonical template. Each module has its **own Activity + `activity_*.xml`**; `item_*.xml` layouts are **shared** — reuse, never copy.
- Shared, never duplicated per module: `TransactionAdp`, `RecentTransactionAdp`, `TransactionItem`, `RecentTransactionItem`, `TransactionDetailActivity`, `TransactionFilterHelper`, `ReceiptHelper`, `item_transaction.xml`, `lyt_shimmer_transaction_item.xml`, `sheet_filter.xml`. Per-module file list: `docs/dos_and_donts.md`.
- `TransactionDetailActivity` has no SMS Receipt button — do not add it back.
- Category icon set in `mapToTransactionItem()` as `R.drawable.img_{category}`.
- `TransactionHistoryDetailActivity` (wallet history) and `TransactionReportDetailActivity` (bill reports) are **intentionally separate** — do not merge or flag as duplication.
- Add `{module}/README.md` only when the module is fully complete; root `README.md` module table may be updated once `HomeActivity` wires it.
