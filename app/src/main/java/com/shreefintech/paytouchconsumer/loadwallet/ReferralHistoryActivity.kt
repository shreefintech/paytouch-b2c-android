package com.shreefintech.paytouchconsumer.loadwallet

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.shreefintech.paytouchconsumer.BaseActivity
import com.shreefintech.paytouchconsumer.R
import com.shreefintech.paytouchconsumer.adapter.ReferralHistoryAdp
import com.shreefintech.paytouchconsumer.databinding.ActivityReferralHistoryBinding
import com.shreefintech.paytouchconsumer.loadwallet.viewmodel.LoadWalletViewModel
import com.shreefintech.paytouchconsumer.retrofit.model.wallet.BonusWalletHistoryItem
import com.shreefintech.paytouchconsumer.utill.ToastUtil
import com.shreefintech.paytouchconsumer.utill.Utility

class ReferralHistoryActivity : BaseActivity() {

    private lateinit var binding: ActivityReferralHistoryBinding
    private val viewModel: LoadWalletViewModel by viewModels()

    private val historyList = ArrayList<BonusWalletHistoryItem>()
    private lateinit var historyAdp: ReferralHistoryAdp

    companion object {
        fun start(context: Context) {
            context.startActivity(Intent(context, ReferralHistoryActivity::class.java))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReferralHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.clRoot) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.onClickListener = onClickListener()
        setupRecyclerView()
        setupSwipeRefresh()
        retryCallback = { load() }
        load()
    }

    private fun setupRecyclerView() {
        historyAdp = ReferralHistoryAdp(mActivity, historyList)
        binding.rvHistory.apply {
            layoutManager = LinearLayoutManager(mActivity)
            adapter = historyAdp
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setColorSchemeResources(R.color.primary)
        binding.swipeRefresh.setOnRefreshListener { load() }
    }

    private fun load() {
        if (!Utility.isInternetAvailable(mActivity)) {
            binding.swipeRefresh.isRefreshing = false
            showNoInternet()
            return
        }
        hideNoInternet()
        if (!binding.swipeRefresh.isRefreshing) {
            binding.pbLoading.visibility = View.VISIBLE
            binding.rvHistory.visibility = View.GONE
            binding.tvEmpty.visibility = View.GONE
        }
        viewModel.fetchBonusWallet(
            onSuccess = { data ->
                binding.pbLoading.visibility = View.GONE
                binding.swipeRefresh.isRefreshing = false
                val referralRows = (data.history ?: emptyList()).filter { it.isReferral }
                historyAdp.updateList(referralRows)
                if (referralRows.isEmpty()) {
                    binding.rvHistory.visibility = View.GONE
                    binding.tvEmpty.visibility = View.VISIBLE
                } else {
                    binding.rvHistory.visibility = View.VISIBLE
                    binding.tvEmpty.visibility = View.GONE
                }
            },
            onError = { msg ->
                binding.pbLoading.visibility = View.GONE
                binding.swipeRefresh.isRefreshing = false
                historyAdp.updateList(emptyList())
                binding.rvHistory.visibility = View.GONE
                binding.tvEmpty.visibility = View.VISIBLE
                ToastUtil.showDelete(mActivity, msg)
            }
        )
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
