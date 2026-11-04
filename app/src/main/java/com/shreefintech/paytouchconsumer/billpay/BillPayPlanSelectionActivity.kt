package com.shreefintech.paytouchconsumer.billpay

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.viewModels
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.Gson
import com.shreefintech.paytouchconsumer.BaseActivity
import com.shreefintech.paytouchconsumer.R
import com.shreefintech.paytouchconsumer.adapter.BillPayPlanAdp
import com.shreefintech.paytouchconsumer.databinding.ActivityBillPayPlanSelectionBinding
import com.shreefintech.paytouchconsumer.billpay.viewmodel.BillPayPlanSelectionViewModel
import com.shreefintech.paytouchconsumer.retrofit.model.billpay.BillPayPlanItem
import com.shreefintech.paytouchconsumer.utill.ToastUtil
import com.shreefintech.paytouchconsumer.utill.Utility

class BillPayPlanSelectionActivity : BaseActivity() {

    private lateinit var binding: ActivityBillPayPlanSelectionBinding
    private val viewModel: BillPayPlanSelectionViewModel by viewModels()
    private lateinit var plansAdp: BillPayPlanAdp

    private val billerId: String by lazy { intent.getStringExtra(EXTRA_BILLER_ID) ?: "" }
    // Prepaid only — DTH plans are not per circle
    private val circleCode: String? by lazy { intent.getStringExtra(EXTRA_CIRCLE_CODE) }

    companion object {
        private const val EXTRA_BILLER_ID = "extra_biller_id"
        private const val EXTRA_CIRCLE_CODE = "extra_circle_code"
        const val EXTRA_SELECTED_PLAN = "extra_selected_plan"

        fun start(activity: Activity, launcher: ActivityResultLauncher<Intent>, billerId: String, circleCode: String?) {
            launcher.launch(
                Intent(activity, BillPayPlanSelectionActivity::class.java).apply {
                    putExtra(EXTRA_BILLER_ID, billerId)
                    circleCode?.let { putExtra(EXTRA_CIRCLE_CODE, it) }
                }
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBillPayPlanSelectionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.clRoot) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.onClickListener = onClickListener()
        setupRecyclerView()
        onBack()
        retryCallback = { loadPlans() }
        loadPlans()
    }

    private fun setupRecyclerView() {
        plansAdp = BillPayPlanAdp(mActivity, ArrayList())
        plansAdp.onClickItem = { plan -> onPlanSelected(plan) }
        binding.rvPlans.apply {
            layoutManager = LinearLayoutManager(mActivity)
            adapter = plansAdp
        }
    }

    private fun loadPlans() {
        if (!Utility.isInternetAvailable(mActivity)) { showNoInternet(); return }
        hideNoInternet()
        viewModel.loadPlans(
            billerId = billerId,
            circleCode = circleCode,
            onLoading = {
                binding.shimmerLayout.visibility = View.VISIBLE
                binding.shimmerLayout.startShimmer()
                binding.rvPlans.visibility = View.GONE
                binding.tvEmpty.visibility = View.GONE
            },
            onSuccess = { plans ->
                if (isDestroyed) return@loadPlans
                binding.shimmerLayout.stopShimmer()
                binding.shimmerLayout.visibility = View.GONE
                if (plans.isEmpty()) {
                    binding.tvEmpty.visibility = View.VISIBLE
                } else {
                    binding.rvPlans.visibility = View.VISIBLE
                    plansAdp.updateList(plans)
                }
            },
            onError = { msg ->
                if (isDestroyed) return@loadPlans
                binding.shimmerLayout.stopShimmer()
                binding.shimmerLayout.visibility = View.GONE
                binding.tvEmpty.visibility = View.VISIBLE
                ToastUtil.showDelete(mActivity, msg)
            }
        )
    }

    private fun onPlanSelected(plan: BillPayPlanItem) {
        setResult(RESULT_OK, Intent().putExtra(EXTRA_SELECTED_PLAN, Gson().toJson(plan)))
        finish()
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
