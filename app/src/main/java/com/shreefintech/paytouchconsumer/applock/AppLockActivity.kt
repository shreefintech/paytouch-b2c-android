package com.shreefintech.paytouchconsumer.applock

import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.shreefintech.paytouchconsumer.BaseActivity
import com.shreefintech.paytouchconsumer.R
import com.shreefintech.paytouchconsumer.databinding.ActivityAppLockBinding
import com.shreefintech.paytouchconsumer.utill.Utility

/**
 * Lock screen shown by [AppLockHelper]. Verifies the user with the phone's own screen lock.
 * Cancel keeps this screen with an Unlock button; Back closes the app.
 */
class AppLockActivity : BaseActivity() {

    private lateinit var binding: ActivityAppLockBinding
    private lateinit var biometricPrompt: BiometricPrompt

    private val promptInfo: BiometricPrompt.PromptInfo by lazy {
        BiometricPrompt.PromptInfo.Builder()
            .setTitle(getString(R.string.titleAppLock))
            .setSubtitle(getString(R.string.msgAppLockPrompt))
            .setAllowedAuthenticators(AUTHENTICATORS)
            .build()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAppLockBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.clRoot) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.onClickListener = onClickListener()

        onBack()
        setupPrompt()
        if (savedInstanceState == null) showPrompt()
    }

    private fun onBack() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Never dismiss the lock — leave the app instead.
                finishAffinity()
            }
        })
    }

    private fun onClickListener(): View.OnClickListener {
        return View.OnClickListener {
            when (it) {
                binding.cvUnlock -> {
                    if (Utility.stopClick()) return@OnClickListener
                    showPrompt()
                }
            }
        }
    }

    private fun setupPrompt() {
        // Must be created in onCreate(); the prompt is bound to this Activity's lifecycle.
        biometricPrompt = BiometricPrompt(
            this,
            ContextCompat.getMainExecutor(this),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onUnlocked()
                }
                // Cancel / error: stay on this screen — the Unlock button shows the prompt again.
            }
        )
    }

    private fun showPrompt() {
        if (BiometricManager.from(this).canAuthenticate(AUTHENTICATORS) != BiometricManager.BIOMETRIC_SUCCESS) {
            // No screen lock set on this phone — there is nothing to verify against.
            onUnlocked()
            return
        }
        biometricPrompt.authenticate(promptInfo)
    }

    private fun onUnlocked() {
        AppLockHelper.unlock()
        finish()
    }

    companion object {
        // WEAK + DEVICE_CREDENTIAL works on every API level from 24; STRONG + DEVICE_CREDENTIAL fails on API 28–29.
        private const val AUTHENTICATORS =
            BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL
    }
}
