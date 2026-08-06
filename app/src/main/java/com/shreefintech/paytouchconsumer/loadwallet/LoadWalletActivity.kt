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
import com.shreefintech.paytouchconsumer.adapter.TransactionAdp
import com.shreefintech.paytouchconsumer.databinding.ActivityLoadWalletBinding
import com.shreefintech.paytouchconsumer.transactions.model.TransactionItem
import com.shreefintech.paytouchconsumer.utill.Utility

class LoadWalletActivity : BaseActivity() {

    private lateinit var binding: ActivityLoadWalletBinding
    private val viewModel: LoadWalletViewModel by viewModels()

    private var currentTab = TAB_TOTAL_BALANCE
    private val transactionList = ArrayList<TransactionItem>()
    private lateinit var transactionAdp: TransactionAdp

    companion object {
        private const val TAB_TOTAL_BALANCE = 0
        private const val TAB_REFERRAL_WALLET = 1
        private const val TAB_MY_WALLET = 2
        private const val TAB_EARN = 3

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

        // TODO(PAYTOUCH-82): Fetch wallet balance from API and update tvWalletBalance
        // TODO(PAYTOUCH-82): Fetch virtual account details and populate card fields
        // TODO(PAYTOUCH-82): Fetch recent wallet transactions for rvTransactions
    }

    private fun setupRecyclerView() {
        transactionAdp = TransactionAdp(mActivity, transactionList)
        transactionAdp.onClickItem = {
            // TODO(PAYTOUCH-82): Navigate to wallet transaction detail
        }
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

        val activeColor = ContextCompat.getColor(mActivity, R.color.primary)
        val inactiveColor = android.graphics.Color.TRANSPARENT
        val activeTextColor = ContextCompat.getColor(mActivity, R.color.white)
        val inactiveTextColor = ContextCompat.getColor(mActivity, R.color.primary)

        listOf(
            binding.cvTabTotalBalance to binding.tvTabTotalBalance,
            binding.cvTabReferralWallet to binding.tvTabReferralWallet,
            binding.cvTabMyWallet to binding.tvTabMyWallet,
            binding.cvTabEarn to binding.tvTabEarn
        ).forEachIndexed { index, (card, text) ->
            val isSelected = index == tab
            card.setCardBackgroundColor(if (isSelected) activeColor else inactiveColor)
            text.setTextColor(if (isSelected) activeTextColor else inactiveTextColor)
        }
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
                binding.cvTabTotalBalance -> {
                    if (Utility.stopClick()) return@OnClickListener
                    selectTab(TAB_TOTAL_BALANCE)
                }
                binding.cvTabReferralWallet -> {
                    if (Utility.stopClick()) return@OnClickListener
                    selectTab(TAB_REFERRAL_WALLET)
                }
                binding.cvTabMyWallet -> {
                    if (Utility.stopClick()) return@OnClickListener
                    selectTab(TAB_MY_WALLET)
                }
                binding.cvTabEarn -> {
                    if (Utility.stopClick()) return@OnClickListener
                    selectTab(TAB_EARN)
                }
                binding.llMakePayment -> {
                    if (Utility.stopClick()) return@OnClickListener
                    // TODO(PAYTOUCH-82): Navigate to Make Payment flow
                }
                binding.llTransactionReport -> {
                    if (Utility.stopClick()) return@OnClickListener
                    // TODO(PAYTOUCH-82): Navigate to wallet transaction report screen
                }
            }
        }
    }
}
