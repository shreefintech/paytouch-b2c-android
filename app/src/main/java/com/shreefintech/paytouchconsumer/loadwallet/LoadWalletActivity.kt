package com.shreefintech.paytouchconsumer.loadwallet

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.databinding.ObservableBoolean
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.shreefintech.paytouchconsumer.BaseActivity
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

    private val showProgressPay = ObservableBoolean(false)
    private var pendingOrderId: String? = null

    private val hdfcLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        val orderId = pendingOrderId ?: return@registerForActivityResult
        checkOrderStatus(orderId)
    }

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
            binding.incPaymentSheet.root.setPadding(0, 0, 0, maxOf(imeInsets.bottom, systemBars.bottom))
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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.getBooleanExtra(Constant.EXTRA_FROM_PAYMENT, false)) {
            if (isPaymentSheetVisible()) hidePaymentSheet()
            sheetBinding.etAmount.setText("")
            sheetBinding.etDescription.setText("")
            fetchWalletData()
            fetchRecentHistory()
        }
    }

    private fun setupPaymentSheet() {
        sheetBinding = binding.incPaymentSheet
        sheetBinding.showProgressPay = showProgressPay
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

    private fun onProceedPayment() {
        val amountStr = sheetBinding.etAmount.text?.toString()?.trim() ?: ""
        val description = sheetBinding.etDescription.text?.toString()?.trim() ?: ""
        if (amountStr.isEmpty()) {
            ToastUtil.showDelete(mActivity, getString(R.string.hintEnterAmount))
            return
        }
        val amount = amountStr.toDoubleOrNull()
        if (amount == null || amount <= 0) {
            ToastUtil.showDelete(mActivity, getString(R.string.hintEnterAmount))
            return
        }
        Utility.hideKeyboard(mActivity)
        viewModel.createHdfcOrder(
            amount = amount,
            description = description,
            onLoading = { showProgressPay.set(true) },
            onSuccess = { orderItem ->
                showProgressPay.set(false)
                pendingOrderId = orderItem.orderId
                val payUrl = orderItem.paymentLinks?.web ?: ""
                val returnUrl = orderItem.returnUrl ?: ""
                hidePaymentSheet()
                hdfcLauncher.launch(
                    HdfcWebViewActivity.newIntent(mActivity, payUrl, returnUrl)
                )
            },
            onError = { msg ->
                showProgressPay.set(false)
                ToastUtil.showDelete(mActivity, msg)
            }
        )
    }

    private fun checkOrderStatus(orderId: String) {
        showLoading()
        viewModel.checkHdfcOrderStatus(
            orderId = orderId,
            onLoading = {},
            onSuccess = { orderItem ->
                hideLoading()
                pendingOrderId = null
                PaymentStatusActivity.start(
                    context = mActivity,
                    item = PaymentStatusItem(
                        orderId = orderItem.orderId ?: orderId,
                        amount  = orderItem.amount ?: "",
                        status  = orderItem.status ?: ""
                    )
                )
            },
            onError = { msg ->
                hideLoading()
                pendingOrderId = null
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
                transactionList.clear()
                transactionList.addAll(list)
                transactionAdp.notifyDataSetChanged()
                updateEmptyState()
            },
            onError = {
                // Intentionally silent: fetchUserWalletData always runs first and shows its own
                // error toast. Recent history is supplementary display only.
                // TODO(B2C-82): show history error independently if the two calls are ever decoupled
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
        binding.tvWalletBalance.text = Utility.formatAmount(data.walletBalance)
        binding.tvVirtualAccountNumber.text = data.virtualAccountNumber ?: "--"
        binding.tvVaWalletBalance.text = Utility.formatAmount(data.wallet?.balance)
        binding.tvAccountHolder.text = data.name ?: data.mobile ?: "--"
        binding.tvQrInvoiceAmount.text = data.vpa ?: "--"
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
                    if (showProgressPay.get()) return@OnClickListener
                    onProceedPayment()
                }
            }
        }
    }
}
