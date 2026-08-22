# No Internet Handling — PayTouch Dashboard

All no-internet handling in this project follows two tiers:

| Scenario | Response |
|---|---|
| **First load of a screen** | Full-screen overlay (`lyt_no_internet`) with Retry button |
| **Pagination / action / form submit** | `ToastUtil.showDelete()` only |

---

## 1. Core Check

```kotlin
// Utility.kt
fun isInternetAvailable(mActivity: Context): Boolean {
    val cm = mActivity.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val network = cm.activeNetwork ?: return false
    val capabilities = cm.getNetworkCapabilities(network) ?: return false
    return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
}
```

**Rule:** Call `Utility.isInternetAvailable()` as the **first line** inside every network function, before any other logic.

---

## 2. BaseActivity — How the Overlay Is Created and Wired

`BaseActivity` overrides `setContentView()` to **inject the no-internet overlay into every screen automatically**. No Activity has to add it to its own layout. The glass blur effect is attached **lazily on first show** to avoid the cost on screens that never go offline.

```kotlin
// BaseActivity.kt

private var noInternetView: LytNoInternetBinding? = null
private var noInternetRoot: FrameLayout? = null
private var glassAttached = false
protected var retryCallback: (() -> Unit)? = null

override fun setContentView(view: View?) {
    // 1. Create a FrameLayout as the new root
    val root = FrameLayout(this)
    noInternetRoot = root

    // 2. Add the Activity's own layout as the first child (sits underneath)
    view?.let { root.addView(it) }

    // 3. Inflate lyt_no_internet.xml; wire the Retry button; hide by default
    noInternetView = LytNoInternetBinding.inflate(layoutInflater)
    noInternetView?.btnRetry?.setOnClickListener { retryCallback?.invoke() }
    noInternetView?.root?.visibility = View.GONE
    noInternetView?.root?.let { root.addView(it) }

    // 4. Pass the combined FrameLayout to the system
    super.setContentView(root)
}

// Called by any Activity when the first network load finds no internet.
// Glass blur is attached the first time the overlay is shown (lazy).
fun showNoInternet() {
    val binding = noInternetView ?: return
    binding.root.visibility = View.VISIBLE
    if (!glassAttached) {
        val root = noInternetRoot ?: return
        LiquidGlassEffect.attach(
            targetView   = binding.flNoInternet,
            rootView     = root,
            cornerRadius = resources.getDimensionPixelSize(R.dimen.no_internet_bg_radius),
            distortion   = 0f,
            strokeWidth  = 1,
            strokeColor  = ContextCompat.getColor(mActivity, R.color.white),
            blur         = resources.getDimensionPixelSize(R.dimen.glass_frem_blur)
        )
        glassAttached = true
    }
    // Load the animated GIF into the overlay (plays once)
    Glide.with(this).asGif().load(R.drawable.gif_no_internet)
        .placeholder(R.drawable.ic_file_not_found)
        .into(binding.ivNoInternet)
}

// Called before every successful network attempt to dismiss the overlay
fun hideNoInternet() {
    noInternetView?.root?.visibility = View.GONE
}
```

### How the view hierarchy looks after `setContentView`

```
FrameLayout  (new root created by BaseActivity)
├── <Activity's own layout>   ← sits underneath, always present
└── lyt_no_internet root      ← sits on top; GONE by default, VISIBLE on no-internet
```

Because the overlay is added **after** the Activity's layout in the same `FrameLayout`, it draws on top and covers the whole screen when shown.

### retryCallback lifecycle

```kotlin
// Activity onCreate() — set the callback BEFORE the first network call
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(binding.root)

    retryCallback = { loadList() }   // pressing "Try Again" reruns loadList()
    loadList()
}
```

`retryCallback` is `null` by default. If the Activity never sets it, the Retry button is a no-op. **Always set it in `onCreate()` before the first network call.**

---

## 3. Full-Screen Overlay Layout (`lyt_no_internet.xml`)

Defined in `app/src/main/res/layout/lyt_no_internet.xml`. Contains:

- Icon: `@drawable/ic_no_internet`
- Title: `@string/textYouLostConnection` → *"You Lost Connection!!"*
- Description: `@string/msgNoInternetDes` → *"No internet connection found. Check your connection or try again!"*
- Button: `LiquidGlassButton` with id `btnRetry`

`BaseActivity` automatically inflates this layout and overlays it on top of every screen's content view. The Retry button calls whatever `retryCallback` the Activity has set.

