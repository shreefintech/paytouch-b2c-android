package com.shreefintech.paytouchconsumer.loadwallet

import android.animation.ValueAnimator
import android.app.Dialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.animation.DecelerateInterpolator
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.RawRes
import androidx.annotation.StringRes
import androidx.core.widget.TextViewCompat
import androidx.databinding.ObservableBoolean
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
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
import com.shreefintech.paytouchconsumer.databinding.DialogMpinBinding
import com.shreefintech.paytouchconsumer.databinding.DialogWithdrawSuccessBinding
import com.shreefintech.paytouchconsumer.Constant
import com.shreefintech.paytouchconsumer.R
import com.shreefintech.paytouchconsumer.adapter.ReferralHistoryAdp
import com.shreefintech.paytouchconsumer.adapter.WalletTransactionAdp
import com.shreefintech.paytouchconsumer.databinding.ActivityLoadWalletBinding
import com.shreefintech.paytouchconsumer.databinding.SheetMakePaymentBinding
import com.shreefintech.paytouchconsumer.databinding.SheetWithdrawBinding
import com.shreefintech.paytouchconsumer.glass.LiquidGlassEffect
import com.shreefintech.paytouchconsumer.loadwallet.model.PaymentStatusItem
import com.shreefintech.paytouchconsumer.loadwallet.model.WalletTransactionItem
import com.shreefintech.paytouchconsumer.loadwallet.viewmodel.LoadWalletViewModel
import com.shreefintech.paytouchconsumer.earningwallet.EarningWalletActivity
import com.shreefintech.paytouchconsumer.rewards.RewardsExplainerActivity
import com.shreefintech.paytouchconsumer.enums.RewardsTier
import com.shreefintech.paytouchconsumer.retrofit.model.rewards.RewardsLevelItem
import com.shreefintech.paytouchconsumer.retrofit.model.wallet.BonusWalletHistoryItem
import com.shreefintech.paytouchconsumer.retrofit.model.wallet.VirtualAccountItem
import com.shreefintech.paytouchconsumer.retrofit.model.wallet.WalletBalanceItem
import com.shreefintech.paytouchconsumer.retrofit.model.wallet.WalletWithdrawalItem
import com.shreefintech.paytouchconsumer.transactions.TransactionHistoryDetailActivity
import com.shreefintech.paytouchconsumer.utill.AnimationHelper
import com.shreefintech.paytouchconsumer.utill.SharedPreferenceHelper
import com.shreefintech.paytouchconsumer.utill.StatusSoundPlayer
import com.shreefintech.paytouchconsumer.utill.ToastUtil
import com.shreefintech.paytouchconsumer.utill.Utility
import com.shreefintech.paytouchconsumer.utill.Utility.gone
import com.shreefintech.paytouchconsumer.utill.Utility.visible
import com.shreefintech.paytouchconsumer.utill.clearMpin
import com.shreefintech.paytouchconsumer.utill.getMpin
import com.shreefintech.paytouchconsumer.utill.setupMpinBoxes

class LoadWalletActivity : BaseActivity() {

    private lateinit var binding: ActivityLoadWalletBinding
    private val viewModel: LoadWalletViewModel by viewModels()

    private var currentTab = TAB_TOTAL_BALANCE
    private val transactionList = ArrayList<WalletTransactionItem>()
    private lateinit var transactionAdp: WalletTransactionAdp
    private val referralList = ArrayList<BonusWalletHistoryItem>()
    private lateinit var referralAdp: ReferralHistoryAdp

    private enum class RecentTab { WALLET, REFERRAL }
    private var currentRecentTab = RecentTab.WALLET

    private lateinit var sheetBinding: SheetMakePaymentBinding
    private lateinit var sheetBehavior: BottomSheetBehavior<View>

    private lateinit var withdrawSheetBinding: SheetWithdrawBinding
    private lateinit var withdrawSheetBehavior: BottomSheetBehavior<View>
    private var selectedPaymentMode = MODE_IMPS

