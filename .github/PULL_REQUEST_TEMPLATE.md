## Summary
<!-- What does this PR do? Keep it short and focused. -->

## Type of Change
- [ ] Bug fix
- [ ] New feature / screen (UI only)
- [ ] New feature / screen (API wiring)
- [ ] Refactor / cleanup
- [ ] Assets only (images / GIF / WebP / drawable)
- [ ] Documentation
- [ ] Dependencies (build.gradle)

## Changed Files
<!-- List EVERY changed file, including shared ones (TransactionAdp, TransactionItem, ApiService, Constant.kt,
     strings.xml, colors.xml, dimens.xml, AndroidManifest.xml). Say why each shared file changed and which other
     screens it affects. -->

| File | Change | Screens affected |
|------|--------|------------------|
|      |        |                  |

## How to Test
<!-- Steps to verify manually. Include every screen touched through a shared file, not just the main screen. -->
1.

---

## Developer Guidelines Agreement

> Before submitting, read [CLAUDE.md](../CLAUDE.md), [docs/dos_and_donts.md](../docs/dos_and_donts.md), and [docs/business_logic.md](../docs/business_logic.md) fully.

- [ ] **I have read `CLAUDE.md` and confirm this PR complies with all guidelines. I take full responsibility for the changes introduced in this PR.**

---

## Self-Review Checklist

### Scope
- [ ] Every changed file relates to the ticket and appears in "Changed Files" above
- [ ] Shared files (`TransactionAdp`, `TransactionItem`, `ApiService`, `strings.xml`, `colors.xml`) are append-only — no existing entries removed or replaced without a clear reason
- [ ] A UI-only PR does not touch `ApiService.kt`, `ApiClient.kt`, or other modules' ViewModels / Activities
- [ ] `AndroidManifest.xml` changes are intentional and explained
- [ ] No new Gradle dependency without team sign-off

### Architecture & Naming
- [ ] All Activities extend `BaseActivity` (never `AppCompatActivity` directly)
- [ ] All SharedPreferences access goes through `SharedPreferenceHelper`
- [ ] All toasts use `ToastUtil` (never raw `Toast.makeText`)
- [ ] All user messages use `Utility.isInternetAvailable()` before every network request
- [ ] Network errors parsed via `ApiHelper.parseErrorMessage()`
- [ ] Hardcoded URLs / keys live in `Constant.kt`, none inline
- [ ] Naming follows conventions: `Adp` adapters, `Item` models, `ViewModel` ViewModels, `Activity` activities
- [ ] Layouts use correct prefix: `activity_`, `item_`, `lyt_`, `sheet_`, `dialog_`, `ic_`, `bg_`, `img_`
- [ ] String IDs use camelCase context prefix: `msg`, `title`, `label`, `hint`, `btn`, `err`, `category`

### UI / View
- [ ] Checked existing drawables / layouts before creating new ones — no duplicates
- [ ] Card/container backgrounds use `MaterialCardView` (no new `<shape>` drawable for what `MaterialCardView` handles)
- [ ] All user-facing strings come from `strings.xml`, none hardcoded in Kotlin/XML
- [ ] `LiquidGlassButton` calls `.attach(root as ViewGroup)` in `onCreate()` after `setContentView()`
- [ ] Single centralized `onClickListener()` with `when (it)` block — no scattered `setOnClickListener()` calls
- [ ] Every button click guarded with `Utility.stopClick()`
- [ ] Screens with `EditText` declare `adjustResize` in `AndroidManifest.xml` and handle window insets
- [ ] Hub/dashboard taps have explicit outcome — no silent no-op; pending modules have `TODO(PAYTOUCH-xxx):` comment

### RecyclerView & Adapter
- [ ] `TransactionAdp` and `TransactionItem` are shared — no per-module duplicate created
- [ ] `TransactionDetailActivity` in `transactions/` is reused — no per-module duplicate created
- [ ] `mapToTransactionItem()` uses `item.id?.toString() ?: "--"` for `userId` (never loop index)
- [ ] `isMobileCategory` flag set correctly in every `mapToTransactionItem()` and `mapToDisplayItem()`
- [ ] `Utility.maskNumber()` used for account numbers shown in list rows
- [ ] `Utility.formatAmount()` overload matches DTO field type (`String?` or `Double?`)
- [ ] `Utility.formatDate()` used — no direct `SimpleDateFormat` calls in ViewModels
- [ ] Adapter updates are targeted: `notifyItemChanged`, `notifyItemRemoved`, `notifyItemRangeInserted` — no full reload for single-item changes

### Loading State
- [ ] Every button-triggered API call shows an in-button `ProgressBar` (one `ObservableBoolean` per button)
- [ ] Button click handler guards against tapping while loading (`if (showProgress.get()) return@OnClickListener`)
- [ ] Dropdown / list field shows inline `ProgressBar` replacing the arrow icon while loading
- [ ] No full-screen overlay or alpha-dimming as the sole loading signal

### Backend Wiring (skip for UI-only PRs)
- [ ] All endpoints declared in `ApiService.kt` or `ApiAdminService.kt` — no direct `Retrofit`/`OkHttpClient` construction
- [ ] All API methods return `Call<T>`, invoked with `.enqueue()` (no `suspend fun` / `Response<T>`)
- [ ] ViewModel base class matches role table: payment VM → `BaseBillViewModel`; report/status/SMS VMs → `AndroidViewModel`
- [ ] No `transaction_id` generated client-side (backend owns ID generation)
- [ ] API response DTOs: every field nullable (`?`) + `@field:SerializedName` on every field
- [ ] Local/activity-to-activity DTOs: no `@field:SerializedName`; nullable only where genuinely optional
- [ ] Objects passed between Activities as JSON string via `Gson().toJson()` (not individual `putExtra` fields)
- [ ] `ToastUtil` called from Activity only — never from ViewModel (use callback to pass message)
- [ ] No `getApplication()` / `applicationContext` for anything involving a View or toast

### Build & Safety
- [ ] Builds with no new warnings or errors
- [ ] No compile-time errors, nullability issues (`?.`, `?:`, `!!` used correctly)
- [ ] No Context leaks, no callbacks after `onDestroy`
- [ ] No heavy work on the main thread; UI updates only on main thread
- [ ] No sensitive data committed (API keys, tokens, credentials, `.env` files)
- [ ] All unused imports, variables, and dead code removed
