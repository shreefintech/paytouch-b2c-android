package com.shreefintech.paytouchconsumer.transactions

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.RawRes
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
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
import com.shreefintech.paytouchconsumer.billpay.transactions.SmsReceiptActivity
import com.shreefintech.paytouchconsumer.billpay.viewmodel.BillPayStatusViewModel
import com.shreefintech.paytouchconsumer.databinding.ActivityBillPaymentStatusBinding
import com.shreefintech.paytouchconsumer.enums.CategoryType
import com.shreefintech.paytouchconsumer.transactions.model.BillPaymentStatusItem
import com.shreefintech.paytouchconsumer.utill.StatusSoundPlayer
import com.shreefintech.paytouchconsumer.utill.ToastUtil
import com.shreefintech.paytouchconsumer.utill.Utility
import com.shreefintech.paytouchconsumer.utill.Utility.gone

/**
 * Result screen after billpay/pay. Polls billpay/status/{client_reference} every 2.5 s until is_final
 * (capped — then stays on "Payment Processing"), and opens the receipt 5 s after a final status.
 */
class BillPaymentStatusActivity : BaseActivity() {

    private lateinit var binding: ActivityBillPaymentStatusBinding

    private val passItem: BillPaymentStatusItem? by lazy {
        intent.getStringExtra(EXTRA_ITEM)
            ?.let { Gson().fromJson(it, BillPaymentStatusItem::class.java) }
    }
    private val clientReference: String by lazy { passItem?.clientReference ?: "--" }
    private val category: CategoryType by lazy { CategoryType.from(passItem?.category) }

    private val statusViewModel: BillPayStatusViewModel by viewModels()

    private val handler = Handler(Looper.getMainLooper())
    private val pollRunnable = Runnable { pollStatus() }
    private var pollAttempts = 0
    private var isFinal = false
    private var currentStatus = ""
    private var amount = ""
    private var isNavigating = false
    private val soundPlayer = StatusSoundPlayer()
    private var isSoundPlayed = false

    companion object {
        private const val EXTRA_ITEM = "extra_item"
        private const val AUTO_RECEIPT_DELAY_MS = 5000L

        fun start(context: Context, item: BillPaymentStatusItem) {
            context.startActivity(
                Intent(context, BillPaymentStatusActivity::class.java).apply {
                    putExtra(EXTRA_ITEM, Gson().toJson(item))
                }
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBillPaymentStatusBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.clRoot) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.onClickListener = onClickListener()
        binding.lytToolbar.ivBack.gone()
        amount = passItem?.amount ?: ""
        isFinal = passItem?.isFinal == true
        populateStatus(passItem?.status ?: "", null)
        onBack()
    }

    override fun onResume() {
        super.onResume()
        if (isNavigating) return
        handler.removeCallbacksAndMessages(null)
        // Final → receipt after a short pause; open → keep polling while in front
        if (isFinal || clientReference == "--") scheduleReceipt() else handler.postDelayed(pollRunnable, Constant.BILLPAY_POLL_INTERVAL_MS)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacksAndMessages(null)
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
        soundPlayer.release()
    }

    // ── Polling ───────────────────────────────────────────────────────────────

    private fun pollStatus() {
        if (isFinal || isNavigating) return
        // ponytail: fixed cap (~60 s), then "Payment Processing" stays and the receipt/Status tab show the final state
        if (pollAttempts >= Constant.BILLPAY_POLL_MAX_ATTEMPTS) return
        pollAttempts++
        statusViewModel.loadStatus(
            clientReference = clientReference,
            onLoading = {},
            onSuccess = { item ->
                if (isFinishing || isDestroyed) return@loadStatus
                item.amount?.let { amount = it.toString() }
                val status = item.status ?: currentStatus
                isFinal = item.isFinal == true
                if (!status.equals(currentStatus, ignoreCase = true) || isFinal) populateStatus(status, item.failureReason)
                if (!lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) return@loadStatus
                if (isFinal) scheduleReceipt() else handler.postDelayed(pollRunnable, Constant.BILLPAY_POLL_INTERVAL_MS)
            },
            onError = {
                // Silent — a dropped poll just retries until the cap
                if (isFinishing || isDestroyed) return@loadStatus
                if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                    handler.postDelayed(pollRunnable, Constant.BILLPAY_POLL_INTERVAL_MS)
                }
            }
        )
    }

