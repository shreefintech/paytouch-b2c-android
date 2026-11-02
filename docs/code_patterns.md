# PayTouch Consumer — Code Patterns

Reference code for the UI rules in `CLAUDE.md` → UI Rules. Read only when implementing that pattern.

### Click Handling

```kotlin
private fun onClickListener(): View.OnClickListener {
    return View.OnClickListener {
        when (it) {
            binding.viewA -> {
                if (Utility.stopClick()) return@OnClickListener
                actionA()
            }
            // Pending module — explicit TODO, never a silent no-op
            binding.cardGas -> {
                if (Utility.stopClick()) return@OnClickListener
                // TODO(PAYTOUCH-585): navigate to GasActivity when implemented
            }
        }
    }
}
```

### Button Loading State

```xml
<data>
    <import type="android.view.View" />
    <variable name="onClickListener" type="android.view.View.OnClickListener" />
    <variable name="showProgressFetch" type="androidx.databinding.ObservableBoolean" />
    <variable name="showProgressPay"   type="androidx.databinding.ObservableBoolean" />
</data>

<LinearLayout
    android:id="@+id/llFetchBill"
    android:layout_width="100dp"
    android:layout_height="@dimen/btn_height"
    android:background="@drawable/bg_toggle_selected"
    android:gravity="center"
    android:onClickListener="@{onClickListener}">

    <androidx.appcompat.widget.AppCompatTextView
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="@string/btnFetchBill"
        android:textColor="@color/white"
        android:textSize="@dimen/text_size_12"
        android:visibility="@{showProgressFetch ? View.GONE : View.VISIBLE}" />

    <ProgressBar
        style="?android:attr/progressBarStyleSmall"
        android:layout_width="20dp"
        android:layout_height="20dp"
        android:indeterminateTint="@color/white"
        android:visibility="@{showProgressFetch ? View.VISIBLE : View.GONE}" />
</LinearLayout>
```

```kotlin
private val showProgressFetch = ObservableBoolean(false)
private val showProgressPay   = ObservableBoolean(false)

// onCreate(), after binding setup:
binding.showProgressFetch = showProgressFetch
binding.showProgressPay   = showProgressPay

// start of API call: showProgressFetch.set(true)
// both onResponse() and onFailure(): showProgressFetch.set(false)

binding.llFetchBill -> {
    if (Utility.stopClick()) return@OnClickListener
    if (showProgressFetch.get()) return@OnClickListener
    onFetchBill()
}
```

### List / Dropdown Field Loading

```xml
<FrameLayout android:layout_width="16dp" android:layout_height="16dp">
    <androidx.appcompat.widget.AppCompatImageView
        android:id="@+id/ivCompanyArrow"
        android:layout_width="14dp"
        android:layout_height="14dp"
        android:layout_gravity="center"
        android:src="@drawable/ic_down_arrow"
        android:tint="@color/primary" />

    <ProgressBar
        android:id="@+id/pbCompanyLoading"
        style="?android:attr/progressBarStyleSmall"
        android:layout_width="16dp"
        android:layout_height="16dp"
        android:indeterminateTint="@color/primary"
        android:visibility="gone" />
</FrameLayout>
```

```kotlin
private fun setOperatorLoading(loading: Boolean) {
    binding.pbCompanyLoading.visibility = if (loading) View.VISIBLE else View.GONE
    binding.ivCompanyArrow.visibility   = if (loading) View.GONE else View.VISIBLE
    binding.flCompanyAnchor.isClickable = !loading
    binding.flCompanyAnchor.isFocusable = !loading
}
```

### LiquidGlassButton

```kotlin
// In onCreate(), after setContentView() — each button individually:
binding.flUpload1.attach(binding.clRoot as ViewGroup)
binding.flSubmit.attach(binding.clRoot as ViewGroup)
```

### Keyboard Handling

```xml
<activity android:name=".YourActivity" android:windowSoftInputMode="adjustResize" />
```

