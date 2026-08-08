package com.shreefintech.paytouchconsumer.dth

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
import com.shreefintech.paytouchconsumer.adapter.DthPlanAdp
import com.shreefintech.paytouchconsumer.databinding.ActivityDthPlanSelectionBinding
import com.shreefintech.paytouchconsumer.dth.viewmodel.DthPlanSelectionViewModel
import com.shreefintech.paytouchconsumer.retrofit.model.dth.DthPlanItem
import com.shreefintech.paytouchconsumer.utill.ToastUtil
import com.shreefintech.paytouchconsumer.utill.Utility

class DthPlanSelectionActivity : BaseActivity() {

    private lateinit var binding: ActivityDthPlanSelectionBinding
    private val viewModel: DthPlanSelectionViewModel by viewModels()
    private lateinit var plansAdp: DthPlanAdp

    private val operatorId: String by lazy { intent.getStringExtra(EXTRA_OPERATOR_ID) ?: "" }

    companion object {
        private const val EXTRA_OPERATOR_ID = "extra_operator_id"
        const val EXTRA_SELECTED_PLAN = "extra_selected_plan"

        fun start(activity: Activity, launcher: ActivityResultLauncher<Intent>, operatorId: String) {
            launcher.launch(
                Intent(activity, DthPlanSelectionActivity::class.java).apply {
                    putExtra(EXTRA_OPERATOR_ID, operatorId)
                }
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDthPlanSelectionBinding.inflate(layoutInflater)
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
        plansAdp = DthPlanAdp(mActivity, ArrayList())
        plansAdp.onClickItem = { plan -> onPlanSelected(plan) }
        binding.rvPlans.apply {
            layoutManager = LinearLayoutManager(mActivity)
            adapter = plansAdp
        }
    }

    private fun loadPlans() {
        viewModel.loadPlans(
            operatorId = operatorId,
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
                    plansAdp.updateList(plans)
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

    private fun onPlanSelected(plan: DthPlanItem) {
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
