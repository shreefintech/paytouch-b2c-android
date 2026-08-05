package com.shreefintech.paytouchconsumer.electricity.viewmodel

import android.app.Application
import com.shreefintech.paytouchconsumer.BaseBillViewModel
import com.shreefintech.paytouchconsumer.R
import com.shreefintech.paytouchconsumer.retrofit.ApiClient
import com.shreefintech.paytouchconsumer.retrofit.ApiHelper
import com.shreefintech.paytouchconsumer.retrofit.model.General
import com.shreefintech.paytouchconsumer.retrofit.model.electricity.ElectricityTransactionReportDataItem
import com.shreefintech.paytouchconsumer.retrofit.model.electricity.ElectricityTransactionReportRequest
import com.shreefintech.paytouchconsumer.transactions.model.TransactionItem
import com.shreefintech.paytouchconsumer.utill.Utility
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class TransactionReportViewModel(application: Application) : BaseBillViewModel(application) {

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

    fun getTransactionReport(
        fromDate: String?,
        toDate: String?,
        status: String?,
        consumerNo: String?,
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
        ApiClient.apiService.getElectricityPaymentReport(
            bearerToken(),
            ElectricityTransactionReportRequest(fromDate, toDate, status, consumerNo, page, PER_PAGE)
        ).enqueue(object : Callback<General<List<ElectricityTransactionReportDataItem>>> {
            override fun onResponse(
                call: Call<General<List<ElectricityTransactionReportDataItem>>>,
                response: Response<General<List<ElectricityTransactionReportDataItem>>>
            ) {
                isLoading = false
                if (response.isSuccessful && response.body()?.data != null) {
                    val rawList = response.body()!!.data!!
                    isLastPage  = rawList.size < PER_PAGE
                    currentPage = page
                    val list = ArrayList<TransactionItem>()
                    rawList.forEach { item -> list.add(mapToTransactionItem(item)) }
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
                call: Call<General<List<ElectricityTransactionReportDataItem>>>,
                t: Throwable
            ) {
                isLoading = false
                onError(t.localizedMessage ?: getString(R.string.errGeneric))
            }
        })
    }

    private fun mapToTransactionItem(item: ElectricityTransactionReportDataItem): TransactionItem {
        return TransactionItem(
            mobileNumber    = item.consumerNo ?: "--",
            transactionId   = item.transactionId ?: "--",
            amount          = "₹%.2f".format(item.amount ?: 0.0),
            status          = item.status ?: "--",
            categoryIconRes = R.drawable.ic_electricity,
            username        = item.customerName ?: "--",
            date            = item.createdAt ?: "--",
            platformFee     = "₹%.2f".format(item.platformFee ?: 0.0),
            totalPayable    = "₹%.2f".format(item.totalPayable ?: 0.0),
            referenceId     = item.transactionId ?: "--",
            userId          = item.id?.toString() ?: "--",
            accountNumber   = item.consumerNo ?: "--",
            companyName     = item.subservice?.takeIf { it.isNotEmpty() } ?: item.operatorId ?: "--"
        )
    }

}
