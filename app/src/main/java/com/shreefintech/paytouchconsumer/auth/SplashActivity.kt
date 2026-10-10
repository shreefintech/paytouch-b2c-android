package com.shreefintech.paytouchconsumer.auth

import android.app.ActivityOptions
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import com.bumptech.glide.Glide
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import com.shreefintech.paytouchconsumer.BaseActivity
import com.shreefintech.paytouchconsumer.BuildConfig
import com.shreefintech.paytouchconsumer.Constant
import com.shreefintech.paytouchconsumer.HomeActivity
import com.shreefintech.paytouchconsumer.R
import com.shreefintech.paytouchconsumer.auth.viewmodel.SplashViewModel
import com.shreefintech.paytouchconsumer.databinding.ActivitySplashBinding
import com.shreefintech.paytouchconsumer.fcm.NotificationHelper
import com.shreefintech.paytouchconsumer.kyc.KycActivity
import com.shreefintech.paytouchconsumer.retrofit.model.UserProfileItem
import com.shreefintech.paytouchconsumer.utill.SharedPreferenceHelper
import com.shreefintech.paytouchconsumer.utill.Utility

class SplashActivity : BaseActivity() {

    private lateinit var binding: ActivitySplashBinding
    private val viewModel: SplashViewModel by viewModels()

    private val handler = Handler(Looper.getMainLooper())

    private var sessionData: UserProfileItem? = null
    private var apiFinished = false
    private var timerFinished = false

    private lateinit var appUpdateManager: AppUpdateManager
    private var updateLaunching = false
    private var updateCheckDone = false

    private val updateLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        updateLaunching = false
        if (result.resultCode == RESULT_CANCELED) {
            // User declined a mandatory update — close; the prompt shows again on next launch
            finishAffinity()
            return@registerForActivityResult
        }
        // RESULT_OK or RESULT_IN_APP_UPDATE_FAILED — never lock users out on a Play-side error
        updateCheckDone = true
        startFlow()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Glide.with(this).load(R.drawable.paytouch_splash).into(binding.ivSplash)
        binding.tvAppVersion.text = getString(R.string.labelAppVersion, BuildConfig.VERSION_NAME)

        retryCallback = { startFlow() }

        appUpdateManager = AppUpdateManagerFactory.create(this)
        checkForUpdate()
    }

    override fun onResume() {
        super.onResume()
        if (updateCheckDone || updateLaunching || isFinishing) return
        // Re-prompt if a previous IMMEDIATE update was started but the activity was recreated mid-flow
        appUpdateManager.appUpdateInfo.addOnSuccessListener { info ->
            if (!updateCheckDone && !updateLaunching &&
                info.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                launchImmediateUpdate(info)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(timerRunnable)
    }

    private fun checkForUpdate() {
        appUpdateManager.appUpdateInfo
            .addOnSuccessListener { info ->
                if (info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE &&
                    info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) {
                    launchImmediateUpdate(info)
                } else if (!updateLaunching) {
                    // No update needed, or update already launched from onResume
                    updateCheckDone = true
                    startFlow()
                }
            }
            .addOnFailureListener {
                if (!updateLaunching) {
                    updateCheckDone = true
                    startFlow()
                }
            }
    }

    private fun launchImmediateUpdate(info: AppUpdateInfo) {
        if (updateLaunching) return
        updateLaunching = true
        appUpdateManager.startUpdateFlowForResult(
            info, updateLauncher,
            AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build()
        )
    }

    private val timerRunnable = Runnable {
        timerFinished = true
        if (apiFinished) redirect()
    }

    private fun startFlow() {
        apiFinished = false
        timerFinished = false
        sessionData = null
        handler.removeCallbacks(timerRunnable)
        handler.postDelayed(timerRunnable, 5000L)
        fetchSession()
    }

    private fun fetchSession() {
        if (!SharedPreferenceHelper.isLoggedIn(mActivity)) {
            onApiDone(null)
            return
        }
        if (!Utility.isInternetAvailable(mActivity)) {
            showNoInternet()
            return
        }
        hideNoInternet()
        val token = SharedPreferenceHelper.getSharedPreferenceString(mActivity, Constant.KEY_TOKEN, "") ?: ""
        val tokenType = SharedPreferenceHelper.getSharedPreferenceString(mActivity, Constant.KEY_TOKEN_TYPE, "Bearer") ?: "Bearer"
        viewModel.validateSession(
            authorization = "$tokenType $token",
            onSuccess = { data -> onApiDone(data) },
            onError = { onApiDone(null) }
        )
    }

    private fun onApiDone(data: UserProfileItem?) {
        sessionData = data
        apiFinished = true
        if (timerFinished) redirect()
    }

    private fun redirect() {
        NotificationHelper.syncToken(mActivity)
        val intent = when {
            sessionData?.requiresKyc == true  -> Intent(mActivity, KycActivity::class.java)
            sessionData?.requiresMpin == true -> ResetMpinActivity.buildCreateIntent(mActivity)
            sessionData != null               -> Intent(mActivity, HomeActivity::class.java)
            else                              -> Intent(mActivity, LoginActivity::class.java)
        }
        navigate(intent)
    }

    private fun navigate(intent: Intent) {
        val opts = ActivityOptions.makeCustomAnimation(
            this, R.anim.anim_activity_fade_in, R.anim.anim_activity_fade_out
        )
        startActivity(
            intent.apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK },
            opts.toBundle()
        )
        finish()
    }
}