    private var currentWalletBalance: String? = null
    private var levelProgressAnimator: ValueAnimator? = null

    private var confirmDialog: Dialog? = null
    private var confirmDialogBinding: DialogConfirmPaymentBinding? = null
    private var pendingPayAmount: Double = 0.0
    private val showProgressPay = ObservableBoolean(false)

    private var withdrawConfirmDialog: Dialog? = null
    private var withdrawConfirmDialogBinding: DialogConfirmWithdrawBinding? = null
    private var pendingWithdrawAmount: Double = 0.0
    private var pendingWithdrawMode: String = MODE_IMPS
    private val showProgressWithdraw = ObservableBoolean(false)

    // The withdrawal API call runs from the MPIN dialog's submit button
    private var mpinDialog: Dialog? = null
    private var mpinDialogBinding: DialogMpinBinding? = null
    private val showProgressMpin = ObservableBoolean(false)

    private var withdrawSuccessDialog: Dialog? = null
    private var withdrawSuccessDialogBinding: DialogWithdrawSuccessBinding? = null
    private var withdrawRequestId: String? = null
    private val withdrawSoundPlayer = StatusSoundPlayer()

    private val showProgressBonus   = ObservableBoolean(false)
    private val showProgressEarning = ObservableBoolean(false)
    private val showProgressRewards = ObservableBoolean(false)

    private var isWalletEntrancePlayed = false
    private var isWalletLoading = false
    private var hasWalletData = false