---

## 4. BaseActivity Methods (Public API)

```kotlin
// Show the full-screen no-internet overlay
fun showNoInternet()

// Hide the overlay
fun hideNoInternet()

// Set this in every Activity's onCreate() before the first network call
var retryCallback: (() -> Unit)? = null
```

---

## 5. String Resources

| Name | Value |
|---|---|
| `msgNoInternet` | "No Internet Connection!!" |
| `textYouLostConnection` | "You Lost Connection!!" |
| `msgNoInternetDes` | "No internet connection found. Check your connection or try again!" |

Use `getString(R.string.msgNoInternet)` for toast messages.

---

## 6. Splash / Session Validation Screen

`SplashActivity` is a special case: it validates the stored session token via a network call. Two users can open the app offline — a **logged-out** user and a **logged-in** user — and they must be handled differently.

| User state | No internet | Correct response |
|---|---|---|
| Not logged in | No internet | Navigate to `LoginActivity` (no session to restore; login anyway) |
| Logged in | No internet | `showNoInternet()` — show retry overlay, wait for connectivity |

```kotlin
private fun checkSession() {
    // Not logged in → send to login regardless of connectivity
    if (!SharedPreferenceHelper.isLoggedIn(mActivity)) {
        navigate(Intent(mActivity, LoginActivity::class.java))
        return
    }
    // Logged in but offline → overlay + retry; do NOT dump to login
    if (!Utility.isInternetAvailable(mActivity)) {
        showNoInternet()
        return
    }
    hideNoInternet()
    // ... validate token via API, then route to Home / KYC / MPIN
}
```

`retryCallback = { checkSession() }` must be set in `onCreate()` so the Retry button re-runs the full session check once connectivity is restored.

---

## 7. List Screen (First Load)

Used on every screen that loads a list on open.

```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    // ...
    retryCallback = { loadList() }   // Must be set before first call
    loadList()
}

private fun loadList() {
    if (!Utility.isInternetAvailable(mActivity)) {
        showNoInternet()             // Full-screen overlay on first load
        return
    }
    hideNoInternet()
    showProgress.set(true)
    // ... API call ...
}
```

When the API returns:
- `hideNoInternet()` is called at the start of every successful network attempt
- `showProgress.set(false)` when done

---

## 8. Pagination (Next Pages)

Pagination fetches are never the first load — use **toast only**, never the full overlay.

```kotlin
private var currentPage = 1
private var isLoading = false
private var isLastPage = false

private fun getList(isFirstPage: Boolean, isFirstCall: Boolean = false) {
    if (!Utility.isInternetAvailable(mActivity)) {
        if (isFirstCall) {
            showNoInternet()                                              // First load → overlay
        } else {
            ToastUtil.showDelete(mActivity, getString(R.string.msgNoInternet))  // Pagination → toast
        }
        return
    }
    hideNoInternet()
    isLoading = true
    if (isFirstPage) showProgress.set(true)
    // ... API call ...
}

// Scroll listener that triggers pagination
binding.rvList.addOnScrollListener(object : RecyclerView.OnScrollListener() {
    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
        super.onScrolled(recyclerView, dx, dy)
        val lm = recyclerView.layoutManager as LinearLayoutManager
        val visible = lm.childCount
        val total = lm.itemCount
        val firstVisible = lm.findFirstVisibleItemPosition()
        if (!isLoading && !isLastPage && (visible + firstVisible >= total) && dy > 0) {
            currentPage++
            getList(isFirstPage = false)
        }
    }
})
```

**Retry resets pagination:**

```kotlin
retryCallback = {
    currentPage = 1
    isLastPage = false
    getList(isFirstPage = true, isFirstCall = true)
}
```

---

## 9. Form / Submit Action

On form screens (OTP verify, update, submit), there is no overlay — just toast + early return.

```kotlin
private fun submitForm() {
    if (!Utility.isInternetAvailable(mActivity)) {
        ToastUtil.showDelete(mActivity, getString(R.string.msgNoInternet))
        return
    }
    showProgress.set(true)
    // ... API call ...
}
```

---

## 10. Action Inside a List (Toggle / Approve / Reject)

When an action mutates a single item already on screen, revert the local state before showing the toast.

