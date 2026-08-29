package com.shreefintech.paytouchconsumer

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import com.shreefintech.paytouchconsumer.auth.LoginActivity
import com.shreefintech.paytouchconsumer.databinding.ActivityHomeBinding
import com.shreefintech.paytouchconsumer.dth.DthActivity
import com.shreefintech.paytouchconsumer.electricity.ElectricityActivity
import com.shreefintech.paytouchconsumer.fastag.FastagActivity
import com.shreefintech.paytouchconsumer.gas.GasActivity
import com.shreefintech.paytouchconsumer.glass.LiquidGlassEffect
import com.shreefintech.paytouchconsumer.loadwallet.LoadWalletActivity
import com.shreefintech.paytouchconsumer.loan.LoanActivity
import com.shreefintech.paytouchconsumer.municipaltax.MunicipalTaxActivity
import com.shreefintech.paytouchconsumer.myaccount.MyAccountActivity
import com.shreefintech.paytouchconsumer.postpaid.PostpaidActivity
import com.shreefintech.paytouchconsumer.prepaid.PrepaidActivity
import com.shreefintech.paytouchconsumer.utill.SharedPreferenceHelper
import androidx.databinding.ObservableBoolean
import com.shreefintech.paytouchconsumer.utill.ToastUtil
import com.shreefintech.paytouchconsumer.utill.Utility
import com.shreefintech.paytouchconsumer.utill.Utility.gone
import com.shreefintech.paytouchconsumer.utill.Utility.visible

class HomeActivity : BaseActivity() {

    private lateinit var binding: ActivityHomeBinding
    private val viewModel: HomeViewModel by viewModels()
    private val showProgressLogout = ObservableBoolean(false)

    private val appUpdateManager by lazy { AppUpdateManagerFactory.create(this) }

    private val updateResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode != Activity.RESULT_OK) {
            // User dismissed/cancelled a mandatory update — re-trigger immediately so the app remains blocked.
            checkForForceUpdate()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        checkForForceUpdate()

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
        binding.lytToolbar.showProgressLogout = showProgressLogout
        binding.lytToolbar.flLogout.visible()
        val listener = onClickListener()
        binding.onClickListener = listener
        binding.lytToolbar.onClickListener = listener
        onBack()
    }

    override fun onResume() {
        super.onResume()
        // If the app was backgrounded mid-update (e.g. user switched apps during download),
        // Play Store pauses the flow instead of cancelling it — resume it here.
        appUpdateManager.appUpdateInfo.addOnSuccessListener { info ->
            if (info.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                appUpdateManager.startUpdateFlowForResult(
                    info,
                    updateResultLauncher,
                    AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build()
                )
            }
        }
    }

    private fun checkForForceUpdate() {
        appUpdateManager.appUpdateInfo.addOnSuccessListener { info ->
            if (info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE &&
                info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)
            ) {
                appUpdateManager.startUpdateFlowForResult(
                    info,
                    updateResultLauncher,
                    AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build()
                )
            }
        }
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
                    viewModel.logout(
                        onLoading = { showProgressLogout.set(true) },
                        onComplete = {
                            SharedPreferenceHelper.clearSharedPreference(mActivity)
                            startActivity(Intent(mActivity, LoginActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            })
                        },
                        onError = { msg ->
                            showProgressLogout.set(false)
                            ToastUtil.showWarning(mActivity, msg)
                        }
                    )
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
            }
        }
    }
}
