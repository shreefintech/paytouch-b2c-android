package com.shreefintech.paytouchconsumer.auth

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.InputType
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.TextWatcher
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.widget.AppCompatEditText
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
import com.shreefintech.paytouchconsumer.onboarding.kyc.KycActivity
import com.shreefintech.paytouchconsumer.retrofit.model.auth.LoginItem
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
            val imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime())
            v.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                maxOf(imeInsets.bottom, systemBars.bottom)
            )
            insets
        }

        LiquidGlassEffect.attach(
            targetView = binding.flCard,
            rootView = binding.root as ViewGroup,
            cornerRadius = resources.getDimensionPixelSize(R.dimen.glass_frem_radius),
            distortion = 0f,
            blur = resources.getDimensionPixelSize(R.dimen.glass_frem_blur)
        )

        binding.onClickListener = onClickListener()
        binding.showProgress = showProgress

        onBack()
        setupInputFilters()
        setupMpinBoxes()
        updateToggleUi(LoginMode.PASSWORD)
    }


    private fun onBack() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finish()
            }
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
        binding.etCredential.filters = arrayOf(InputFilter.LengthFilter(20), emojiFilter)
    }

    private fun setupMpinBoxes() {
        wireBoxes(
            listOf(binding.etMpin1, binding.etMpin2, binding.etMpin3, binding.etMpin4)
        )
    }

    private fun wireBoxes(boxes: List<AppCompatEditText>) {
        boxes.forEachIndexed { index, editText ->
            editText.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    if (s?.length == 1 && index < boxes.lastIndex) {
                        boxes[index + 1].requestFocus()
                    }
                }
            })
            editText.setOnKeyListener { _, keyCode, event ->
                if (keyCode == KeyEvent.KEYCODE_DEL
                    && event.action == KeyEvent.ACTION_DOWN
                    && editText.text.isNullOrEmpty()
                    && index > 0
                ) {
                    boxes[index - 1].let { prev ->
                        prev.requestFocus()
                        prev.text?.clear()
                    }
                    true
                } else {
                    false
                }
            }
        }
    }

    private fun collectMpin(): String =
        listOf(binding.etMpin1, binding.etMpin2, binding.etMpin3, binding.etMpin4)
            .joinToString("") { it.text.toString() }

    private fun updateToggleUi(mode: LoginMode) {
        currentMode = mode
        when (mode) {
            LoginMode.PASSWORD -> {
                binding.tvBtnPassword.setBackgroundResource(R.drawable.bg_toggle_selected)
                binding.tvBtnMpin.setBackgroundResource(R.drawable.bg_toggle_unselected)
                binding.tvBtnPassword.setTextColor(ContextCompat.getColor(this, R.color.white))
                binding.tvBtnMpin.setTextColor(getThemeColor(androidx.appcompat.R.attr.colorPrimary))
                binding.tvCredentialLabel.text = getString(R.string.label_password)
                binding.tvForgotPassword.text = getString(R.string.forgot_password)
                binding.etCredential.inputType =
                    InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                binding.flPasswordInput.visible()
                binding.clMpinBoxes.gone()
                listOf(binding.etMpin1, binding.etMpin2, binding.etMpin3, binding.etMpin4)
                    .forEach { it.text?.clear() }
            }

            LoginMode.MPIN -> {
                binding.tvBtnPassword.setBackgroundResource(R.drawable.bg_toggle_unselected)
                binding.tvBtnMpin.setBackgroundResource(R.drawable.bg_toggle_selected)
                binding.tvBtnPassword.setTextColor(getThemeColor(androidx.appcompat.R.attr.colorPrimary))
                binding.tvBtnMpin.setTextColor(ContextCompat.getColor(this, R.color.white))
                binding.tvCredentialLabel.text = getString(R.string.label_mpin)
                binding.tvForgotPassword.text = getString(R.string.labelForgotMpin)
                binding.flPasswordInput.gone()
                binding.clMpinBoxes.visible()
                binding.etMpin1.requestFocus()
            }
        }
        isPasswordVisible = false
        binding.ivPasswordToggle.setImageResource(R.drawable.ic_eye_off)
        binding.etCredential.text?.clear()
    }

    private fun onNext() {
        Utility.hideKeyboard(binding.root)
        val mobile = binding.etMobile.text?.toString()?.trim() ?: ""
        val credential = when (currentMode) {
            LoginMode.PASSWORD -> binding.etCredential.text?.toString() ?: ""
            LoginMode.MPIN -> collectMpin()
        }

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

            currentMode == LoginMode.PASSWORD && credential.length < 8 -> {
                msg = getString(R.string.msgPasswordShort)
                binding.etCredential.requestFocus()
            }

            currentMode == LoginMode.MPIN && credential.isEmpty() -> {
                msg = getString(R.string.msgMpinEmpty)
                binding.etMpin1.requestFocus()
            }

            currentMode == LoginMode.MPIN && credential.length != 4 -> {
                msg = getString(R.string.msgMpinInvalid)
                listOf(binding.etMpin1, binding.etMpin2, binding.etMpin3, binding.etMpin4)
                    .firstOrNull { it.text.isNullOrEmpty() }?.requestFocus()
            }


            else -> handleSignIn(mobile, credential)
        }
        if (msg != "") {
            ToastUtil.showDelete(mActivity, msg)
        }
    }

    private fun handleSignIn(mobile: String, credential: String) {

        viewModel.login(
            mobile = mobile,
            credential = credential,
            mode = currentMode,
            onLoading = { showProgress.set(true) },
            onSuccess = { data ->
                showProgress.set(false)
                ToastUtil.showSuccess(mActivity, getString(R.string.msgLoginSuccess))
                navigateAfterLogin(data)
            },
            onError = { msg ->
                showProgress.set(false)
                ToastUtil.showDelete(mActivity, msg)
            }
        )
    }

    private fun navigateAfterLogin(data: LoginItem?) {
        val intent = when {
            data?.requiresKyc == true            -> Intent(mActivity, KycActivity::class.java)
            data?.requiresMpin == true           -> Intent(mActivity, ResetMpinActivity::class.java)
            data?.requiresVirtualAccount == true -> Intent(mActivity, CreateVirtualAccountActivity::class.java)
            else                                 -> Intent(mActivity, HomeActivity::class.java)
        }
        startActivity(intent)
        finishAffinity()
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
                    val mobile = binding.etMobile.text?.toString()?.trim() ?: ""
                    if (mobile.isEmpty() || mobile.length != 10) {
                        ToastUtil.showDelete(mActivity, getString(R.string.msgMobileInvalid))
                        binding.etMobile.requestFocus()
                        return@OnClickListener
                    }
                    val flowType =
                        if (currentMode == LoginMode.MPIN) Constant.FLOW_RESET_MPIN else Constant.FLOW_RESET_PASSWORD
                    startActivity(OtpVerificationActivity.newIntent(mActivity, flowType, mobile))
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
