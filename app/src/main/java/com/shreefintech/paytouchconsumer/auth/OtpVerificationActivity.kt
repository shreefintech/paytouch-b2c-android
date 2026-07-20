package com.shreefintech.paytouchconsumer.auth

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.text.Editable
import android.text.TextWatcher
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
import com.shreefintech.paytouchconsumer.R
import com.shreefintech.paytouchconsumer.databinding.ActivityOtpVerificationBinding
import com.shreefintech.paytouchconsumer.glass.LiquidGlassEffect
import com.shreefintech.paytouchconsumer.utill.ToastUtil
import com.shreefintech.paytouchconsumer.utill.Utility

class OtpVerificationActivity : BaseActivity() {

    companion object {
        fun newIntent(context: Context, flowType: String): Intent =
            Intent(context, OtpVerificationActivity::class.java)
                .putExtra(Constant.EXTRA_FLOW_TYPE, flowType)
    }

    private lateinit var binding: ActivityOtpVerificationBinding
    private val viewModel: OtpVerificationViewModel by viewModels()
    private var showProgress = ObservableBoolean(false)
    private var countDownTimer: CountDownTimer? = null
    private var isResendEnabled = false

    private val flowType by lazy {
        intent.getStringExtra(Constant.EXTRA_FLOW_TYPE) ?: Constant.FLOW_RESET_PASSWORD
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOtpVerificationBinding.inflate(layoutInflater)
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
        setupOtpBoxes()
        startResendTimer()
    }

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
    }

    private fun onBack() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() { finish() }
        })
    }

    private fun setupOtpBoxes() {
        val boxes = listOf(
            binding.etOtp1, binding.etOtp2, binding.etOtp3,
            binding.etOtp4, binding.etOtp5, binding.etOtp6
        )
        boxes.forEachIndexed { index, editText ->
            editText.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
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

    private fun startResendTimer() {
        isResendEnabled = false
        binding.tvResendOtp.isClickable = false
        binding.tvResendOtp.setTextColor(ContextCompat.getColor(mActivity, R.color.hint_color))
        countDownTimer?.cancel()
        countDownTimer = object : CountDownTimer(180_000L, 1_000L) {
            override fun onTick(millisUntilFinished: Long) {
                val min = millisUntilFinished / 60_000
                val sec = (millisUntilFinished % 60_000) / 1_000
                binding.tvResendOtp.text = getString(
                    R.string.resendOtpTimer,
                    String.format("%02d:%02d", min, sec)
                )
            }
            override fun onFinish() {
                isResendEnabled = true
                binding.tvResendOtp.isClickable = true
                binding.tvResendOtp.text = getString(R.string.resendOtp)
                binding.tvResendOtp.setTextColor(ContextCompat.getColor(mActivity, R.color.primary))
            }
        }.start()
    }

    private fun collectOtp(): String {
        return listOf(
            binding.etOtp1, binding.etOtp2, binding.etOtp3,
            binding.etOtp4, binding.etOtp5, binding.etOtp6
        ).joinToString("") { it.text.toString() }
    }

    private fun validate(): Boolean {
        Utility.hideKeyboard(binding.clRoot)
        val otp = collectOtp()
        val msg = when {
            otp.isEmpty()    -> getString(R.string.msgOtpEmpty)
            otp.length != 6  -> getString(R.string.msgOtpIncomplete)
            else             -> null
        }
        if (msg != null) { ToastUtil.showDelete(mActivity, msg); return false }
        return true
    }

    private fun onSubmitOtp() {
        if (!validate()) return
        viewModel.verifyOtp(
            context   = mActivity,
            otp       = collectOtp(),
            flowType  = flowType,
            onLoading = { showProgress.set(true) },
            onSuccess = {
                showProgress.set(false)
                navigateToNextScreen()
            },
            onError   = { msg -> showProgress.set(false); ToastUtil.showDelete(mActivity, msg) }
        )
    }

    private fun onResendOtp() {
        viewModel.resendOtp(
            context   = mActivity,
            flowType  = flowType,
            onLoading = { showProgress.set(true) },
            onSuccess = { showProgress.set(false); startResendTimer() },
            onError   = { msg -> showProgress.set(false); ToastUtil.showDelete(mActivity, msg) }
        )
    }

    private fun navigateToNextScreen() {
        val intent = if (flowType == Constant.FLOW_RESET_MPIN) {
            Intent(mActivity, ResetMpinActivity::class.java)
        } else {
            Intent(mActivity, ResetPasswordActivity::class.java)
        }
        startActivity(intent)
        finish()
    }

    private fun onClickListener(): View.OnClickListener {
        return View.OnClickListener { view ->
            when (view) {
                binding.llSubmitOtp -> {
                    if (Utility.stopClick()) return@OnClickListener
                    onSubmitOtp()
                }
                binding.tvResendOtp -> {
                    if (!isResendEnabled || Utility.stopClick()) return@OnClickListener
                    onResendOtp()
                }
                binding.tvBackToSignIn -> {
                    if (Utility.stopClick()) return@OnClickListener
                    finish()
                }
            }
        }
    }
}
