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
import com.shreefintech.paytouchconsumer.kyc.KycActivity
import com.shreefintech.paytouchconsumer.utill.SharedPreferenceHelper
import com.shreefintech.paytouchconsumer.utill.Utility

class SplashActivity : BaseActivity() {

    private lateinit var binding: ActivitySplashBinding
    private val viewModel: SplashViewModel by viewModels()

    private val handler = Handler(Looper.getMainLooper())

    private var sessionValid = false
    private var nextStep: String? = null
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
        sessionValid = false
        nextStep = null
        handler.removeCallbacks(timerRunnable)
        handler.postDelayed(timerRunnable, 5000L)
        fetchSession()
    }

    private fun fetchSession() {
        if (!SharedPreferenceHelper.isLoggedIn(mActivity)) {
            onApiDone(false, null)
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
            onSuccess = { step -> onApiDone(true, step) },
            onError = { onApiDone(false, null) }
        )
    }

    private fun onApiDone(valid: Boolean, step: String?) {
        sessionValid = valid
        nextStep = step
        apiFinished = true
        if (timerFinished) redirect()
    }

    private fun redirect() {
        val intent = when {
            !sessionValid                                        -> Intent(mActivity, LoginActivity::class.java)
            nextStep == "kyc_required" ||
            nextStep == "pending_approval"                       -> Intent(mActivity, KycActivity::class.java)
            else                                                 -> Intent(mActivity, HomeActivity::class.java)
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
