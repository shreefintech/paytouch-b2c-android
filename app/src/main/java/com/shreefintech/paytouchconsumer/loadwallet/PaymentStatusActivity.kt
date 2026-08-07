package com.shreefintech.paytouchconsumer.loadwallet

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.databinding.ObservableBoolean
import com.shreefintech.paytouchconsumer.BaseActivity
import com.shreefintech.paytouchconsumer.Constant
import com.shreefintech.paytouchconsumer.R
import com.shreefintech.paytouchconsumer.databinding.ActivityPaymentStatusBinding
import com.shreefintech.paytouchconsumer.retrofit.ApiClient
import com.shreefintech.paytouchconsumer.retrofit.ApiHelper
import com.shreefintech.paytouchconsumer.retrofit.model.hdfc.HdfcOrderResponseItem
import com.shreefintech.paytouchconsumer.utill.SharedPreferenceHelper
import com.shreefintech.paytouchconsumer.utill.ToastUtil
import com.shreefintech.paytouchconsumer.utill.Utility
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class PaymentStatusActivity : BaseActivity() {

    private lateinit var binding: ActivityPaymentStatusBinding

    private val orderId: String by lazy { intent.getStringExtra(EXTRA_ORDER_ID) ?: "" }
    private val amount: String by lazy { intent.getStringExtra(EXTRA_AMOUNT) ?: "" }
    private val statusStr: String by lazy { intent.getStringExtra(EXTRA_STATUS) ?: "" }
    private val payLink: String by lazy { intent.getStringExtra(EXTRA_PAY_LINK) ?: "" }

    private val showProgressCheck = ObservableBoolean(false)

    private val countdownHandler = Handler(Looper.getMainLooper())
    private var countdownSeconds = COUNTDOWN_START
    private var countdownCancelled = false

    private val countdownRunnable = object : Runnable {
        override fun run() {
            if (countdownCancelled) return
            if (countdownSeconds > 0) {
                binding.tvContinue.text = getString(R.string.btnContinue) + " ($countdownSeconds)"
                countdownSeconds--
                countdownHandler.postDelayed(this, 1000L)
            } else {
                goToWallet()
            }
        }
    }

    companion object {
        private const val EXTRA_ORDER_ID = "extra_order_id"
        private const val EXTRA_AMOUNT = "extra_amount"
        private const val EXTRA_STATUS = "extra_status"
        private const val EXTRA_PAY_LINK = "extra_pay_link"
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

        fun start(
            context: Context,
            orderId: String,
            amount: String,
            status: String,
            payLink: String
        ) {
            context.startActivity(
                Intent(context, PaymentStatusActivity::class.java).apply {
                    putExtra(EXTRA_ORDER_ID, orderId)
                    putExtra(EXTRA_AMOUNT, amount)
                    putExtra(EXTRA_STATUS, status)
                    putExtra(EXTRA_PAY_LINK, payLink)
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
        countdownCancelled = true
        countdownHandler.removeCallbacks(countdownRunnable)
    }

    private fun populateStatus(status: String) {
        val (label, color, iconRes) = resolveStatus(status)
        binding.tvStatusLabel.text = label

        val drawable = binding.flStatusIcon.background.mutate() as GradientDrawable
        drawable.setColor(Color.parseColor(color))

        binding.ivStatusIcon.setImageResource(iconRes)
        binding.tvOrderId.text = orderId
        binding.tvAmount.text = Utility.formatAmount(amount)
    }

    private fun resolveStatus(status: String): Triple<String, String, Int> {
        return when (status.uppercase()) {
            STATUS_CHARGED, STATUS_AUTHORIZED ->
                Triple(getString(R.string.msgPaymentSuccessful), "#00C853", R.drawable.ic_success)
            STATUS_NEW ->
                Triple(getString(R.string.msgPaymentInitiated), "#0A66FF", R.drawable.ic_pending)
            STATUS_PENDING_VBV, STATUS_AUTHORIZING, STATUS_STARTED ->
                Triple(getString(R.string.msgPaymentProcessing), "#FF8F00", R.drawable.ic_pending)
            STATUS_JUSPAY_DECLINED, STATUS_AUTHENTICATION_FAILED, STATUS_AUTHORIZATION_FAILED ->
                Triple(getString(R.string.msgPaymentFailed), "#D32F2F", R.drawable.ic_cross)
            STATUS_AUTO_REFUNDED ->
                Triple(getString(R.string.msgAmountRefunded), "#7A63FF", R.drawable.ic_success)
            else ->
                Triple(getString(R.string.msgPaymentPending), "#FF8F00", R.drawable.ic_pending)
        }
    }

    private fun startCountdown() {
        countdownSeconds = COUNTDOWN_START
        countdownCancelled = false
        countdownHandler.post(countdownRunnable)
    }

    private fun goToWallet() {
        val intent = Intent(this, LoadWalletActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("from_payment", true)
        }
        startActivity(intent)
        finish()
    }

    private fun recheckStatus() {
        if (showProgressCheck.get()) return
        if (!Utility.isInternetAvailable(mActivity)) {
            ToastUtil.showDelete(mActivity, getString(R.string.msgNoInternet))
            return
        }
        val token = "Bearer " + (SharedPreferenceHelper.getSharedPreferenceString(
            mActivity, Constant.KEY_TOKEN, ""
        ) ?: "")
        showProgressCheck.set(true)
        ApiClient.apiService.getHdfcOrderStatus(token, orderId)
            .enqueue(object : Callback<HdfcOrderResponseItem> {
                override fun onResponse(
                    call: Call<HdfcOrderResponseItem>,
                    response: Response<HdfcOrderResponseItem>
                ) {
                    showProgressCheck.set(false)
                    if (response.isSuccessful && response.body()?.data != null) {
                        val newStatus = response.body()!!.data!!.status ?: statusStr
                        populateStatus(newStatus)
                    } else {
                        ToastUtil.showDelete(
                            mActivity,
                            ApiHelper.parseErrorMessage(
                                mActivity, response.code(), response.errorBody()?.string()
                            )
                        )
                    }
                }

                override fun onFailure(call: Call<HdfcOrderResponseItem>, t: Throwable) {
                    showProgressCheck.set(false)
                    ToastUtil.showDelete(mActivity, t.localizedMessage ?: getString(R.string.errGeneric))
                }
            })
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
                    countdownCancelled = true
                    countdownHandler.removeCallbacks(countdownRunnable)
                    goToWallet()
                }
                binding.btnCheckStatus -> {
                    if (Utility.stopClick()) return@OnClickListener
                    recheckStatus()
                }
            }
        }
    }
}
