package com.shreefintech.paytouchconsumer

import android.app.Dialog
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.text.InputFilter
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Space
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.DrawableRes
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.ImageViewCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.shreefintech.paytouchconsumer.databinding.DialogConfirmLogoutBinding
import com.shreefintech.paytouchconsumer.databinding.DialogReuploadAadhaarBinding
import com.shreefintech.paytouchconsumer.auth.LoginActivity
import com.shreefintech.paytouchconsumer.auth.NextStepRouter
import com.shreefintech.paytouchconsumer.billpay.BillPayActivity
import com.shreefintech.paytouchconsumer.enums.CategoryType
import com.shreefintech.paytouchconsumer.databinding.ActivityHomeBinding
import com.shreefintech.paytouchconsumer.glass.LiquidGlassEffect
import com.shreefintech.paytouchconsumer.loadwallet.LoadWalletActivity
import com.shreefintech.paytouchconsumer.location.LocationPermissionHelper
import com.shreefintech.paytouchconsumer.myaccount.MyAccountActivity
import com.shreefintech.paytouchconsumer.retrofit.model.CityItem
import com.shreefintech.paytouchconsumer.retrofit.model.StateItem
import com.shreefintech.paytouchconsumer.utill.loadAddressCities
import com.shreefintech.paytouchconsumer.utill.loadAddressStates
import com.shreefintech.paytouchconsumer.utill.sendLocation
import com.shreefintech.paytouchconsumer.widget.CustomDropdown
// import com.shreefintech.paytouchconsumer.rewards.LevelUpActivity  // TODO(B2C-147): restore with checkLevelUp
import androidx.databinding.ObservableBoolean
import com.shreefintech.paytouchconsumer.utill.BannerSliderHelper
import com.shreefintech.paytouchconsumer.utill.BankDetailsDialogHelper
import com.shreefintech.paytouchconsumer.utill.FilePickerUtil
import com.shreefintech.paytouchconsumer.utill.SharedPreferenceHelper
import com.shreefintech.paytouchconsumer.utill.ToastUtil
import com.shreefintech.paytouchconsumer.utill.Utility
import com.shreefintech.paytouchconsumer.utill.Utility.gone
import com.shreefintech.paytouchconsumer.utill.Utility.visible
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class HomeActivity : BaseActivity() {

    private lateinit var binding: ActivityHomeBinding
    private val viewModel: HomeViewModel by viewModels()

    private var logoutDialog: Dialog? = null
    private var logoutDialogBinding: DialogConfirmLogoutBinding? = null
    private val showProgressLogout = ObservableBoolean(false)

    // Field-initialised: activity-result launchers must be registered before onStart()
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* granted or denied — either way nothing to do; FCM delivers as long as permission holds */ }

    private val locationHelper = LocationPermissionHelper(this) { location ->
        location?.let { viewModel.sendLocation(it) }
        // Asked after the location flow ends so the two system permission popups never overlap
        requestNotificationPermission()
    }

    // True when the location flow ended while Home was not in front — asked again in onResume()
    private var isNotificationPermissionPending = false

    // ── Bill Pay tiles (billpay/categories) ──
    private val showProgressCategories = ObservableBoolean(false)
    private var hasLoadedCategories = false
    private var hasRequestedCategories = false

    // ── Aadhaar dialog (blocking — resubmit when both Aadhaar items are rejected, add when next_step is aadhaar_required) ──
    private val showProgressAadhaar = ObservableBoolean(false)
    private var aadhaarDialog: Dialog? = null
    private var aadhaarBinding: DialogReuploadAadhaarBinding? = null
    private var aadhaarUri: Uri? = null
    // Add mode only — kyc/individual/aadhaar also takes state_id + city_id
    private var stateList = listOf<StateItem>()
    private var cityList = listOf<CityItem>()
    private var selectedStateId = ""
    private var selectedCityId = ""
    private val aadhaarDir by lazy { File(filesDir, Constant.KYC_AADHAAR_REUPLOAD_DIR) }
    private lateinit var filePickerUtil: FilePickerUtil

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.clRoot) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        LiquidGlassEffect.attach(
            targetView = binding.flCard,
            rootView = binding.root as ViewGroup,
            cornerRadius = resources.getDimensionPixelSize(R.dimen.glass_frem_radius),
            distortion = 0f,
            tintColor = ContextCompat.getColor(mActivity, R.color.home_card_bg),
            blur = resources.getDimensionPixelSize(R.dimen.glass_frem_blur)
        )

        binding.lytToolbar.ivBack.gone()
        binding.lytToolbar.flLogout.visible()
        binding.lytToolbar.ivLogo.layoutParams.height =
            resources.getDimensionPixelSize(R.dimen.home_toolbar_logo_height)

        binding.lytToolbar.ivLogo.requestLayout()

        loadCategoryIcons()

        // TODO(B2C-262): seasonal Diwali — revert after festival
        BannerSliderHelper(binding.incBannerSlider.vpBanner).attachToLifecycle(this)

        val listener = onClickListener()
        binding.onClickListener = listener
        binding.lytToolbar.onClickListener = listener
        binding.showProgressCategories = showProgressCategories
        onBack()
        setupAadhaarPicker()
        // Bill tiles stay hidden until billpay/categories says which ones have billers
        layoutCategoryTiles(emptySet())
        loadBillPayCategories(showError = true)

        // Skip on config change / process restore — once per Home launch is enough
        if (savedInstanceState == null) {
            locationHelper.start()
        }
        // Every create — the dialog doesn't survive recreation, so the block must be re-applied
        checkKycStep()
    }

    private fun checkKycStep() {
        viewModel.checkAadhaarRejected(
            onAadhaarDialog = { isAddMode -> if (!isFinishing && !isDestroyed) showReuploadAadhaarDialog(isAddMode) },
            onRedirect = { nextStep ->
                if (isFinishing || isDestroyed) return@checkAadhaarRejected
                startActivity(NextStepRouter.intentFor(mActivity, nextStep, null).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                })
            }
        )
    }

    override fun onResume() {
        super.onResume()
        // TODO(B2C-147): re-enable once /level moves to the B2C backend — the old backend 401s the
        //  B2C token and SessionInterceptor then logs the user out right after login
        // viewModel.checkLevelUp { levelData ->
        //     // Skip if the user already left Home (e.g. opened a module) before the response arrived.
        //     if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
        //         LevelUpActivity.start(mActivity, levelData)
        //     }
        // }
        if (isNotificationPermissionPending) requestNotificationPermission()
        // Refresh on every return to the Dashboard — silent; the first load (onCreate) and Retry show errors
        if (hasRequestedCategories && !showProgressCategories.get()) loadBillPayCategories(showError = false)
    }

    // ── Bill Pay tiles ────────────────────────────────────────

    private fun loadBillPayCategories(showError: Boolean) {
        hasRequestedCategories = true
        viewModel.loadBillPayCategories(
            onLoading = { showProgressCategories.set(true) },
            onSuccess = onSuccess@{ categories ->
                showProgressCategories.set(false)
                if (isDestroyed) return@onSuccess
                hasLoadedCategories = true
                binding.llCategoriesRetryRow.gone()
                // Listed categories with no tile (broadband_postpaid, water, insurance, …) are ignored
                layoutCategoryTiles(categories.map { CategoryType.from(it.category) }.toSet())
            },
            onError = onError@{ msg ->
                showProgressCategories.set(false)
                if (isDestroyed) return@onError
                // Keep the last good tiles; only a never-loaded Dashboard swaps them for the Retry row
                if (!hasLoadedCategories) binding.llCategoriesRetryRow.visible()
                if (showError) ToastUtil.showDelete(mActivity, msg)
            }
        )
    }

    /** Shows the tiles of [categories] (plus My Account, always) in rows of three, in the layout's order. */
    private fun layoutCategoryTiles(categories: Set<CategoryType>) {
        val tiles = listOf(
            CategoryType.ELECTRICITY to binding.llElectricity,
            CategoryType.GAS to binding.llGas,
            CategoryType.PREPAID to binding.llPrepaid,
            CategoryType.POSTPAID to binding.llPostpaid,
            CategoryType.DTH to binding.llDth,
            CategoryType.FASTAG to binding.llFastag,
            CategoryType.LOAN to binding.llLoan,
            CategoryType.MUNICIPAL_TAX to binding.llTax,
            null to binding.llMyAccount
        ).mapNotNull { (category, tile) ->
            // The card wraps the clickable LinearLayout
            val card = tile.parent as? View ?: return@mapNotNull null
            card to (category == null || category in categories)
        }
        val rows = listOf(binding.llCategoryRow1, binding.llCategoryRow2, binding.llCategoryRow3)
        rows.forEach { it.removeAllViews() }
        val shown = tiles.filter { it.second }.map { it.first }
        tiles.filterNot { it.second }.forEach { (card, _) -> card.gone() }
        val gap = resources.getDimensionPixelSize(R.dimen.margin_small)
        shown.chunked(TILES_PER_ROW).forEachIndexed { rowIndex, rowTiles ->
            val row = rows.getOrNull(rowIndex) ?: return@forEachIndexed
            (0 until TILES_PER_ROW).forEach { column ->
                // Empty slots keep the remaining tiles at a third of the width
                val view = rowTiles.getOrNull(column)?.also { it.visible() } ?: Space(mActivity)
                val params = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                params.marginStart = if (column > 0) gap else 0
                params.marginEnd = if (column < TILES_PER_ROW - 1) gap else 0
                row.addView(view, params)
            }
        }
        rows.forEachIndexed { index, row ->
            row.visibility = if (index < (shown.size + TILES_PER_ROW - 1) / TILES_PER_ROW) View.VISIBLE else View.GONE
        }
    }

    // Never pop the system dialog over another screen (e.g. a category opened mid location fix)
    private fun requestNotificationPermission() {
        if (!lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            isNotificationPermissionPending = true
            return
        }
        isNotificationPermissionPending = false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(mActivity, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // Animated WebP — decoded on all API levels via the webpdecoder Glide integration.
    // If decoding fails the static ic_ drawable from the layout is restored and styled so it
    // stays visible (white icons on a white card need their pill background / tint back).
    private fun loadCategoryIcons() {
        listOf(
            binding.ivElectricity to R.drawable.img_electricity,
            binding.ivGas to R.drawable.img_gas,
            binding.ivPrepaid to R.drawable.img_prepaid,
            binding.ivPostpaid to R.drawable.img_postpaid,
            binding.ivDth to R.drawable.img_dth,
            binding.ivFastag to R.drawable.img_fastag,
            binding.ivLoan to R.drawable.img_loan,
            binding.ivTax to R.drawable.img_municipal_tax,
            binding.ivMyAccount to R.drawable.img_my_account
        ).forEach { (imageView, animatedRes) ->
            loadAnimatedIcon(imageView, animatedRes) {
                imageView.setBackgroundResource(R.drawable.bg_toggle_selected)
            }
        }
        loadAnimatedIcon(binding.ivLoadWallet, R.drawable.img_load_wallet) {
            ImageViewCompat.setImageTintList(
                binding.ivLoadWallet,
                ColorStateList.valueOf(ContextCompat.getColor(mActivity, R.color.white))
            )
        }
    }

    private fun loadAnimatedIcon(imageView: ImageView, @DrawableRes animatedRes: Int, onFallback: () -> Unit) {
        val fallback = imageView.drawable
        Glide.with(this)
            .load(animatedRes)
            .placeholder(fallback)
            .error(fallback)
            .listener(object : RequestListener<Drawable> {
                override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Drawable>, isFirstResource: Boolean): Boolean {
                    onFallback()
                    return false
                }

                override fun onResourceReady(resource: Drawable, model: Any, target: Target<Drawable>?, dataSource: DataSource, isFirstResource: Boolean) = false
            })
            .into(imageView)
    }

    private fun showLogoutDialog() {
        showProgressLogout.set(false)
        val dialogBinding = DataBindingUtil.inflate<DialogConfirmLogoutBinding>(
            layoutInflater, R.layout.dialog_confirm_logout, null, false
        )
        logoutDialogBinding = dialogBinding
        dialogBinding.showProgress = showProgressLogout

        val dialog = Dialog(mActivity)
        logoutDialog = dialog
        dialog.setContentView(dialogBinding.root)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.85).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog.setCancelable(true)
        dialog.setOnDismissListener {
            logoutDialog = null
            logoutDialogBinding = null
        }

        dialogBinding.onClickListener = onClickListener()
        dialog.show()
    }


    // ── Aadhaar Re-upload ─────────────────────────────────────

    private fun setupAadhaarPicker() {
        // Registers activity-result launchers — must run before onStart()
        filePickerUtil = FilePickerUtil(this)
        filePickerUtil.onSuccess = { result ->
            aadhaarUri = result.uri
            aadhaarBinding?.let { showAadhaarPreview(it, result.uri) }
        }
        filePickerUtil.onError = { error ->
            ToastUtil.showDelete(
                mActivity,
                filePickerUtil.getErrorMessage(error),
                inWindow = error != FilePickerUtil.FilePickerError.CameraPermissionBlocked
            )
        }
    }

    private fun showReuploadAadhaarDialog(isAddMode: Boolean) {
        if (aadhaarDialog?.isShowing == true) return
        val dialogBinding = DialogReuploadAadhaarBinding.inflate(layoutInflater)
        showProgressAadhaar.set(false)
        aadhaarUri = null
        selectedStateId = ""
        selectedCityId = ""
        cityList = emptyList()
        dialogBinding.isAddMode = isAddMode
        dialogBinding.showProgress = showProgressAadhaar
        dialogBinding.onClickListener = aadhaarClickListener(dialogBinding)
        dialogBinding.etAadhaarNumber.filters = arrayOf(
            InputFilter.LengthFilter(12), Utility.digitFilter(), Utility.EmojiExcludeFilter()
        )

        val dialog = Dialog(mActivity)
        dialog.setContentView(dialogBinding.root)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.window?.setWindowAnimations(R.style.DialogScaleFadeAnimation)
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.88).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        // Blocks the app — no back, no outside tap, no close button; only a successful submit dismisses it
        dialog.setCancelable(false)
        dialog.setCanceledOnTouchOutside(false)
        dialog.setOnDismissListener {
            aadhaarDialog = null
            aadhaarBinding = null
        }
        aadhaarDialog = dialog
        aadhaarBinding = dialogBinding
        dialog.show()

        val root = dialogBinding.llRoot as ViewGroup
        dialogBinding.flUploadAadhaarBtn.attach(root)
        listOf(dialogBinding.flEditAadhaar, dialogBinding.flDeleteAadhaar).forEach {
            LiquidGlassEffect.attach(
                targetView = it,
                rootView = root,
                cornerRadius = resources.getDimensionPixelSize(R.dimen.filter_btn_radius),
                distortion = 0f,
                blur = resources.getDimensionPixelSize(R.dimen.filter_btn_blure),
                tintColor = ContextCompat.getColor(mActivity, R.color.filter_bg)
            )
        }
        if (isAddMode && stateList.isEmpty()) loadStates(dialogBinding)
    }

    private fun loadStates(dialogBinding: DialogReuploadAadhaarBinding) {
        viewModel.loadAddressStates(
            onLoading = { setDropdownLoading(dialogBinding.pbStateLoading, dialogBinding.ivStateArrow, dialogBinding.flStateAnchor, true) },
            onSuccess = onSuccess@{ states ->
                if (isDestroyed) return@onSuccess
                setDropdownLoading(dialogBinding.pbStateLoading, dialogBinding.ivStateArrow, dialogBinding.flStateAnchor, false)
                stateList = states
            },
            onError = onError@{ msg ->
                if (isDestroyed) return@onError
                setDropdownLoading(dialogBinding.pbStateLoading, dialogBinding.ivStateArrow, dialogBinding.flStateAnchor, false)
                ToastUtil.showDelete(mActivity, msg)
            }
        )
    }

    private fun loadCities(dialogBinding: DialogReuploadAadhaarBinding, stateId: String) {
        viewModel.loadAddressCities(
            stateId = stateId,
            onLoading = { setDropdownLoading(dialogBinding.pbCityLoading, dialogBinding.ivCityArrow, dialogBinding.flCityAnchor, true) },
            onSuccess = onSuccess@{ cities ->
                if (isDestroyed) return@onSuccess
                setDropdownLoading(dialogBinding.pbCityLoading, dialogBinding.ivCityArrow, dialogBinding.flCityAnchor, false)
                // State changed while this was loading — keep only the latest state's cities
                if (stateId == selectedStateId) cityList = cities
            },
            onError = onError@{ msg ->
                if (isDestroyed) return@onError
                setDropdownLoading(dialogBinding.pbCityLoading, dialogBinding.ivCityArrow, dialogBinding.flCityAnchor, false)
                ToastUtil.showDelete(mActivity, msg)
            }
        )
    }

    // Small spinner replaces the arrow in the field slot
    private fun setDropdownLoading(progress: View, arrow: View, anchor: View, loading: Boolean) {
        progress.visibility = if (loading) View.VISIBLE else View.GONE
        arrow.visibility = if (loading) View.GONE else View.VISIBLE
        anchor.isClickable = !loading
        anchor.isFocusable = !loading
    }

    private fun showStateDropdown(dialogBinding: DialogReuploadAadhaarBinding) {
        if (stateList.isEmpty()) {
            loadStates(dialogBinding)
            return
        }
        CustomDropdown.showDropdown(
            activity = mActivity,
            anchorView = dialogBinding.flStateAnchor,
            arrowView = dialogBinding.ivStateArrow,
            textView = dialogBinding.tvState,
            items = stateList.map { it.name.orEmpty() }
        ) { _, index ->
            val state = stateList.getOrNull(index) ?: return@showDropdown
            selectedStateId = state.id.orEmpty()
            dialogBinding.tvState.setTextColor(ContextCompat.getColor(mActivity, R.color.black))
            selectedCityId = ""
            cityList = emptyList()
            dialogBinding.tvCity.text = getString(R.string.hintSelectCity)
            dialogBinding.tvCity.setTextColor(ContextCompat.getColor(mActivity, android.R.color.darker_gray))
            if (selectedStateId.isNotEmpty()) loadCities(dialogBinding, selectedStateId)
        }
    }

    private fun showCityDropdown(dialogBinding: DialogReuploadAadhaarBinding) {
        if (selectedStateId.isEmpty()) {
            ToastUtil.showDelete(mActivity, getString(R.string.msgStateEmpty))
            return
        }
        if (cityList.isEmpty()) {
            loadCities(dialogBinding, selectedStateId)
            return
        }
        CustomDropdown.showDropdown(
            activity = mActivity,
            anchorView = dialogBinding.flCityAnchor,
            arrowView = dialogBinding.ivCityArrow,
            textView = dialogBinding.tvCity,
            items = cityList.map { it.name.orEmpty() }
        ) { _, index ->
            val city = cityList.getOrNull(index) ?: return@showDropdown
            selectedCityId = city.id.orEmpty()
            dialogBinding.tvCity.setTextColor(ContextCompat.getColor(mActivity, R.color.black))
        }
    }

    private fun aadhaarClickListener(dialogBinding: DialogReuploadAadhaarBinding): View.OnClickListener {
        return View.OnClickListener { view ->
            when (view) {
                dialogBinding.flUploadAadhaarBtn, dialogBinding.flEditAadhaar -> {
                    if (Utility.stopClick()) return@OnClickListener
                    if (showProgressAadhaar.get()) return@OnClickListener
                    filePickerUtil.showSourceChooser(aadhaarDir)
                }
                dialogBinding.ivDeleteAadhaar -> {
                    if (Utility.stopClick()) return@OnClickListener
                    if (showProgressAadhaar.get()) return@OnClickListener
                    clearAadhaarSlot(dialogBinding)
                }
                dialogBinding.flStateAnchor -> {
                    if (Utility.stopClick()) return@OnClickListener
                    if (showProgressAadhaar.get()) return@OnClickListener
                    Utility.hideKeyboard(dialogBinding.root)
                    showStateDropdown(dialogBinding)
                }
                dialogBinding.flCityAnchor -> {
                    if (Utility.stopClick()) return@OnClickListener
                    if (showProgressAadhaar.get()) return@OnClickListener
                    Utility.hideKeyboard(dialogBinding.root)
                    showCityDropdown(dialogBinding)
                }
                dialogBinding.cardSubmit -> {
                    if (Utility.stopClick()) return@OnClickListener
                    if (showProgressAadhaar.get()) return@OnClickListener
                    onSubmitAadhaar(dialogBinding)
                }
            }
        }
    }

    private fun showAadhaarPreview(dialogBinding: DialogReuploadAadhaarBinding, uri: Uri) {
        dialogBinding.llUploadPrompt.visibility = View.GONE
        dialogBinding.ivPreviewAadhaar.visibility = View.VISIBLE
        dialogBinding.llEditDeleteAadhaar.visibility = View.VISIBLE

        if (contentResolver.getType(uri) == "application/pdf") {
            dialogBinding.ivPreviewAadhaar.setImageResource(R.drawable.ic_file_not_found)
            val targetWidth = dialogBinding.ivPreviewAadhaar.width.takeIf { it > 0 }
                ?: resources.displayMetrics.widthPixels / 2
            lifecycleScope.launch(Dispatchers.IO) {
                val bmp = Utility.renderPdfFirstPage(mActivity, uri, targetWidth)
                withContext(Dispatchers.Main) {
                    // Dialog may have been replaced / file changed while rendering
                    if (aadhaarBinding !== dialogBinding || aadhaarUri != uri) {
                        bmp?.recycle()
                        return@withContext
                    }
                    Glide.with(this@HomeActivity).load(bmp).into(dialogBinding.ivPreviewAadhaar)
                }
            }
        } else {
            Glide.with(this)
                .load(uri)
                .placeholder(R.drawable.ic_file_not_found)
                .error(R.drawable.ic_file_not_found)
                .into(dialogBinding.ivPreviewAadhaar)
        }
    }

    private fun clearAadhaarSlot(dialogBinding: DialogReuploadAadhaarBinding) {
        aadhaarUri = null
        Glide.with(this).clear(dialogBinding.ivPreviewAadhaar)
        dialogBinding.ivPreviewAadhaar.visibility = View.GONE
        dialogBinding.llEditDeleteAadhaar.visibility = View.GONE
        dialogBinding.llUploadPrompt.visibility = View.VISIBLE
    }

    private fun onSubmitAadhaar(dialogBinding: DialogReuploadAadhaarBinding) {
        Utility.hideKeyboard(dialogBinding.root)
        val aadhaar = dialogBinding.etAadhaarNumber.text?.toString()?.trim().orEmpty()
        val uri = aadhaarUri
        val isAddMode = dialogBinding.isAddMode
        val msg = when {
            aadhaar.isEmpty() -> { dialogBinding.etAadhaarNumber.requestFocus(); getString(R.string.msgAadharEmpty) }
            aadhaar.length != 12 -> { dialogBinding.etAadhaarNumber.requestFocus(); getString(R.string.msgAadharInvalid) }
            isAddMode && selectedStateId.isEmpty() -> getString(R.string.msgStateEmpty)
            isAddMode && selectedCityId.isEmpty() -> getString(R.string.msgCityEmpty)
            uri == null -> getString(R.string.msgAadhaarPhotoRequired)
            else -> null
        }
        if (msg != null || uri == null) {
            ToastUtil.showDelete(mActivity, msg ?: getString(R.string.errGeneric))
            return
        }

        viewModel.reuploadAadhaar(
            aadhaarNumber = aadhaar,
            aadhaarUri = uri,
            stateId = selectedStateId.takeIf { isAddMode },
            cityId = selectedCityId.takeIf { isAddMode },
            onLoading = { showProgressAadhaar.set(true) },
            onSuccess = onSuccess@{ message ->
                showProgressAadhaar.set(false)
                if (isDestroyed) return@onSuccess
                aadhaarDialog?.dismiss()
                aadhaarUri = null
                Utility.deleteKycDirs(mActivity, Constant.KYC_AADHAAR_REUPLOAD_DIR)
                ToastUtil.showSuccess(mActivity, message)
                // auth/me is the source of truth after every submit — resubmit usually moves on to pending_approval,
                // add moves on to bank_details_required (Add Bank Details popup before the first money action)
                checkKycStep()
            },
            onError = onError@{ errMsg ->
                showProgressAadhaar.set(false)
                if (isDestroyed) return@onError
                ToastUtil.showDelete(mActivity, errMsg)
            }
        )
    }

    override fun onDestroy() {
        // Dismiss here so the dialog window doesn't leak past the Activity
        aadhaarDialog?.dismiss()
        logoutDialog?.dismiss()
        // Its callback holds this Activity — a recreated Home starts its own check
        viewModel.cancelAadhaarCheck()
        if (isFinishing) Utility.deleteKycDirs(mActivity, Constant.KYC_AADHAAR_REUPLOAD_DIR)
        super.onDestroy()
    }

    private fun onBack() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                isEnabled = false
                onBackPressedDispatcher.onBackPressed()
            }
        })
    }

    private fun onClickListener(): View.OnClickListener {
        return View.OnClickListener { view ->
            when (view) {
                binding.lytToolbar.ivLogout -> {
                    if (Utility.stopClick()) return@OnClickListener
                    showLogoutDialog()
                }

                binding.llElectricity -> {
                    if (Utility.stopClick()) return@OnClickListener
                    BillPayActivity.start(mActivity, CategoryType.ELECTRICITY)
                }

                binding.llGas -> {
                    if (Utility.stopClick()) return@OnClickListener
                    BillPayActivity.start(mActivity, CategoryType.GAS)
                }

                binding.llPrepaid -> {
                    if (Utility.stopClick()) return@OnClickListener
                    BillPayActivity.start(mActivity, CategoryType.PREPAID)
                }

                binding.llPostpaid -> {
                    if (Utility.stopClick()) return@OnClickListener
                    BillPayActivity.start(mActivity, CategoryType.POSTPAID)
                }

                binding.llDth -> {
                    if (Utility.stopClick()) return@OnClickListener
                    BillPayActivity.start(mActivity, CategoryType.DTH)

                }

                binding.llFastag -> {
                    if (Utility.stopClick()) return@OnClickListener
                    // TODO(PAYTOUCH-FASTAG): open BillPayActivity for CategoryType.FASTAG once the backend routes it — iOS shows "Coming soon" too
                    ToastUtil.showWarning(mActivity, getString(R.string.labelComingSoon))
                }

                binding.llLoan -> {
                    if (Utility.stopClick()) return@OnClickListener
                    BillPayActivity.start(mActivity, CategoryType.LOAN)
                }

                binding.llTax -> {
                    if (Utility.stopClick()) return@OnClickListener
                    BillPayActivity.start(mActivity, CategoryType.MUNICIPAL_TAX)
                }

                binding.llCategoriesRetry -> {
                    if (Utility.stopClick()) return@OnClickListener
                    if (showProgressCategories.get()) return@OnClickListener
                    loadBillPayCategories(showError = true)
                }

                binding.llMyAccount -> {
                    if (Utility.stopClick()) return@OnClickListener
                    BankDetailsDialogHelper.runIfBankAdded(mActivity) { MyAccountActivity.start(mActivity) }
                }

                binding.cvLoadWallet -> {
                    if (Utility.stopClick()) return@OnClickListener
                    BankDetailsDialogHelper.runIfBankAdded(mActivity) { LoadWalletActivity.start(mActivity) }
                }

                logoutDialogBinding?.btnLogout -> {
                    if (Utility.stopClick()) return@OnClickListener
                    if (showProgressLogout.get()) return@OnClickListener
                    if (!Utility.isInternetAvailable(mActivity)) {
                        ToastUtil.showWarning(mActivity, getString(R.string.msgNoInternet), inWindow = false)
                        return@OnClickListener
                    }
                    viewModel.logout(
                        onLoading = { showProgressLogout.set(true) },
                        onComplete = {
                            logoutDialog?.dismiss()
                            SharedPreferenceHelper.clearSharedPreference(mActivity)
                            startActivity(Intent(mActivity, LoginActivity::class.java).apply {
                                flags =
                                    Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            })
                        },
                        onError = { msg ->
                            showProgressLogout.set(false)
                            ToastUtil.showWarning(mActivity, msg, inWindow = false)
                        }
                    )
                }

                logoutDialogBinding?.btnCancel -> {
                    if (Utility.stopClick()) return@OnClickListener
                    logoutDialog?.dismiss()
                }

            }
        }
    }

    companion object {
        private const val TILES_PER_ROW = 3
    }
}
