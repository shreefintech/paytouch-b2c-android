package com.shreefintech.paytouchconsumer.loadwallet

import android.app.Dialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.res.ColorStateList
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.widget.TextViewCompat
import androidx.databinding.ObservableBoolean
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.shreefintech.paytouchconsumer.BaseActivity
import com.shreefintech.paytouchconsumer.databinding.DialogConfirmPaymentBinding
import com.shreefintech.paytouchconsumer.databinding.DialogConfirmWithdrawBinding
import com.shreefintech.paytouchconsumer.databinding.DialogWithdrawSuccessBinding
import com.shreefintech.paytouchconsumer.Constant
import com.shreefintech.paytouchconsumer.R
import com.shreefintech.paytouchconsumer.adapter.WalletTransactionAdp
import com.shreefintech.paytouchconsumer.databinding.ActivityLoadWalletBinding
import com.shreefintech.paytouchconsumer.databinding.SheetMakePaymentBinding
import com.shreefintech.paytouchconsumer.databinding.SheetWithdrawBinding
import com.shreefintech.paytouchconsumer.glass.LiquidGlassEffect
import com.shreefintech.paytouchconsumer.loadwallet.model.PaymentStatusItem
import com.shreefintech.paytouchconsumer.loadwallet.model.WalletTransactionItem
import com.shreefintech.paytouchconsumer.loadwallet.viewmodel.LoadWalletViewModel
import com.shreefintech.paytouchconsumer.retrofit.model.WalletDataItem
import com.shreefintech.paytouchconsumer.retrofit.model.wallet.WithdrawDataItem
import com.shreefintech.paytouchconsumer.transactions.TransactionHistoryDetailActivity
import com.shreefintech.paytouchconsumer.utill.AnimationHelper
import com.shreefintech.paytouchconsumer.utill.SharedPreferenceHelper
import com.shreefintech.paytouchconsumer.utill.ToastUtil
import com.shreefintech.paytouchconsumer.utill.Utility
import com.shreefintech.paytouchconsumer.utill.Utility.gone
import com.shreefintech.paytouchconsumer.utill.Utility.visible

class LoadWalletActivity : BaseActivity() {

    private lateinit var binding: ActivityLoadWalletBinding
    private val viewModel: LoadWalletViewModel by viewModels()

    private var currentTab = TAB_TOTAL_BALANCE
    private val transactionList = ArrayList<WalletTransactionItem>()
    private lateinit var transactionAdp: WalletTransactionAdp

    private lateinit var sheetBinding: SheetMakePaymentBinding
    private lateinit var sheetBehavior: BottomSheetBehavior<View>

    private lateinit var withdrawSheetBinding: SheetWithdrawBinding
    private lateinit var withdrawSheetBehavior: BottomSheetBehavior<View>
    private var selectedPaymentMode = MODE_IMPS

    private var currentWalletBalance: String? = null

    private var confirmDialog: Dialog? = null
    private var confirmDialogBinding: DialogConfirmPaymentBinding? = null
    private var pendingPayAmount: Double = 0.0
    private var pendingPayDescription: String = ""
    private val showProgressPay = ObservableBoolean(false)

    private var withdrawConfirmDialog: Dialog? = null
    private var withdrawConfirmDialogBinding: DialogConfirmWithdrawBinding? = null
    private var pendingWithdrawAmount: Double = 0.0
    private var pendingWithdrawMode: String = MODE_IMPS
    private var pendingWithdrawNarration: String = ""
    private val showProgressWithdraw = ObservableBoolean(false)

    private var withdrawSuccessDialog: Dialog? = null
    private var withdrawSuccessDialogBinding: DialogWithdrawSuccessBinding? = null
    private var withdrawRequestId: String? = null

    private var isWalletEntrancePlayed = false
    private var isWalletLoading = false
    private var hasWalletData = false

