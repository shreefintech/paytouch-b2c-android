package com.shreefintech.paytouchconsumer.prepaid

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.viewModels
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.Gson
import com.shreefintech.paytouchconsumer.BaseActivity
import com.shreefintech.paytouchconsumer.R
import com.shreefintech.paytouchconsumer.adapter.PrepaidPlanAdp
import com.shreefintech.paytouchconsumer.databinding.ActivityPrepaidPlanSelectionBinding
import com.shreefintech.paytouchconsumer.glass.LiquidGlassEffect
import com.shreefintech.paytouchconsumer.prepaid.viewmodel.PrepaidPlanSelectionViewModel
import com.shreefintech.paytouchconsumer.retrofit.model.prepaid.PrepaidPlanItem
import com.shreefintech.paytouchconsumer.utill.ToastUtil
import com.shreefintech.paytouchconsumer.utill.Utility

class PrepaidPlanSelectionActivity : BaseActivity() {

    private lateinit var binding: ActivityPrepaidPlanSelectionBinding
    private val viewModel: PrepaidPlanSelectionViewModel by viewModels()
    private lateinit var plansAdp: PrepaidPlanAdp

    private val mPlanList = ArrayList<PrepaidPlanItem>()

    private val operatorId: String by lazy { intent.getStringExtra(EXTRA_OPERATOR_ID) ?: "" }
    private val circleId: String by lazy { intent.getStringExtra(EXTRA_CIRCLE_ID) ?: "" }

    companion object {
        private const val EXTRA_OPERATOR_ID = "extra_operator_id"
        private const val EXTRA_CIRCLE_ID = "extra_circle_id"
        const val EXTRA_SELECTED_PLAN = "extra_selected_plan"

        fun start(activity: Activity, launcher: ActivityResultLauncher<Intent>, operatorId: String, circleId: String) {
            launcher.launch(
                Intent(activity, PrepaidPlanSelectionActivity::class.java).apply {
                    putExtra(EXTRA_OPERATOR_ID, operatorId)
                    putExtra(EXTRA_CIRCLE_ID, circleId)
                }
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPrepaidPlanSelectionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.clRoot) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.onClickListener = onClickListener()
        setupRecyclerView()
        onBack()
        loadPlans()
    }

    private fun setupRecyclerView() {
        plansAdp = PrepaidPlanAdp(mActivity, mPlanList)
        plansAdp.onClickItem = { plan -> onPlanSelected(plan) }
        binding.rvPlans.apply {
            layoutManager = LinearLayoutManager(mActivity)
            adapter = plansAdp
        }
    }

    private fun loadPlans() {
        viewModel.loadPlans(
            operatorId = operatorId,
            circleId = circleId,
            onLoading = {
                binding.shimmerLayout.visibility = View.VISIBLE
                binding.shimmerLayout.startShimmer()
                binding.rvPlans.visibility = View.GONE
                binding.tvEmpty.visibility = View.GONE
            },
            onSuccess = { plans ->
                binding.shimmerLayout.stopShimmer()
                binding.shimmerLayout.visibility = View.GONE
                if (plans.isEmpty()) {
                    binding.tvEmpty.visibility = View.VISIBLE
                } else {
                    binding.rvPlans.visibility = View.VISIBLE
                    mPlanList.clear()
                    mPlanList.addAll(plans)
                    plansAdp.notifyDataSetChanged()
                }
            },
            onError = { msg ->
                binding.shimmerLayout.stopShimmer()
                binding.shimmerLayout.visibility = View.GONE
                binding.tvEmpty.visibility = View.VISIBLE
                ToastUtil.showDelete(mActivity, msg)
            }
        )
    }

    private fun onPlanSelected(plan: PrepaidPlanItem) {
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
