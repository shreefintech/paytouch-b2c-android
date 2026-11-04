package com.shreefintech.paytouchconsumer.billpay.transactions

import android.content.Context
import android.content.Intent
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
import com.shreefintech.paytouchconsumer.Constant
import com.shreefintech.paytouchconsumer.R
import com.shreefintech.paytouchconsumer.adapter.TransactionAdp
import com.shreefintech.paytouchconsumer.billpay.viewmodel.BillPayTransactionsViewModel
import com.shreefintech.paytouchconsumer.databinding.ActivityTransactionReportBinding
import com.shreefintech.paytouchconsumer.enums.CategoryType
import com.shreefintech.paytouchconsumer.glass.LiquidGlassEffect
import com.shreefintech.paytouchconsumer.retrofit.model.billpay.BillPayTransactionItem
import com.shreefintech.paytouchconsumer.transactions.TransactionDetailActivity
import com.shreefintech.paytouchconsumer.transactions.model.TransactionItem
import com.shreefintech.paytouchconsumer.utill.ToastUtil
import com.shreefintech.paytouchconsumer.utill.TransactionFilterHelper
import com.shreefintech.paytouchconsumer.utill.Utility

/**
 * Transaction Report for one Bill Pay category — GET transactions?category=. Success / Failed go to the server as
 * `status`; Pending, the date range and the account number are filtered in the app.
 */
class TransactionReportActivity : BaseActivity() {

    private lateinit var binding: ActivityTransactionReportBinding
    private val viewModel: BillPayTransactionsViewModel by viewModels()
    private lateinit var transactionAdp: TransactionAdp
    private lateinit var filterHelper: TransactionFilterHelper

    private val category: CategoryType by lazy { CategoryType.from(intent.getStringExtra(EXTRA_CATEGORY)) }

    private val mAllList     = ArrayList<TransactionItem>()
    private val mDisplayList = ArrayList<TransactionItem>()

    // Applied filter — reused on each paginated page load (dates are yyyy-MM-dd from the filter sheet)
    private var filterFromDate:   String? = null
    private var filterToDate:     String? = null
    private var filterStatus:     String? = null
    private var filterConsumerNo: String? = null

