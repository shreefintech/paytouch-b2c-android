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
import com.shreefintech.paytouchconsumer.databinding.ActivityHomeBinding
import com.shreefintech.paytouchconsumer.dth.DthActivity
import com.shreefintech.paytouchconsumer.electricity.ElectricityActivity
import com.shreefintech.paytouchconsumer.fastag.FastagActivity
import com.shreefintech.paytouchconsumer.gas.GasActivity
import com.shreefintech.paytouchconsumer.glass.LiquidGlassEffect
import com.shreefintech.paytouchconsumer.loadwallet.LoadWalletActivity
import com.shreefintech.paytouchconsumer.location.LocationPermissionHelper
import com.shreefintech.paytouchconsumer.loan.LoanActivity
import com.shreefintech.paytouchconsumer.municipaltax.MunicipalTaxActivity
import com.shreefintech.paytouchconsumer.myaccount.MyAccountActivity
import com.shreefintech.paytouchconsumer.postpaid.PostpaidActivity
import com.shreefintech.paytouchconsumer.prepaid.PrepaidActivity
import com.shreefintech.paytouchconsumer.rewards.LevelUpActivity
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

    // ── Aadhaar re-upload dialog (blocking — shown while /api/user says aadhaar_rejected) ──
    private val showProgressAadhaar = ObservableBoolean(false)
    private var aadhaarDialog: Dialog? = null
    private var aadhaarBinding: DialogReuploadAadhaarBinding? = null
    private var aadhaarUri: Uri? = null
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

        // TODO(B2C-194): seasonal Navratri — revert after festival
        BannerSliderHelper(this, binding.incBannerSlider.vpBanner).attachToLifecycle(this)

        val listener = onClickListener()
        binding.onClickListener = listener
        binding.lytToolbar.onClickListener = listener
        onBack()
        setupAadhaarPicker()

        // Skip on config change / process restore — once per Home launch is enough
        if (savedInstanceState == null) {
            locationHelper.start()
        }
        // Every create — the dialog doesn't survive recreation, so the block must be re-applied
        viewModel.checkAadhaarRejected {
            if (!isFinishing && !isDestroyed) showReuploadAadhaarDialog()
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.checkLevelUp { levelData ->
            // Skip if the user already left Home (e.g. opened a module) before the response arrived.
            if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                LevelUpActivity.start(mActivity, levelData)
            }
        }
        if (isNotificationPermissionPending) requestNotificationPermission()
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

    private fun showReuploadAadhaarDialog() {
        if (aadhaarDialog?.isShowing == true) return
        val dialogBinding = DialogReuploadAadhaarBinding.inflate(layoutInflater)
        showProgressAadhaar.set(false)
        aadhaarUri = null
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
                    Glide.with(mActivity).load(bmp).into(dialogBinding.ivPreviewAadhaar)
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
        val msg = when {
            aadhaar.isEmpty() -> { dialogBinding.etAadhaarNumber.requestFocus(); getString(R.string.msgAadharEmpty) }
            aadhaar.length != 12 -> { dialogBinding.etAadhaarNumber.requestFocus(); getString(R.string.msgAadharInvalid) }
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
            onLoading = { showProgressAadhaar.set(true) },
            onSuccess = onSuccess@{ message ->
                showProgressAadhaar.set(false)
                if (isDestroyed) return@onSuccess
                aadhaarDialog?.dismiss()
                aadhaarUri = null
                Utility.deleteKycDirs(mActivity, Constant.KYC_AADHAAR_REUPLOAD_DIR)
                ToastUtil.showSuccess(mActivity, message)
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
                    startActivity(Intent(mActivity, ElectricityActivity::class.java))
                }

                binding.llGas -> {
                    if (Utility.stopClick()) return@OnClickListener
                    startActivity(Intent(mActivity, GasActivity::class.java))
                }

                binding.llPrepaid -> {
                    if (Utility.stopClick()) return@OnClickListener
                    startActivity(Intent(mActivity, PrepaidActivity::class.java))
                }

                binding.llPostpaid -> {
                    if (Utility.stopClick()) return@OnClickListener
                    startActivity(Intent(mActivity, PostpaidActivity::class.java))
                }

                binding.llDth -> {
                    if (Utility.stopClick()) return@OnClickListener
                    startActivity(Intent(mActivity, DthActivity::class.java))

                }

                binding.llFastag -> {
                    if (Utility.stopClick()) return@OnClickListener
                    ToastUtil.showWarning(mActivity, getString(R.string.coming_soon))
                }

                binding.llLoan -> {
                    if (Utility.stopClick()) return@OnClickListener
                    startActivity(Intent(mActivity, LoanActivity::class.java))
                }

                binding.llTax -> {
                    if (Utility.stopClick()) return@OnClickListener
                    startActivity(Intent(mActivity, MunicipalTaxActivity::class.java))
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
}
