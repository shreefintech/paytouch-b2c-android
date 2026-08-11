package com.shreefintech.paytouchconsumer.loadwallet

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.annotation.DrawableRes
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.gif.GifDrawable
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.google.gson.Gson
import com.shreefintech.paytouchconsumer.BaseActivity
import com.shreefintech.paytouchconsumer.Constant
import com.shreefintech.paytouchconsumer.R
import com.shreefintech.paytouchconsumer.databinding.ActivityPaymentStatusBinding
import com.shreefintech.paytouchconsumer.loadwallet.model.PaymentStatusItem
import com.shreefintech.paytouchconsumer.utill.ToastUtil
import com.shreefintech.paytouchconsumer.utill.Utility
import com.shreefintech.paytouchconsumer.utill.Utility.gone

class PaymentStatusActivity : BaseActivity() {

    private lateinit var binding: ActivityPaymentStatusBinding

    private val passItem: PaymentStatusItem? by lazy {
        intent.getStringExtra(EXTRA_ITEM)
            ?.let { Gson().fromJson(it, PaymentStatusItem::class.java) }
    }
    private val orderId: String by lazy { passItem?.orderId ?: "" }
    private val amount: String by lazy { passItem?.amount ?: "" }
    private val statusStr: String by lazy { passItem?.status ?: "" }

    private val autoFinishHandler = Handler(Looper.getMainLooper())
    private var isNavigating = false

    companion object {
        private const val EXTRA_ITEM = "extra_item"

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

        ViewCompat.setOnApplyWindowInsetsListener(binding.clRoot) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.onClickListener = onClickListener()
        binding.lytToolbar.ivBack.gone()
        populateStatus(statusStr)
        autoFinishHandler.postDelayed({ goToWallet() }, 5000L)
        onBack()
    }

    override fun onDestroy() {
        super.onDestroy()
        autoFinishHandler.removeCallbacksAndMessages(null)
    }

    private fun populateStatus(status: String) {
        val display = resolveStatus(status)
        binding.tvStatusLabel.text = display.label
        binding.tvStatusLabel.setTextColor(display.statusColor)
        binding.tvStatusDescription.text = display.description
        binding.tvAmountLabel.text = display.amountLabel
        binding.tvOrderId.text = orderId
        binding.tvAmount.text = Utility.formatAmount(amount)
        binding.tvAmount.setTextColor(display.statusColor)
        loadGif(display.gifRes)
    }

    private data class StatusDisplay(
        val label: String,
        val description: String,
        val amountLabel: String,
        val statusColor: Int,
        @DrawableRes val gifRes: Int
    )

    private fun resolveStatus(status: String): StatusDisplay {
        val successColor = Color.parseColor("#07974F")
        val pendingColor = Color.parseColor("#D89633")
        val failedColor = Color.parseColor("#D84C55")

        return when (status.uppercase()) {
            STATUS_CHARGED, STATUS_AUTHORIZED -> StatusDisplay(
                label = getString(R.string.msgPaymentSuccessful),
                description = getString(R.string.msgPaymentSuccessDescription),
                amountLabel = getString(R.string.labelAmountPaid),
                statusColor = successColor,
                gifRes = R.drawable.gif_success
            )

            STATUS_NEW -> StatusDisplay(
                label = getString(R.string.msgPaymentInitiated),
                description = getString(R.string.msgPaymentInitiatedDescription),
                amountLabel = getString(R.string.labelAmountPending),
                statusColor = pendingColor,
                gifRes = R.drawable.gif_pending
            )

            STATUS_PENDING_VBV, STATUS_AUTHORIZING, STATUS_STARTED -> StatusDisplay(
                label = getString(R.string.msgPaymentProcessing),
                description = getString(R.string.msgPaymentPendingDescription),
                amountLabel = getString(R.string.labelAmountPending),
                statusColor = pendingColor,
                gifRes = R.drawable.gif_pending
            )

            STATUS_JUSPAY_DECLINED, STATUS_AUTHENTICATION_FAILED, STATUS_AUTHORIZATION_FAILED -> StatusDisplay(
                label = getString(R.string.msgPaymentFailed),
                description = getString(R.string.msgPaymentFailedDescription),
                amountLabel = getString(R.string.labelAmountFailed),
                statusColor = failedColor,
                gifRes = R.drawable.gif_rejected
            )

            STATUS_AUTO_REFUNDED -> StatusDisplay(
                label = getString(R.string.msgAmountRefunded),
                description = getString(R.string.msgPaymentRefundedDescription),
                amountLabel = getString(R.string.labelAmountRefunded),
                statusColor = failedColor,
                gifRes = R.drawable.gif_rejected
            )

            else -> StatusDisplay(
                label = getString(R.string.msgPaymentPending),
                description = getString(R.string.msgPaymentPendingDescription),
                amountLabel = getString(R.string.labelAmountPending),
                statusColor = pendingColor,
                gifRes = R.drawable.gif_pending
            )
        }
    }

    private fun loadGif(@DrawableRes gifRes: Int) {
        Glide.with(mActivity)
            .asGif()
            .load(gifRes)
            .listener(object : RequestListener<GifDrawable> {
                override fun onLoadFailed(
                    p0: GlideException?,
                    p1: Any?,
                    p2: Target<GifDrawable?>,
                    p3: Boolean
                ): Boolean = false

                override fun onResourceReady(
                    resource: GifDrawable,
                    model: Any,
                    target: Target<GifDrawable>?,
                    dataSource: DataSource,
                    isFirstResource: Boolean
                ): Boolean {
                    resource.setLoopCount(1)
                    return false
                }
            })
            .into(binding.ivGif)
    }

    private fun copyOrderId() {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("order_id", orderId))
        ToastUtil.showSuccess(mActivity, getString(R.string.msgOrderIdCopied))
    }

    private fun goToWallet() {
        if (isNavigating) return
        isNavigating = true
        autoFinishHandler.removeCallbacksAndMessages(null)
        startActivity(
            Intent(this, LoadWalletActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra(Constant.EXTRA_FROM_PAYMENT, true)
            }
        )
        finish()
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
                binding.ivCopyOrderId -> {
                    if (Utility.stopClick()) return@OnClickListener
                    copyOrderId()
                }
            }
        }
    }
}