    companion object {
        private const val TAB_TOTAL_BALANCE = 0
        private const val MODE_IMPS = "IMPS"
        private const val MODE_NEFT = "NEFT"
        private const val MODE_RTGS = "RTGS"

        fun start(context: Context) {
            context.startActivity(Intent(context, LoadWalletActivity::class.java))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoadWalletBinding.inflate(layoutInflater)
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
            if (imeInsets.bottom > 0) Utility.scrollToFocused(mActivity)
            binding.incPaymentSheet.root.setPadding(
                0,
                0,
                0,
                maxOf(imeInsets.bottom, systemBars.bottom)
            )
            binding.incWithdrawSheet.root.setPadding(
                0,
                0,
                0,
                maxOf(imeInsets.bottom, systemBars.bottom)
            )
            insets
        }

        LiquidGlassEffect.attach(
            targetView = binding.flBalanceCard,
            rootView = binding.clRoot as ViewGroup,
            cornerRadius = resources.getDimensionPixelSize(R.dimen.glass_normal_radius),
            tintColor = ContextCompat.getColor(mActivity, R.color.wallet_card_bg),
            distortion = 0f,
            blur = resources.getDimensionPixelSize(R.dimen.glass_frem_blur),
        )

        LiquidGlassEffect.attach(
            targetView = binding.flVirtualAccCard,
            rootView = binding.clRoot as ViewGroup,
            cornerRadius = resources.getDimensionPixelSize(R.dimen.glass_normal_radius),
            tintColor = ContextCompat.getColor(mActivity, R.color.wallet_card_bg),
            distortion = 0f,
            solidStroke = true,
            strokeColor = ContextCompat.getColor(mActivity, R.color.primary),
            strokeWidth = 1,
            blur = resources.getDimensionPixelSize(R.dimen.glass_frem_blur),
        )

        binding.onClickListener = onClickListener()
        setupRecyclerView()
        setupPaymentSheet()
        setupWithdrawSheet()
        selectTab(TAB_TOTAL_BALANCE)
        onBack()
        retryCallback = { loadData() }
        loadData()
    }

    override fun onResume() {
        super.onResume()
        val orderId = SharedPreferenceHelper.getSharedPreferenceString(mActivity, Constant.KEY_PENDING_ORDER_ID, null)
            ?.takeIf { it.isNotEmpty() } ?: return
        val amount  = SharedPreferenceHelper.getSharedPreferenceString(mActivity, Constant.KEY_PENDING_AMOUNT, null) ?: ""
        HdfcPaymentHelper.clearPendingState(mActivity)
        hideNoInternet()
        showLoading()
        viewModel.checkOrderStatus(
            orderId   = orderId,
            onSuccess = { data ->
                hideLoading()
                PaymentStatusActivity.start(
                    mActivity,
                    PaymentStatusItem(
                        orderId = data.orderId ?: orderId,
                        amount  = data.amount ?: amount,
                        status  = data.status ?: Constant.HDFC_STATUS_NEW
                    )
                )
            },
            onError = {
                hideLoading()
                PaymentStatusActivity.start(
                    mActivity,
                    PaymentStatusItem(orderId = orderId, amount = amount, status = Constant.HDFC_STATUS_NEW)
                )
            }
        )
    }

