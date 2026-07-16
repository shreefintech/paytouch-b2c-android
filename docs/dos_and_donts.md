# PayTouch Consumer — DOs and DON'Ts for the New Project

Every rule here is derived from a specific pattern (good or bad) found in the existing codebase. The reason is given for each so you can judge edge cases rather than blindly following the rule.

---

## DOs — Patterns to Keep and Enforce

### Architecture

**DO use a single `ApiClient` object as the only Retrofit source.**
The existing code correctly uses singleton Retrofit instances. The new project must have exactly ONE `ApiClient` class that all ViewModels talk to via a Repository. No Activity should create its own Retrofit instance.

**DO use a single `SharedPreferenceHelper` class as the ONLY SharedPreferences access point.**
The existing code has at least three separate SharedPreferences stores with overlapping keys. The new project must funnel every read and write through one class. No Activity or ViewModel should call `getSharedPreferences()` directly.

**DO use `Constant.kt` for all URLs, keys, and string constants.**
The existing code correctly centralizes many constants. The new project must put every URL, API key, SharedPreferences key name, and bundle key in `Constant.kt`. Never hardcode a string that is used in more than one place.

**DO call `Utility.isInternetAvailable()` before every network request.**
The existing code does this consistently in many places. It is a critical UX requirement — never make an API call without this check. Show a user-facing message if there is no internet.

**DO generate transaction IDs client-side before submitting a payment.**
The existing format `PYTCH[DDMMYYYYHHMMSS]M` is correct and should be preserved in the new project. This ID is the link between local DB records and server records.

**DO store every payment attempt in the local Room database immediately after attempting.**
The existing app uses Room for offline history. This is correct. Write to Room on every payment attempt — success or failure — so history is always available.

**DO play audio feedback after every payment.**
`payment_success.mp3` and `payment_failed.mp3` are part of the UX contract. The new project must call `PaymentSoundPlayer` equivalents after every transaction result. Clean up the MediaPlayer in `onDestroy()`.

**DO enforce the mandatory onboarding sequence: KYC → MPIN → Virtual Account.**
This is a business requirement. The server drives this via flags (`requires_kyc`, `requires_mpin`, `requires_virtual_account`). Always check these flags after login and route accordingly. Never allow a user to skip a step.

**DO apply platform fee calculation before showing the final payment amount.**
The fee tiers (₹4 / ₹8 / ₹20 / ₹30 based on amount range) must be calculated and displayed before the user confirms payment. Put this logic in a ViewModel, not a Fragment or Activity.

**DO use `SessionInterceptor` (or equivalent) to handle 401 responses globally.**
The existing code has an OkHttp interceptor that catches 401 and forces logout. This is correct. The new project must implement the same mechanism as an OkHttp interceptor, not in every ViewModel callback.

**DO validate inputs before any API call.**
- Mobile: exactly 10 digits
- PAN: regex `[A-Z]{5}[0-9]{4}[A-Z]{1}`
- Aadhaar: exactly 12 digits
- Password: minimum 8 characters
- MPIN: exactly 4 digits, confirmation must match
- Email: standard email format
These validations exist in the current code. Keep them and centralize them in a `Validator.kt` utility.

**DO generate a `PYTCH`-format transaction ID for every payment before calling the API.**
This ID is generated client-side using `TransactionIdGenerator`. Keep this pattern — it allows the app to create a local DB record before the API call completes.

**DO use the `when` block + `Utility.stopClick()` guard for all click handling.**
Centralized click handling with a double-tap guard is explicitly required. One `setOnClickListener` per screen that delegates to a `handleClick()` function using a `when` block.

**DO extend `BaseActivity` for every screen.**
All Activities must extend `BaseActivity` to inherit common setup (theme, toolbar, common utilities).

**DO use `ToastUtil` for all user-facing messages.**
Never call `Toast.makeText()` directly in any Activity, Fragment, or ViewModel. Route all messages through `ToastUtil`.

**DO use `ApiHelper.parseErrorMessage()` to extract error text from failed API responses.**
The existing code uses a regex pattern (`:([^:]+)$`) to extract the meaningful part of server error messages. The new project must use this consistently — never show a raw exception message to the user.

---

### UI/UX

**DO use a date picker for all date fields (DOB, date range filters).**
The existing code correctly uses a date picker dialog for DOB in KYC and date range selectors in report screens. Never let users type dates as raw text.

**DO auto-calculate age from the selected date of birth.**
The KYC form calculates age from the DOB picker result. Keep this — never make users enter their age manually.

**DO persist "remember me" state so the mobile number pre-fills on next login.**
The existing code saves the mobile number in `LOGIN_PREF` when "remember me" is checked. This UX should be preserved.

**DO keep the dual login mode (password and MPIN toggle).**
Users can switch between password and MPIN login. The last-used mode is saved and restored. Keep this behavior.

**DO use `notifyItemChanged(position)` for single-item RecyclerView updates.**
Never call `notifyDataSetChanged()` for a single item change. This is a performance rule and is called out explicitly in the project requirements.

