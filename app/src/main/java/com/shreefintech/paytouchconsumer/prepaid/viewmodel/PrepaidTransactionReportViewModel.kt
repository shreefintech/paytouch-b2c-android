package com.shreefintech.paytouchconsumer.prepaid.viewmodel

import android.app.Application
import com.shreefintech.paytouchconsumer.BaseBillViewModel
import com.shreefintech.paytouchconsumer.R
import com.shreefintech.paytouchconsumer.retrofit.ApiClient
import com.shreefintech.paytouchconsumer.retrofit.ApiHelper
import com.shreefintech.paytouchconsumer.retrofit.model.General
import com.shreefintech.paytouchconsumer.retrofit.model.prepaid.PrepaidTransactionReportDataItem
import com.shreefintech.paytouchconsumer.retrofit.model.prepaid.PrepaidTransactionReportRequest
import com.shreefintech.paytouchconsumer.transactions.model.TransactionItem
import com.shreefintech.paytouchconsumer.utill.Utility
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class PrepaidTransactionReportViewModel(application: Application) : BaseBillViewModel(application) {

    companion object {
        private const val PER_PAGE = 20
    }

    private var currentPage = 0

    var isLastPage = false
        private set

    var isLoading = false
        private set

    fun canLoadMore() = !isLoading && !isLastPage

    fun nextPage() = currentPage + 1

    fun loadReport(
        fromDate: String?,
        toDate: String?,
        status: String?,
        mobileNo: String?,
        page: Int,
        onLoading: () -> Unit,
        onSuccess: (ArrayList<TransactionItem>) -> Unit,
        onError: (String) -> Unit
    ) {
        if (page == 1) {
            // Fresh load: reset pagination state so a new filter/clear always goes through
            isLastPage  = false
            currentPage = 0
            isLoading   = false
        }
        if (isLoading) return
        if (!Utility.isInternetAvailable(getApplication())) {
            onError(getString(R.string.msgNoInternet))
            return
        }
        isLoading = true
        onLoading()
        ApiClient.apiService.getPrepaidPaymentReport(
            bearerToken(),
            PrepaidTransactionReportRequest(fromDate, toDate, status, mobileNo, page, PER_PAGE)
        ).enqueue(object : Callback<General<List<PrepaidTransactionReportDataItem>>> {
            override fun onResponse(
                call: Call<General<List<PrepaidTransactionReportDataItem>>>,
                response: Response<General<List<PrepaidTransactionReportDataItem>>>
            ) {
                isLoading = false
                if (response.isSuccessful && response.body()?.data != null) {
                    val rawList = response.body()!!.data!!
                    isLastPage  = rawList.size < PER_PAGE
                    currentPage = page
                    val list = ArrayList<TransactionItem>()
                    rawList.forEachIndexed { index, item -> list.add(mapToTransactionItem(index, item)) }
                    onSuccess(list)
                } else {
                    onError(
                        ApiHelper.parseErrorMessage(
                            getApplication(), response.code(), response.errorBody()?.string()
                        )
                    )
                }
            }

            override fun onFailure(
                call: Call<General<List<PrepaidTransactionReportDataItem>>>,
                t: Throwable
            ) {
                isLoading = false
                onError(t.localizedMessage ?: getString(R.string.errGeneric))
            }
        })
    }

    private fun mapToTransactionItem(index: Int, item: PrepaidTransactionReportDataItem): TransactionItem {
        return TransactionItem(
            mobileNumber    = item.mobileNo ?: "--",
            transactionId   = item.txnId ?: "--",
            amount          = "₹${item.amount ?: "0.00"}",
            status          = item.status ?: "--",
            categoryIconRes = R.drawable.ic_prepaid,
            username        = item.mobileNo ?: "--",
            date            = item.createdAt ?: "--",
            platformFee     = "₹0.00",
            totalPayable    = "₹${item.amount ?: "0.00"}",
            referenceId     = item.txnId ?: "--",
            userId          = (index + 1).toString(),
            accountNumber   = item.mobileNo ?: "--",
            companyName     = item.operator?.takeIf { it.isNotEmpty() } ?: item.service ?: "--"
        )
    }
}
