package com.shreefintech.paytouchconsumer.earningwallet

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.databinding.ObservableBoolean
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.shreefintech.paytouchconsumer.BaseActivity
import com.shreefintech.paytouchconsumer.Constant
import com.shreefintech.paytouchconsumer.R
import com.shreefintech.paytouchconsumer.databinding.ActivityEarningWalletBinding
import com.shreefintech.paytouchconsumer.databinding.SheetEarningLockBinding
import com.shreefintech.paytouchconsumer.databinding.SheetEarningOptInBinding
import com.shreefintech.paytouchconsumer.databinding.SheetEarningWithdrawBinding
import com.shreefintech.paytouchconsumer.earningwallet.viewmodel.EarningWalletViewModel
import com.shreefintech.paytouchconsumer.retrofit.model.WalletDataItem
import com.shreefintech.paytouchconsumer.retrofit.model.rewards.EarningWalletItem
import com.shreefintech.paytouchconsumer.utill.ToastUtil
import com.shreefintech.paytouchconsumer.utill.Utility
import com.shreefintech.paytouchconsumer.utill.Utility.gone
import com.shreefintech.paytouchconsumer.utill.Utility.visible

class EarningWalletActivity : BaseActivity() {

    private lateinit var binding: ActivityEarningWalletBinding

    private val viewModel: EarningWalletViewModel by viewModels()

    private lateinit var optInSheet: SheetEarningOptInBinding
    private lateinit var optInBehavior: BottomSheetBehavior<View>

    private lateinit var lockSheet: SheetEarningLockBinding
    private lateinit var lockBehavior: BottomSheetBehavior<View>

    private lateinit var withdrawSheet: SheetEarningWithdrawBinding
    private lateinit var withdrawBehavior: BottomSheetBehavior<View>

    private val showProgressOptIn = ObservableBoolean(false)
    private val showProgressLock = ObservableBoolean(false)
    private val showProgressWithdraw = ObservableBoolean(false)

    private var isOptedIn = false
    private var isGoldStage = false
    private var lockedBalance = 0.0
    private var walletBalance = 0.0
    private var interestAccrued = 0.0
    private var resultCode = 0
    private var isDataLoaded = false

