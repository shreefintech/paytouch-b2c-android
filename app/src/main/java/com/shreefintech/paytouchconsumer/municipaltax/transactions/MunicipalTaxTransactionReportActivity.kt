package com.shreefintech.paytouchconsumer.municipaltax.transactions

import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
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
import com.shreefintech.paytouchconsumer.adapter.TransactionAdp
import com.shreefintech.paytouchconsumer.databinding.ActivityMunicipalTaxTransactionReportBinding
import com.shreefintech.paytouchconsumer.glass.LiquidGlassEffect
import com.shreefintech.paytouchconsumer.municipaltax.viewmodel.MunicipalTaxTransactionReportViewModel
import com.shreefintech.paytouchconsumer.transactions.TransactionDetailActivity
import com.shreefintech.paytouchconsumer.transactions.model.TransactionItem
import com.shreefintech.paytouchconsumer.utill.ToastUtil
import com.shreefintech.paytouchconsumer.utill.TransactionFilterHelper
import com.shreefintech.paytouchconsumer.utill.Utility

class MunicipalTaxTransactionReportActivity : BaseActivity() {

    private lateinit var binding: ActivityMunicipalTaxTransactionReportBinding
    private val viewModel: MunicipalTaxTransactionReportViewModel by viewModels()
    private lateinit var transactionAdp: TransactionAdp
    private lateinit var filterHelper: TransactionFilterHelper

    private val mAllList     = ArrayList<TransactionItem>()
    private val mDisplayList = ArrayList<TransactionItem>()

    private var filterFromDate:   String? = null
    private var filterToDate:     String? = null
    private var filterStatus:     String? = null
    private var filterSubscriberNo: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMunicipalTaxTransactionReportBinding.inflate(layoutInflater)
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
            binding.incFilterSheet.root.setPadding(0, 0, 0, maxOf(imeInsets.bottom, systemBars.bottom))
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
        setupSearch()
        setupFilterSheet()
        binding.onClickListener = onClickListener()
        onBack()
        callReport(null, null, null, null)
    }

    private fun setupRecyclerView() {
        transactionAdp = TransactionAdp(mActivity, mDisplayList)
        transactionAdp.onClickItem = { item -> TransactionDetailActivity.start(mActivity, item) }
        binding.rvTransactions.apply {
            layoutManager = LinearLayoutManager(mActivity)
            adapter       = transactionAdp
        }
        binding.rvTransactions.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (dy <= 0) return
                val lm          = recyclerView.layoutManager as LinearLayoutManager
                val lastVisible = lm.findLastVisibleItemPosition()
                if (lastVisible >= lm.itemCount - 3 && viewModel.canLoadMore()) {
                    loadPage(viewModel.nextPage())
                }
            }
        })
    }

    private fun setupSearch() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                filterList(s?.toString()?.trim() ?: "")
            }
        })
    }

    private fun setupFilterSheet() {
        filterHelper = TransactionFilterHelper(
            activity     = mActivity,
            sheetBinding = binding.incFilterSheet,
            bgOverlay    = binding.viewBg,
            onApply      = { fromDate, toDate, status, mobileNo ->
                callReport(fromDate, toDate, status, mobileNo)
            },
            onClear      = {
                binding.etSearch.setText("")
                callReport(null, null, null, null)
            }
        )
        filterHelper.setup()
    }

    private fun callReport(fromDate: String?, toDate: String?, status: String?, subscriberNo: String?) {
        filterFromDate    = fromDate
        filterToDate      = toDate
        filterStatus      = status
        filterSubscriberNo = subscriberNo
        loadPage(1)
    }

    private fun loadPage(page: Int) {
        viewModel.loadReport(
            fromDate     = filterFromDate,
            toDate       = filterToDate,
            status       = filterStatus,
            subscriberNo = filterSubscriberNo,
            page         = page,
            onLoading    = { if (page == 1) showShimmer(true) else showFooterLoader(true) },
            onSuccess    = { list ->
                if (page == 1) {
                    showShimmer(false)
                    mAllList.clear()
                    mAllList.addAll(list)
                    filterList(currentQuery())
                } else {
                    showFooterLoader(false)
                    mAllList.addAll(list)
                    appendToDisplayList(list)
                }
            },
            onError = { msg ->
                if (page == 1) {
                    showShimmer(false)
                    mAllList.clear()
                    filterList(currentQuery())
                } else {
                    showFooterLoader(false)
                }
                if (msg.isNotEmpty()) ToastUtil.showDelete(mActivity, msg)
            }
        )
    }

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

    private fun filterList(query: String) {
        mDisplayList.clear()
        if (query.isEmpty()) {
            mDisplayList.addAll(mAllList)
        } else {
            val lower = query.lowercase()
            mAllList.filterTo(mDisplayList) {
                it.mobileNumber.lowercase().contains(lower) ||
                        it.transactionId.lowercase().contains(lower)
            }
        }
        transactionAdp.notifyDataSetChanged()
        binding.tvEmpty.visibility = if (mDisplayList.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun appendToDisplayList(newItems: ArrayList<TransactionItem>) {
        val query       = currentQuery()
        val insertStart = mDisplayList.size
        val toAdd: List<TransactionItem> = if (query.isEmpty()) {
            newItems
        } else {
            val lower = query.lowercase()
            newItems.filter {
                it.mobileNumber.lowercase().contains(lower) ||
                        it.transactionId.lowercase().contains(lower)
            }
        }
        if (toAdd.isNotEmpty()) {
            mDisplayList.addAll(toAdd)
            transactionAdp.notifyItemRangeInserted(insertStart, toAdd.size)
        }
        binding.tvEmpty.visibility = if (mDisplayList.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun currentQuery() = binding.etSearch.text?.toString()?.trim() ?: ""

    private fun onBack() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (filterHelper.isVisible()) filterHelper.hide() else finish()
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
                binding.ivFilter -> {
                    if (Utility.stopClick()) return@OnClickListener
                    filterHelper.show()
                }
            }
        }
    }
}