    override fun onDestroy() {
        withdrawSuccessDialog?.dismiss()
        super.onDestroy()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.getBooleanExtra(Constant.EXTRA_FROM_PAYMENT, false)) {
            Utility.hideKeyboard(mActivity)
            if (isPaymentSheetVisible()) hidePaymentSheet()
            if (isWithdrawSheetVisible()) hideWithdrawSheet()
            sheetBinding.etAmount.clearFocus()
            sheetBinding.etDescription.clearFocus()
            sheetBinding.etAmount.setText("")
            sheetBinding.etDescription.setText("")
            fetchWalletData()
            fetchRecentHistory()
        }
    }

    private fun setupPaymentSheet() {
        sheetBinding = binding.incPaymentSheet
        sheetBinding.onClickListener = onClickListener()
        sheetBehavior = BottomSheetBehavior.from(sheetBinding.root)
        sheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN

        sheetBehavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                when (newState) {
                    BottomSheetBehavior.STATE_EXPANDED -> binding.viewBg.visible()
                    BottomSheetBehavior.STATE_SETTLING -> binding.viewBg.visible()
                    BottomSheetBehavior.STATE_HIDDEN -> binding.viewBg.gone()
                    else -> {}
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                binding.viewBg.alpha = slideOffset.coerceIn(0f, 1f)
            }
        })
    }

    private fun setupWithdrawSheet() {
        withdrawSheetBinding = binding.incWithdrawSheet
        withdrawSheetBinding.onClickListener = onClickListener()
        withdrawSheetBehavior = BottomSheetBehavior.from(withdrawSheetBinding.root)
        withdrawSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN

        withdrawSheetBehavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                when (newState) {
                    BottomSheetBehavior.STATE_EXPANDED -> binding.viewBg.visible()
                    BottomSheetBehavior.STATE_SETTLING -> binding.viewBg.visible()
                    BottomSheetBehavior.STATE_HIDDEN -> binding.viewBg.gone()
                    else -> {}
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                binding.viewBg.alpha = slideOffset.coerceIn(0f, 1f)
            }
        })
        selectPaymentMode(MODE_IMPS)
    }

    private fun showWithdrawSheet() {
        withdrawSheetBinding.etAmount.setText("")
        withdrawSheetBinding.etNarration.setText("")
        selectPaymentMode(MODE_IMPS)
        binding.viewBg.visible()
        withdrawSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
    }

    private fun hideWithdrawSheet() {
        Utility.hideKeyboard(mActivity)
        withdrawSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
    }

    private fun isWithdrawSheetVisible() = withdrawSheetBehavior.state != BottomSheetBehavior.STATE_HIDDEN

    private fun selectPaymentMode(mode: String) {
        selectedPaymentMode = mode
        val selected = ContextCompat.getDrawable(mActivity, R.drawable.bg_toggle_selected)
        val unselected = ContextCompat.getDrawable(mActivity, R.drawable.bg_toggle_unselected)
        val white = ContextCompat.getColor(mActivity, R.color.white)
        val primary = ContextCompat.getColor(mActivity, R.color.primary)

        withdrawSheetBinding.tvTabImps.background = if (mode == MODE_IMPS) selected else unselected
        withdrawSheetBinding.tvTabImps.setTextColor(if (mode == MODE_IMPS) white else primary)
        withdrawSheetBinding.tvTabNeft.background = if (mode == MODE_NEFT) selected else unselected
        withdrawSheetBinding.tvTabNeft.setTextColor(if (mode == MODE_NEFT) white else primary)
        withdrawSheetBinding.tvTabRtgs.background = if (mode == MODE_RTGS) selected else unselected
        withdrawSheetBinding.tvTabRtgs.setTextColor(if (mode == MODE_RTGS) white else primary)
    }

    private fun validateAndShowWithdrawConfirmDialog() {
        if (!Utility.isInternetAvailable(mActivity)) {
            ToastUtil.showDelete(mActivity, getString(R.string.msgNoInternet))
            return
        }
        val amountStr = withdrawSheetBinding.etAmount.text?.toString()?.trim() ?: ""
        if (amountStr.isEmpty()) {
            ToastUtil.showDelete(mActivity, getString(R.string.errEnterAmount))
            return
        }
        val amount = amountStr.toDoubleOrNull()
        if (amount == null) {
            ToastUtil.showDelete(mActivity, getString(R.string.errEnterAmount))
            return
        }
        if (amount < 100) {
            ToastUtil.showDelete(mActivity, getString(R.string.errMinWithdrawAmount))
            return
        }
        val balance = currentWalletBalance?.toDoubleOrNull() ?: 0.0
        if (amount > balance) {
            ToastUtil.showDelete(mActivity, getString(R.string.errInsufficientBalance))
            return
        }
        val narration = withdrawSheetBinding.etNarration.text?.toString()?.trim() ?: ""
        hideWithdrawSheet()
        showWithdrawConfirmDialog(amount, selectedPaymentMode, narration)
    }

    private fun showWithdrawConfirmDialog(amount: Double, mode: String, narration: String) {
        pendingWithdrawAmount = amount
        pendingWithdrawMode = mode
        pendingWithdrawNarration = narration
        showProgressWithdraw.set(false)

        val dialogBinding = DialogConfirmWithdrawBinding.inflate(layoutInflater)
        withdrawConfirmDialogBinding = dialogBinding
        dialogBinding.onClickListener = onClickListener()
        dialogBinding.showProgressWithdraw = showProgressWithdraw

        val dialog = Dialog(mActivity)
        withdrawConfirmDialog = dialog
        dialog.setContentView(dialogBinding.root)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.window?.setWindowAnimations(R.style.DialogScaleFadeAnimation)
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.88).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog.setCancelable(true)
        dialog.setOnDismissListener {
            withdrawConfirmDialog = null
            withdrawConfirmDialogBinding = null
        }

        dialogBinding.tvAmount.text = Utility.formatAmount(amount.toString())
        dialogBinding.tvPaymentMode.text = mode
        dialogBinding.tvAvailableBalance.text = Utility.formatAmount(currentWalletBalance)

        dialog.show()
    }

    private fun startWithdraw() {
        viewModel.withdrawWallet(
            amount = pendingWithdrawAmount,
            paymentMode = pendingWithdrawMode,
            narration = pendingWithdrawNarration,
            onLoading = { showProgressWithdraw.set(true) },
            onSuccess = { data ->
                showProgressWithdraw.set(false)
                withdrawConfirmDialog?.dismiss()
                showWithdrawSuccessDialog(data)
                fetchWalletData()
            },
            onError = { msg ->
                showProgressWithdraw.set(false)
                ToastUtil.showDelete(mActivity, msg, inWindow = false)
            }
        )
    }

    private fun showWithdrawSuccessDialog(data: WithdrawDataItem) {
        if (isFinishing || isDestroyed) return
        withdrawSuccessDialog?.dismiss()
        withdrawRequestId = data.requestId

        val dialogBinding = DialogWithdrawSuccessBinding.inflate(layoutInflater)
        withdrawSuccessDialogBinding = dialogBinding
        dialogBinding.onClickListener = onClickListener()

        val dialog = Dialog(mActivity)
        withdrawSuccessDialog = dialog
        dialog.setContentView(dialogBinding.root)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.window?.setWindowAnimations(R.style.DialogScaleFadeAnimation)
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.88).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog.setCancelable(true)
        dialog.setCanceledOnTouchOutside(false)
        dialog.setOnDismissListener {
            withdrawSuccessDialog = null
            withdrawSuccessDialogBinding = null
            withdrawRequestId = null
        }

        bindWithdrawDetails(dialogBinding, data)
        dialog.show()
    }

    private fun bindWithdrawDetails(dialogBinding: DialogWithdrawSuccessBinding, data: WithdrawDataItem) {
        val status = resolveWithdrawStatus(data.status)
        Glide.with(mActivity).asGif().load(status.gifRes).into(dialogBinding.ivStatusGif)

        dialogBinding.tvTitle.setText(status.titleRes)
        dialogBinding.tvSubtitle.text = data.statusMessage?.takeIf { it.isNotBlank() }
            ?: getString(status.subtitleRes)
        dialogBinding.tvAmount.text = Utility.formatAmount(data.amount)
        dialogBinding.tvStatus.text = data.statusLabel?.takeIf { it.isNotBlank() }
            ?: data.status?.takeIf { it.isNotBlank() }
            ?: getString(R.string.labelProcessing)
        dialogBinding.tvStatus.setBackgroundResource(status.chipBgRes)
        val chipTextColor = ContextCompat.getColor(mActivity, status.chipTextColorRes)
        dialogBinding.tvStatus.setTextColor(chipTextColor)
        TextViewCompat.setCompoundDrawableTintList(dialogBinding.tvStatus, ColorStateList.valueOf(chipTextColor))

        val accountLast4 = data.accountNumberMasked?.takeLast(4).orEmpty()
        dialogBinding.tvToBank.text = when {
            data.bankName.isNullOrBlank() -> accountLast4.ifEmpty { "--" }
            accountLast4.isEmpty() -> data.bankName
            else -> getString(R.string.labelBankAccountMasked, data.bankName, accountLast4)
        }

        val transferMode = data.transferMode?.takeIf { it.isNotBlank() } ?: pendingWithdrawMode
        dialogBinding.tvTransferMode.text = transferMode
        dialogBinding.tvInstantBadge.visibility =
            if (transferMode.equals(MODE_IMPS, ignoreCase = true)) View.VISIBLE else View.GONE

        dialogBinding.tvRequestId.text = data.requestId?.takeIf { it.isNotBlank() } ?: "--"
        dialogBinding.ivCopyRequestId.visibility =
            if (data.requestId.isNullOrBlank()) View.GONE else View.VISIBLE
        dialogBinding.tvDateTime.text = data.requestedAtDisplay?.takeIf { it.isNotBlank() } ?: "--"
    }

    private data class WithdrawStatusDisplay(
        @StringRes val titleRes: Int,
        @StringRes val subtitleRes: Int,
        @DrawableRes val gifRes: Int,
        @DrawableRes val chipBgRes: Int,
        @ColorRes val chipTextColorRes: Int
    )

    private fun resolveWithdrawStatus(status: String?): WithdrawStatusDisplay {
        return when (status?.uppercase()) {
            Constant.WITHDRAW_STATUS_SUCCESS, Constant.WITHDRAW_STATUS_COMPLETED -> WithdrawStatusDisplay(
                titleRes = R.string.titleWithdrawalSuccessful,
                subtitleRes = R.string.msgWithdrawalCredited,
                gifRes = R.drawable.gif_success,
                chipBgRes = R.drawable.bg_status_success,
                chipTextColorRes = R.color.toast_text_success
            )

            Constant.WITHDRAW_STATUS_FAILED, Constant.WITHDRAW_STATUS_REJECTED, Constant.WITHDRAW_STATUS_REVERSED -> WithdrawStatusDisplay(
                titleRes = R.string.titleWithdrawalFailed,
                subtitleRes = R.string.msgWithdrawalFailed,
                gifRes = R.drawable.gif_rejected,
                chipBgRes = R.drawable.bg_status_failed,
                chipTextColorRes = R.color.toast_text_delete
            )

            else -> WithdrawStatusDisplay(
                titleRes = R.string.titleWithdrawalRequestSent,
                subtitleRes = R.string.msgWithdrawalReachBank,
                gifRes = R.drawable.gif_pending,
                chipBgRes = R.drawable.bg_status_pending,
                chipTextColorRes = R.color.toast_text_warning
            )
        }
    }

    private fun copyWithdrawRequestId() {
        val requestId = withdrawRequestId?.takeIf { it.isNotBlank() } ?: return
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("request_id", requestId))
        ToastUtil.showSuccess(mActivity, getString(R.string.msgRequestIdCopied))
    }

    private fun loadData() {
        if (!Utility.isInternetAvailable(mActivity)) {
            showNoInternet()
            return
        }
        hideNoInternet()
        fetchWalletData()
        fetchRecentHistory()
    }

    private fun validateAndShowConfirmDialog() {
        if (!Utility.isInternetAvailable(mActivity)) {
            ToastUtil.showDelete(mActivity, getString(R.string.msgNoInternet))
            return
        }

        val amountStr = sheetBinding.etAmount.text?.toString()?.trim() ?: ""
        val description = sheetBinding.etDescription.text?.toString()?.trim() ?: ""
        if (amountStr.isEmpty()) {
            ToastUtil.showDelete(mActivity, getString(R.string.errEnterAmount))
            return
        }
        val amount = amountStr.toDoubleOrNull()
        if (amount == null || amount <= 0) {
            ToastUtil.showDelete(mActivity, getString(R.string.errEnterAmount))
            return
        }
        hidePaymentSheet()
        showConfirmDialog(amount, description)
    }

    private fun showConfirmDialog(amount: Double, description: String) {
        pendingPayAmount = amount
        pendingPayDescription = description
        showProgressPay.set(false)

        val dialogBinding = DialogConfirmPaymentBinding.inflate(layoutInflater)
        confirmDialogBinding = dialogBinding
        dialogBinding.onClickListener = onClickListener()
        dialogBinding.showProgressPay = showProgressPay

        val dialog = Dialog(mActivity)
        confirmDialog = dialog
        dialog.setContentView(dialogBinding.root)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.window?.setWindowAnimations(R.style.DialogScaleFadeAnimation)
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.88).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog.setCancelable(true)
        dialog.setOnDismissListener {
            confirmDialog = null
            confirmDialogBinding = null
        }

        dialogBinding.tvAmount.text = Utility.formatAmount(amount.toString())
        dialogBinding.tvAvailableBalance.text = Utility.formatAmount(currentWalletBalance)

        dialog.show()
    }

    private fun startHdfcFlow() {
        viewModel.createHdfcOrder(
            amount = pendingPayAmount,
            description = pendingPayDescription,
            onLoading = { showProgressPay.set(true) },
            onSuccess = { data ->
                showProgressPay.set(false)
                val status = data.status?.uppercase().orEmpty()
                if (HdfcPaymentHelper.isFailedStatus(status)) {
                    confirmDialog?.dismiss()
                    PaymentStatusActivity.start(
                        mActivity,
                        PaymentStatusItem(
                            orderId = data.orderId ?: "",
                            amount  = data.amount ?: "",
                            status  = data.status ?: Constant.HDFC_STATUS_NEW
                        )
                    )
                    return@createHdfcOrder
                }
                val payUrl = data.paymentLinks?.web.orEmpty()
                if (payUrl.isEmpty()) {
                    ToastUtil.showDelete(mActivity, getString(R.string.errGeneric), inWindow = false)
                    return@createHdfcOrder
                }
                Utility.hideKeyboard(mActivity)
                confirmDialog?.dismiss()
                HdfcPaymentHelper.launchPayment(
                    context   = mActivity,
                    orderId   = data.orderId ?: "",
                    amount    = data.amount ?: "",
                    payUrl    = payUrl,
                    returnUrl = data.returnUrl ?: ""
                )
            },
            onError = { msg ->
                showProgressPay.set(false)
                ToastUtil.showDelete(mActivity, msg, inWindow = false)
            }
        )
    }

    private fun showPaymentSheet() {
        sheetBinding.etAmount.setText("")
        sheetBinding.etDescription.setText("")
        binding.viewBg.visible()
        sheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
    }

    private fun hidePaymentSheet() {
        Utility.hideKeyboard(mActivity)
        sheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
    }

    private fun isPaymentSheetVisible() = sheetBehavior.state != BottomSheetBehavior.STATE_HIDDEN

    private fun fetchWalletData() {
        viewModel.fetchUserWalletData(
            onLoading = { showLoading() },
            onSuccess = { data ->
                hideLoading()
                populateWalletData(data)
            },
            onError = { msg ->
                hideLoading()
                ToastUtil.showDelete(mActivity, msg)
            }
        )
    }

    private fun fetchRecentHistory() {
        viewModel.fetchRecentHistory(
            onSuccess = { list ->
                transactionAdp.updateList(list)
                updateEmptyState()
            },
            onError = { msg ->
                ToastUtil.showDelete(mActivity, msg)
                updateEmptyState()
            }
        )
    }

    // Shimmer and balance content belong to the Total Balance tab only — selectTab() re-applies
    // this state when the user switches tabs mid-load.
    private fun showLoading() {
        isWalletLoading = true
        binding.llTotalBalanceContent.visibility = View.GONE
        if (currentTab != TAB_TOTAL_BALANCE) return
        binding.shimmerWallet.visibility = View.VISIBLE
        binding.shimmerWallet.startShimmer()
    }

    private fun hideLoading() {
        isWalletLoading = false
        binding.shimmerWallet.stopShimmer()
        binding.shimmerWallet.visibility = View.GONE
        binding.llTotalBalanceContent.visibility =
            if (currentTab == TAB_TOTAL_BALANCE) View.VISIBLE else View.GONE
    }

    // Deferred until the Total Balance tab is visible so the entrance never plays on a hidden view.
    private fun playWalletEntranceIfNeeded() {
        if (isWalletEntrancePlayed || !hasWalletData || currentTab != TAB_TOTAL_BALANCE) return
        isWalletEntrancePlayed = true
        AnimationHelper.animateChildren(binding.llTotalBalanceContent)
    }

    private fun populateWalletData(data: WalletDataItem) {
        hasWalletData = true
        playWalletEntranceIfNeeded()
        currentWalletBalance = data.walletBalance
        binding.tvWalletBalance.text = Utility.formatAmount(data.walletBalance)
        binding.tvVirtualAccountNumber.text = data.virtualAccountNumber ?: "--"
        binding.tvVaWalletBalance.text = Utility.formatAmount(data.wallet?.balance)
        binding.tvAccountHolder.text = data.name ?: "--"
        binding.tvIfscCode.text = data.ifsc ?: "--"
        binding.tvBankName.text = data.bankName ?: "--"
        Glide.with(mActivity as Context)
            .load(data.qrCodeUrl)
            .placeholder(R.drawable.ic_qr)
            .error(R.drawable.ic_qr)
            .into(binding.ivQrCode)
        val status = data.wallet?.status
        if (!status.isNullOrEmpty()) {
            binding.tvActiveStatus.text = status.replaceFirstChar { it.uppercaseChar() }
        }
    }

    private fun setupRecyclerView() {
        transactionAdp = WalletTransactionAdp(mActivity, transactionList)
        transactionAdp.onClickItem = { transactionId ->
            if (!Utility.stopClick()) TransactionHistoryDetailActivity.start(mActivity, transactionId)
        }
        binding.rvTransactions.apply {
            layoutManager = LinearLayoutManager(mActivity)
            adapter = transactionAdp
        }
        // Keep both views hidden until fetchRecentHistory() resolves — avoids flashing "No transactions" while loading
        binding.tvNoTransactions.visibility = View.GONE
        binding.rvTransactions.visibility = View.GONE
    }

    private fun updateEmptyState() {
        val isEmpty = transactionList.isEmpty()
        binding.tvNoTransactions.visibility = if (isEmpty) View.VISIBLE else View.GONE
        binding.rvTransactions.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }

    private fun selectTab(tab: Int) {
        currentTab = tab
        val isTotalBalance = tab == TAB_TOTAL_BALANCE
        if (isTotalBalance && isWalletLoading) {
            binding.shimmerWallet.visibility = View.VISIBLE
            binding.shimmerWallet.startShimmer()
        } else {
            binding.shimmerWallet.stopShimmer()
            binding.shimmerWallet.visibility = View.GONE
        }
        val showContent = isTotalBalance && !isWalletLoading
        binding.llTotalBalanceContent.visibility = if (showContent) View.VISIBLE else View.GONE
        if (showContent) playWalletEntranceIfNeeded()
        binding.tvComingSoon.visibility = if (isTotalBalance) View.GONE else View.VISIBLE
    }

    private fun onBack() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                when {
                    isPaymentSheetVisible() -> hidePaymentSheet()
                    isWithdrawSheetVisible() -> hideWithdrawSheet()
                    else -> {
                        isEnabled = false
                        onBackPressedDispatcher.onBackPressed()
                    }
                }
            }
        })
    }

    private fun onClickListener(): View.OnClickListener {
        return View.OnClickListener { view ->
            Utility.hideKeyboard(mActivity)
            when (view) {
                binding.lytToolbar.ivBack -> {
                    if (Utility.stopClick()) return@OnClickListener
                    onBackPressedDispatcher.onBackPressed()
                }

                binding.llMakePayment -> {
                    if (Utility.stopClick()) return@OnClickListener
                    showPaymentSheet()
                }

                binding.llWithdraw -> {
                    if (Utility.stopClick()) return@OnClickListener
                    showWithdrawSheet()
                }

                binding.llTransactionReport -> {
                    if (Utility.stopClick()) return@OnClickListener
                    WalletTransactionsActivity.start(mActivity)
                }

                sheetBinding.ivClose -> {
                    if (Utility.stopClick()) return@OnClickListener
                    hidePaymentSheet()
                }

                sheetBinding.btnProceedPayment -> {
                    if (Utility.stopClick()) return@OnClickListener
                    validateAndShowConfirmDialog()
                }

                withdrawSheetBinding.tvTabImps -> selectPaymentMode(MODE_IMPS)
                withdrawSheetBinding.tvTabNeft -> selectPaymentMode(MODE_NEFT)
                withdrawSheetBinding.tvTabRtgs -> selectPaymentMode(MODE_RTGS)

                withdrawSheetBinding.btnProceedWithdraw -> {
                    if (Utility.stopClick()) return@OnClickListener
                    validateAndShowWithdrawConfirmDialog()
                }

                confirmDialogBinding?.cardClose -> {
                    confirmDialog?.dismiss()
                }

                confirmDialogBinding?.cardPaySecurely -> {
                    if (Utility.stopClick()) return@OnClickListener
                    if (showProgressPay.get()) return@OnClickListener
                    startHdfcFlow()
                }

                withdrawConfirmDialogBinding?.cardClose -> {
                    withdrawConfirmDialog?.dismiss()
                }

                withdrawConfirmDialogBinding?.cardWithdrawSecurely -> {
                    if (Utility.stopClick()) return@OnClickListener
                    if (showProgressWithdraw.get()) return@OnClickListener
                    startWithdraw()
                }

                withdrawSuccessDialogBinding?.ivCopyRequestId -> {
                    if (Utility.stopClick()) return@OnClickListener
                    copyWithdrawRequestId()
                }

                withdrawSuccessDialogBinding?.cardDone -> {
                    if (Utility.stopClick()) return@OnClickListener
                    withdrawSuccessDialog?.dismiss()
                }
            }
        }
    }
}