    // Pages in a row that brought no matching rows (the next one is auto-loaded)
    private var emptyAutoPages = 0
    // A loaded page already reached rows older than filterFromDate — no later page can match
    private var isPastFromDate = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTransactionReportBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.clRoot) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime())
            v.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                maxOf(imeInsets.bottom, systemBars.bottom)
            )

            binding.incFilterSheet.root.setPadding(
                0,
                0,
                0,
                maxOf(imeInsets.bottom, systemBars.bottom)
            )
            insets
        }

        LiquidGlassEffect.attach(
            targetView = binding.flCard,
            rootView = binding.clRoot as ViewGroup,
            cornerRadius = resources.getDimensionPixelSize(R.dimen.glass_frem_radius),
            distortion = 0f,
            blur = resources.getDimensionPixelSize(R.dimen.glass_frem_blur),
            strokeColor = ContextCompat.getColor(mActivity, R.color.glass_stroke_primary),
            strokeWidth = 1,
            solidStroke = true,
        )

        setupRecyclerView()
        setupSearch()
        setupFilterSheet()

        binding.onClickListener = onClickListener()
        onBack()

        retryCallback = { loadPage(1) }
        callReport(null, null, null, null)
    }

    // ── Setup ─────────────────────────────────────────────────────────────────

    private fun setupRecyclerView() {
        transactionAdp = TransactionAdp(mActivity, mDisplayList)
        transactionAdp.onClickItem = { item ->
            TransactionDetailActivity.start(mActivity, item)
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
                if (lastVisible >= totalItems - 3 && canLoadNextPage()) {
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
            activity = mActivity,
            sheetBinding = binding.incFilterSheet,
            bgOverlay = binding.viewBg,
            onApply = { fromDate, toDate, status, consumerNo ->
                callReport(fromDate, toDate, status, consumerNo)
            },
            onClear = {
                binding.etSearch.setText("")
                callReport(null, null, null, null)
            },
            isMobileCategory = category.isMobileCategory,
            isVehicleCategory = category.isVehicleCategory
        )
        filterHelper.setup()
    }

    // ── Data Loading ──────────────────────────────────────────────────────────

    private fun callReport(fromDate: String?, toDate: String?, status: String?, consumerNo: String?) {
        filterFromDate   = fromDate
        filterToDate     = toDate
        filterStatus     = status?.lowercase()
        filterConsumerNo = consumerNo
        loadPage(1)
    }

    // success / failed are server filters; pending is not a server value here
    private fun serverStatus(): String? =
        filterStatus?.takeIf { it == Constant.BILLPAY_STATUS_SUCCESS || it == Constant.BILLPAY_STATUS_FAILED }

    private fun matchesLocalFilter(item: BillPayTransactionItem): Boolean {
        if (filterStatus == Constant.BILLPAY_STATUS_PENDING) {
            val status = item.status?.lowercase()
            if (status != Constant.BILLPAY_STATUS_PENDING && status != Constant.BILLPAY_STATUS_PROCESSING &&
                status != Constant.BILLPAY_STATUS_UNCONFIRMED
            ) return false
        }
        // created_at starts with yyyy-MM-dd, so plain string compare orders dates
        val day = item.createdAt?.take(10).orEmpty()
        filterFromDate?.let { if (day.isEmpty() || day < it) return false }
        filterToDate?.let { if (day.isEmpty() || day > it) return false }
        filterConsumerNo?.let { query ->
            if (item.inputParams.orEmpty().values.none { it.contains(query, ignoreCase = true) }) return false
        }
        return true
    }

    private fun canLoadNextPage() = !isPastFromDate && viewModel.canLoadMore()

    private fun loadPage(page: Int) {
        if (!Utility.isInternetAvailable(mActivity)) {
            if (page == 1) showNoInternet() else ToastUtil.showDelete(mActivity, getString(R.string.msgNoInternet))
            return
        }
        if (page == 1) hideNoInternet()
        viewModel.loadPage(
            category   = category,
            status     = serverStatus(),
            page       = page,
            onLoading  = {
                if (page == 1) showShimmer(true) else showFooterLoader(true)
            },
            onSuccess  = { rows ->
                if (isDestroyed) return@loadPage
                val list = ArrayList(rows.filter { matchesLocalFilter(it) }.map { viewModel.mapToTransactionItem(it, category) })
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
                // Rows are newest first — once a page reaches past the From date, older pages can't match
                if (page == 1) isPastFromDate = false
                filterFromDate?.let { from ->
                    val oldestDay = rows.lastOrNull()?.createdAt?.take(10)
                    if (oldestDay != null && oldestDay < from) isPastFromDate = true
                }
                // Filtered in the app — keep paging while a page brought no matching rows, up to a cap
                // TODO(B2C-309): fixed cap; move date / account filters server-side when the API takes them
                if (page == 1) emptyAutoPages = 0
                emptyAutoPages = if (list.isEmpty()) emptyAutoPages + 1 else 0
                if (emptyAutoPages in 1 until BillPayTransactionsViewModel.MAX_AUTO_PAGES && canLoadNextPage()) {
                    loadPage(viewModel.nextPage())
                }
            },
            onError    = { msg ->
                if (isDestroyed) return@loadPage
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

    // ── UI Helpers ────────────────────────────────────────────────────────────

    private fun showShimmer(show: Boolean) {
        if (show) {
            binding.rvTransactions.visibility = View.GONE
            binding.tvEmpty.visibility = View.GONE
            binding.shimmerLayout.visibility = View.VISIBLE
            binding.shimmerLayout.startShimmer()
        } else {
            binding.shimmerLayout.stopShimmer()
            binding.shimmerLayout.visibility = View.GONE
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

    private fun appendToDisplayList(newItems: List<TransactionItem>) {
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

    // ── Navigation ────────────────────────────────────────────────────────────

    private fun onBack() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (filterHelper.isVisible()) {
                    filterHelper.hide()
                } else {
                    finish()
                }
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

    companion object {
        private const val EXTRA_CATEGORY = "extra_category"

        fun start(context: Context, category: CategoryType) {
            context.startActivity(
                Intent(context, TransactionReportActivity::class.java).putExtra(EXTRA_CATEGORY, category.value)
            )
        }
    }
}