```kotlin
ViewCompat.setOnApplyWindowInsetsListener(binding.clRoot) { v, insets ->
    val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
    val imeInsets  = insets.getInsets(WindowInsetsCompat.Type.ime())
    v.setPadding(systemBars.left, systemBars.top, systemBars.right, maxOf(imeInsets.bottom, systemBars.bottom))
    insets
}
// before button actions: Utility.hideKeyboard(mActivity)
```

### Back Handling

```kotlin
private fun onBack() {   // call in onCreate()
    onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            if (overlayHelper.isVisible()) {
                overlayHelper.hide()
            } else {
                isEnabled = false
                onBackPressedDispatcher.onBackPressed()
            }
        }
    })
}
```

### Result Handling

```kotlin
// Current screen
private var resultCode = 0   // 0 = no changes, 1 = data modified

private fun onBack() {
    onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            setResult(resultCode)
            finish()
        }
    })
}

// Previous screen
private val launcher = registerForActivityResult(
    ActivityResultContracts.StartActivityForResult()
) { result ->
    if (result.resultCode == 1) refreshData()
}
```

### Intent Data Passing (Object Transfer)

```kotlin
// Companion object of the receiving Activity
private const val EXTRA_ITEM = "extra_item"

fun start(context: Context, item: MyItem) {
    context.startActivity(
        Intent(context, MyDetailActivity::class.java).apply {
            putExtra(EXTRA_ITEM, Gson().toJson(item))
        }
    )
}

// Receiver — access in onCreate() or later, never before super.onCreate()
private val myItem: MyItem? by lazy {
    intent.getStringExtra(EXTRA_ITEM)?.let { Gson().fromJson(it, MyItem::class.java) }
}
// In every function using it: val item = myItem ?: return
```

### Bottom Sheet

Sheet XML (`sheet_*.xml`):

```xml
<androidx.constraintlayout.widget.ConstraintLayout
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:background="@drawable/bottom_sheet_bg"
    app:behavior_hideable="true"
    app:behavior_peekHeight="0dp"
    app:behavior_skipCollapsed="true"
    app:layout_behavior="com.google.android.material.bottomsheet.BottomSheetBehavior">
    <!-- Drag handle, title row with close button, form fields, action button -->
</androidx.constraintlayout.widget.ConstraintLayout>
```

Activity layout — sheet and `viewBg` overlay as direct children of `CoordinatorLayout`:

```xml
<androidx.coordinatorlayout.widget.CoordinatorLayout>
    <androidx.constraintlayout.widget.ConstraintLayout ... />   <!-- main content -->
    <View android:id="@+id/viewBg" ... android:visibility="gone" />  <!-- dim overlay -->
    <include android:id="@+id/incSheet" layout="@layout/sheet_xyz" />
</androidx.coordinatorlayout.widget.CoordinatorLayout>
```

```kotlin
private lateinit var sheetBinding: SheetXyzBinding
private lateinit var sheetBehavior: BottomSheetBehavior<View>

private fun setupSheet() {
    sheetBinding = binding.incSheet
    sheetBehavior = BottomSheetBehavior.from(sheetBinding.root)
    sheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN

    sheetBehavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
        override fun onStateChanged(bottomSheet: View, newState: Int) {
            when (newState) {
                BottomSheetBehavior.STATE_EXPANDED -> binding.viewBg.visibility = View.VISIBLE
                BottomSheetBehavior.STATE_HIDDEN   -> {
                    Utility.hideKeyboard(mActivity)
                    binding.viewBg.visibility = View.GONE
                }
                else -> {}
            }
        }
        override fun onSlide(bottomSheet: View, slideOffset: Float) {}
    })
}

// In the insets listener, keep the sheet above the nav bar:
binding.incSheet.root.setPadding(0, 0, 0, systemBars.bottom)
```

Canonical reference: `sheet_filter.xml` (TransactionReportActivity).
