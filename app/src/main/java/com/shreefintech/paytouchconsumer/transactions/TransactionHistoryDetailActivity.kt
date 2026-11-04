package com.shreefintech.paytouchconsumer.transactions

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.shreefintech.paytouchconsumer.BaseActivity
import com.shreefintech.paytouchconsumer.Constant
import com.shreefintech.paytouchconsumer.R
import com.shreefintech.paytouchconsumer.databinding.ActivityTransactionHistoryDetailBinding
import com.shreefintech.paytouchconsumer.databinding.ItemDetailRowBinding
import com.shreefintech.paytouchconsumer.glass.LiquidGlassEffect
import com.shreefintech.paytouchconsumer.loadwallet.model.WalletHistoryDetailPassItem
import com.shreefintech.paytouchconsumer.loadwallet.model.WalletHistoryDetailPassItem.Source
import com.shreefintech.paytouchconsumer.retrofit.model.billpay.BillPayTransactionItem
import com.shreefintech.paytouchconsumer.retrofit.model.transactions.TransactionHistoryDetailItem
import com.shreefintech.paytouchconsumer.retrofit.model.wallet.WalletTopupItem
import com.shreefintech.paytouchconsumer.retrofit.model.wallet.WalletWithdrawalItem
import com.shreefintech.paytouchconsumer.transactions.viewmodel.TransactionHistoryDetailViewModel
import com.shreefintech.paytouchconsumer.utill.AnimationHelper
import com.shreefintech.paytouchconsumer.utill.ToastUtil
import com.shreefintech.paytouchconsumer.utill.Utility

class TransactionHistoryDetailActivity : BaseActivity() {

    private lateinit var binding: ActivityTransactionHistoryDetailBinding
    private val viewModel: TransactionHistoryDetailViewModel by viewModels()

    private val transactionId: String by lazy {
        intent.getStringExtra(EXTRA_TRANSACTION_ID) ?: ""
    }

    // My Wallet history row — when set, the detail comes from the call its source names (or the row itself)
    private val walletItem: WalletHistoryDetailPassItem? by lazy {
        val json = intent.getStringExtra(EXTRA_ITEM) ?: return@lazy null
        try {
            Gson().fromJson(json, WalletHistoryDetailPassItem::class.java)
        } catch (e: JsonSyntaxException) {
            Utility.logError(e)
            null
        }
    }

    private enum class StatusTone { SUCCESS, FAILED, PENDING }

