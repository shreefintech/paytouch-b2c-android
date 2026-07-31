package com.shreefintech.paytouchconsumer.auth

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.activity.viewModels
import com.shreefintech.paytouchconsumer.BaseActivity
import com.shreefintech.paytouchconsumer.Constant
import com.shreefintech.paytouchconsumer.HomeActivity
import com.shreefintech.paytouchconsumer.auth.viewmodel.SplashViewModel
import com.shreefintech.paytouchconsumer.databinding.ActivitySplashBinding
import com.shreefintech.paytouchconsumer.onboarding.CreateVirtualAccountActivity
import com.shreefintech.paytouchconsumer.onboarding.UploadKycActivity
import com.shreefintech.paytouchconsumer.retrofit.model.UserProfileItem
import com.shreefintech.paytouchconsumer.utill.SharedPreferenceHelper
import com.shreefintech.paytouchconsumer.utill.Utility

class SplashActivity : BaseActivity() {

    private lateinit var binding: ActivitySplashBinding
    private val viewModel: SplashViewModel by viewModels()

    private val handler = Handler(Looper.getMainLooper())
    private val checkSessionRunnable = Runnable { checkSession() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        handler.postDelayed(checkSessionRunnable, 2000L)
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(checkSessionRunnable)
    }

    private fun checkSession() {
        if (!SharedPreferenceHelper.isLoggedIn(mActivity)) {
            navigate(Intent(mActivity, LoginActivity::class.java))
            return
        }
        if (!Utility.isInternetAvailable(mActivity)) {
            navigate(Intent(mActivity, LoginActivity::class.java))
            return
        }
        val token     = SharedPreferenceHelper.getSharedPreferenceString(mActivity, Constant.KEY_TOKEN, "") ?: ""
        val tokenType = SharedPreferenceHelper.getSharedPreferenceString(mActivity, Constant.KEY_TOKEN_TYPE, "Bearer") ?: "Bearer"
        binding.progressBar.visibility = View.VISIBLE
        viewModel.validateSession(
            authorization = "$tokenType $token",
            onSuccess = { data ->
                binding.progressBar.visibility = View.GONE
                routeByFlags(data)
            },
            onError = {
                binding.progressBar.visibility = View.GONE
                navigate(Intent(mActivity, LoginActivity::class.java))
            }
        )
    }

    private fun routeByFlags(data: UserProfileItem?) {
        val intent = when {
            data?.requiresKyc == true            -> Intent(mActivity, UploadKycActivity::class.java)
            data?.requiresMpin == true           -> Intent(mActivity, ResetMpinActivity::class.java)
            data?.requiresVirtualAccount == true -> Intent(mActivity, CreateVirtualAccountActivity::class.java)
            else                                 -> Intent(mActivity, HomeActivity::class.java)
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
