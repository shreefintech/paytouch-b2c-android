package com.shreefintech.paytouchconsumer

import android.app.Dialog
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.ImageViewCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Lifecycle
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.shreefintech.paytouchconsumer.databinding.DialogConfirmLogoutBinding
import com.shreefintech.paytouchconsumer.auth.LoginActivity
import com.shreefintech.paytouchconsumer.databinding.ActivityHomeBinding
import com.shreefintech.paytouchconsumer.dth.DthActivity
import com.shreefintech.paytouchconsumer.electricity.ElectricityActivity
import com.shreefintech.paytouchconsumer.fcm.NotificationHelper
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
import androidx.databinding.ObservableBoolean
import com.shreefintech.paytouchconsumer.utill.SharedPreferenceHelper
import com.shreefintech.paytouchconsumer.utill.ToastUtil
import com.shreefintech.paytouchconsumer.utill.Utility
import com.shreefintech.paytouchconsumer.utill.Utility.gone
import com.shreefintech.paytouchconsumer.utill.Utility.visible

class HomeActivity : BaseActivity() {

    private lateinit var binding: ActivityHomeBinding
    private val viewModel: HomeViewModel by viewModels()

    private var logoutDialog: Dialog? = null
    private var logoutDialogBinding: DialogConfirmLogoutBinding? = null
    private val showProgressLogout = ObservableBoolean(false)

    // Field-initialised: activity-result launchers must be registered before onStart()
    private val locationHelper = LocationPermissionHelper(this) { location ->
        location?.let { viewModel.sendLocation(it) }
        // Asked after the location flow ends so the two system permission popups never overlap
        requestNotificationPermission()
    }

    // True when the location flow ended while Home was not in front — asked again in onResume()
    private var isNotificationPermissionPending = false

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
            blur = resources.getDimensionPixelSize(R.dimen.glass_frem_blur)
        )

        binding.lytToolbar.ivBack.gone()
        binding.lytToolbar.flLogout.visible()
        binding.lytToolbar.ivLogo.layoutParams.height =
            resources.getDimensionPixelSize(R.dimen.home_toolbar_logo_height)

        binding.lytToolbar.ivLogo.requestLayout()

        loadCategoryIcons()

        val listener = onClickListener()
        binding.onClickListener = listener
        binding.lytToolbar.onClickListener = listener
        onBack()

        // Skip on config change / process restore — once per Home launch is enough
        if (savedInstanceState == null) {
            NotificationHelper.syncToken(mActivity)
            locationHelper.start()
        }
    }

    override fun onResume() {
        super.onResume()
        if (isNotificationPermissionPending) requestNotificationPermission()
    }

    // Never pop the system dialog over another screen (e.g. a category opened mid location fix)
    private fun requestNotificationPermission() {
        if (!lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            isNotificationPermissionPending = true
            return
        }
        isNotificationPermissionPending = false
        NotificationHelper.requestPermission(mActivity)
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
                    startActivity(Intent(mActivity, FastagActivity::class.java))
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
                    MyAccountActivity.start(mActivity)
                }

                binding.cvLoadWallet -> {
                    if (Utility.stopClick()) return@OnClickListener
                    LoadWalletActivity.start(mActivity)
                }

                logoutDialogBinding?.btnLogout -> {
                    if (Utility.stopClick()) return@OnClickListener
                    if (showProgressLogout.get()) return@OnClickListener
                    if (!Utility.isInternetAvailable(mActivity)) {
                        ToastUtil.showWarning(mActivity, getString(R.string.msgNoInternet))
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
                            ToastUtil.showWarning(mActivity, msg)
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
