package com.shreefintech.paytouchconsumer.loadwallet

import android.app.Dialog
import android.content.Context
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
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.shreefintech.paytouchconsumer.BaseActivity
import com.shreefintech.paytouchconsumer.databinding.DialogConfirmPaymentBinding
import com.shreefintech.paytouchconsumer.Constant
import com.shreefintech.paytouchconsumer.R
import com.shreefintech.paytouchconsumer.adapter.WalletTransactionAdp
import com.shreefintech.paytouchconsumer.databinding.ActivityLoadWalletBinding
import com.shreefintech.paytouchconsumer.databinding.SheetMakePaymentBinding
import com.shreefintech.paytouchconsumer.glass.LiquidGlassEffect
import com.shreefintech.paytouchconsumer.loadwallet.model.PaymentStatusItem
import com.shreefintech.paytouchconsumer.loadwallet.model.WalletTransactionItem
import com.shreefintech.paytouchconsumer.loadwallet.viewmodel.LoadWalletViewModel
import com.shreefintech.paytouchconsumer.retrofit.model.WalletDataItem
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

    private var currentWalletBalance: String? = null

    private var confirmDialog: Dialog? = null
    private var confirmDialogBinding: DialogConfirmPaymentBinding? = null
    private var pendingPayAmount: Double = 0.0
    private var pendingPayDescription: String = ""
    private val showProgressPay = ObservableBoolean(false)

    companion object {
        private const val TAB_TOTAL_BALANCE = 0

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
            binding.incPaymentSheet.root.setPadding(
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
        selectTab(TAB_TOTAL_BALANCE)
        onBack()
        fetchWalletData()
        fetchRecentHistory()
    }

    override fun onResume() {
        super.onResume()
        val orderId = SharedPreferenceHelper.getSharedPreferenceString(mActivity, Constant.KEY_PENDING_ORDER_ID, null)
            ?.takeIf { it.isNotEmpty() } ?: return
        val amount  = SharedPreferenceHelper.getSharedPreferenceString(mActivity, Constant.KEY_PENDING_AMOUNT, null) ?: ""
        HdfcPaymentHelper.clearPendingState(mActivity)
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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.getBooleanExtra(Constant.EXTRA_FROM_PAYMENT, false)) {
            Utility.hideKeyboard(mActivity)
            if (isPaymentSheetVisible()) hidePaymentSheet()
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

    private fun validateAndShowConfirmDialog() {
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
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.88).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog.setCancelable(true)

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
                    ToastUtil.showDelete(mActivity, getString(R.string.errGeneric))
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
                ToastUtil.showDelete(mActivity, msg)
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
            }
        )
    }

    private fun showLoading() {
        binding.viewDimmer.visibility = View.VISIBLE
        binding.pbLoading.visibility = View.VISIBLE
    }

    private fun hideLoading() {
        binding.viewDimmer.visibility = View.GONE
        binding.pbLoading.visibility = View.GONE
    }

    private fun populateWalletData(data: WalletDataItem) {
        currentWalletBalance = data.walletBalance
        binding.tvWalletBalance.text = Utility.formatAmount(data.walletBalance)
        binding.tvVirtualAccountNumber.text = data.virtualAccountNumber ?: "--"
        binding.tvVaWalletBalance.text = Utility.formatAmount(data.wallet?.balance)
        binding.tvAccountHolder.text = data.name ?: data.mobile ?: "--"
        // TODO(B2C-82): hide until backend provides the correct QR invoice amount field
        binding.tvQrInvoiceAmount.visibility = View.GONE
        binding.tvIfscCode.text = data.ifsc ?: "--"
        val status = data.wallet?.status
        if (!status.isNullOrEmpty()) {
            binding.tvActiveStatus.text = status.replaceFirstChar { it.uppercaseChar() }
        }
    }

    private fun setupRecyclerView() {
        transactionAdp = WalletTransactionAdp(mActivity, transactionList)
        binding.rvTransactions.apply {
            layoutManager = LinearLayoutManager(mActivity)
            adapter = transactionAdp
        }
        updateEmptyState()
    }

    private fun updateEmptyState() {
        val isEmpty = transactionList.isEmpty()
        binding.tvNoTransactions.visibility = if (isEmpty) View.VISIBLE else View.GONE
        binding.rvTransactions.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }

    private fun selectTab(tab: Int) {
        currentTab = tab
        val isTotalBalance = tab == TAB_TOTAL_BALANCE
        binding.llTotalBalanceContent.visibility = if (isTotalBalance) View.VISIBLE else View.GONE
        binding.tvComingSoon.visibility = if (isTotalBalance) View.GONE else View.VISIBLE
    }

    private fun onBack() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (isPaymentSheetVisible()) {
                    hidePaymentSheet()
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
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

                confirmDialogBinding?.cardClose -> {
                    confirmDialog?.dismiss()
                }

                confirmDialogBinding?.cardPaySecurely -> {
                    if (Utility.stopClick()) return@OnClickListener
                    if (showProgressPay.get()) return@OnClickListener
                    startHdfcFlow()
                }
            }
        }
    }
}
