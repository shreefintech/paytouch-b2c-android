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

`BaseActivity` overrides `setContentView()` to **inject the no-internet overlay into every screen automatically**. No Activity has to add it to its own layout.

```kotlin
// BaseActivity.kt

private var noInternetView: LytNoInternetBinding? = null
var retryCallback: (() -> Unit)? = null

override fun setContentView(view: View?) {

    // 1. Create a FrameLayout as the new root
    val root = FrameLayout(this)

    // 2. Add the Activity's own layout as the first child (sits underneath)
    view?.let { root.addView(it) }

    // 3. Inflate lyt_no_internet.xml and attach the glass-blur background effect
    noInternetView = LytNoInternetBinding.inflate(layoutInflater)
    LiquidGlassEffect.attach(
        targetView   = noInternetView!!.frameBg,
        rootView     = noInternetView!!.root as ViewGroup,
        cornerRadius = resources.getDimensionPixelSize(R.dimen.glass_frem_radius),
        distortion   = 0f,
        blur         = resources.getDimensionPixelSize(R.dimen.glass_frem_blur)
    )

    // 4. Attach LiquidGlassButton's own blur effect (required for every LiquidGlassButton)
    noInternetView?.let { it.btnRetry.attach(it.root as ViewGroup) }

    // 5. Wire the Retry button to whatever retryCallback the Activity sets
    noInternetView?.btnRetry?.setOnClickListener {
        retryCallback?.invoke()
    }

    // 6. Hide it by default — shown only on demand
    noInternetView?.root?.gone()

    // 7. Add the overlay on top of the Activity's layout (Z-order: overlay is on top)
    root.addView(noInternetView!!.root)

    // 8. Pass the combined FrameLayout to the system instead of the original view
    super.setContentView(root)
}

// Called by any Activity when the first network load finds no internet
fun showNoInternet() {
    noInternetView?.root?.visible()
}

// Called before every successful network attempt to dismiss the overlay
fun hideNoInternet() {
    noInternetView?.root?.gone()
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

## 6. List Screen (First Load)

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

## 7. Pagination (Next Pages)

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

## 8. Form / Submit Action

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

## 9. Action Inside a List (Toggle / Approve / Reject)

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

## 10. ViewModel Callback Pattern

ViewModels pass `onNoInternet` as a callback — the Activity decides what to show.

```kotlin
// ViewModel
fun getList(
    activity: Activity,
    onNoInternet: () -> Unit,
    onStart: () -> Unit,
    onSuccess: () -> Unit,
    onError: (String) -> Unit,
    onFailure: () -> Unit
) {
    if (!Utility.isInternetAvailable(activity)) {
        onNoInternet()
        return
    }
    onStart()
    // ... API call ...
}

// Activity — first load
viewModel.getList(
    activity = mActivity,
    onNoInternet = { showNoInternet() },           // overlay
    onStart     = { hideNoInternet(); showProgress.set(true) },
    onSuccess   = { showProgress.set(false); mAdapter.notifyDataSetChanged() },
    onError     = { msg -> showProgress.set(false); ToastUtil.showDelete(mActivity, msg) },
    onFailure   = { showProgress.set(false); ToastUtil.showDelete(mActivity, getString(R.string.err_generic)) }
)
```

---

## 11. Decision Table

| Context | Check | Response |
|---|---|---|
| List — first open (`isFirstCall = true`) | `isInternetAvailable` | `showNoInternet()` |
| List — pagination (`isFirstCall = false`) | `isInternetAvailable` | `ToastUtil.showDelete(msgNoInternet)` |
| Form submit / button action | `isInternetAvailable` | `ToastUtil.showDelete(msgNoInternet)` |
| Bottom sheet / detail fetch | `isInternetAvailable` | `ToastUtil.showDelete(msgNoInternet)` |
| Toggle/approve/reject on list item | `isInternetAvailable` | Revert local state → `ToastUtil.showDelete(msgNoInternet)` |
| ViewModel method | `isInternetAvailable` | Call `onNoInternet()` callback |

---

## 12. Quick Checklist (new screen)

- [ ] Activity extends `BaseActivity`
- [ ] `retryCallback = { loadList() }` set in `onCreate()` before first call
- [ ] First call passes `isFirstCall = true` → `showNoInternet()`
- [ ] Every subsequent call (pagination, retry) uses `ToastUtil.showDelete`
- [ ] Form submits: `ToastUtil.showDelete` + `return` before `showProgress`
- [ ] ViewModel methods accept `onNoInternet: () -> Unit` and call it before any API work
- [ ] `hideNoInternet()` called at the start of every successful network attempt
