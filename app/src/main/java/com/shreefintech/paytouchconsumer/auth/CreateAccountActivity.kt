package com.shreefintech.paytouchconsumer.auth

import android.os.Bundle
import android.text.InputFilter
import android.text.InputType
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.util.Patterns
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.databinding.ObservableBoolean
import com.shreefintech.paytouchconsumer.BaseActivity
import com.shreefintech.paytouchconsumer.R
import com.shreefintech.paytouchconsumer.databinding.ActivityCreateAccountBinding
import com.shreefintech.paytouchconsumer.glass.LiquidGlassEffect
import com.shreefintech.paytouchconsumer.utill.ToastUtil
import com.shreefintech.paytouchconsumer.utill.Utility

class CreateAccountActivity : BaseActivity() {

    private lateinit var binding: ActivityCreateAccountBinding
    private val viewModel: CreateAccountViewModel by viewModels()
    private var showProgress = ObservableBoolean(false)
    private var isPasswordVisible = false
    private var isConfirmPasswordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateAccountBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.clRoot) { v, insets ->
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
            rootView = binding.clRoot as ViewGroup,
            cornerRadius = resources.getDimensionPixelSize(R.dimen.glass_frem_radius),
            distortion = 0f,
            blur = resources.getDimensionPixelSize(R.dimen.glass_frem_blur)
        )

        binding.onClickListener = onClickListener()
        binding.showProgress = showProgress

        onBack()
        setupInputFilters()
        setupTermsText()
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
        binding.etEmail.filters = arrayOf(InputFilter.LengthFilter(100), emojiFilter)
        binding.etReferralCode.filters = arrayOf(InputFilter.LengthFilter(50), emojiFilter)
        binding.etPassword.filters = arrayOf(InputFilter.LengthFilter(20), emojiFilter)
        binding.etConfirmPassword.filters = arrayOf(InputFilter.LengthFilter(20), emojiFilter)
    }

    private fun setupTermsText() {
        val fullText = getString(R.string.labelTermsConditions)
        val linkText = getString(R.string.termsLinkText)
        val spannable = SpannableString(fullText)
        val linkStart = fullText.indexOf(linkText)
        if (linkStart < 0) return

        val linkEnd = linkStart + linkText.length

        spannable.setSpan(object : ClickableSpan() {
            override fun onClick(widget: View) {
                // TODO(PAYTOUCH-487): open terms & conditions URL
            }

            override fun updateDrawState(ds: TextPaint) {
                super.updateDrawState(ds)
                ds.color = ContextCompat.getColor(this@CreateAccountActivity, R.color.primary)
                ds.isUnderlineText = true
            }
        }, linkStart, linkEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        binding.tvTerms.text = spannable
        binding.tvTerms.movementMethod = LinkMovementMethod.getInstance()
        binding.tvTerms.highlightColor = ContextCompat.getColor(this, android.R.color.transparent)
    }

    private fun validate(): Boolean {
        Utility.hideKeyboard(binding.clRoot)
        val mobile = binding.etMobile.text?.toString()?.trim() ?: ""
        val email = binding.etEmail.text?.toString()?.trim() ?: ""
        val password = binding.etPassword.text?.toString() ?: ""
        val confirmPassword = binding.etConfirmPassword.text?.toString() ?: ""

        val msg = when {
            mobile.isEmpty() -> {
                binding.etMobile.requestFocus()
                getString(R.string.msgMobileEmpty)
            }

            mobile.length != 10 -> {
                binding.etMobile.requestFocus()
                getString(R.string.msgMobileInvalid)
            }

            email.isEmpty() -> {
                binding.etEmail.requestFocus()
                getString(R.string.msgEmailEmpty)
            }

            !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                binding.etEmail.requestFocus()
                getString(R.string.msgEmailInvalid)
            }

            password.isEmpty() -> {
                binding.etPassword.requestFocus()
                getString(R.string.msgPasswordEmpty)
            }

            password.length < 8 -> {
                binding.etPassword.requestFocus()
                getString(R.string.msgPasswordShort)
            }

            confirmPassword.isEmpty() -> {
                binding.etConfirmPassword.requestFocus()
                getString(R.string.msgConfirmPasswordEmpty)
            }

            confirmPassword != password -> {
                binding.etConfirmPassword.requestFocus()
                getString(R.string.msgPasswordMismatch)
            }

            !binding.cbTerms.isChecked -> getString(R.string.msgTermsNotAccepted)
            else -> null
        }

        if (msg != null) {
            ToastUtil.showDelete(mActivity, msg)
            return false
        }
        return true
    }

    private fun onCreateAccount() {
        if (!validate()) return
        val mobile       = binding.etMobile.text?.toString()?.trim()      ?: ""
        val email        = binding.etEmail.text?.toString()?.trim()        ?: ""
        val referralCode = binding.etReferralCode.text?.toString()?.trim() ?: ""
        val password     = binding.etPassword.text?.toString()             ?: ""
        viewModel.register(
            context      = mActivity,
            mobile       = mobile,
            email        = email,
            referralCode = referralCode,
            password     = password,
            onLoading    = { showProgress.set(true) },
            onSuccess    = { showProgress.set(false) },
            onError      = { msg -> showProgress.set(false); ToastUtil.showDelete(mActivity, msg) }
        )
    }

    private fun onClickListener(): View.OnClickListener {
        return View.OnClickListener { view ->
            when (view) {
                binding.llCreateAccount -> {
                    if (Utility.stopClick()) return@OnClickListener
                    onCreateAccount()
                }

                binding.tvBackToSignIn -> {
                    if (Utility.stopClick()) return@OnClickListener
                    finish()
                }

                binding.ivPasswordToggle -> {
                    togglePasswordVisibility()
                }

                binding.ivConfirmPasswordToggle -> {
                    toggleConfirmPasswordVisibility()
                }
            }
        }
    }

    private fun togglePasswordVisibility() {
        isPasswordVisible = !isPasswordVisible
        binding.etPassword.inputType = if (isPasswordVisible) {
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
        } else {
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        binding.etPassword.setSelection(binding.etPassword.text?.length ?: 0)
        binding.ivPasswordToggle.setImageResource(
            if (isPasswordVisible) R.drawable.ic_eye_on else R.drawable.ic_eye_off
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
