package com.shreefintech.paytouchconsumer.loadwallet

import android.content.Context
import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.databinding.ObservableBoolean
import com.google.gson.Gson
import com.shreefintech.paytouchconsumer.BaseActivity
import com.shreefintech.paytouchconsumer.Constant
import com.shreefintech.paytouchconsumer.R
import com.shreefintech.paytouchconsumer.databinding.ActivityPaymentStatusBinding
import com.shreefintech.paytouchconsumer.loadwallet.model.PaymentStatusItem
import com.shreefintech.paytouchconsumer.loadwallet.viewmodel.PaymentStatusViewModel
import com.shreefintech.paytouchconsumer.utill.ToastUtil
import com.shreefintech.paytouchconsumer.utill.Utility

class PaymentStatusActivity : BaseActivity() {

    private lateinit var binding: ActivityPaymentStatusBinding
    private val viewModel: PaymentStatusViewModel by viewModels()

    private val passItem: PaymentStatusItem? by lazy {
        intent.getStringExtra(EXTRA_ITEM)?.let { Gson().fromJson(it, PaymentStatusItem::class.java) }
    }
    private val orderId: String by lazy { passItem?.orderId ?: "" }
    private val amount: String by lazy { passItem?.amount ?: "" }
    private val statusStr: String by lazy { passItem?.status ?: "" }

    private val showProgressCheck = ObservableBoolean(false)

    private val countdownHandler = Handler(Looper.getMainLooper())
    private var countdownSeconds = COUNTDOWN_START
    private var isCountdownCancelled = false

    private val countdownRunnable = object : Runnable {
        override fun run() {
            if (isCountdownCancelled) return
            if (countdownSeconds > 0) {
                binding.tvContinue.text = getString(R.string.btnContinueCountdown, countdownSeconds)
                countdownSeconds--
                countdownHandler.postDelayed(this, 1000L)
            } else {
                goToWallet()
            }
        }
    }

    companion object {
        private const val EXTRA_ITEM = "extra_item"
        private const val COUNTDOWN_START = 5

        private const val STATUS_CHARGED = "CHARGED"
        private const val STATUS_AUTHORIZED = "AUTHORIZED"
        private const val STATUS_NEW = "NEW"
        private const val STATUS_PENDING_VBV = "PENDING_VBV"
        private const val STATUS_AUTHORIZING = "AUTHORIZING"
        private const val STATUS_STARTED = "STARTED"
        private const val STATUS_JUSPAY_DECLINED = "JUSPAY_DECLINED"
        private const val STATUS_AUTHENTICATION_FAILED = "AUTHENTICATION_FAILED"
        private const val STATUS_AUTHORIZATION_FAILED = "AUTHORIZATION_FAILED"
        private const val STATUS_AUTO_REFUNDED = "AUTO_REFUNDED"

        fun start(context: Context, item: PaymentStatusItem) {
            context.startActivity(
                Intent(context, PaymentStatusActivity::class.java).apply {
                    putExtra(EXTRA_ITEM, Gson().toJson(item))
                }
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPaymentStatusBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.onClickListener = onClickListener()
        binding.showProgressCheck = showProgressCheck

        populateStatus(statusStr)
        startCountdown()
        onBack()
    }

    override fun onDestroy() {
        super.onDestroy()
        isCountdownCancelled = true
        countdownHandler.removeCallbacks(countdownRunnable)
    }

    private fun populateStatus(status: String) {
        val (label, colorRes, iconRes) = resolveStatus(status)
        binding.tvStatusLabel.text = label

        // bg_payment_status_icon.xml is a <shape> drawable — GradientDrawable at runtime
        val drawable = binding.flStatusIcon.background.mutate() as GradientDrawable
        drawable.setColor(ContextCompat.getColor(mActivity, colorRes))

        binding.ivStatusIcon.setImageResource(iconRes)
        binding.tvOrderId.text = orderId
        binding.tvAmount.text = Utility.formatAmount(amount)
    }

    private data class StatusDisplay(
        val label: String,
        @ColorRes val colorRes: Int,
        @DrawableRes val iconRes: Int
    )

    private fun resolveStatus(status: String): StatusDisplay {
        return when (status.uppercase()) {
            STATUS_CHARGED, STATUS_AUTHORIZED ->
                StatusDisplay(getString(R.string.msgPaymentSuccessful), R.color.colorPaymentSuccess, R.drawable.ic_success)
            STATUS_NEW ->
                StatusDisplay(getString(R.string.msgPaymentInitiated), R.color.colorPaymentInitiated, R.drawable.ic_pending)
            STATUS_PENDING_VBV, STATUS_AUTHORIZING, STATUS_STARTED ->
                StatusDisplay(getString(R.string.msgPaymentProcessing), R.color.colorPaymentProcessing, R.drawable.ic_pending)
            STATUS_JUSPAY_DECLINED, STATUS_AUTHENTICATION_FAILED, STATUS_AUTHORIZATION_FAILED ->
                StatusDisplay(getString(R.string.msgPaymentFailed), R.color.colorPaymentFailed, R.drawable.ic_cross)
            STATUS_AUTO_REFUNDED ->
                StatusDisplay(getString(R.string.msgAmountRefunded), R.color.colorPaymentRefunded, R.drawable.ic_success)
            else ->
                StatusDisplay(getString(R.string.msgPaymentPending), R.color.colorPaymentProcessing, R.drawable.ic_pending)
        }
    }

    private fun startCountdown() {
        countdownSeconds = COUNTDOWN_START
        isCountdownCancelled = false
        countdownHandler.post(countdownRunnable)
    }

    private fun goToWallet() {
        val intent = Intent(this, LoadWalletActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(Constant.EXTRA_FROM_PAYMENT, true)
        }
        startActivity(intent)
        finish()
    }

    private fun recheckStatus() {
        viewModel.recheckStatus(
            orderId   = orderId,
            onLoading = { showProgressCheck.set(true) },
            onSuccess = { orderItem ->
                showProgressCheck.set(false)
                populateStatus(orderItem.status ?: statusStr)
            },
            onError   = { msg ->
                showProgressCheck.set(false)
                ToastUtil.showDelete(mActivity, msg)
            }
        )
    }

    private fun onBack() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                goToWallet()
            }
        })
    }

    private fun onClickListener(): View.OnClickListener {
        return View.OnClickListener { view ->
            when (view) {
                binding.btnContinue -> {
                    if (Utility.stopClick()) return@OnClickListener
                    isCountdownCancelled = true
                    countdownHandler.removeCallbacks(countdownRunnable)
                    goToWallet()
                }
                binding.btnCheckStatus -> {
                    if (Utility.stopClick()) return@OnClickListener
                    if (showProgressCheck.get()) return@OnClickListener
                    recheckStatus()
                }
            }
        }
    }
}
