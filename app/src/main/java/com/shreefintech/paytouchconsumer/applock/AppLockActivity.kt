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
import com.shreefintech.paytouchconsumer.utill.AnimationHelper
import com.shreefintech.paytouchconsumer.utill.Utility

/**
 * Lock screen shown by [AppLockHelper]. Verifies the user with the phone's own screen lock.
 * Transparent window (GPay-style): the screen the user left stays visible behind the prompt, but
 * this window still takes every touch. Cancel dims it and shows an Unlock panel; Back closes the app.
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

        // Scrim stays full-screen behind the system bars; only the panel clears the navigation bar.
        val panelPaddingBottom = binding.clPanel.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(binding.clPanel) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(v.paddingLeft, v.paddingTop, v.paddingRight, panelPaddingBottom + systemBars.bottom)
            insets
        }

        binding.onClickListener = onClickListener()

        onBack()
        setupPrompt()
        if (savedInstanceState == null) {
            showPrompt()
        } else {
            // Recreated (rotation / process restore) — the prompt may be gone, so keep Unlock reachable.
            showUnlockPanel(animate = false)
        }
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

                // Cancel / lockout / error: stay locked — the Unlock button shows the prompt again.
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    when (errorCode) {
                        // The phone has no screen lock after all — same outcome as the pre-check.
                        BiometricPrompt.ERROR_NO_DEVICE_CREDENTIAL,
                        BiometricPrompt.ERROR_HW_NOT_PRESENT -> onUnlocked()
                        else -> showUnlockPanel(animate = true)
                    }
                }
            }
        )
    }

    private fun showPrompt() {
        when (BiometricManager.from(this).canAuthenticate(AUTHENTICATORS)) {
            // No screen lock set on this phone — there is nothing to verify against.
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED,
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> {
                onUnlocked()
                return
            }
            // Any other result (transient / unknown) must not skip the lock — let the prompt decide.
        }
        biometricPrompt.authenticate(promptInfo)
    }

    private fun showUnlockPanel(animate: Boolean) {
        if (binding.clPanel.visibility == View.VISIBLE) return
        binding.viewBg.visibility = View.VISIBLE
        binding.clPanel.visibility = View.VISIBLE
        if (animate) AnimationHelper.animateEntrance(binding.clPanel, 0)
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