    companion object {
        fun start(context: Context) {
            context.startActivity(Intent(context, EarningWalletActivity::class.java))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEarningWalletBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupSheets()
        setupInsets()
        bindClickListeners()
        onBack()
        retryCallback = { loadData() }
        loadData()
    }

    // region Setup

    private fun setupSheets() {
        optInSheet = binding.incSheetOptIn
        optInBehavior = BottomSheetBehavior.from(optInSheet.root)
        optInBehavior.state = BottomSheetBehavior.STATE_HIDDEN
        optInBehavior.addBottomSheetCallback(sheetCallback())

        lockSheet = binding.incSheetLock
        lockBehavior = BottomSheetBehavior.from(lockSheet.root)
        lockBehavior.state = BottomSheetBehavior.STATE_HIDDEN
        lockBehavior.addBottomSheetCallback(sheetCallback())

        withdrawSheet = binding.incSheetWithdraw
        withdrawBehavior = BottomSheetBehavior.from(withdrawSheet.root)
        withdrawBehavior.state = BottomSheetBehavior.STATE_HIDDEN
        withdrawBehavior.addBottomSheetCallback(sheetCallback())

        lockSheet.etLockAmount.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val entered = s?.toString()?.toDoubleOrNull() ?: 0.0
                updateLockValidation(entered)
            }
        })

        withdrawSheet.etWithdrawAmount.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val entered = s?.toString()?.toDoubleOrNull() ?: 0.0
                updateWithdrawState(entered)
            }
        })

        optInSheet.cbOptInTerms.setOnCheckedChangeListener { _, isChecked ->
            setOptInButtonEnabled(isChecked)
        }
    }

    private fun setupInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.clRoot) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, maxOf(imeInsets.bottom, systemBars.bottom))
            if (imeInsets.bottom > 0) Utility.scrollToFocused(mActivity)
            optInSheet.root.setPadding(0, 0, 0, systemBars.bottom)
            lockSheet.root.setPadding(0, 0, 0, systemBars.bottom)
            withdrawSheet.root.setPadding(0, 0, 0, systemBars.bottom)
            insets
        }
    }

    private fun bindClickListeners() {
        val listener = onClickListener()
        binding.onClickListener = listener
        optInSheet.onClickListener = listener
        optInSheet.showProgressOptIn = showProgressOptIn
        lockSheet.onClickListener = listener
        lockSheet.showProgressLock = showProgressLock
        withdrawSheet.onClickListener = listener
        withdrawSheet.showProgressWithdraw = showProgressWithdraw
    }

    private fun sheetCallback(): BottomSheetBehavior.BottomSheetCallback {
        return object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                when (newState) {
                    BottomSheetBehavior.STATE_EXPANDED -> binding.viewBg.visible()
                    BottomSheetBehavior.STATE_HIDDEN -> {
                        Utility.hideKeyboard(mActivity)
                        if (!isAnySheetOpen()) binding.viewBg.gone()
                    }
                    else -> {}
                }
            }
            override fun onSlide(bottomSheet: View, slideOffset: Float) {}
        }
    }

    private fun isAnySheetOpen() =
        optInBehavior.state == BottomSheetBehavior.STATE_EXPANDED ||
        lockBehavior.state == BottomSheetBehavior.STATE_EXPANDED ||
        withdrawBehavior.state == BottomSheetBehavior.STATE_EXPANDED

    // endregion

    // region Data Loading

    private fun loadData() {
        if (!Utility.isInternetAvailable(mActivity)) {
            showNoInternet()
            return
        }
        hideNoInternet()
        viewModel.fetchEarningWallet(
            onLoading = {},
            onSuccess = { populateEarningWallet(it) },
            onError = { ToastUtil.showDelete(mActivity, it) }
        )
        viewModel.fetchWalletData(
            onSuccess = { populateWalletBalance(it) },
            onError = { ToastUtil.showDelete(mActivity, it) }
        )
        viewModel.fetchLevel(
            onSuccess = { isGoldStage = it.stage?.equals("gold", ignoreCase = true) == true },
            onError = { /* non-critical enrichment — isGoldStage defaults false; gold-stage withdrawal warning silently hidden on failure */ }
        )
    }

    private fun refreshData() {
        viewModel.fetchEarningWallet(
            onLoading = {},
            onSuccess = { populateEarningWallet(it) },
            onError = { ToastUtil.showDelete(mActivity, it) }
        )
        viewModel.fetchWalletData(
            onSuccess = { populateWalletBalance(it) },
            onError = { ToastUtil.showDelete(mActivity, it) }
        )
    }

    // endregion

    // region Populate UI

    private fun populateEarningWallet(item: EarningWalletItem) {
        isDataLoaded = true
        isOptedIn = item.optedIn == true
        lockedBalance = item.principal ?: 0.0
        interestAccrued = item.interestAccrued ?: 0.0
        val pendingAmt = item.pendingWithdrawalAmount ?: 0.0

        binding.tvLockedBalance.text = Utility.formatAmount(lockedBalance)

        if (interestAccrued > 0.0) {
            binding.cardInterestPill.visible()
            binding.tvInterestPill.text = "+${Utility.formatAmount(interestAccrued)} ${getString(R.string.labelInterestEarned)}"
        } else {
            binding.cardInterestPill.gone()
        }

        val agingMs = if (lockedBalance > 0.0) Utility.parseIsoMillis(item.agingStartedAt) else null
        if (agingMs != null) {
            binding.cardStatus.visible()
            updateStatusCard(item.agingStartedAt)
        } else {
            binding.cardStatus.gone()
        }

        if (pendingAmt > 0.0) {
            binding.llPendingPill.visible()
            val dateStr = Utility.formatDate(item.withdrawalAvailableAt, "dd MMM")
            binding.tvPendingPill.text = getString(R.string.msgPendingWithdrawal, Utility.formatAmount(pendingAmt), dateStr)
            binding.btnWithdraw.isEnabled = false
            binding.btnWithdraw.alpha = 0.45f
        } else {
            binding.llPendingPill.gone()
            binding.btnWithdraw.isEnabled = true
            binding.btnWithdraw.alpha = 1.0f
        }
    }

    private fun updateStatusCard(agingStartedAt: String?) {
        val agingMs = Utility.parseIsoMillis(agingStartedAt) ?: return
        val activeDateMs = agingMs + Constant.EARNING_LOCK_DAYS.toLong() * 24 * 60 * 60 * 1000

        val agingDateStr = Utility.formatDate(agingStartedAt, "d MMM yyyy")
        val activeDateStr = Utility.formatMillis(activeDateMs, "d MMM yyyy")

        val now = System.currentTimeMillis()
        val isActive = now >= activeDateMs

        if (isActive) {
            binding.cardStatusBadge.setCardBackgroundColor(
                ContextCompat.getColor(mActivity, R.color.earningSuccessBg)
            )
            binding.tvStatusBadge.setTextColor(ContextCompat.getColor(mActivity, R.color.earningSuccessText))
            binding.tvStatusBadge.text = getString(R.string.labelStatusActive)
            binding.progressStatus.progress = Constant.EARNING_LOCK_DAYS
            binding.progressStatus.progressTintList =
                ColorStateList.valueOf(ContextCompat.getColor(mActivity, R.color.earningSuccessBar))
            binding.progressStatus.progressBackgroundTintList =
                ColorStateList.valueOf(ContextCompat.getColor(mActivity, R.color.earningSuccessBg))
            binding.tvStatusDate.text = getString(R.string.labelEarningsSince, activeDateStr)
        } else {
            binding.cardStatusBadge.setCardBackgroundColor(
                ContextCompat.getColor(mActivity, R.color.earningWarnBg)
            )
            binding.tvStatusBadge.setTextColor(ContextCompat.getColor(mActivity, R.color.earningWarnTitle))
            binding.tvStatusBadge.text = getString(R.string.labelStatusLocked)
            val daysElapsed = ((now - agingMs) / (1000L * 60 * 60 * 24)).toInt()
                .coerceIn(0, Constant.EARNING_LOCK_DAYS - 1)
            binding.progressStatus.progress = daysElapsed
            binding.progressStatus.progressTintList =
                ColorStateList.valueOf(ContextCompat.getColor(mActivity, R.color.earningWarnTitle))
            binding.progressStatus.progressBackgroundTintList =
                ColorStateList.valueOf(ContextCompat.getColor(mActivity, R.color.earningWarnBg))
            binding.tvStatusDate.text = getString(R.string.labelLockedSince, agingDateStr, activeDateStr)
        }
    }

    private fun populateWalletBalance(item: WalletDataItem) {
        walletBalance = item.walletBalance?.toDoubleOrNull() ?: 0.0
    }

    private fun updateWithdrawState(entered: Double) {
        val isOverLimit = entered > 0.0 && entered > lockedBalance
        withdrawSheet.tvWithdrawError.visibility = if (isOverLimit) View.VISIBLE else View.GONE

        val showGoldWarn = isGoldStage && entered > 0.0 && !isOverLimit
        if (showGoldWarn) {
            withdrawSheet.llGoldWarning.visible()
            val remaining = (lockedBalance - entered).coerceAtLeast(0.0)
            withdrawSheet.tvGoldWarning.text = getString(R.string.msgGoldWarning, Utility.formatAmount(remaining))
        } else {
            withdrawSheet.llGoldWarning.gone()
        }

        val showInterest = interestAccrued > 0.0 && entered > 0.0 && !isOverLimit
        if (showInterest) {
            withdrawSheet.llInterestRow.visible()
            withdrawSheet.tvLostInterest.text = getString(R.string.msgLostInterest, Utility.formatAmount(interestAccrued))
        } else {
            withdrawSheet.llInterestRow.gone()
        }

        withdrawSheet.llWarnBox.visibility = if (showGoldWarn || showInterest) View.VISIBLE else View.GONE
        setWithdrawButtonEnabled(entered > 0.0 && !isOverLimit)
    }

    private fun updateLockValidation(entered: Double) {
        val isOverBalance = entered > 0.0 && entered > walletBalance
        lockSheet.tvLockError.visibility = if (isOverBalance) View.VISIBLE else View.GONE
        setLockButtonEnabled(entered > 0.0 && !isOverBalance)
    }

    // endregion

    // region Sheet Actions

    private fun openOptInSheet() {
        optInSheet.cbOptInTerms.isChecked = false
        setOptInButtonEnabled(false)
        optInBehavior.state = BottomSheetBehavior.STATE_EXPANDED
    }

    private fun openLockSheet() {
        val defaultAmountStr = Constant.EARNING_DEFAULT_LOCK_AMOUNT.toString()
        lockSheet.etLockAmount.setText(defaultAmountStr)
        lockSheet.etLockAmount.setSelection(defaultAmountStr.length)
        lockSheet.tvLockWalletBalance.text = Utility.formatAmount(walletBalance)
        val activeDateMs = System.currentTimeMillis() + Constant.EARNING_LOCK_DAYS.toLong() * 24 * 60 * 60 * 1000
        lockSheet.tvStartsEarning.text = getString(R.string.msgStartsEarning, Utility.formatMillis(activeDateMs, "d MMM yyyy"))
        lockSheet.tvLockError.gone()
        setLockButtonEnabled(walletBalance >= Constant.EARNING_DEFAULT_LOCK_AMOUNT)
        lockBehavior.state = BottomSheetBehavior.STATE_EXPANDED
    }

    private fun openWithdrawSheet() {
        withdrawSheet.etWithdrawAmount.text?.clear()
        withdrawSheet.tvWithdrawLockedBalance.text = Utility.formatAmount(lockedBalance)
        withdrawSheet.tvWithdrawError.gone()
        withdrawSheet.llWarnBox.gone()
        setWithdrawButtonEnabled(false)
        withdrawBehavior.state = BottomSheetBehavior.STATE_EXPANDED
    }

    private fun onOptIn() {
        if (showProgressOptIn.get()) return
        if (!optInSheet.cbOptInTerms.isChecked) {
            ToastUtil.showDelete(mActivity, getString(R.string.errAgreeToTerms))
            return
        }
        viewModel.postOptIn(
            onLoading = { showProgressOptIn.set(true) },
            onSuccess = { msg ->
                showProgressOptIn.set(false)
                optInBehavior.state = BottomSheetBehavior.STATE_HIDDEN
                if (msg.isNotEmpty()) ToastUtil.showSuccess(mActivity, msg)
                isOptedIn = true
                refreshData()
            },
            onError = { msg ->
                showProgressOptIn.set(false)
                ToastUtil.showDelete(mActivity, msg)
            }
        )
    }

    private fun onLock() {
        if (showProgressLock.get()) return
        val amount = lockSheet.etLockAmount.text?.toString()?.trim()?.toDoubleOrNull()
        if (amount == null || amount <= 0.0) {
            ToastUtil.showDelete(mActivity, getString(R.string.errMinAmount))
            return
        }
        if (amount > walletBalance) {
            ToastUtil.showDelete(mActivity, getString(R.string.errExceedsWalletBalance))
            return
        }
        viewModel.postLock(
            amount = amount,
            onLoading = { showProgressLock.set(true) },
            onSuccess = { msg ->
                showProgressLock.set(false)
                resultCode = 1
                lockBehavior.state = BottomSheetBehavior.STATE_HIDDEN
                if (msg.isNotEmpty()) ToastUtil.showSuccess(mActivity, msg)
                refreshData()
            },
            onError = { msg ->
                showProgressLock.set(false)
                ToastUtil.showDelete(mActivity, msg)
            }
        )
    }

    private fun onWithdraw() {
        if (showProgressWithdraw.get()) return
        val amount = withdrawSheet.etWithdrawAmount.text?.toString()?.trim()?.toDoubleOrNull()
        if (amount == null || amount <= 0.0) {
            ToastUtil.showDelete(mActivity, getString(R.string.errMinAmount))
            return
        }
        if (amount > lockedBalance) {
            ToastUtil.showDelete(mActivity, getString(R.string.errExceedsLockedBalance))
            return
        }
        viewModel.postWithdraw(
            amount = amount,
            onLoading = { showProgressWithdraw.set(true) },
            onSuccess = { msg ->
                showProgressWithdraw.set(false)
                resultCode = 1
                withdrawBehavior.state = BottomSheetBehavior.STATE_HIDDEN
                if (msg.isNotEmpty()) ToastUtil.showSuccess(mActivity, msg)
                refreshData()
            },
            onError = { msg ->
                showProgressWithdraw.set(false)
                ToastUtil.showDelete(mActivity, msg)
            }
        )
    }

    // endregion

    // region Back & Clicks

    private fun onBack() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                when {
                    withdrawBehavior.state == BottomSheetBehavior.STATE_EXPANDED ->
                        withdrawBehavior.state = BottomSheetBehavior.STATE_HIDDEN
                    lockBehavior.state == BottomSheetBehavior.STATE_EXPANDED ->
                        lockBehavior.state = BottomSheetBehavior.STATE_HIDDEN
                    optInBehavior.state == BottomSheetBehavior.STATE_EXPANDED ->
                        optInBehavior.state = BottomSheetBehavior.STATE_HIDDEN
                    else -> {
                        setResult(resultCode)
                        isEnabled = false
                        onBackPressedDispatcher.onBackPressed()
                    }
                }
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
                binding.btnLockMoney -> {
                    if (Utility.stopClick()) return@OnClickListener
                    if (!isDataLoaded) return@OnClickListener
                    if (!isOptedIn) openOptInSheet() else openLockSheet()
                }
                binding.btnWithdraw -> {
                    if (Utility.stopClick()) return@OnClickListener
                    openWithdrawSheet()
                }
                optInSheet.ivCloseOptIn -> {
                    if (Utility.stopClick()) return@OnClickListener
                    optInBehavior.state = BottomSheetBehavior.STATE_HIDDEN
                }
                optInSheet.llTerms -> {
                    optInSheet.cbOptInTerms.isChecked = !optInSheet.cbOptInTerms.isChecked
                }
                optInSheet.btnOptIn -> {
                    if (Utility.stopClick()) return@OnClickListener
                    onOptIn()
                }
                lockSheet.ivCloseLock -> {
                    if (Utility.stopClick()) return@OnClickListener
                    lockBehavior.state = BottomSheetBehavior.STATE_HIDDEN
                }
                lockSheet.chipLock500 -> {
                    if (Utility.stopClick()) return@OnClickListener
                    lockSheet.etLockAmount.setText("500")
                    lockSheet.etLockAmount.setSelection(3)
                }
                lockSheet.chipLock1000 -> {
                    if (Utility.stopClick()) return@OnClickListener
                    val amtStr = Constant.EARNING_DEFAULT_LOCK_AMOUNT.toString()
                    lockSheet.etLockAmount.setText(amtStr)
                    lockSheet.etLockAmount.setSelection(amtStr.length)
                }
                lockSheet.chipLockAll -> {
                    if (Utility.stopClick()) return@OnClickListener
                    val all = walletBalance.toLong().toString()
                    lockSheet.etLockAmount.setText(all)
                    lockSheet.etLockAmount.setSelection(all.length)
                }
                lockSheet.btnLock -> {
                    if (Utility.stopClick()) return@OnClickListener
                    onLock()
                }
                withdrawSheet.ivCloseWithdraw -> {
                    if (Utility.stopClick()) return@OnClickListener
                    withdrawBehavior.state = BottomSheetBehavior.STATE_HIDDEN
                }
                withdrawSheet.btnWithdrawSheet -> {
                    if (Utility.stopClick()) return@OnClickListener
                    onWithdraw()
                }
            }
        }
    }

    // endregion

    private fun setOptInButtonEnabled(enabled: Boolean) {
        val bgColor = if (enabled) R.color.primary else R.color.earningDisabledBg
        val textColor = if (enabled) R.color.white else R.color.earningDisabledText
        optInSheet.btnOptIn.setCardBackgroundColor(ContextCompat.getColor(mActivity, bgColor))
        optInSheet.tvBtnOptInLabel.setTextColor(ContextCompat.getColor(mActivity, textColor))
        optInSheet.ivBtnOptInIcon.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(mActivity, textColor))
        optInSheet.btnOptIn.isClickable = enabled
        optInSheet.btnOptIn.isFocusable = enabled
    }

    private fun setLockButtonEnabled(enabled: Boolean) {
        val bgColor = if (enabled) R.color.primary else R.color.earningDisabledBg
        val textColor = if (enabled) R.color.white else R.color.earningDisabledText
        lockSheet.btnLock.setCardBackgroundColor(ContextCompat.getColor(mActivity, bgColor))
        lockSheet.tvBtnLockLabel.setTextColor(ContextCompat.getColor(mActivity, textColor))
        lockSheet.ivBtnLockIcon.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(mActivity, textColor))
        lockSheet.btnLock.isClickable = enabled
        lockSheet.btnLock.isFocusable = enabled
    }

    private fun setWithdrawButtonEnabled(enabled: Boolean) {
        val bgColor = if (enabled) R.color.primary else R.color.earningDisabledBg
        val textColor = if (enabled) R.color.white else R.color.earningDisabledText
        withdrawSheet.btnWithdrawSheet.setCardBackgroundColor(ContextCompat.getColor(mActivity, bgColor))
        withdrawSheet.tvBtnWithdrawLabel.setTextColor(ContextCompat.getColor(mActivity, textColor))
        withdrawSheet.ivBtnWithdrawIcon.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(mActivity, textColor))
        withdrawSheet.btnWithdrawSheet.isClickable = enabled
        withdrawSheet.btnWithdrawSheet.isFocusable = enabled
    }

}
