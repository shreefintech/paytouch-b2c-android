package com.shreefintech.paytouchconsumer.auth

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.viewModels
import com.bumptech.glide.Glide
import com.shreefintech.paytouchconsumer.BaseActivity
import com.shreefintech.paytouchconsumer.Constant
import com.shreefintech.paytouchconsumer.HomeActivity
import com.shreefintech.paytouchconsumer.R
import com.shreefintech.paytouchconsumer.auth.viewmodel.SplashViewModel
import com.shreefintech.paytouchconsumer.databinding.ActivitySplashBinding
import com.shreefintech.paytouchconsumer.onboarding.kyc.KycActivity
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Glide.with(this).asGif().load(R.drawable.gif_splash).into(binding.ivSplash)

        retryCallback = { startFlow() }
        startFlow()
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(timerRunnable)
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
        val intent = when {
            sessionData?.requiresKyc == true  -> Intent(mActivity, KycActivity::class.java)
            sessionData?.requiresMpin == true -> ResetMpinActivity.buildCreateIntent(mActivity)
            sessionData != null               -> Intent(mActivity, HomeActivity::class.java)
            else                              -> Intent(mActivity, LoginActivity::class.java)
        }
        navigate(intent)
    }

    private fun navigate(intent: Intent) {
        startActivity(intent.apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        finish()
    }
}
