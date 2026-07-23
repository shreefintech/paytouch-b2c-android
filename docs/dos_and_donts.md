# PayTouch Consumer — DOs and DON'Ts

Every rule here is derived from a specific pattern or requirement for this project. The reason is given for each so you can judge edge cases rather than blindly following the rule.

---

## DOs — Patterns to Keep and Enforce

### Architecture

**DO use `ApiClient` as the only Retrofit source.**
This project has exactly one `ApiClient` class. ViewModels call `ApiClient.apiService` directly for standard API calls. No Activity should create its own Retrofit instance. If additional base URLs are needed (e.g., VPS backend), create one named client per URL with a clear, stable name.

**DO use `SharedPreferenceHelper` as the ONLY SharedPreferences access point.**
Every read and write must go through `SharedPreferenceHelper`. No Activity or ViewModel should call `getSharedPreferences()` directly. Having multiple scattered stores for the same user data causes bugs and confusion.

**DO put all URLs, keys, and string constants in `Constant.kt`.**
Never hardcode a string that is used in more than one place. Every URL, API key, SharedPreferences key name, and bundle key belongs in `Constant.kt`.

**DO call `Utility.isInternetAvailable()` before every network request.**
This is a critical UX requirement. Never make an API call without this check. Show a user-facing message via `ToastUtil` if there is no internet.

**DO generate transaction IDs client-side before submitting a payment.**
The format `PYTCH[DDMMYYYYHHMMSS]M` is correct and must be preserved. This ID is the link between local DB records and server records.

**DO enforce the mandatory onboarding sequence: KYC → MPIN → Virtual Account.**
The server drives this via flags (`requires_kyc`, `requires_mpin`, `requires_virtual_account`). Always check these flags after login and route accordingly. Never allow a user to skip a step.

**DO apply platform fee calculation before showing the final payment amount.**
Fee tiers (₹4 / ₹8 / ₹20 / ₹30 based on amount range) must be calculated and displayed before the user confirms payment. This logic belongs in a ViewModel, not an Activity.

**DO handle 401 responses globally via an OkHttp interceptor.**
A `SessionInterceptor` (or equivalent) must catch any 401 response, clear all SharedPreferences, and launch LoginActivity with a cleared back stack. This must not be implemented per-ViewModel.

**DO validate inputs before any API call.**
| Field | Rule |
|---|---|
| Mobile | Exactly 10 digits |
| PAN card | Regex `[A-Z]{5}[0-9]{4}[A-Z]{1}` |
| Aadhaar | Exactly 12 digits |
| Password | Minimum 8 characters |
| MPIN | Exactly 4 digits; confirmation must match |
| Email | Standard email format |

**DO use the `when` block + `Utility.stopClick()` guard for all click handling.**
One centralized `onClickListener()` per Activity that delegates to a `when` block. Every case must call `Utility.stopClick()` before any navigation, API call, or state change.

**DO extend `BaseActivity` for every screen.**
All Activities must extend `BaseActivity`. Never extend `AppCompatActivity` directly.

**DO use `ToastUtil` for all user-facing messages.**
Never call `Toast.makeText()` directly in any Activity, ViewModel, or utility class.

**DO use `ApiHelper.parseErrorMessage()` to extract error text from failed API responses.**
Never show a raw server error string or exception message to the user. Always parse it through `ApiHelper`.

**DO use a date picker for all date input fields.**
For DOB, date range filters, and any other date field — use a picker dialog, never a free-text input.

**DO auto-calculate age from the selected date of birth.**
Never make users manually enter their age. Calculate it from the DOB picker result.

**DO use `notifyItemChanged(position)` for single-item RecyclerView updates.**
Never call `notifyDataSetChanged()` for a single item change. Use targeted updates.

**DO store every payment attempt in Room DB immediately after attempting.**
Write to Room on every payment attempt — success or failure — so history is always available offline.

**DO play audio feedback after every payment.**
`payment_success.mp3` and `payment_failed.mp3` must play after every transaction result. Clean up the MediaPlayer in `onDestroy()`.