    private fun scheduleReceipt() {
        handler.removeCallbacksAndMessages(null)
        handler.postDelayed({
            if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) openReceipt()
        }, AUTO_RECEIPT_DELAY_MS)
    }

    // ── UI ────────────────────────────────────────────────────────────────────

    private fun populateStatus(status: String, failureReason: String?) {
        val changed = !status.equals(currentStatus, ignoreCase = true)
        currentStatus = status
        val display = resolveStatus(status)
        val statusColor = ContextCompat.getColor(mActivity, display.statusColorRes)
        binding.tvStatusLabel.text = display.label
        binding.tvStatusLabel.setTextColor(statusColor)
        binding.tvStatusDescription.text = failureReason?.takeIf { display.isFailed && it.isNotBlank() } ?: display.description
        binding.tvAmountLabel.text = display.amountLabel
        binding.tvTransactionId.text = clientReference
        binding.ivCopyId.visibility = if (clientReference == "--") View.GONE else View.VISIBLE
        binding.tvAmount.text = Utility.formatAmount(amount)
        binding.tvAmount.setTextColor(statusColor)
        if (changed) {
            isSoundPlayed = false
            loadGif(display.gifRes, display.soundRes)
        }
    }

    private data class StatusDisplay(
        val label: String,
        val description: String,
        val amountLabel: String,
        @ColorRes val statusColorRes: Int,
        @DrawableRes val gifRes: Int,
        @RawRes val soundRes: Int,
        val isFailed: Boolean = false
    )

    // success → Successful; failed / expired → Failed; pending / processing / unconfirmed → Processing
    private fun resolveStatus(status: String): StatusDisplay {
        return when (status.lowercase()) {
            Constant.BILLPAY_STATUS_SUCCESS -> StatusDisplay(
                label = getString(R.string.msgPaymentSuccessful),
                description = getString(R.string.msgPaymentSuccessDescription),
                amountLabel = getString(R.string.labelAmountPaid),
                statusColorRes = R.color.colorStatusSuccess,
                gifRes = R.drawable.gif_success,
                soundRes = R.raw.bbps_success
            )
            Constant.BILLPAY_STATUS_FAILED, Constant.BILLPAY_STATUS_EXPIRED -> StatusDisplay(
                label = getString(R.string.msgPaymentFailed),
                description = getString(R.string.msgPaymentFailedDescription),
                amountLabel = getString(R.string.labelAmountFailed),
                statusColorRes = R.color.colorStatusFailed,
                gifRes = R.drawable.gif_rejected,
                soundRes = R.raw.bbps_failure,
                isFailed = true
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
        Glide.with(this@BillPaymentStatusActivity)
            .asGif()
            .load(gifRes)
            .placeholder(R.drawable.ic_file_not_found)
            .error(R.drawable.ic_file_not_found)
            .listener(object : RequestListener<GifDrawable> {
                override fun onLoadFailed(
                    p0: GlideException?, p1: Any?, p2: Target<GifDrawable?>, p3: Boolean
                ): Boolean = false

                override fun onResourceReady(
                    resource: GifDrawable, model: Any,
                    target: Target<GifDrawable>?, dataSource: DataSource, isFirstResource: Boolean
                ): Boolean {
                    resource.setLoopCount(1)
                    playSound(soundRes)
                    return false
                }
            })
            .into(binding.ivGif)
    }

    private fun playSound(@RawRes soundRes: Int) {
        if (isSoundPlayed || isFinishing || isDestroyed) return
        isSoundPlayed = true
        soundPlayer.play(mActivity, soundRes)
    }

    private fun copyTransactionId() {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("client_reference", clientReference))
        ToastUtil.showSuccess(mActivity, getString(R.string.msgCopiedToClipboard))
        binding.ivCopyId.setImageResource(R.drawable.ic_toast_tick)
        handler.postDelayed({ binding.ivCopyId.setImageResource(R.drawable.ic_copy) }, 1500L)
    }

    private fun openReceipt() {
        if (isNavigating) return
        isNavigating = true
        handler.removeCallbacksAndMessages(null)
        if (clientReference != "--") {
            SmsReceiptActivity.start(mActivity, category, clientReference, fromPayment = true)
        } else {
            SmsReceiptActivity.start(mActivity, category, fromPayment = true)
        }
        finish()
    }

    private fun onBack() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                openReceipt()
            }
        })
    }

    private fun onClickListener(): View.OnClickListener {
        return View.OnClickListener { view ->
            when (view) {
                binding.ivCopyId -> {
                    if (Utility.stopClick()) return@OnClickListener
                    copyTransactionId()
                }
            }
        }
    }
}
