package com.shreefintech.paytouchconsumer.loan.transactions

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.databinding.ObservableBoolean
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.shreefintech.paytouchconsumer.BaseActivity
import com.shreefintech.paytouchconsumer.R
import com.shreefintech.paytouchconsumer.adapter.TransactionAdp
import com.shreefintech.paytouchconsumer.databinding.ActivityLoanTransactionStatusBinding
import com.shreefintech.paytouchconsumer.glass.LiquidGlassEffect
import com.shreefintech.paytouchconsumer.loan.viewmodel.LoanTransactionStatusViewModel
import com.shreefintech.paytouchconsumer.transactions.TransactionDetailActivity
import com.shreefintech.paytouchconsumer.transactions.model.TransactionItem
import com.shreefintech.paytouchconsumer.utill.ToastUtil
import com.shreefintech.paytouchconsumer.utill.Utility

class LoanTransactionStatusActivity : BaseActivity() {

    private lateinit var binding: ActivityLoanTransactionStatusBinding
    private val viewModel: LoanTransactionStatusViewModel by viewModels()
    private lateinit var transactionAdp: TransactionAdp

    private val mArrayList         = ArrayList<TransactionItem>()
    private val showProgressSearch = ObservableBoolean(false)

    private var activeQuery: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoanTransactionStatusBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.clRoot) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val imeInsets  = insets.getInsets(WindowInsetsCompat.Type.ime())
            v.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                maxOf(imeInsets.bottom, systemBars.bottom)
            )
            insets
        }

        LiquidGlassEffect.attach(
            targetView   = binding.flCard,
            rootView     = binding.clRoot as ViewGroup,
            cornerRadius = resources.getDimensionPixelSize(R.dimen.glass_frem_radius),
            distortion   = 0f,
            blur         = resources.getDimensionPixelSize(R.dimen.glass_frem_blur),
            strokeColor  = Color.argb(180, 213, 38, 98),
            strokeWidth  = 1,
            solidStroke  = true,
        )

        binding.showProgressSearch = showProgressSearch
        binding.onClickListener    = onClickListener()

        binding.etSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) { onSearch(); true } else false
        }

        setupRecyclerView()
        onBack()

        loadPage(1)
    }

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

    private fun onSearch() {
        val query = binding.etSearch.text?.toString()?.trim() ?: ""
        if (query.isEmpty()) {
            ToastUtil.showDelete(mActivity, getString(R.string.msgEnterSearchQuery))
            return
        }
        Utility.hideKeyboard(mActivity)
        activeQuery = query
        loadPage(1)
    }

    private fun loadPage(page: Int) {
        viewModel.loadStatus(
            query     = activeQuery,
            page      = page,
            onLoading = {
                if (page == 1) showShimmer(true) else showFooterLoader(true)
            },
            onSuccess = { list ->
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
            },
            onError   = { msg ->
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

    private fun showShimmer(show: Boolean) {
        showProgressSearch.set(show)
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
                binding.llSearch -> {
                    if (Utility.stopClick()) return@OnClickListener
                    if (showProgressSearch.get()) return@OnClickListener
                    onSearch()
                }
            }
        }
    }
}
