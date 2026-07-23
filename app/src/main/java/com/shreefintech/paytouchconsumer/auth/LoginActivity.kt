package com.shreefintech.paytouchconsumer.auth

import android.content.Intent
import android.os.Bundle
import android.text.InputFilter
import android.text.InputType
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.databinding.ObservableBoolean
import com.shreefintech.paytouchconsumer.BaseActivity
import com.shreefintech.paytouchconsumer.Constant
import com.shreefintech.paytouchconsumer.HomeActivity
import com.shreefintech.paytouchconsumer.R
import com.shreefintech.paytouchconsumer.auth.viewmodel.LoginViewModel
import com.shreefintech.paytouchconsumer.databinding.ActivityLoginBinding
import com.shreefintech.paytouchconsumer.enums.LoginMode
import com.shreefintech.paytouchconsumer.glass.LiquidGlassEffect
import com.shreefintech.paytouchconsumer.onboarding.CreateVirtualAccountActivity
import com.shreefintech.paytouchconsumer.onboarding.UploadKycActivity
import com.shreefintech.paytouchconsumer.retrofit.model.LoginItem
import com.shreefintech.paytouchconsumer.utill.SharedPreferenceHelper
import com.shreefintech.paytouchconsumer.utill.ToastUtil
import com.shreefintech.paytouchconsumer.utill.Utility
import com.shreefintech.paytouchconsumer.utill.Utility.getThemeColor
import com.shreefintech.paytouchconsumer.utill.Utility.gone
import com.shreefintech.paytouchconsumer.utill.Utility.visible