---

## DON'Ts — Anti-Patterns Found in This Codebase That Must Never Be Repeated

### Architecture

**DON'T put network calls in Activities or Fragments.**
The biggest structural problem in the existing codebase. Activities call Retrofit directly, handle callbacks directly, and update UI directly. All network calls must go in a Repository, called from a ViewModel, with results posted to LiveData/StateFlow.

**DON'T store a static mutable field for a token or any shared state.**
`Constant.TOKEN` is a public static field that gets overwritten by the Shreefintech token fetch. This is a race condition and a global state anti-pattern. Use a Repository singleton or SharedPreferenceHelper to pass tokens safely.

**DON'T use multiple SharedPreferences stores for the same user.**
The existing app has at least four separate SharedPreferences stores (`AUTH`, `app_prefs`, `LOGIN_PREF`, `PAYTOUCH_Session`) with overlapping keys. This creates confusion and bugs. The new project has one store accessed through one class.

**DON'T create multiple Retrofit instances for the same base URL or for overlapping data.**
The existing app has `ApiClient`, `ApiClientPayTouch`, `ApiClientAdmin`, and `PaymentApiClient` — four separate instances with confusing naming. The new project has one `ApiClient` per distinct base URL, with clear, stable names.

**DON'T hardcode a development/ngrok URL in any file that will be compiled for production.**
`PaymentApiClient` hardcodes `https://tablet-frying-shy.ngrok-free.dev/` — a ngrok tunnel that will stop working. All URLs must be in `Constant.kt` and must be stable production URLs.

**DON'T put business logic (fee calculation, validation, status routing) in Adapters or Activities.**
Fee calculation and routing logic are scattered across Activity code in the existing app. The new project must put all domain logic in a ViewModel or use-case class, keeping Activities as pure UI responders.

**DON'T create Activity references or Context references inside ViewModels.**
ViewModels must not hold references to Activities, Fragments, or Context (except `ApplicationContext` via `AndroidViewModel`). Holding Activity context in a ViewModel causes memory leaks.

**DON'T use `SessionManager.java` (the legacy class) — use only `SharedPreferenceHelper`.**
The existing code has both `SessionManager.java` (legacy, ~169 lines) and `SharePrefManager.java` (modern). They overlap. The new project has only one.

**DON'T suppress errors from the VPS sync silently without logging.**
The existing code makes fire-and-forget VPS calls. If they fail, nothing happens and nothing is logged. Even if VPS failures are non-blocking, they must be logged so issues can be detected.

**DON'T read from the database on the main thread.**
Room must always be accessed via coroutines (`Dispatchers.IO`) or RxJava. The existing code sometimes reads Room data directly from Activity callbacks. This will cause `IllegalStateException` and ANRs.

**DON'T load more than 30 seconds timeout without communicating progress to the user.**
All Retrofit instances are configured with 30-second timeouts. Any operation that takes more than 2 seconds must show a loading indicator.

**DON'T skip the internet check before any API call.**
Some flows in the existing code call the API without first checking connectivity. The new project must call `Utility.isInternetAvailable()` every time, no exceptions.

**DON'T use `notifyDataSetChanged()` on a RecyclerView for a single-item update.**
The existing code does this in several adapters. It causes full list re-renders and visual flicker. Use `notifyItemChanged(position)` or `DiffUtil`.

**DON'T mix legacy and new API endpoints for the same feature.**
Some payment flows use the old `dashboard.shreefintechsolutions.com` endpoint for some steps and `paytouch.in` for others. In the new project, each feature must use exactly one API base URL for all its calls.

**DON'T use raw `Toast.makeText()` calls directly in Activities.**
There are many direct Toast calls in the existing code. All messages must go through `ToastUtil`.

**DON'T store user-facing strings directly in Java/Kotlin code.**
All user messages must be in `strings.xml`. The existing code has many hardcoded strings.

**DON'T create a new Glide `.load()` call without specifying error and placeholder drawables.**
Image loading without a placeholder shows blank space while loading. Always specify `.placeholder()` and `.error()`.

**DON'T use `startActivity()` without calling `Utility.stopClick()` first.**
The existing code has multiple places where rapid taps can launch duplicate Activities. Every click handler must be guarded.

**DON'T use `onActivityResult()` / `startActivityForResult()` — they are deprecated.**
The existing code uses `ActivityResultLauncher` for file picking (correct). Do not introduce the old `startActivityForResult()` pattern anywhere.

**DON'T name adapters with the suffix `Adapter` — use `Adp`.**
Project naming convention is `OrderAdp` not `OrderAdapter`. The existing code mixes both. The new project uses `Adp` everywhere.

**DON'T name models with generic suffixes like `Response` or `Model` — use `Item`.**
Convention is `OrderItem`, not `OrderResponse` or `OrderModel`. Extract only the domain object from the API response; the wrapper `{ success, message, data }` never leaves the Repository layer.