**DO use `ActivityResultLauncher` for all activity results and file picking.**
The `startActivityForResult()` / `onActivityResult()` APIs are deprecated. Use `registerForActivityResult()` everywhere.

**DO prefer Data Binding (`android:onClickListener="@{onClickListener}"`) over programmatic `setOnClickListener()`.**
Data Binding is the project's established pattern. Use it consistently.

---

## DON'Ts — Anti-Patterns to Avoid

### Architecture

**DON'T put network calls in Activities.**
All network calls must go in a ViewModel. The ViewModel calls `ApiClient.apiService` directly and surfaces results via `onLoading`/`onSuccess`/`onError` callbacks. Activities are pure UI responders.

**Repository, LiveData, and StateFlow are NOT required for standard one-shot API calls.** Use them only when:
- Data must outlive the screen (shared across multiple destinations)
- Data is heavy and needs caching or pagination
- Data represents a continuous stream of updates (WebSocket, polling)
- Multiple screens must observe the same data simultaneously

**DON'T create multiple SharedPreferences stores for the same user.**
Use one store, accessed through `SharedPreferenceHelper`. Do not call `getSharedPreferences()` with different store names for the same user's session data.

**DON'T create multiple Retrofit instances for the same base URL.**
One `ApiClient` per distinct base URL. If a second distinct backend is needed (e.g., VPS admin), create exactly one named client for it — not a new instance for each screen.

**DON'T hardcode any URL, key, or token in Activity/ViewModel code.**
All constants belong in `Constant.kt`. Especially: no hardcoded development URLs (ngrok, localhost) in any file that will be compiled for production.

**DON'T store a static mutable field for a token or shared state.**
Storing a dynamic token in a `companion object` or `static` field is a race condition anti-pattern. Use `SharedPreferenceHelper` or a Repository singleton.

**DON'T put business logic in Adapters or Activities.**
Fee calculation, status routing, and validation belong in a ViewModel or dedicated utility. Activities respond to UI events and update views — nothing more.

**DON'T store Activity or Context references as fields inside ViewModels.**
ViewModels must not hold `Activity`, `Fragment`, or `Context` as a class-level property — this causes memory leaks. Passing `context` as a function parameter for a single operation is acceptable.

**DON'T skip the internet check before any API call.**
`Utility.isInternetAvailable()` must be called every time, no exceptions.

**DON'T call `notifyDataSetChanged()` on a RecyclerView for a single-item update.**
This causes full list re-renders and visual flicker. Use `notifyItemChanged(position)`.

**DON'T use raw `Toast.makeText()` directly.**
All messages go through `ToastUtil`. This is non-negotiable.

**DON'T store user-facing strings in Kotlin/Java code.**
All user-visible messages must be in `strings.xml`.

**DON'T load images with Glide without specifying a placeholder and error drawable.**
`Glide.with(context).load(url).into(view)` without `.placeholder()` and `.error()` leaves blank space during load and on failure.

**DON'T use `startActivity()` without calling `Utility.stopClick()` first.**
Rapid taps can launch duplicate Activities. Every click handler must be guarded before any navigation.

**DON'T name adapters with the suffix `Adapter` — use `Adp`.**
Convention: `TransactionAdp`, not `TransactionAdapter`. Consistent naming makes the codebase scannable.

**DON'T name models with suffixes like `Response` or `Model` — use `Item`.**
Convention: `TransactionItem`, not `TransactionResponse` or `TransactionModel`.

**DON'T access Room DB on the main thread.**
Room must always be accessed via coroutines (`Dispatchers.IO`) or RxJava. Direct access from Activity callbacks causes `IllegalStateException` and ANRs.

**DON'T mix multiple API backends for the same feature.**
Each feature must use exactly one base URL for all its calls. Do not call `paytouch.in` for step A and `admin.paytouch.in` for step B of the same payment flow.

**DON'T suppress VPS sync failures silently.**
VPS sync calls are non-blocking, but failures must be logged so issues can be detected and debugged.
