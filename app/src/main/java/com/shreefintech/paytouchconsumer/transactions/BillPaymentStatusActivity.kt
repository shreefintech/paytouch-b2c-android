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
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import com.google.gson.Gson
import com.shreefintech.paytouchconsumer.BaseActivity
import com.shreefintech.paytouchconsumer.R
import com.shreefintech.paytouchconsumer.databinding.ActivityBillPaymentStatusBinding
import com.shreefintech.paytouchconsumer.dth.transactions.DthSmsReceiptActivity
import com.shreefintech.paytouchconsumer.electricity.transactions.SmsReceiptActivity
import com.shreefintech.paytouchconsumer.fastag.transactions.FastagSmsReceiptActivity
import com.shreefintech.paytouchconsumer.gas.transactions.GasSmsReceiptActivity
import com.shreefintech.paytouchconsumer.loan.transactions.LoanSmsReceiptActivity
import com.shreefintech.paytouchconsumer.municipaltax.transactions.MunicipalTaxSmsReceiptActivity
import com.shreefintech.paytouchconsumer.postpaid.transactions.PostpaidSmsReceiptActivity
import com.shreefintech.paytouchconsumer.prepaid.transactions.PrepaidSmsReceiptActivity
import com.shreefintech.paytouchconsumer.transactions.model.BillPaymentStatusItem
import com.shreefintech.paytouchconsumer.transactions.viewmodel.TransactionHistoryDetailViewModel
import com.shreefintech.paytouchconsumer.utill.StatusSoundPlayer
import com.shreefintech.paytouchconsumer.utill.ToastUtil
import com.shreefintech.paytouchconsumer.utill.Utility
import com.shreefintech.paytouchconsumer.utill.Utility.gone

class BillPaymentStatusActivity : BaseActivity() {

    private lateinit var binding: ActivityBillPaymentStatusBinding

    private val passItem: BillPaymentStatusItem? by lazy {
        intent.getStringExtra(EXTRA_ITEM)
            ?.let { Gson().fromJson(it, BillPaymentStatusItem::class.java) }
    }
    private val transactionId: String by lazy { passItem?.transactionId ?: "--" }
    private val amount: String by lazy { passItem?.amount ?: "" }
    private val statusStr: String by lazy { passItem?.status ?: "" }
    private val category: String by lazy { passItem?.category ?: "" }

    private val detailViewModel: TransactionHistoryDetailViewModel by viewModels()

    private val autoFinishHandler = Handler(Looper.getMainLooper())
    private var isNavigating = false
    private val soundPlayer = StatusSoundPlayer()
    private var isSoundPlayed = false

    companion object {
        const val CATEGORY_ELECTRICITY  = "ELECTRICITY"
        const val CATEGORY_DTH          = "DTH"
        const val CATEGORY_GAS          = "GAS"
        const val CATEGORY_LOAN         = "LOAN"
        const val CATEGORY_MUNICIPAL_TAX = "MUNICIPAL_TAX"
        const val CATEGORY_PREPAID      = "PREPAID"
        const val CATEGORY_FASTAG       = "FASTAG"
        const val CATEGORY_POSTPAID     = "POSTPAID"

        private const val EXTRA_ITEM    = "extra_item"
        private const val STATUS_SUCCESS = "SUCCESS"
        private const val STATUS_FAILED  = "FAILED"

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
        populateStatus(statusStr)
        fetchConfirmedStatus()
        onBack()
    }

    override fun onResume() {
        super.onResume()
        if (!isNavigating) {
            autoFinishHandler.removeCallbacksAndMessages(null)
            autoFinishHandler.postDelayed({
                if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) openReceipt()
            }, 5000L)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        autoFinishHandler.removeCallbacksAndMessages(null)
        soundPlayer.release()
    }

