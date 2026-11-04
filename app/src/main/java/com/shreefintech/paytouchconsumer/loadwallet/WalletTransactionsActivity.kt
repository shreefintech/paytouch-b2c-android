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
import com.shreefintech.paytouchconsumer.adapter.WalletTransactionAdp
import com.shreefintech.paytouchconsumer.databinding.ActivityWalletTransactionsBinding
import com.shreefintech.paytouchconsumer.glass.LiquidGlassEffect
import com.shreefintech.paytouchconsumer.loadwallet.model.WalletHistoryTab
import com.shreefintech.paytouchconsumer.loadwallet.model.WalletTransactionItem
import com.shreefintech.paytouchconsumer.loadwallet.viewmodel.WalletTransactionsViewModel
import com.shreefintech.paytouchconsumer.transactions.TransactionHistoryDetailActivity
import com.shreefintech.paytouchconsumer.utill.ToastUtil
import com.shreefintech.paytouchconsumer.utill.Utility

// My Wallet → View All for one history sub tab (20 rows a page)
class WalletTransactionsActivity : BaseActivity() {

    private lateinit var binding: ActivityWalletTransactionsBinding
    private val walletTxVm: WalletTransactionsViewModel by viewModels()

    private val transactionList = ArrayList<WalletTransactionItem>()
    private lateinit var transactionAdp: WalletTransactionAdp

    private val tab by lazy {
        WalletHistoryTab.entries.firstOrNull { it.name == intent.getStringExtra(EXTRA_TAB) } ?: WalletHistoryTab.MAIN
    }

    companion object {
        private const val EXTRA_TAB = "extra_tab"

        fun start(context: Context, tab: WalletHistoryTab) {
            context.startActivity(
                Intent(context, WalletTransactionsActivity::class.java).apply {
                    putExtra(EXTRA_TAB, tab.name)
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

        binding.tvTitle.text = getString(R.string.titleWalletHistory, getString(tab.labelRes))
        binding.onClickListener = onClickListener()
        setupAdapter()
        retryCallback = { loadPage(1) }
        loadPage(1)
    }

    // ── Setup ─────────────────────────────────────────────────────────────────

    private fun setupAdapter() {
        transactionAdp = WalletTransactionAdp(mActivity, transactionList)
        transactionAdp.onClickItem = { row ->
            if (!Utility.stopClick()) {
                val detail = row.detail
                if (detail != null) TransactionHistoryDetailActivity.start(mActivity, detail)
                else TransactionHistoryDetailActivity.start(mActivity, row.transactionId)
            }
        }

        binding.rvTransactions.apply {
            layoutManager = LinearLayoutManager(mActivity)
            adapter = transactionAdp
        }

        binding.rvTransactions.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (dy <= 0) return
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
        if (!Utility.isInternetAvailable(mActivity)) {
            if (page == 1) showNoInternet() else ToastUtil.showDelete(mActivity, getString(R.string.msgNoInternet))
            return
        }
        if (page == 1) hideNoInternet()
        walletTxVm.loadHistory(
            tab       = tab,
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
        val empty = transactionList.isEmpty()
        binding.tvEmpty.visibility = if (empty) View.VISIBLE else View.GONE
        binding.rvTransactions.visibility = if (empty) View.GONE else View.VISIBLE
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
