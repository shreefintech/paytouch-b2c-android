package com.shreefintech.paytouchconsumer.electricity.transactions

import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.shreefintech.paytouchconsumer.BaseActivity
import com.shreefintech.paytouchconsumer.Constant
import com.shreefintech.paytouchconsumer.R
import com.shreefintech.paytouchconsumer.adapter.TransactionAdp
import com.shreefintech.paytouchconsumer.databinding.ActivityTransactionReportBinding
import com.shreefintech.paytouchconsumer.electricity.model.TransactionItem
import com.shreefintech.paytouchconsumer.glass.LiquidGlassEffect
import com.shreefintech.paytouchconsumer.retrofit.ApiClient
import com.shreefintech.paytouchconsumer.retrofit.ApiHelper
import com.shreefintech.paytouchconsumer.retrofit.model.General
import com.shreefintech.paytouchconsumer.retrofit.model.electricity.ElectricityTransactionReportDataItem
import com.shreefintech.paytouchconsumer.retrofit.model.electricity.ElectricityTransactionReportRequest
import com.shreefintech.paytouchconsumer.utill.SharedPreferenceHelper
import com.shreefintech.paytouchconsumer.utill.ToastUtil
import com.shreefintech.paytouchconsumer.utill.TransactionFilterHelper
import com.shreefintech.paytouchconsumer.utill.Utility
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class TransactionReportActivity : BaseActivity() {

    private lateinit var binding: ActivityTransactionReportBinding
    private lateinit var transactionAdp: TransactionAdp
    private lateinit var filterHelper: TransactionFilterHelper

    private val mAllList     = ArrayList<TransactionItem>()
    private val mDisplayList = ArrayList<TransactionItem>()
    private var hasSearched  = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTransactionReportBinding.inflate(layoutInflater)
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
            binding.incFilterSheet.root.setPadding(0, 0, 0, systemBars.bottom)
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

        setupRecyclerView()
        setupSearch()
        setupFilterSheet()

        binding.onClickListener = onClickListener()
        onBack()

        filterHelper.show()
    }

    private fun setupRecyclerView() {
        transactionAdp = TransactionAdp(mActivity, mDisplayList)
        transactionAdp.onClickItem = { item ->
            TransactionDetailActivity.start(mActivity, item)
        }
        binding.rvTransactions.apply {
            layoutManager = LinearLayoutManager(mActivity)
            adapter       = transactionAdp
        }
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
            activity    = mActivity,
            sheetBinding = binding.incFilterSheet,
            bgOverlay   = binding.viewBg,
            onApply     = { fromDate, toDate, status, consumerNo ->
                callReport(fromDate, toDate, status, consumerNo)
            },
            onClear     = {
                hasSearched = false
                mAllList.clear()
                mDisplayList.clear()
                binding.etSearch.setText("")
                transactionAdp.notifyDataSetChanged()
                binding.tvEmpty.visibility = View.GONE
                binding.shimmerLayout.stopShimmer()
                binding.shimmerLayout.visibility = View.GONE
                binding.rvTransactions.visibility = View.VISIBLE
            }
        )
        filterHelper.setup()
    }

    private fun callReport(
        fromDate: String?,
        toDate: String?,
        status: String?,
        consumerNo: String?
    ) {
        if (!Utility.isInternetAvailable(mActivity)) {
            ToastUtil.showDelete(mActivity, getString(R.string.msgNoInternet))
            return
        }
        hasSearched = true
        showShimmer(true)
        val token   = "Bearer ${SharedPreferenceHelper.getSharedPreferenceString(mActivity, Constant.KEY_TOKEN, "")}"
        val request = ElectricityTransactionReportRequest(fromDate, toDate, status, consumerNo)
        ApiClient.apiService.getElectricityPaymentReport(token, request)
            .enqueue(object : Callback<General<List<ElectricityTransactionReportDataItem>>> {
                override fun onResponse(
                    call: Call<General<List<ElectricityTransactionReportDataItem>>>,
                    response: Response<General<List<ElectricityTransactionReportDataItem>>>
                ) {
                    showShimmer(false)
                    mAllList.clear()
                    if (response.isSuccessful && response.body()?.data != null) {
                        response.body()!!.data!!.forEachIndexed { index, item ->
                            mAllList.add(
                                TransactionItem(
                                    mobileNumber   = item.subscriberNo ?: "",
                                    transactionId  = item.transactionId ?: "",
                                    amount         = "₹%.2f".format(item.totalPayable),
                                    status         = item.status ?: "",
                                    categoryIconRes = R.drawable.ic_electricity,
                                    username       = item.customerName ?: "",
                                    date           = item.createdAt ?: "",
                                    platformFee    = "₹%.2f".format(item.platformFee),
                                    totalPayable   = "₹%.2f".format(item.totalPayable),
                                    referenceId    = item.transactionId ?: "",
                                    userId         = (index + 1).toString(),
                                    accountNumber  = item.subscriberNo ?: "",
                                    companyName    = if (!item.subservice.isNullOrEmpty()) item.subservice else item.operatorId ?: ""
                                )
                            )
                        }
                    } else {
                        val msg = ApiHelper.parseErrorMessage(
                            mActivity, response.code(), response.errorBody()?.string()
                        )
                        if (msg.isNotEmpty()) ToastUtil.showDelete(mActivity, msg)
                    }
                    filterList(binding.etSearch.text?.toString()?.trim() ?: "")
                }

                override fun onFailure(
                    call: Call<General<List<ElectricityTransactionReportDataItem>>>,
                    t: Throwable
                ) {
                    showShimmer(false)
                    mAllList.clear()
                    filterList("")
                    ToastUtil.showDelete(mActivity, t.localizedMessage ?: getString(R.string.err_generic))
                }
            })
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
        binding.tvEmpty.visibility =
            if (mDisplayList.isEmpty() && hasSearched) View.VISIBLE else View.GONE
    }

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
                binding.flFilter -> {
                    if (Utility.stopClick()) return@OnClickListener
                    filterHelper.show()
                }
            }
        }
    }
}