```kotlin
private val onToggle: (Int, Boolean) -> Unit = { position, isChecked ->
    val item = viewModel.mArrayList.getOrNull(position) ?: return@onToggle

    // Optimistic update
    viewModel.mArrayList[position] = item.copy(isActive = isChecked)
    mAdapter.notifyItemChanged(position)

    viewModel.toggleStatus(
        activity = mActivity,
        onNoInternet = {
            // Revert optimistic update, then toast
            viewModel.mArrayList[position] = item.copy(isActive = !isChecked)
            mAdapter.notifyItemChanged(position)
            ToastUtil.showDelete(mActivity, getString(R.string.msgNoInternet))
        },
        onError = { msg ->
            viewModel.mArrayList[position] = item.copy(isActive = !isChecked)
            mAdapter.notifyItemChanged(position)
            ToastUtil.showDelete(mActivity, msg)
        },
        // ...
    )
}
```

---

## 11. ViewModel Callback Pattern

ViewModels use a **three-callback** convention: `onLoading`, `onSuccess`, `onError`. The ViewModel checks internet internally and calls `onError(msgNoInternet)` if offline. The Activity layer does its own pre-check to decide whether to show the full overlay or just a toast.

```kotlin
// ViewModel — internet checked inside, error surfaced via onError
fun loadData(
    onLoading: () -> Unit,
    onSuccess: (DataItem) -> Unit,
    onError: (String) -> Unit
) {
    if (!Utility.isInternetAvailable(getApplication())) {
        onError(getString(R.string.msgNoInternet))
        return
    }
    onLoading()
    ApiClient.apiService.getData(bearerToken()).enqueue(object : Callback<General<DataItem>> {
        override fun onResponse(...) {
            if (response.isSuccessful && response.body()?.data != null) onSuccess(response.body()!!.data!!)
            else onError(ApiHelper.parseErrorMessage(getApplication(), response.code(), response.errorBody()?.string()))
        }
        override fun onFailure(...) { onError(t.localizedMessage ?: getString(R.string.errGeneric)) }
    })
}

// Activity — first load: check internet here first to pick the right UX response
private fun loadData() {
    if (!Utility.isInternetAvailable(mActivity)) {
        showNoInternet()   // full-screen overlay for first load
        return
    }
    hideNoInternet()
    viewModel.loadData(
        onLoading = { showProgress.set(true) },
        onSuccess = { data -> showProgress.set(false); populate(data) },
        onError   = { msg -> showProgress.set(false); ToastUtil.showDelete(mActivity, msg) }
    )
}
```

**Why two checks?** The Activity check happens *before* calling the ViewModel so it can decide `showNoInternet()` vs toast. The ViewModel check is a defensive guard for direct calls (e.g. from pagination scroll events where the Activity passes the page number in directly).

---

## 12. Decision Table

| Context | Check | Response |
|---|---|---|
| Splash — user **not** logged in | `isInternetAvailable` | Navigate to `LoginActivity` |
| Splash — user **logged in** | `isInternetAvailable` | `showNoInternet()` |
| List — first open (`isFirstCall = true`) | `isInternetAvailable` | `showNoInternet()` |
| List — pagination (`isFirstCall = false`) | `isInternetAvailable` | `ToastUtil.showDelete(msgNoInternet)` |
| Form submit / button action | `isInternetAvailable` | `ToastUtil.showDelete(msgNoInternet)` |
| Bottom sheet / detail fetch | `isInternetAvailable` | `ToastUtil.showDelete(msgNoInternet)` |
| Toggle/approve/reject on list item | `isInternetAvailable` | Revert local state → `ToastUtil.showDelete(msgNoInternet)` |
| ViewModel method | `isInternetAvailable` | Call `onNoInternet()` callback |

---

## 13. Quick Checklist (new screen)

- [ ] Activity extends `BaseActivity`
- [ ] `retryCallback = { loadData() }` set in `onCreate()` before first call
- [ ] First-load function: check `isInternetAvailable` → `showNoInternet()` if offline, `hideNoInternet()` then call ViewModel if online
- [ ] Pagination / form submit / button actions: check `isInternetAvailable` → `ToastUtil.showDelete` (no overlay)
- [ ] ViewModel functions: check `isInternetAvailable` → call `onError(msgNoInternet)` if offline; call `onLoading()` only after the check passes
- [ ] `onError` callback in Activity always calls `showProgress.set(false)` (or equivalent loading reset) before showing a toast
- [ ] `hideNoInternet()` called at the start of every successful network attempt