    private fun populateStatus(status: String) {
        val display = resolveStatus(status)
        val statusColor = ContextCompat.getColor(mActivity, display.statusColorRes)
        binding.tvStatusLabel.text = display.label
        binding.tvStatusLabel.setTextColor(statusColor)
        binding.tvStatusDescription.text = display.description
        binding.tvAmountLabel.text = display.amountLabel
        binding.tvTransactionId.text = transactionId
        binding.ivCopyId.visibility = if (transactionId == "--") View.GONE else View.VISIBLE
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
            STATUS_SUCCESS -> StatusDisplay(
                label = getString(R.string.msgPaymentSuccessful),
                description = getString(R.string.msgPaymentSuccessDescription),
                amountLabel = getString(R.string.labelAmountPaid),
                statusColorRes = R.color.colorStatusSuccess,
                gifRes = R.drawable.gif_success,
                soundRes = R.raw.success_sound
            )
            STATUS_FAILED -> StatusDisplay(
                label = getString(R.string.msgPaymentFailed),
                description = getString(R.string.msgPaymentFailedDescription),
                amountLabel = getString(R.string.labelAmountFailed),
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
        clipboard.setPrimaryClip(ClipData.newPlainText("transaction_id", transactionId))
        ToastUtil.showSuccess(mActivity, getString(R.string.msgOrderIdCopied))
        binding.ivCopyId.setImageResource(R.drawable.ic_toast_tick)
        autoFinishHandler.postDelayed({ binding.ivCopyId.setImageResource(R.drawable.ic_copy) }, 1500L)
    }

    // Best-effort: the /transaction-history/detail endpoint serves both wallet and bill payment
    // records by transaction ID. onError is silent — optimistic status from the payment response
    // is already shown; this only upgrades it if the backend confirms a different status.
    private fun fetchConfirmedStatus() {
        if (transactionId == "--") return
        detailViewModel.loadDetail(
            transactionId = transactionId,
            onLoading = {},
            onSuccess = { item ->
                val confirmedStatus = item.status ?: return@loadDetail
                if (confirmedStatus.uppercase() != statusStr.uppercase()) {
                    isSoundPlayed = false
                    populateStatus(confirmedStatus)
                }
            },
            onError = {}
        )
    }

    private fun openReceipt() {
        if (isNavigating) return
        isNavigating = true
        autoFinishHandler.removeCallbacksAndMessages(null)
        val hasId = transactionId != "--"
        when (category) {
            CATEGORY_ELECTRICITY   -> if (hasId) SmsReceiptActivity.start(mActivity, transactionId, fromPayment = true) else SmsReceiptActivity.start(mActivity, fromPayment = true)
            CATEGORY_DTH           -> if (hasId) DthSmsReceiptActivity.start(mActivity, transactionId, fromPayment = true) else DthSmsReceiptActivity.start(mActivity, fromPayment = true)
            CATEGORY_GAS           -> if (hasId) GasSmsReceiptActivity.start(mActivity, transactionId, fromPayment = true) else GasSmsReceiptActivity.start(mActivity, fromPayment = true)
            CATEGORY_LOAN          -> if (hasId) LoanSmsReceiptActivity.start(mActivity, transactionId, fromPayment = true) else LoanSmsReceiptActivity.start(mActivity, fromPayment = true)
            CATEGORY_MUNICIPAL_TAX -> if (hasId) MunicipalTaxSmsReceiptActivity.start(mActivity, transactionId, fromPayment = true) else MunicipalTaxSmsReceiptActivity.start(mActivity, fromPayment = true)
            CATEGORY_PREPAID       -> if (hasId) PrepaidSmsReceiptActivity.start(mActivity, transactionId, fromPayment = true) else PrepaidSmsReceiptActivity.start(mActivity, fromPayment = true)
            CATEGORY_FASTAG        -> if (hasId) FastagSmsReceiptActivity.start(mActivity, transactionId, fromPayment = true) else FastagSmsReceiptActivity.start(mActivity, fromPayment = true)
            CATEGORY_POSTPAID      -> if (hasId) PostpaidSmsReceiptActivity.start(mActivity, transactionId, fromPayment = true) else PostpaidSmsReceiptActivity.start(mActivity, fromPayment = true)
            else -> Utility.logError(IllegalStateException("BillPaymentStatusActivity: unknown category $category"))
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