class LoginActivity : BaseActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val viewModel: LoginViewModel by viewModels()
    private var showProgress = ObservableBoolean(false)
    private var isPasswordVisible = false
    private var currentMode = LoginMode.PASSWORD

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
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
            rootView     = binding.root as ViewGroup,
            cornerRadius = resources.getDimensionPixelSize(R.dimen.glass_frem_radius),
            distortion   = 0f,
            blur         = resources.getDimensionPixelSize(R.dimen.glass_frem_blur)
        )

        binding.onClickListener = onClickListener()
        binding.showProgress    = showProgress

        onBack()
        setupInputFilters()
        updateToggleUi(LoginMode.PASSWORD)
    }

    override fun onStart() {
        super.onStart()
        loadSavedCredentials()
    }

    private fun onBack() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() { finish() }
        })
    }

    private fun setupInputFilters() {
        val emojiFilter = Utility.EmojiExcludeFilter()
        binding.etMobile.filters = arrayOf(
            InputFilter.LengthFilter(10),
            InputFilter { source, start, end, _, _, _ ->
                val sub = source.subSequence(start, end)
                if (sub.all { it.isDigit() }) null else sub.filter { it.isDigit() }
            },
            emojiFilter
        )
        setupCredentialFilter(currentMode)
    }

    private fun setupCredentialFilter(mode: LoginMode) {
        val emojiFilter = Utility.EmojiExcludeFilter()
        binding.etCredential.filters = when (mode) {
            LoginMode.PASSWORD -> arrayOf(InputFilter.LengthFilter(20), emojiFilter)
            LoginMode.MPIN     -> arrayOf(
                InputFilter.LengthFilter(4),
                InputFilter { source, start, end, _, _, _ ->
                    val sub = source.subSequence(start, end)
                    if (sub.all { it.isDigit() }) null else sub.filter { it.isDigit() }
                },
                emojiFilter
            )
        }
    }

    private fun updateToggleUi(mode: LoginMode) {
        currentMode = mode
        when (mode) {
            LoginMode.PASSWORD -> {
                binding.tvBtnPassword.setBackgroundResource(R.drawable.bg_toggle_selected)
                binding.tvBtnMpin.setBackgroundResource(R.drawable.bg_toggle_unselected)
                binding.tvBtnPassword.setTextColor(ContextCompat.getColor(this, R.color.white))
                binding.tvBtnMpin.setTextColor(getThemeColor(com.bumptech.glide.R.attr.colorPrimary))
                binding.tvCredentialLabel.text  = getString(R.string.label_password)
                binding.etCredential.hint       = getString(R.string.hint_password)
                binding.tvForgotPassword.text   = getString(R.string.forgot_password)
                binding.etCredential.inputType  =
                    InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                binding.ivPasswordToggle.visible()
            }
            LoginMode.MPIN -> {
                binding.tvBtnPassword.setBackgroundResource(R.drawable.bg_toggle_unselected)
                binding.tvBtnMpin.setBackgroundResource(R.drawable.bg_toggle_selected)
                binding.tvBtnPassword.setTextColor(getThemeColor(com.bumptech.glide.R.attr.colorPrimary))
                binding.tvBtnMpin.setTextColor(ContextCompat.getColor(this, R.color.white))
                binding.tvCredentialLabel.text  = getString(R.string.label_mpin)
                binding.etCredential.hint       = getString(R.string.hint_mpin)
                binding.tvForgotPassword.text   = getString(R.string.labelForgotMpin)
                binding.etCredential.inputType  =
                    InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
                binding.ivPasswordToggle.gone()
            }
        }
        isPasswordVisible = false
        binding.ivPasswordToggle.setImageResource(R.drawable.ic_eye_off)
        binding.etCredential.text?.clear()
        setupCredentialFilter(mode)
    }

    private fun onNext() {
        Utility.hideKeyboard(binding.root)
        val mobile     = binding.etMobile.text?.toString()?.trim() ?: ""
        val credential = binding.etCredential.text?.toString() ?: ""

        var msg = ""

        when {
            mobile.isEmpty() -> {
                msg = getString(R.string.msgMobileEmpty)
                binding.etMobile.requestFocus()
            }
            mobile.length != 10 -> {
                msg = getString(R.string.msgMobileInvalid)
                binding.etMobile.requestFocus()
            }
            currentMode == LoginMode.PASSWORD && credential.isEmpty() -> {
                msg = getString(R.string.msgPasswordEmpty)
                binding.etCredential.requestFocus()
            }
            currentMode == LoginMode.PASSWORD && credential.length < 6 -> {
                msg = getString(R.string.msgPasswordShort)
                binding.etCredential.requestFocus()
            }
            currentMode == LoginMode.MPIN && credential.isEmpty() -> {
                msg = getString(R.string.msgMpinEmpty)
                binding.etCredential.requestFocus()
            }
            currentMode == LoginMode.MPIN && credential.length != 4 -> {
                msg = getString(R.string.msgMpinInvalid)
                binding.etCredential.requestFocus()
            }
            else -> handleSignIn(mobile, credential)
        }
        if(msg != ""){
            ToastUtil.showDelete(mActivity, msg)
        }
    }

    private fun handleSignIn(mobile: String, credential: String) {
        if (binding.cbSaveCredentials.isChecked) saveCredentials(mobile, credential)
        else clearSavedCredentials()
        viewModel.login(
            mobile     = mobile,
            credential = credential,
            mode       = currentMode,
            onLoading  = { showProgress.set(true) },
            onSuccess  = { data -> showProgress.set(false); navigateAfterLogin(data) },
            onError    = { msg -> showProgress.set(false); ToastUtil.showDelete(mActivity, msg) }
        )
    }

    private fun loadSavedCredentials() {
        val remember = SharedPreferenceHelper.getSharedPreferenceBoolean(this, Constant.KEY_REMEMBER, false)
        if (remember) {
            val mobile = SharedPreferenceHelper.getSharedPreferenceString(this, Constant.KEY_LOGIN_MOBILE, "") ?: ""
            val cred   = SharedPreferenceHelper.getSharedPreferenceString(this, Constant.KEY_LOGIN_TYPE_PASSWORD, "") ?: ""
            binding.etMobile.setText(mobile)
            binding.etCredential.setText(cred)
            binding.cbSaveCredentials.isChecked = true
        }
    }

    private fun saveCredentials(mobile: String, credential: String) {
        SharedPreferenceHelper.setSharedPreferenceBoolean(this, Constant.KEY_REMEMBER, true)
        SharedPreferenceHelper.setSharedPreferenceString(this, Constant.KEY_LOGIN_MOBILE, mobile)
        SharedPreferenceHelper.setSharedPreferenceString(this, Constant.KEY_LOGIN_TYPE_PASSWORD, credential)
    }

    private fun navigateAfterLogin(data: LoginItem?) {
        val user = data?.user
        val intent = when {
            user?.requiresKyc == true            -> Intent(mActivity, UploadKycActivity::class.java)
            user?.requiresVirtualAccount == true -> Intent(mActivity, CreateVirtualAccountActivity::class.java)
            else                                 -> Intent(mActivity, HomeActivity::class.java)
        }
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun clearSavedCredentials() {
        SharedPreferenceHelper.setSharedPreferenceBoolean(this, Constant.KEY_REMEMBER, false)
        SharedPreferenceHelper.setSharedPreferenceString(this, Constant.KEY_LOGIN_MOBILE, "")
        SharedPreferenceHelper.setSharedPreferenceString(this, Constant.KEY_LOGIN_TYPE_PASSWORD, "")
    }

    private fun onClickListener(): View.OnClickListener {
        return View.OnClickListener { view ->
            when (view) {
                binding.tvBtnPassword -> {
                    updateToggleUi(LoginMode.PASSWORD)
                }
                binding.tvBtnMpin -> {
                    updateToggleUi(LoginMode.MPIN)
                }
                binding.llSignIn -> {
                    if (Utility.stopClick()) return@OnClickListener
                    onNext()
                }
                binding.tvForgotPassword -> {
                    if (Utility.stopClick()) return@OnClickListener
                    val flowType = if (currentMode == LoginMode.MPIN) {
                        Constant.FLOW_RESET_MPIN
                    } else {
                        Constant.FLOW_RESET_PASSWORD
                    }
                    startActivity(OtpVerificationActivity.newIntent(mActivity, flowType))
                }
                binding.llCreateAccount -> {
                    if (Utility.stopClick()) return@OnClickListener
                    startActivity(Intent(mActivity, CreateAccountActivity::class.java))
                }
                binding.ivPasswordToggle -> {
                    togglePasswordVisibility()
                }
            }
        }
    }

    private fun togglePasswordVisibility() {
        isPasswordVisible = !isPasswordVisible
        binding.etCredential.inputType = if (isPasswordVisible) {
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
        } else {
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        binding.etCredential.setSelection(binding.etCredential.text?.length ?: 0)
        binding.ivPasswordToggle.setImageResource(
            if (isPasswordVisible) R.drawable.ic_eye_on else R.drawable.ic_eye_off
        )
    }
}
