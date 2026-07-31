package com.shreefintech.paytouchconsumer.electricity.transactions

import android.content.Context
import android.content.Intent
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
import com.shreefintech.paytouchconsumer.BaseActivity
import com.shreefintech.paytouchconsumer.R
import com.shreefintech.paytouchconsumer.adapter.TransactionAdp
import com.shreefintech.paytouchconsumer.databinding.ActivityElectricityTransactionStatusBinding
import com.shreefintech.paytouchconsumer.electricity.model.TransactionItem
import com.shreefintech.paytouchconsumer.electricity.viewmodel.ElectricityTransactionStatusViewModel
import com.shreefintech.paytouchconsumer.glass.LiquidGlassEffect
import com.shreefintech.paytouchconsumer.utill.ToastUtil
import com.shreefintech.paytouchconsumer.utill.Utility

class ElectricityTransactionStatusActivity : BaseActivity() {

    private lateinit var binding: ActivityElectricityTransactionStatusBinding
    private val viewModel: ElectricityTransactionStatusViewModel by viewModels()
    private lateinit var transactionAdp: TransactionAdp

    private val mArrayList          = ArrayList<TransactionItem>()
    private val showProgressSearch  = ObservableBoolean(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityElectricityTransactionStatusBinding.inflate(layoutInflater)
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
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                searchStatus(); true
            } else false
        }

        setupRecyclerView()
        onBack()

        prefillTransactionId?.let { txnId ->
            binding.etSearch.setText(txnId)
            searchStatus()
        }
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
        if (prefillTransactionId == null) {
            getTransactionStatusList()
        }
    }

    private fun searchStatus() {
        val query = binding.etSearch.text?.toString()?.trim() ?: ""
        if (query.isEmpty()) {
            ToastUtil.showDelete(mActivity, getString(R.string.msgEnterSearchQuery))
            return
        }
        Utility.hideKeyboard(mActivity)
        getTransactionStatusList(query)
    }

    // TODO(PAYTOUCH-570): Add showNoInternet() / hideNoInternet() / setNoInternetRetryCallback { getTransactionStatusList() }
    //  once the no-internet placeholder design is finalised.
    private fun getTransactionStatusList(query : String? = null){
        viewModel.searchTransactionStatus(
            query = query,
            onLoading = { showShimmer(true) },
            onSuccess = { list ->
                showShimmer(false)
                mArrayList.clear()
                mArrayList.addAll(list)
                transactionAdp.notifyDataSetChanged()
                binding.tvEmpty.visibility = if (mArrayList.isEmpty()) View.VISIBLE else View.GONE
            },
            onError = { msg ->
                showShimmer(false)
                mArrayList.clear()
                transactionAdp.notifyDataSetChanged()
                binding.tvEmpty.visibility = View.VISIBLE
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
                binding.llSearch -> {
                    if (Utility.stopClick()) return@OnClickListener
                    if (showProgressSearch.get()) return@OnClickListener
                    searchStatus()
                }
            }
        }
    }

    private val prefillTransactionId: String? by lazy {
        intent.getStringExtra(EXTRA_TRANSACTION_ID)
    }

    companion object {
        private const val EXTRA_TRANSACTION_ID = "extra_transaction_id"

        fun start(context: Context, transactionId: String? = null) {
            context.startActivity(
                Intent(context, ElectricityTransactionStatusActivity::class.java).apply {
                    transactionId?.let { putExtra(EXTRA_TRANSACTION_ID, it) }
                }
            )
        }
    }
}
