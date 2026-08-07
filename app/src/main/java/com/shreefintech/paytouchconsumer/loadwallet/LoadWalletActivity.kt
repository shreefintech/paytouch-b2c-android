package com.shreefintech.paytouchconsumer.loadwallet

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.shreefintech.paytouchconsumer.BaseActivity
import com.shreefintech.paytouchconsumer.R
import com.shreefintech.paytouchconsumer.adapter.WalletTransactionAdp
import com.shreefintech.paytouchconsumer.databinding.ActivityLoadWalletBinding
import com.shreefintech.paytouchconsumer.loadwallet.model.WalletTransactionItem
import com.shreefintech.paytouchconsumer.retrofit.model.WalletDataItem
import com.shreefintech.paytouchconsumer.utill.ToastUtil
import com.shreefintech.paytouchconsumer.utill.Utility

class LoadWalletActivity : BaseActivity() {

    private lateinit var binding: ActivityLoadWalletBinding
    private val viewModel: LoadWalletViewModel by viewModels()

    private var currentTab = TAB_TOTAL_BALANCE
    private val transactionList = ArrayList<WalletTransactionItem>()
    private lateinit var transactionAdp: WalletTransactionAdp

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
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.onClickListener = onClickListener()
        setupRecyclerView()
        selectTab(TAB_TOTAL_BALANCE)
        onBack()
        fetchWalletData()
        fetchRecentHistory()
    }

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
            onError = { /* silent — wallet data already shows its own error */ }
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

        val activeColor    = ContextCompat.getColor(mActivity, R.color.primary)
        val inactiveColor  = android.graphics.Color.TRANSPARENT
        val activeText     = ContextCompat.getColor(mActivity, R.color.white)
        val inactiveText   = ContextCompat.getColor(mActivity, R.color.primary)
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
                binding.llMakePayment -> {
                    if (Utility.stopClick()) return@OnClickListener
                    // TODO(PAYTOUCH-82): Navigate to Make Payment flow
                }
                binding.llTransactionReport -> {
                    if (Utility.stopClick()) return@OnClickListener
                    WalletTransactionsActivity.start(mActivity)
                }
            }
        }
    }
}
