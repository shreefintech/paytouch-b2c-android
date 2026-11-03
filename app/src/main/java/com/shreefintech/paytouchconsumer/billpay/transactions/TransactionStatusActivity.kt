package com.shreefintech.paytouchconsumer.billpay.transactions

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.shreefintech.paytouchconsumer.BaseActivity
import com.shreefintech.paytouchconsumer.Constant
import com.shreefintech.paytouchconsumer.R
import com.shreefintech.paytouchconsumer.adapter.TransactionAdp
import com.shreefintech.paytouchconsumer.billpay.viewmodel.BillPayTransactionsViewModel
import com.shreefintech.paytouchconsumer.databinding.ActivityTransactionStatusBinding
import com.shreefintech.paytouchconsumer.enums.CategoryType
import com.shreefintech.paytouchconsumer.glass.LiquidGlassEffect
import com.shreefintech.paytouchconsumer.retrofit.model.billpay.BillPayTransactionItem
import com.shreefintech.paytouchconsumer.transactions.TransactionDetailActivity
import com.shreefintech.paytouchconsumer.transactions.model.TransactionItem
import com.shreefintech.paytouchconsumer.utill.ToastUtil
import com.shreefintech.paytouchconsumer.utill.Utility

/**
 * Transaction Status for one Bill Pay category — GET transactions?category=. Filter tabs: Success / Failed go to the
 * server as `status`; Pending (pending, processing, unconfirmed) is filtered in the app.
 */
class TransactionStatusActivity : BaseActivity() {

    private lateinit var binding: ActivityTransactionStatusBinding
    private val viewModel: BillPayTransactionsViewModel by viewModels()
    private lateinit var transactionAdp: TransactionAdp

    private val category: CategoryType by lazy { CategoryType.from(intent.getStringExtra(EXTRA_CATEGORY)) }

    private val mArrayList = ArrayList<TransactionItem>()
    private var selectedTab = Tab.ALL

    private enum class Tab(val serverStatus: String?) {
        ALL(null),
        SUCCESS(Constant.BILLPAY_STATUS_SUCCESS),
        FAILED(Constant.BILLPAY_STATUS_FAILED),
        PENDING(null)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTransactionStatusBinding.inflate(layoutInflater)
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
        updateTabs()
        onBack()

        retryCallback = { loadPage(1) }
        loadPage(1)
    }

    // ── Setup ─────────────────────────────────────────────────────────────────

    private fun setupRecyclerView() {
        transactionAdp = TransactionAdp(mActivity, mArrayList)
        transactionAdp.onClickItem = { item ->
            TransactionDetailActivity.start(mActivity, item)
        }
        binding.rvTransactions.apply {
            layoutManager = LinearLayoutManager(mActivity)
            adapter       = transactionAdp
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

    private fun onTabSelected(tab: Tab) {
        if (tab == selectedTab) return
        selectedTab = tab
        updateTabs()
        loadPage(1)
    }

    private fun updateTabs() {
        listOf(
            Triple(binding.cvStatusAll, binding.tvStatusAll, Tab.ALL),
            Triple(binding.cvStatusSuccess, binding.tvStatusSuccess, Tab.SUCCESS),
            Triple(binding.cvStatusFailed, binding.tvStatusFailed, Tab.FAILED),
            Triple(binding.cvStatusPending, binding.tvStatusPending, Tab.PENDING)
        ).forEach { (card: MaterialCardView, text: AppCompatTextView, tab) ->
            val selected = tab == selectedTab
            card.setCardBackgroundColor(
                ContextCompat.getColor(mActivity, if (selected) R.color.primary else R.color.filter_status_bg)
            )
            text.setTextColor(ContextCompat.getColor(mActivity, if (selected) R.color.white else R.color.primary))
        }
    }

    // ── Data Loading ──────────────────────────────────────────────────────────

    private fun loadPage(page: Int) {
        if (!Utility.isInternetAvailable(mActivity)) {
            if (page == 1) showNoInternet() else ToastUtil.showDelete(mActivity, getString(R.string.msgNoInternet))
            return
        }
        if (page == 1) hideNoInternet()
        val tab = selectedTab
        viewModel.loadPage(
            category  = category,
            status    = tab.serverStatus,
            page      = page,
            onLoading = {
                if (page == 1) showShimmer(true) else showFooterLoader(true)
            },
            onSuccess = { rows ->
                if (isDestroyed || tab != selectedTab) return@loadPage
                val list = rows.filter { tab != Tab.PENDING || it.isOpen() }
                    .map { viewModel.mapToTransactionItem(it, category) }
                if (page == 1) {
                    showShimmer(false)
                    mArrayList.clear()
                    mArrayList.addAll(list)
                    transactionAdp.notifyDataSetChanged()
                } else {
                    showFooterLoader(false)
                    val insertStart = mArrayList.size
                    mArrayList.addAll(list)
                    transactionAdp.notifyItemRangeInserted(insertStart, list.size)
                }
                binding.tvEmpty.visibility = if (mArrayList.isEmpty()) View.VISIBLE else View.GONE
                // Pending is filtered here — keep paging while a page brought no visible rows
                if (list.isEmpty() && tab == Tab.PENDING && viewModel.canLoadMore()) loadPage(viewModel.nextPage())
            },
            onError   = { msg ->
                if (isDestroyed) return@loadPage
                if (page == 1) {
                    showShimmer(false)
                    mArrayList.clear()
                    transactionAdp.notifyDataSetChanged()
                    binding.tvEmpty.visibility = View.VISIBLE
                } else {
                    showFooterLoader(false)
                }
                if (msg.isNotEmpty()) ToastUtil.showDelete(mActivity, msg)
            }
        )
    }

    private fun BillPayTransactionItem.isOpen(): Boolean = when (status?.lowercase()) {
        Constant.BILLPAY_STATUS_PENDING, Constant.BILLPAY_STATUS_PROCESSING, Constant.BILLPAY_STATUS_UNCONFIRMED -> true
        else -> false
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

    // ── Navigation ────────────────────────────────────────────────────────────

    private fun onBack() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() { finish() }
        })
    }

    private fun onClickListener(): View.OnClickListener {
        return View.OnClickListener { view ->
            when (view) {
                binding.lytToolbar.ivBack -> {
                    if (Utility.stopClick()) return@OnClickListener
                    finish()
                }
                binding.cvStatusAll -> {
                    if (Utility.stopClick()) return@OnClickListener
                    onTabSelected(Tab.ALL)
                }
                binding.cvStatusSuccess -> {
                    if (Utility.stopClick()) return@OnClickListener
                    onTabSelected(Tab.SUCCESS)
                }
                binding.cvStatusFailed -> {
                    if (Utility.stopClick()) return@OnClickListener
                    onTabSelected(Tab.FAILED)
                }
                binding.cvStatusPending -> {
                    if (Utility.stopClick()) return@OnClickListener
                    onTabSelected(Tab.PENDING)
                }
            }
        }
    }

    // ── Intent / Companion ────────────────────────────────────────────────────

    companion object {
        private const val EXTRA_CATEGORY = "extra_category"

        fun start(context: Context, category: CategoryType) {
            context.startActivity(
                Intent(context, TransactionStatusActivity::class.java).putExtra(EXTRA_CATEGORY, category.value)
            )
        }
    }
}
