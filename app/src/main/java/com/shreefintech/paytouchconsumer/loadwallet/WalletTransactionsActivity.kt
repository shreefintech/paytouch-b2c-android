package com.shreefintech.paytouchconsumer.loadwallet

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.shreefintech.paytouchconsumer.BaseActivity
import com.shreefintech.paytouchconsumer.R
import com.shreefintech.paytouchconsumer.adapter.ReferralHistoryAdp
import com.shreefintech.paytouchconsumer.adapter.WalletTransactionAdp
import com.shreefintech.paytouchconsumer.databinding.ActivityWalletTransactionsBinding
import com.shreefintech.paytouchconsumer.glass.LiquidGlassEffect
import com.shreefintech.paytouchconsumer.loadwallet.model.WalletTransactionItem
import com.shreefintech.paytouchconsumer.loadwallet.viewmodel.LoadWalletViewModel
import com.shreefintech.paytouchconsumer.loadwallet.viewmodel.WalletTransactionsViewModel
import com.shreefintech.paytouchconsumer.retrofit.model.wallet.BonusWalletHistoryItem
import com.shreefintech.paytouchconsumer.transactions.TransactionHistoryDetailActivity
import com.shreefintech.paytouchconsumer.utill.ToastUtil
import com.shreefintech.paytouchconsumer.utill.Utility

class WalletTransactionsActivity : BaseActivity() {

    private lateinit var binding: ActivityWalletTransactionsBinding
    private val walletTxVm: WalletTransactionsViewModel by viewModels()
    private val loadWalletVm: LoadWalletViewModel by viewModels()

    private val transactionList = ArrayList<WalletTransactionItem>()
    private lateinit var transactionAdp: WalletTransactionAdp

    private val referralList = ArrayList<BonusWalletHistoryItem>()
    private lateinit var referralAdp: ReferralHistoryAdp

    private val tab by lazy { intent.getStringExtra(EXTRA_TAB) ?: TAB_WALLET }

    companion object {
        const val TAB_WALLET   = "wallet"
        const val TAB_REFERRAL = "referral"
        private const val EXTRA_TAB = "extra_tab"

        fun start(context: Context, tab: String = TAB_WALLET) {
            context.startActivity(
                Intent(context, WalletTransactionsActivity::class.java).apply {
                    putExtra(EXTRA_TAB, tab)
                }
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWalletTransactionsBinding.inflate(layoutInflater)
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

        binding.tvTitle.text = getString(
            if (tab == TAB_REFERRAL) R.string.titleReferralHistory else R.string.titleTransactionReport
        )
        binding.onClickListener = onClickListener()
        setupAdapters()
        retryCallback = { loadPage(1) }
        loadPage(1)
    }

    // ── Setup ─────────────────────────────────────────────────────────────────

    private fun setupAdapters() {
        transactionAdp = WalletTransactionAdp(mActivity, transactionList)
        transactionAdp.onClickItem = { transactionId ->
            if (!Utility.stopClick()) TransactionHistoryDetailActivity.start(mActivity, transactionId)
        }
        referralAdp = ReferralHistoryAdp(mActivity, referralList)

        binding.rvTransactions.apply {
            layoutManager = LinearLayoutManager(mActivity)
            adapter = if (tab == TAB_REFERRAL) referralAdp else transactionAdp
        }

        binding.rvTransactions.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (tab != TAB_WALLET || dy <= 0) return
                val lm          = recyclerView.layoutManager as LinearLayoutManager
                val lastVisible = lm.findLastVisibleItemPosition()
                val totalItems  = lm.itemCount
                if (lastVisible >= totalItems - 3 && walletTxVm.canLoadMore()) {
                    loadPage(walletTxVm.nextPage())
                }
            }
        })
    }

    // ── Data Loading ──────────────────────────────────────────────────────────

    private fun loadPage(page: Int) {
        if (tab == TAB_REFERRAL) {
            loadReferrals()
            return
        }
        if (!Utility.isInternetAvailable(mActivity)) {
            if (page == 1) showNoInternet() else ToastUtil.showDelete(mActivity, getString(R.string.msgNoInternet))
            return
        }
        if (page == 1) hideNoInternet()
        walletTxVm.loadHistory(
            page      = page,
            onLoading = { if (page == 1) showShimmer(true) else showFooterLoader(true) },
            onSuccess = { list ->
                if (page == 1) {
                    showShimmer(false)
                    transactionAdp.updateList(list)
                } else {
                    showFooterLoader(false)
                    val insertStart = transactionList.size
                    transactionList.addAll(list)
                    transactionAdp.notifyItemRangeInserted(insertStart, list.size)
                }
                updateEmptyState()
            },
            onError   = { msg ->
                if (page == 1) {
                    showShimmer(false)
                    transactionList.clear()
                    transactionAdp.notifyDataSetChanged()
                } else {
                    showFooterLoader(false)
                }
                updateEmptyState()
                if (msg.isNotEmpty()) ToastUtil.showDelete(mActivity, msg)
            }
        )
    }

    private fun loadReferrals() {
        if (!Utility.isInternetAvailable(mActivity)) { showNoInternet(); return }
        hideNoInternet()
        showShimmer(true)
        loadWalletVm.fetchBonusWallet(
            onSuccess = { data ->
                showShimmer(false)
                referralAdp.updateList((data.history ?: emptyList()).filter { it.isReferral })
                updateEmptyState()
            },
            onError = { msg ->
                showShimmer(false)
                referralAdp.updateList(emptyList())
                updateEmptyState()
                if (msg.isNotEmpty()) ToastUtil.showDelete(mActivity, msg)
            }
        )
    }

    // ── UI Helpers ────────────────────────────────────────────────────────────

    private fun showShimmer(show: Boolean) {
        if (show) {
            binding.rvTransactions.visibility = View.GONE
            binding.tvEmpty.visibility        = View.GONE
            binding.shimmerLayout.visibility  = View.VISIBLE
            binding.shimmerLayout.startShimmer()
        } else {
            binding.shimmerLayout.stopShimmer()
            binding.shimmerLayout.visibility  = View.GONE
            binding.rvTransactions.visibility = View.VISIBLE
        }
    }

    private fun showFooterLoader(show: Boolean) {
        binding.pbLoadMore.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun updateEmptyState() {
        val empty = if (tab == TAB_REFERRAL) referralList.isEmpty() else transactionList.isEmpty()
        binding.tvEmpty.visibility = if (empty) View.VISIBLE else View.GONE
        if (empty) binding.rvTransactions.visibility = View.GONE
    }

    // ── Navigation ────────────────────────────────────────────────────────────

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
