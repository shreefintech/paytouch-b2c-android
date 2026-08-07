package com.shreefintech.paytouchconsumer.loadwallet

import android.content.Context
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
import androidx.recyclerview.widget.RecyclerView
import com.shreefintech.paytouchconsumer.BaseActivity
import com.shreefintech.paytouchconsumer.R
import com.shreefintech.paytouchconsumer.adapter.WalletTransactionAdp
import com.shreefintech.paytouchconsumer.databinding.ActivityWalletTransactionsBinding
import com.shreefintech.paytouchconsumer.glass.LiquidGlassEffect
import com.shreefintech.paytouchconsumer.loadwallet.model.WalletTransactionItem
import com.shreefintech.paytouchconsumer.loadwallet.viewmodel.WalletTransactionsViewModel
import com.shreefintech.paytouchconsumer.utill.ToastUtil
import com.shreefintech.paytouchconsumer.utill.Utility

class WalletTransactionsActivity : BaseActivity() {

    private lateinit var binding: ActivityWalletTransactionsBinding
    private val viewModel: WalletTransactionsViewModel by viewModels()

    private val transactionList = ArrayList<WalletTransactionItem>()
    private lateinit var transactionAdp: WalletTransactionAdp

    companion object {
        fun start(context: Context) {
            context.startActivity(Intent(context, WalletTransactionsActivity::class.java))
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

        binding.onClickListener = onClickListener()
        setupRecyclerView()
        onBack()
        loadPage(1)
    }

    // ── Setup ─────────────────────────────────────────────────────────────────

    private fun setupRecyclerView() {
        transactionAdp = WalletTransactionAdp(mActivity, transactionList)
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
                if (lastVisible >= totalItems - 3 && viewModel.canLoadMore()) {
                    loadPage(viewModel.nextPage())
                }
            }
        })
    }

    // ── Data Loading ──────────────────────────────────────────────────────────

    private fun loadPage(page: Int) {
        viewModel.loadHistory(
            page      = page,
            onLoading = {
                if (page == 1) showShimmer(true) else showFooterLoader(true)
            },
            onSuccess = { list ->
                if (page == 1) {
                    showShimmer(false)
                    transactionList.clear()
                    transactionList.addAll(list)
                    transactionAdp.notifyDataSetChanged()
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
        binding.tvEmpty.visibility = if (transactionList.isEmpty()) View.VISIBLE else View.GONE
        if (transactionList.isEmpty()) binding.rvTransactions.visibility = View.GONE
    }

    // ── Navigation ────────────────────────────────────────────────────────────

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
