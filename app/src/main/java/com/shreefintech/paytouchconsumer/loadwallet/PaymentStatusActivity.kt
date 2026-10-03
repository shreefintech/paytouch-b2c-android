package com.shreefintech.paytouchconsumer.loadwallet

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.RawRes
import androidx.core.content.ContextCompat
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
import com.shreefintech.paytouchconsumer.utill.AnimationHelper
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
    private var mediaPlayer: MediaPlayer? = null
    private var isSoundPlayed = false

    companion object {
        private const val EXTRA_ITEM = "extra_item"

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
        releaseSound()
    }

    private fun populateStatus(status: String) {
        val display = resolveStatus(status)
        val statusColor = ContextCompat.getColor(mActivity, display.statusColorRes)
        binding.tvStatusLabel.text = display.label
        binding.tvStatusLabel.setTextColor(statusColor)
        binding.tvStatusDescription.text = display.description
        binding.tvAmountLabel.text = display.amountLabel
        binding.tvOrderId.text = orderId
        binding.tvAmount.text = Utility.formatAmount(amount)
        binding.tvAmount.setTextColor(statusColor)
        loadGif(display.gifRes, display.soundRes)
    }

    private data class StatusDisplay(
        val label: String,
        val description: String,
        val amountLabel: String,
        @ColorRes val statusColorRes: Int,
        @DrawableRes val gifRes: Int,
        @RawRes val soundRes: Int
    )

    private fun resolveStatus(status: String): StatusDisplay {
        return when (status.uppercase()) {
            Constant.HDFC_STATUS_CHARGED, Constant.HDFC_STATUS_AUTHORIZED -> StatusDisplay(
                label = getString(R.string.msgPaymentSuccessful),
                description = getString(R.string.msgPaymentSuccessDescription),
                amountLabel = getString(R.string.labelAmountPaid),
                statusColorRes = R.color.colorStatusSuccess,
                gifRes = R.drawable.gif_success,
                soundRes = R.raw.success_sound
            )

            Constant.HDFC_STATUS_NEW -> StatusDisplay(
                label = getString(R.string.msgPaymentInitiated),
                description = getString(R.string.msgPaymentInitiatedDescription),
                amountLabel = getString(R.string.labelAmountPending),
                statusColorRes = R.color.colorStatusPending,
                gifRes = R.drawable.gif_pending,
                soundRes = R.raw.pending_sound
            )

            Constant.HDFC_STATUS_PENDING_VBV, Constant.HDFC_STATUS_AUTHORIZING, Constant.HDFC_STATUS_STARTED -> StatusDisplay(
                label = getString(R.string.msgPaymentProcessing),
                description = getString(R.string.msgPaymentPendingDescription),
                amountLabel = getString(R.string.labelAmountPending),
                statusColorRes = R.color.colorStatusPending,
                gifRes = R.drawable.gif_pending,
                soundRes = R.raw.pending_sound
            )

            Constant.HDFC_STATUS_JUSPAY_DECLINED, Constant.HDFC_STATUS_AUTHENTICATION_FAILED, Constant.HDFC_STATUS_AUTHORIZATION_FAILED -> StatusDisplay(
                label = getString(R.string.msgPaymentFailed),
                description = getString(R.string.msgPaymentFailedDescription),
                amountLabel = getString(R.string.labelAmountFailed),
                statusColorRes = R.color.colorStatusFailed,
                gifRes = R.drawable.gif_rejected,
                soundRes = R.raw.failed_sound
            )

            Constant.HDFC_STATUS_AUTO_REFUNDED -> StatusDisplay(
                label = getString(R.string.msgAmountRefunded),
                description = getString(R.string.msgPaymentRefundedDescription),
                amountLabel = getString(R.string.labelAmountRefunded),
                statusColorRes = R.color.colorStatusFailed,
                gifRes = R.drawable.gif_rejected,
                soundRes = R.raw.failed_sound
            )

            else -> StatusDisplay(
                label = getString(R.string.msgPaymentPending),
                description = getString(R.string.msgPaymentPendingDescription),
                amountLabel = getString(R.string.labelAmountPending),
                statusColorRes = R.color.colorStatusPending,
                gifRes = R.drawable.gif_pending,
                soundRes = R.raw.pending_sound
            )
        }
    }

    private fun loadGif(@DrawableRes gifRes: Int, @RawRes soundRes: Int) {
        Glide.with(mActivity)
            .asGif()
            .load(gifRes)
            .placeholder(R.drawable.ic_file_not_found)
            .error(R.drawable.ic_file_not_found)
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
                    // Glide starts the GIF right after this returns — start the sound with it.
                    playSound(soundRes)
                    return false
                }
            })
            .into(binding.ivGif)
    }

    private fun playSound(@RawRes soundRes: Int) {
        // Glide can deliver the resource again (e.g. memory-cache reload) — the sound plays once per screen.
        if (isSoundPlayed || isFinishing || isDestroyed) return
        isSoundPlayed = true
        try {
            mediaPlayer = MediaPlayer.create(mActivity, soundRes)?.apply {
                setOnCompletionListener { releaseSound() }
                start()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            releaseSound()
        }
    }

    private fun releaseSound() {
        mediaPlayer?.release()
        mediaPlayer = null
    }

    private fun copyOrderId() {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("order_id", orderId))
        ToastUtil.showSuccess(mActivity, getString(R.string.msgOrderIdCopied))
        binding.ivCopyOrderId.setImageResource(R.drawable.ic_toast_tick)
        autoFinishHandler.postDelayed({ binding.ivCopyOrderId.setImageResource(R.drawable.ic_copy) }, 1500L)
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
