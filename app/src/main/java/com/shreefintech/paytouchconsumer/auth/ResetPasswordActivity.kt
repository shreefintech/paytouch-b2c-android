package com.shreefintech.paytouchconsumer.auth

import android.content.Intent
import android.os.Bundle
import android.text.InputFilter
import android.text.InputType
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.databinding.ObservableBoolean
import com.shreefintech.paytouchconsumer.BaseActivity
import com.shreefintech.paytouchconsumer.R
import com.shreefintech.paytouchconsumer.auth.viewmodel.ResetPasswordViewModel
import com.shreefintech.paytouchconsumer.databinding.ActivityResetPasswordBinding
import com.shreefintech.paytouchconsumer.glass.LiquidGlassEffect
import com.shreefintech.paytouchconsumer.utill.ToastUtil
import com.shreefintech.paytouchconsumer.utill.Utility

class ResetPasswordActivity : BaseActivity() {

    private lateinit var binding: ActivityResetPasswordBinding
    private val viewModel: ResetPasswordViewModel by viewModels()
    private var showProgress = ObservableBoolean(false)
    private var isNewPasswordVisible     = false
    private var isConfirmPasswordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResetPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.clRoot) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val imeInsets  = insets.getInsets(WindowInsetsCompat.Type.ime())
            v.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                maxOf(imeInsets.bottom, systemBars.bottom)
            )
            insets
        }

        LiquidGlassEffect.attach(
            targetView   = binding.flCard,
            rootView     = binding.clRoot as ViewGroup,
            cornerRadius = resources.getDimensionPixelSize(R.dimen.glass_frem_radius),
            distortion   = 0f,
            blur         = resources.getDimensionPixelSize(R.dimen.glass_frem_blur)
        )

        binding.onClickListener = onClickListener()
        binding.showProgress    = showProgress

        onBack()
        setupInputFilters()
    }

    private fun onBack() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() { navigateToLogin() }
        })
    }

    private fun setupInputFilters() {
        val emojiFilter = Utility.EmojiExcludeFilter()
        binding.etNewPassword.filters     = arrayOf(InputFilter.LengthFilter(30), emojiFilter)
        binding.etConfirmPassword.filters = arrayOf(InputFilter.LengthFilter(30), emojiFilter)
    }

    private fun validate(): Boolean {
        Utility.hideKeyboard(binding.clRoot)
        val newPassword     = binding.etNewPassword.text?.toString()     ?: ""
        val confirmPassword = binding.etConfirmPassword.text?.toString() ?: ""

        val msg = when {
            newPassword.isEmpty() -> {
                binding.etNewPassword.requestFocus()
                getString(R.string.msgNewPasswordEmpty)
            }
            newPassword.length < 8 -> {
                binding.etNewPassword.requestFocus()
                getString(R.string.msgNewPasswordShort)
            }
            !isPasswordStrong(newPassword) -> {
                binding.etNewPassword.requestFocus()
                getString(R.string.msgPasswordWeak)
            }
            confirmPassword.isEmpty() -> {
                binding.etConfirmPassword.requestFocus()
                getString(R.string.msgConfirmNewPasswordEmpty)
            }
            confirmPassword != newPassword -> {
                binding.etConfirmPassword.requestFocus()
                getString(R.string.msgPasswordMismatch)
            }
            else -> null
        }
        if (msg != null) { ToastUtil.showDelete(mActivity, msg); return false }
        return true
    }

    private fun isPasswordStrong(password: String): Boolean {
        val hasUppercase = password.any { it.isUpperCase() }
        val hasDigit     = password.any { it.isDigit() }
        val hasSpecial   = password.any { !it.isLetterOrDigit() }
        return hasUppercase && hasDigit && hasSpecial
    }

    private fun onChangePassword() {
        if (!validate()) return
        val newPassword = binding.etNewPassword.text?.toString() ?: ""
        viewModel.changePassword(
            context     = mActivity,
            newPassword = newPassword,
            onLoading   = { showProgress.set(true) },
            onSuccess   = { showProgress.set(false); navigateToLogin() },
            onError     = { msg -> showProgress.set(false); ToastUtil.showDelete(mActivity, msg) }
        )
    }

    private fun navigateToLogin() {
        val intent = Intent(mActivity, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        startActivity(intent)
        finish()
    }

    private fun onClickListener(): View.OnClickListener {
        return View.OnClickListener { view ->
            when (view) {
                binding.llChangePassword -> {
                    if (Utility.stopClick()) return@OnClickListener
                    onChangePassword()
                }
                binding.tvBackToSignIn -> {
                    if (Utility.stopClick()) return@OnClickListener
                    navigateToLogin()
                }
                binding.ivNewPasswordToggle -> {
                    toggleNewPasswordVisibility()
                }
                binding.ivConfirmPasswordToggle -> {
                    toggleConfirmPasswordVisibility()
                }
            }
        }
    }

    private fun toggleNewPasswordVisibility() {
        isNewPasswordVisible = !isNewPasswordVisible
        binding.etNewPassword.inputType = if (isNewPasswordVisible) {
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
        } else {
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        binding.etNewPassword.setSelection(binding.etNewPassword.text?.length ?: 0)
        binding.ivNewPasswordToggle.setImageResource(
            if (isNewPasswordVisible) R.drawable.ic_eye_on else R.drawable.ic_eye_off
        )
    }

    private fun toggleConfirmPasswordVisibility() {
        isConfirmPasswordVisible = !isConfirmPasswordVisible
        binding.etConfirmPassword.inputType = if (isConfirmPasswordVisible) {
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
        } else {
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        binding.etConfirmPassword.setSelection(binding.etConfirmPassword.text?.length ?: 0)
        binding.ivConfirmPasswordToggle.setImageResource(
            if (isConfirmPasswordVisible) R.drawable.ic_eye_on else R.drawable.ic_eye_off
        )
    }
}
