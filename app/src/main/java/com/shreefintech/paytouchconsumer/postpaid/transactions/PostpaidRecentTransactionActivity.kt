package com.shreefintech.paytouchconsumer.postpaid.transactions

import android.content.Context
import android.content.Intent
import android.graphics.Color
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
import com.shreefintech.paytouchconsumer.databinding.ActivityPostpaidRecentTransactionBinding
import com.shreefintech.paytouchconsumer.glass.LiquidGlassEffect
import com.shreefintech.paytouchconsumer.postpaid.viewmodel.PostpaidRecentTransactionViewModel
import com.shreefintech.paytouchconsumer.utill.ToastUtil
import com.shreefintech.paytouchconsumer.utill.Utility

class PostpaidRecentTransactionActivity : BaseActivity() {

    companion object {
        fun start(context: Context) {
            context.startActivity(Intent(context, PostpaidRecentTransactionActivity::class.java))
        }
    }

    private lateinit var binding: ActivityPostpaidRecentTransactionBinding
    private lateinit var recentTransactionAdp: RecentTransactionAdp
    private val viewModel: PostpaidRecentTransactionViewModel by viewModels()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPostpaidRecentTransactionBinding.inflate(layoutInflater)
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
        loadInitialData()
        // TODO(PAYTOUCH-570): Add showNoInternet() / hideNoInternet() / setNoInternetRetryCallback { loadInitialData() }
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
                if (total > 0 && lastVisible >= total - 3) {
                    loadNextPage()
                }
            }
        })
    }

    // TODO(PAYTOUCH-570): Add showNoInternet() / hideNoInternet() / setNoInternetRetryCallback { loadInitialData() }
    //  once the no-internet placeholder design is finalised.
    private fun loadInitialData() {
        viewModel.loadOperatorsThenData(
            onLoading = {
                binding.shimmerLayout.visibility = View.VISIBLE
                binding.shimmerLayout.startShimmer()
                binding.rvTransactions.visibility = View.GONE
                binding.tvEmpty.visibility        = View.GONE
            },
            onSuccess = { items ->
                binding.shimmerLayout.stopShimmer()
                binding.shimmerLayout.visibility = View.GONE
                if (items.isEmpty()) {
                    binding.tvEmpty.visibility = View.VISIBLE
                } else {
                    binding.rvTransactions.visibility = View.VISIBLE
                    recentTransactionAdp.updateList(items)
                }
            },
            onError = { error ->
                binding.shimmerLayout.stopShimmer()
                binding.shimmerLayout.visibility = View.GONE
                binding.tvEmpty.visibility       = View.VISIBLE
                ToastUtil.showDelete(mActivity, error)
            }
        )
    }

    private fun loadNextPage() {
        viewModel.loadNextPage(
            onLoading = {
                binding.pbLoadMore.visibility = View.VISIBLE
            },
            onSuccess = { items ->
                binding.pbLoadMore.visibility = View.GONE
                if (items.isNotEmpty()) {
                    recentTransactionAdp.appendList(items)
                }
            },
            onError = { error ->
                binding.pbLoadMore.visibility = View.GONE
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
}
