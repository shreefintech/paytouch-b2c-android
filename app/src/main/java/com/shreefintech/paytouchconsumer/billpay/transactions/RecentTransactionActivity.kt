package com.shreefintech.paytouchconsumer.billpay.transactions

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
import com.shreefintech.paytouchconsumer.adapter.RecentTransactionAdp
import com.shreefintech.paytouchconsumer.billpay.viewmodel.BillPayTransactionsViewModel
import com.shreefintech.paytouchconsumer.databinding.ActivityRecentTransactionBinding
import com.shreefintech.paytouchconsumer.enums.CategoryType
import com.shreefintech.paytouchconsumer.glass.LiquidGlassEffect
import com.shreefintech.paytouchconsumer.utill.ToastUtil
import com.shreefintech.paytouchconsumer.utill.Utility

// Recent Transactions for one Bill Pay category — GET transactions?category=
class RecentTransactionActivity : BaseActivity() {

    private lateinit var binding: ActivityRecentTransactionBinding
    private lateinit var recentTransactionAdp: RecentTransactionAdp
    private val viewModel: BillPayTransactionsViewModel by viewModels()

    private val category: CategoryType by lazy { CategoryType.from(intent.getStringExtra(EXTRA_CATEGORY)) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRecentTransactionBinding.inflate(layoutInflater)
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

        setupRecyclerView()

        binding.onClickListener = onClickListener()
        onBack()

        retryCallback = { loadPage(1) }
        loadPage(1)
    }

    private fun setupRecyclerView() {
        recentTransactionAdp = RecentTransactionAdp(mActivity, ArrayList())
        binding.rvTransactions.apply {
            layoutManager = LinearLayoutManager(mActivity)
            adapter = recentTransactionAdp
        }
        binding.rvTransactions.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (dy <= 0) return
                val lm          = recyclerView.layoutManager as LinearLayoutManager
                val lastVisible = lm.findLastVisibleItemPosition()
                val total       = recentTransactionAdp.itemCount
                if (total > 0 && lastVisible >= total - 3 && viewModel.canLoadMore()) {
                    loadPage(viewModel.nextPage())
                }
            }
        })
    }

    private fun loadPage(page: Int) {
        if (!Utility.isInternetAvailable(mActivity)) {
            if (page == 1) showNoInternet() else ToastUtil.showDelete(mActivity, getString(R.string.msgNoInternet))
            return
        }
        if (page == 1) hideNoInternet()
        viewModel.loadPage(
            category = category,
            status = null,
            page = page,
            onLoading = {
                if (page == 1) {
                    binding.shimmerLayout.visibility = View.VISIBLE
                    binding.shimmerLayout.startShimmer()
                    binding.rvTransactions.visibility = View.GONE
                    binding.tvEmpty.visibility        = View.GONE
                } else {
                    binding.pbLoadMore.visibility = View.VISIBLE
                }
            },
            onSuccess = { rows ->
                if (isDestroyed) return@loadPage
                val items = rows.map { viewModel.mapToDisplayItem(it, category) }
                if (page == 1) {
                    binding.shimmerLayout.stopShimmer()
                    binding.shimmerLayout.visibility = View.GONE
                    if (items.isEmpty()) {
                        binding.tvEmpty.visibility = View.VISIBLE
                    } else {
                        binding.rvTransactions.visibility = View.VISIBLE
                        recentTransactionAdp.updateList(items)
                    }
                } else {
                    binding.pbLoadMore.visibility = View.GONE
                    if (items.isNotEmpty()) recentTransactionAdp.appendList(items)
                }
            },
            onError = { error ->
                if (isDestroyed) return@loadPage
                if (page == 1) {
                    binding.shimmerLayout.stopShimmer()
                    binding.shimmerLayout.visibility = View.GONE
                    binding.tvEmpty.visibility       = View.VISIBLE
                } else {
                    binding.pbLoadMore.visibility = View.GONE
                }
                ToastUtil.showDelete(mActivity, error)
            }
        )
    }

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
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        }
    }

    companion object {
        private const val EXTRA_CATEGORY = "extra_category"

        fun start(context: Context, category: CategoryType) {
            context.startActivity(
                Intent(context, RecentTransactionActivity::class.java).putExtra(EXTRA_CATEGORY, category.value)
            )
        }
    }
}