    companion object {
        private const val EXTRA_TRANSACTION_ID = "extra_transaction_id"
        private const val EXTRA_ITEM = "extra_item"

        fun start(context: Context, transactionId: String) {
            context.startActivity(
                Intent(context, TransactionHistoryDetailActivity::class.java).apply {
                    putExtra(EXTRA_TRANSACTION_ID, transactionId)
                }
            )
        }

        fun start(context: Context, item: WalletHistoryDetailPassItem) {
            context.startActivity(
                Intent(context, TransactionHistoryDetailActivity::class.java).apply {
                    putExtra(EXTRA_ITEM, Gson().toJson(item))
                }
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTransactionHistoryDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.clRoot) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        LiquidGlassEffect.attach(
            targetView   = binding.flCard,
            rootView     = binding.clRoot as ViewGroup,
            cornerRadius = resources.getDimensionPixelSize(R.dimen.glass_frem_radius),
            distortion   = 0f,
            blur         = resources.getDimensionPixelSize(R.dimen.glass_frem_blur),
            strokeColor  = ContextCompat.getColor(mActivity, R.color.glass_stroke_primary),
            strokeWidth  = 1,
            solidStroke  = true,
        )

        binding.onClickListener = onClickListener()
        onBack()

        retryCallback = { loadDetail() }
        loadDetail()
    }

    private fun loadDetail() {
        val item = walletItem
        if (item != null) {
            loadWalletDetail(item)
            return
        }
        if (transactionId.isEmpty()) { finish(); return }
        if (!Utility.isInternetAvailable(mActivity)) {
            showNoInternet()
            return
        }
        hideNoInternet()
        viewModel.loadDetail(
            transactionId = transactionId,
            onLoading     = { showLoading(true) },
            onSuccess     = { item ->
                showLoading(false)
                populateData(item)
            },
            onError       = { msg -> showError(msg) }
        )
    }

    private fun populateData(item: TransactionHistoryDetailItem) {
        binding.svContent.visibility = View.VISIBLE
        AnimationHelper.animateEntrance(binding.flCard, 0)

        binding.tvAmount.text        = Utility.formatAmount(item.amount)
        binding.tvTransactionId.text = item.referenceId ?: "--"
        binding.tvIdentifierLabel.text = item.identifierLabel ?: "--"
        binding.tvIdentifier.text    = item.identifier ?: "--"
        binding.tvDate.text          = Utility.formatDate(item.createdAt, "dd/MM/yyyy hh:mm a")

        val status = item.status ?: "--"
        val tone = when (status.lowercase()) {
            "success" -> StatusTone.SUCCESS
            "failed"  -> StatusTone.FAILED
            else      -> StatusTone.PENDING
        }
        applyStatusChip(status.replaceFirstChar { it.uppercaseChar() }, tone)

        val isHdfc = item.type == Constant.TRANSACTION_TYPE_HDFC
        if (isHdfc) {
            item.paymentMethodDisplayName?.let { method ->
                binding.llPaymentMethod.visibility = View.VISIBLE
                binding.tvPaymentMethod.text       = method
            }
            item.rrn?.let { rrn ->
                binding.llBankRefNo.visibility = View.VISIBLE
                binding.tvBankRefNo.text       = rrn
            }
        } else {
            item.platformFee?.let { fee ->
                binding.llPlatformFee.visibility = View.VISIBLE
                binding.tvPlatformFee.text       = Utility.formatAmount(fee)
            }
            item.totalPayable?.let { total ->
                binding.llTotalPayable.visibility = View.VISIBLE
                binding.tvTotalPayable.text       = Utility.formatAmount(total)
            }
        }
    }

    // ── Wallet history detail ─────────────────────────────────────────────────

    private fun loadWalletDetail(item: WalletHistoryDetailPassItem) {
        val reference = item.clientReference?.takeIf { it.isNotBlank() }
        if (item.source == Source.ROW || reference == null) {
            populateRow(item)
            return
        }
        if (!Utility.isInternetAvailable(mActivity)) {
            showNoInternet()
            return
        }
        hideNoInternet()
        val onLoading = { showLoading(true) }
        val onNotFound = { showNotFound() }
        val onError = { msg: String -> showError(msg) }
        when (item.source) {
            Source.BILLPAY -> viewModel.loadBillPayStatus(reference, onLoading, { data ->
                showLoading(false)
                populateBillPay(item, data)
            }, onNotFound, onError)

            Source.TOPUP -> viewModel.loadTopup(reference, onLoading, { data ->
                showLoading(false)
                populateTopup(item, data)
            }, onNotFound, onError)

            Source.WITHDRAWAL -> viewModel.loadWithdrawal(reference, onLoading, { data ->
                showLoading(false)
                populateWithdrawal(item, data)
            }, onNotFound, onError)

            Source.ROW -> populateRow(item)
        }
    }

    // No detail call: Transaction ID, Description, Balance After, Date; no status badge
    private fun populateRow(item: WalletHistoryDetailPassItem) {
        showWalletDetail(
            amount        = item.amount,
            status        = null,
            transactionId = item.txnReference,
            createdAt     = item.createdAt,
            rows          = listOf(
                R.string.labelDescription to item.description,
                R.string.labelBalanceAfter to item.balanceAfter?.let { Utility.formatAmount(it) }
            )
        )
    }

    private fun populateBillPay(item: WalletHistoryDetailPassItem, data: BillPayTransactionItem) {
        // Platform Fee = charge + gst_amount
        val charge = data.charge
        val gst = data.gstAmount
        val platformFee = if (charge == null && gst == null) null else ((charge ?: 0.0) + (gst ?: 0.0)).toString()
        showWalletDetail(
            amount        = data.amount?.toString() ?: item.amount,
            status        = data.status,
            transactionId = data.clientReference ?: item.clientReference,
            createdAt     = data.createdAt ?: item.createdAt,
            rows          = listOf(
                R.string.labelBiller to data.billerName,
                R.string.labelOperatorReference to data.providerRef,
                R.string.labelPlatformFee to platformFee?.let { Utility.formatAmount(it) },
                R.string.labelTotalAmount to data.totalAmount?.let { Utility.formatAmount(it.toString()) },
                R.string.labelFailureReason to data.failureReason
            )
        )
    }

    private fun populateTopup(item: WalletHistoryDetailPassItem, data: WalletTopupItem) {
        showWalletDetail(
            amount        = data.amount ?: item.amount,
            status        = data.status,
            // Tone follows the top-up result rule: open → pending, final success set → success, on_hold → pending
            toneStatus    = data.resultStatus(),
            transactionId = data.clientReference ?: item.clientReference,
            createdAt     = data.createdAt ?: item.createdAt,
            rows          = listOf(
                R.string.labelPaymentMethod to data.paymentMethodDisplayName,
                R.string.labelFee to data.fee?.let { Utility.formatAmount(it) },
                R.string.labelCreditAmount to data.creditAmount?.let { Utility.formatAmount(it) },
                R.string.labelFailureReason to data.failureReason
            )
        )
    }

    private fun populateWithdrawal(item: WalletHistoryDetailPassItem, data: WalletWithdrawalItem) {
        val bankName = data.bank?.bankName
        val last4 = data.bank?.accountMasked?.takeLast(4).orEmpty()
        val bank = when {
            bankName.isNullOrBlank() -> last4.ifEmpty { null }
            last4.isEmpty() -> bankName
            else -> getString(R.string.labelBankAccountMasked, bankName, last4)
        }
        showWalletDetail(
            amount        = data.amount ?: item.amount,
            status        = data.status,
            transactionId = data.clientReference ?: item.clientReference,
            createdAt     = data.createdAt ?: item.createdAt,
            rows          = listOf(
                R.string.labelBank to bank,
                R.string.labelUtr to data.utr,
                R.string.labelFee to data.fee?.let { Utility.formatAmount(it) },
                R.string.labelTotalDebited to data.totalDebited?.let { Utility.formatAmount(it) },
                R.string.labelFailureReason to data.failureReason
            )
        )
    }

    // status null = no badge; toneStatus overrides which status picks the badge colour
    private fun showWalletDetail(
        amount: String?,
        status: String?,
        transactionId: String?,
        createdAt: String?,
        rows: List<Pair<Int, String?>>,
        toneStatus: String? = status
    ) {
        binding.svContent.visibility = View.VISIBLE
        AnimationHelper.animateEntrance(binding.flCard, 0)

        binding.tvAmount.text        = Utility.formatAmount(amount)
        binding.tvTransactionId.text = transactionId?.takeIf { it.isNotBlank() } ?: "--"
        binding.tvDate.text          = Utility.formatDate(createdAt, "dd/MM/yyyy hh:mm a")
        binding.llIdentifier.visibility = View.GONE

        if (status.isNullOrBlank()) {
            binding.cvStatus.visibility = View.GONE
        } else {
            val label = status.replace('_', ' ').replaceFirstChar { it.uppercaseChar() }
            applyStatusChip(label, toneOf(toneStatus))
        }

        binding.llWalletRows.removeAllViews()
        rows.forEach { (labelRes, value) -> addWalletRow(labelRes, value) }
        binding.llWalletRows.visibility = if (binding.llWalletRows.childCount > 0) View.VISIBLE else View.GONE
    }

    private fun addWalletRow(@StringRes labelRes: Int, value: String?) {
        if (value.isNullOrBlank()) return
        val row = ItemDetailRowBinding.inflate(layoutInflater, binding.llWalletRows, true)
        row.tvLabel.setText(labelRes)
        row.tvValue.text = value
    }

    private fun toneOf(status: String?): StatusTone {
        val raw = status?.uppercase().orEmpty()
        return when (raw) {
            in Constant.TOPUP_SUCCESS_STATUSES   -> StatusTone.SUCCESS
            in Constant.WITHDRAW_FAILED_STATUSES -> StatusTone.FAILED
            else                                 -> StatusTone.PENDING
        }
    }

    private fun applyStatusChip(label: String, tone: StatusTone) {
        binding.cvStatus.visibility = View.VISIBLE
        binding.tvStatus.text = label
        val (bgColor, textColor) = when (tone) {
            StatusTone.SUCCESS -> Pair(R.color.toast_bg_success, R.color.toast_text_success)
            StatusTone.FAILED  -> Pair(R.color.toast_bg_delete, R.color.form_wizard_reject)
            StatusTone.PENDING -> Pair(R.color.toast_bg_warning, R.color.orange)
        }
        binding.cvStatus.setCardBackgroundColor(ContextCompat.getColor(mActivity, bgColor))
        binding.tvStatus.setTextColor(ContextCompat.getColor(mActivity, textColor))
    }

    // ── UI state ──────────────────────────────────────────────────────────────

    private fun showLoading(loading: Boolean) {
        binding.pbLoading.visibility = if (loading) View.VISIBLE else View.GONE
        if (loading) binding.svContent.visibility = View.GONE
    }

    private fun showError(msg: String) {
        showLoading(false)
        binding.tvEmpty.visibility = View.VISIBLE
        if (msg.isNotEmpty()) ToastUtil.showDelete(mActivity, msg)
    }

    // 404 from a wallet detail call — no detail record for that reference
    private fun showNotFound() {
        showLoading(false)
        binding.tvEmpty.setText(R.string.msgNoDataFound)
        binding.tvEmpty.visibility = View.VISIBLE
    }

    private fun onBack() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finish()
            }
        })
    }

    private fun onClickListener(): View.OnClickListener {
        return View.OnClickListener { view ->
            when (view) {
                binding.lytToolbar.ivBack -> {
                    if (Utility.stopClick()) return@OnClickListener
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        }
    }
}