    private val earningWalletLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result -> if (result.resultCode == 1) loadData() }

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
        binding.showProgressBonus   = showProgressBonus
        binding.showProgressEarning = showProgressEarning
        binding.showProgressRewards = showProgressRewards
        setupRecyclerView()
        setupPaymentSheet()
        setupWithdrawSheet()
        selectTab(TAB_TOTAL_BALANCE)
        selectRecentTab(RecentTab.WALLET)
        onBack()
        retryCallback = { loadData() }
        loadData()
    }

    override fun onResume() {
        super.onResume()
        // KEY_PENDING_ORDER_ID now holds the top-up's client_reference
        val clientReference = SharedPreferenceHelper.getSharedPreferenceString(mActivity, Constant.KEY_PENDING_ORDER_ID, null)
            ?.takeIf { it.isNotEmpty() } ?: return
        val amount = SharedPreferenceHelper.getSharedPreferenceString(mActivity, Constant.KEY_PENDING_AMOUNT, null) ?: ""
        HdfcPaymentHelper.clearPendingState(mActivity)
        hideNoInternet()
        showLoading()
        viewModel.pollTopupStatus(
            clientReference = clientReference,
            onResult = onResult@{ data ->
                if (isFinishing || isDestroyed) return@onResult
                hideLoading()
                // Not final after polling (or unreadable) → status screen shows pending
                PaymentStatusActivity.start(
                    mActivity,
                    PaymentStatusItem(
                        orderId = data?.clientReference ?: clientReference,
                        amount  = data?.amount ?: amount,
                        status  = data?.status ?: Constant.HDFC_STATUS_NEW
                    )
                )
            }
        )
    }

    override fun onDestroy() {
        mpinDialog?.dismiss()
        withdrawSuccessDialog?.dismiss()
        withdrawSoundPlayer.release()
        levelProgressAnimator?.cancel()
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
        // TODO(B2C-298): G28 — POST wallet/withdrawals takes no payment mode or narration — both stay visible but are not sent
        hideWithdrawSheet()
        showWithdrawConfirmDialog(amount, selectedPaymentMode)
    }

    private fun showWithdrawConfirmDialog(amount: Double, mode: String) {
        pendingWithdrawAmount = amount
        pendingWithdrawMode = mode
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
        dialogBinding.tvFee.text = "--"
        dialogBinding.tvTotal.text = "--"

        dialog.show()

        viewModel.fetchWithdrawalFee(amount) { fee ->
            val b = withdrawConfirmDialogBinding?.takeIf { it === dialogBinding } ?: return@fetchWithdrawalFee
            b.tvFee.text = fee?.fee?.let { Utility.formatAmount(it) } ?: "--"
            b.tvTotal.text = fee?.total?.let { Utility.formatAmount(it) } ?: "--"
        }
    }

    // Same MPIN popup as EditBankDetailsActivity — opens over the confirm dialog, which stays until the withdrawal succeeds
    private fun showWithdrawMpinDialog() {
        if (mpinDialog?.isShowing == true) return
        val dialogBinding = DialogMpinBinding.inflate(layoutInflater)
        mpinDialogBinding = dialogBinding
        showProgressMpin.set(false)
        dialogBinding.showProgress = showProgressMpin
        dialogBinding.setupMpinBoxes()
        dialogBinding.onClickListener = onClickListener()

        val dialog = Dialog(mActivity)
        dialog.setContentView(dialogBinding.root)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.window?.setWindowAnimations(R.style.DialogScaleFadeAnimation)
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.88).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        // requestFocus() alone doesn't raise the keyboard inside a Dialog
        dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
        dialog.setCancelable(false)
        dialog.setOnDismissListener {
            mpinDialog = null
            mpinDialogBinding = null
        }
        mpinDialog = dialog
        dialog.show()
    }

    private fun submitWithdrawMpin(dialogBinding: DialogMpinBinding) {
        Utility.hideKeyboard(dialogBinding.root)
        val mpin = dialogBinding.getMpin()
        val msg = when {
            mpin.isEmpty() -> getString(R.string.msgMpinEmpty)
            mpin.length != 4 -> getString(R.string.msgMpinInvalid)
            else -> null
        }
        if (msg != null) {
            ToastUtil.showDelete(mActivity, msg, inWindow = false)
            return
        }
        startWithdraw(mpin)
    }

    private fun startWithdraw(mpin: String) {
        viewModel.withdraw(
            amount = pendingWithdrawAmount,
            mpin = mpin,
            onLoading = { showProgressMpin.set(true) },
            onSuccess = onSuccess@{ data ->
                showProgressMpin.set(false)
                if (isFinishing || isDestroyed) return@onSuccess
                mpinDialog?.dismiss()
                withdrawConfirmDialog?.dismiss()
                showWithdrawSuccessDialog(data)
                fetchWalletData()
            },
            onError = onError@{ msg ->
                showProgressMpin.set(false)
                if (isFinishing || isDestroyed) return@onError
                mpinDialogBinding?.clearMpin()
                ToastUtil.showDelete(mActivity, msg, inWindow = false)
            }
        )
    }

    private fun showWithdrawSuccessDialog(data: WalletWithdrawalItem) {
        if (isFinishing || isDestroyed) return
        withdrawSuccessDialog?.dismiss()
        withdrawRequestId = data.clientReference

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
        withdrawSoundPlayer.play(mActivity, withdrawSoundRes(data.status))
        dialog.show()

        val clientReference = data.clientReference?.takeIf { it.isNotBlank() } ?: return
        if (data.isFinal == true) return
        // Popup opens as pending; re-bind once GET wallet/withdrawals/{client_reference} reports a new status
        viewModel.pollWithdrawalStatus(clientReference, onResult = onResult@{ latest ->
            if (isFinishing || isDestroyed || latest == null) return@onResult
            val b = withdrawSuccessDialogBinding?.takeIf { withdrawRequestId == clientReference } ?: return@onResult
            if (latest.status.equals(data.status, ignoreCase = true)) return@onResult
            bindWithdrawDetails(b, latest)
            withdrawSoundPlayer.play(mActivity, withdrawSoundRes(latest.status))
            fetchWalletData()
        })
    }

    @RawRes
    private fun withdrawSoundRes(status: String?): Int = when (status?.uppercase()) {
        Constant.WITHDRAW_STATUS_SUCCESS, Constant.WITHDRAW_STATUS_COMPLETED -> R.raw.success_sound
        Constant.WITHDRAW_STATUS_FAILED, Constant.WITHDRAW_STATUS_REJECTED, Constant.WITHDRAW_STATUS_REVERSED -> R.raw.failed_sound
        else -> R.raw.pending_sound
    }

    private fun bindWithdrawDetails(dialogBinding: DialogWithdrawSuccessBinding, data: WalletWithdrawalItem) {
        val status = resolveWithdrawStatus(data.status)
        Glide.with(this@LoadWalletActivity).asGif().load(status.gifRes).into(dialogBinding.ivStatusGif)

        dialogBinding.tvTitle.setText(status.titleRes)
        dialogBinding.tvSubtitle.text = data.failureReason?.takeIf { it.isNotBlank() }
            ?: data.statusMessage?.takeIf { it.isNotBlank() }
            ?: getString(status.subtitleRes)
        dialogBinding.tvAmount.text = Utility.formatAmount(data.amount)
        dialogBinding.tvStatus.text = data.status?.takeIf { it.isNotBlank() }
            ?.replaceFirstChar { it.uppercaseChar() }
            ?: getString(R.string.labelProcessing)
        dialogBinding.tvStatus.setBackgroundResource(status.chipBgRes)
        val chipTextColor = ContextCompat.getColor(mActivity, status.chipTextColorRes)
        dialogBinding.tvStatus.setTextColor(chipTextColor)
        TextViewCompat.setCompoundDrawableTintList(dialogBinding.tvStatus, ColorStateList.valueOf(chipTextColor))

        val bankName = data.bank?.bankName
        val accountLast4 = data.bank?.accountMasked?.takeLast(4).orEmpty()
        dialogBinding.tvToBank.text = when {
            bankName.isNullOrBlank() -> accountLast4.ifEmpty { "--" }
            accountLast4.isEmpty() -> bankName
            else -> getString(R.string.labelBankAccountMasked, bankName, accountLast4)
        }

        // Server's transfer_mode (always IMPS per the guide); falls back to what the user picked
        val transferMode = data.transferMode?.takeIf { it.isNotBlank() } ?: pendingWithdrawMode
        dialogBinding.tvTransferMode.text = transferMode
        dialogBinding.tvInstantBadge.visibility =
            if (transferMode.equals(MODE_IMPS, ignoreCase = true)) View.VISIBLE else View.GONE

        dialogBinding.tvRequestId.text = data.clientReference?.takeIf { it.isNotBlank() } ?: "--"
        dialogBinding.ivCopyRequestId.visibility =
            if (data.clientReference.isNullOrBlank()) View.GONE else View.VISIBLE
        dialogBinding.tvDateTime.text = Utility.formatDate(data.createdAt)
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
        fetchReferralHistory()
        fetchEarningWallet()
        fetchRewardsLevel()
    }

    // Bonus balance comes from wallet/balance (populateWalletBalance); this loads the referral rows only
    private fun fetchReferralHistory() {
        viewModel.fetchReferralHistory(
            onSuccess = { rows ->
                referralAdp.updateList(rows.take(2))
                if (currentRecentTab == RecentTab.REFERRAL) updateEmptyState()
            },
            onError = { msg ->
                ToastUtil.showDelete(mActivity, msg)
                referralAdp.updateList(emptyList())
                if (currentRecentTab == RecentTab.REFERRAL) updateEmptyState()
            }
        )
    }

    private fun fetchRewardsLevel() {
        showProgressRewards.set(true)
        viewModel.fetchRewardsLevel(
            onSuccess = { data ->
                showProgressRewards.set(false)
                populateLevel(data)
            },
            onError = {
                showProgressRewards.set(false)
                binding.tvTierName.text = "--"
                binding.ivTierBadge.setImageDrawable(null)
                binding.tvCashbackPct.text = "--"
                binding.tvNextLevel.text = "--"
            }
        )
    }

    private fun fetchEarningWallet() {
        showProgressEarning.set(true)
        viewModel.fetchEarningWallet(
            onSuccess = { data ->
                showProgressEarning.set(false)
                binding.tvEarningBalance.text = Utility.formatAmount(data.principal)
            },
            onError = {
                showProgressEarning.set(false)
                binding.tvEarningBalance.text = "--"
            }
        )
    }

    private fun populateLevel(data: RewardsLevelItem) {
        val tier = RewardsTier.from(data.stage)
        binding.tvTierName.text = data.label ?: getString(tier.labelRes)
        binding.tvCashbackPct.text = getString(R.string.labelCashbackPct, data.cashbackPercent ?: 0.0)
        binding.ivTierBadge.setImageResource(tier.badgeRes)

        val next = data.next
        val remaining = next?.remaining ?: 0.0
        binding.tvNextLevel.text = when {
            remaining > 0 -> getString(R.string.labelMoreToNextLevel, Utility.formatAmount(remaining.toString()))
            else -> getString(R.string.msgTopTier)
        }

        val current = next?.current ?: 0.0
        val target = next?.target ?: 0.0
        val progress = if (target > 0) (current / target).toFloat().coerceIn(0f, 1f) else 1f
        animateLevelProgress(progress)
    }

    private fun animateLevelProgress(progress: Float) {
        levelProgressAnimator?.cancel()
        levelProgressAnimator = ValueAnimator.ofInt(0, (progress * 100).toInt()).apply {
            duration = 1200
            startDelay = 600
            interpolator = DecelerateInterpolator()
            addUpdateListener { anim -> binding.viewProgressFill.progress = anim.animatedValue as Int }
            start()
        }
    }

    private fun validateAndShowConfirmDialog() {
        if (!Utility.isInternetAvailable(mActivity)) {
            ToastUtil.showDelete(mActivity, getString(R.string.msgNoInternet))
            return
        }

        // TODO(B2C-298): G28 — POST wallet/topups takes no description — field stays visible but is not sent
        val amountStr = sheetBinding.etAmount.text?.toString()?.trim() ?: ""
        if (amountStr.isEmpty()) {
            ToastUtil.showDelete(mActivity, getString(R.string.errEnterAmount))
            return
        }
        val amount = amountStr.toDoubleOrNull()
        if (amount == null || amount <= 0) {
            ToastUtil.showDelete(mActivity, getString(R.string.errEnterAmount))
            return
        }
        // TODO(B2C-298): G2 — HDFC min/max not shared; server's 422 message covers out-of-range amounts
        hidePaymentSheet()
        showConfirmDialog(amount)
    }

    private fun showConfirmDialog(amount: Double) {
        pendingPayAmount = amount
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
        dialogBinding.tvFee.text = "--"
        dialogBinding.tvCreditAmount.text = "--"

        dialog.show()

        viewModel.fetchTopupFee(amount) { fee ->
            val b = confirmDialogBinding?.takeIf { it === dialogBinding } ?: return@fetchTopupFee
            b.tvFee.text = fee?.fee?.let { Utility.formatAmount(it) } ?: "--"
            b.tvCreditAmount.text = fee?.creditAmount?.let { Utility.formatAmount(it) } ?: "--"
        }
    }

    private fun startTopup() {
        viewModel.createTopup(
            amount = pendingPayAmount,
            onLoading = { showProgressPay.set(true) },
            onSuccess = onSuccess@{ data ->
                showProgressPay.set(false)
                if (isFinishing || isDestroyed) return@onSuccess
                val clientReference = data.clientReference.orEmpty()
                val amount = data.amount ?: pendingPayAmount.toString()
                if (HdfcPaymentHelper.isFailedStatus(data.status.orEmpty())) {
                    confirmDialog?.dismiss()
                    PaymentStatusActivity.start(
                        mActivity,
                        PaymentStatusItem(
                            orderId = clientReference,
                            amount  = amount,
                            status  = data.status ?: Constant.HDFC_STATUS_NEW
                        )
                    )
                    return@onSuccess
                }
                // TODO(B2C-298): G1 — response shape unverified; without a reference or link the payment can't be opened or tracked
                val payUrl = data.paymentLink.orEmpty()
                if (clientReference.isEmpty() || payUrl.isEmpty()) {
                    ToastUtil.showDelete(mActivity, getString(R.string.errGeneric), inWindow = false)
                    return@onSuccess
                }
                Utility.hideKeyboard(mActivity)
                confirmDialog?.dismiss()
                HdfcPaymentHelper.launchPayment(
                    context   = mActivity,
                    orderId   = clientReference,
                    amount    = amount,
                    payUrl    = payUrl,
                    returnUrl = data.returnUrl.orEmpty()
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

    // Balances come from wallet/balance; the VA card from virtual-account (+ holder name from auth/me)
    private fun fetchWalletData() {
        fetchWalletBalance()
        viewModel.fetchVirtualAccount(
            onLoading = { showLoading() },
            onSuccess = { data ->
                if (isFinishing || isDestroyed) return@fetchVirtualAccount
                hideLoading()
                populateWalletData(data)
            },
            onError = { msg ->
                hideLoading()
                ToastUtil.showDelete(mActivity, msg)
            }
        )
        viewModel.fetchAccountHolderName { name ->
            if (!isFinishing && !isDestroyed) binding.tvAccountHolder.text = name
        }
    }

    // No toast on error — fetchVirtualAccount() already reports a failing wallet load
    private fun fetchWalletBalance() {
        showProgressBonus.set(true)
        viewModel.fetchWalletBalance(
            onSuccess = { data ->
                showProgressBonus.set(false)
                if (isFinishing || isDestroyed) return@fetchWalletBalance
                populateWalletBalance(data)
            },
            onError = {
                showProgressBonus.set(false)
                currentWalletBalance = null
                binding.tvWalletBalance.text = "--"
                binding.tvVaWalletBalance.text = "--"
                binding.tvBonusBalance.text = "--"
            }
        )
    }

    private fun populateWalletBalance(data: WalletBalanceItem) {
        // TODO(B2C-298): G10 — main_wallet assumed to be the user-facing balance; G14 — has_wallet=false shape unknown, shown as 0
        currentWalletBalance = if (data.hasWallet == false) "0" else data.mainWallet
        binding.tvWalletBalance.text = Utility.formatAmount(currentWalletBalance)
        binding.tvVaWalletBalance.text = Utility.formatAmount(currentWalletBalance)
        binding.tvBonusBalance.text = Utility.formatAmount(data.bonusWallet)
    }

    private fun fetchRecentHistory() {
        viewModel.fetchRecentHistory(
            onSuccess = { list ->
                transactionAdp.updateList(list)
                if (currentRecentTab == RecentTab.WALLET) updateEmptyState()
            },
            onError = { msg ->
                ToastUtil.showDelete(mActivity, msg)
                if (currentRecentTab == RecentTab.WALLET) updateEmptyState()
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

    // VA wallet balance and holder name are set by populateWalletBalance() / fetchAccountHolderName()
    private fun populateWalletData(data: VirtualAccountItem) {
        hasWalletData = true
        playWalletEntranceIfNeeded()
        binding.tvVirtualAccountNumber.text = data.bankAcc?.takeIf { it.isNotBlank() } ?: "--"
        binding.tvIfscCode.text = data.ifsc?.takeIf { it.isNotBlank() } ?: "--"
        binding.tvBankName.text = data.bankName?.takeIf { it.isNotBlank() } ?: "--"
        Glide.with(mActivity as Context)
            .load(data.qrCodeUrl)
            .placeholder(R.drawable.ic_qr)
            .error(R.drawable.ic_qr)
            .into(binding.ivQrCode)
        binding.tvActiveStatus.setText(
            if (data.isActive == false) R.string.labelInactiveStatus else R.string.labelActiveStatus
        )
    }

    private fun setupRecyclerView() {
        transactionAdp = WalletTransactionAdp(mActivity, transactionList)
        transactionAdp.onClickItem = { transactionId ->
            if (!Utility.stopClick()) TransactionHistoryDetailActivity.start(mActivity, transactionId)
        }
        referralAdp = ReferralHistoryAdp(mActivity, referralList)
        // Adapter is attached by selectRecentTab() so the list follows the selected tab
        binding.rvTransactions.layoutManager = LinearLayoutManager(mActivity)
        // Keep both views hidden until fetchRecentHistory() resolves — avoids flashing "No transactions" while loading
        binding.tvNoTransactions.visibility = View.GONE
        binding.rvTransactions.visibility = View.GONE
    }

    private fun updateEmptyState() {
        val isEmpty = if (currentRecentTab == RecentTab.WALLET) transactionList.isEmpty() else referralList.isEmpty()
        binding.tvNoTransactions.visibility = if (isEmpty) View.VISIBLE else View.GONE
        binding.rvTransactions.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }

    private fun selectRecentTab(tab: RecentTab) {
        currentRecentTab = tab
        updateRecentTabVisuals()
        binding.rvTransactions.adapter = if (tab == RecentTab.WALLET) transactionAdp else referralAdp
        updateEmptyState()
    }

    private fun updateRecentTabVisuals() {
        val primary = ContextCompat.getColor(mActivity, R.color.primary)
        val white = ContextCompat.getColor(mActivity, R.color.white)
        val transparent = ContextCompat.getColor(mActivity, android.R.color.transparent)
        if (currentRecentTab == RecentTab.WALLET) {
            binding.cvTabWallet.setCardBackgroundColor(primary)
            binding.tvTabWallet.setTextColor(white)
            binding.cvTabReferralHistory.setCardBackgroundColor(transparent)
            binding.tvTabReferralHistory.setTextColor(primary)
        } else {
            binding.cvTabWallet.setCardBackgroundColor(transparent)
            binding.tvTabWallet.setTextColor(primary)
            binding.cvTabReferralHistory.setCardBackgroundColor(primary)
            binding.tvTabReferralHistory.setTextColor(white)
        }
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

                binding.cardEarningWallet -> {
                    if (Utility.stopClick()) return@OnClickListener
                    earningWalletLauncher.launch(Intent(mActivity, EarningWalletActivity::class.java))
                }

                binding.cardRewardsLevel -> {
                    if (Utility.stopClick()) return@OnClickListener
                    RewardsExplainerActivity.start(mActivity)
                }

                binding.llMakePayment -> {
                    if (Utility.stopClick()) return@OnClickListener
                    showPaymentSheet()
                }

                binding.llWithdraw -> {
                    if (Utility.stopClick()) return@OnClickListener
                    showWithdrawSheet()
                }

                binding.llViewAll -> {
                    if (Utility.stopClick()) return@OnClickListener
                    val tab = if (currentRecentTab == RecentTab.WALLET)
                        WalletTransactionsActivity.TAB_WALLET
                    else
                        WalletTransactionsActivity.TAB_REFERRAL
                    WalletTransactionsActivity.start(mActivity, tab)
                }

                binding.cvTabWallet -> {
                    if (Utility.stopClick()) return@OnClickListener
                    if (currentRecentTab != RecentTab.WALLET) selectRecentTab(RecentTab.WALLET)
                }

                binding.cvTabReferralHistory -> {
                    if (Utility.stopClick()) return@OnClickListener
                    if (currentRecentTab != RecentTab.REFERRAL) selectRecentTab(RecentTab.REFERRAL)
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
                    startTopup()
                }

                withdrawConfirmDialogBinding?.cardClose -> {
                    withdrawConfirmDialog?.dismiss()
                }

                withdrawConfirmDialogBinding?.cardWithdrawSecurely -> {
                    if (Utility.stopClick()) return@OnClickListener
                    showWithdrawMpinDialog()
                }

                mpinDialogBinding?.cardClose -> {
                    if (Utility.stopClick()) return@OnClickListener
                    if (showProgressMpin.get()) return@OnClickListener
                    mpinDialog?.dismiss()
                }

                mpinDialogBinding?.cardSubmit -> {
                    if (Utility.stopClick()) return@OnClickListener
                    if (showProgressMpin.get()) return@OnClickListener
                    mpinDialogBinding?.let { submitWithdrawMpin(it) }
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
