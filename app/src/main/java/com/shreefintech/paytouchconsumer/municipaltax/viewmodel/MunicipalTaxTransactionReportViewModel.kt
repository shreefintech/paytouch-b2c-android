package com.shreefintech.paytouchconsumer.municipaltax.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.shreefintech.paytouchconsumer.R
import com.shreefintech.paytouchconsumer.retrofit.ApiClient
import com.shreefintech.paytouchconsumer.retrofit.ApiHelper
import com.shreefintech.paytouchconsumer.retrofit.model.General
import com.shreefintech.paytouchconsumer.retrofit.model.municipaltax.MunicipalTaxTransactionReportDataItem
import com.shreefintech.paytouchconsumer.retrofit.model.municipaltax.MunicipalTaxTransactionReportRequest
import com.shreefintech.paytouchconsumer.transactions.model.TransactionItem
import com.shreefintech.paytouchconsumer.utill.Utility
import com.shreefintech.paytouchconsumer.utill.bearerToken
import com.shreefintech.paytouchconsumer.utill.getString
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MunicipalTaxTransactionReportViewModel(application: Application) : AndroidViewModel(application) {

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
        subscriberNo: String?,
        page: Int,
        onLoading: () -> Unit,
        onSuccess: (ArrayList<TransactionItem>) -> Unit,
        onError: (String) -> Unit
    ) {
        if (page == 1) {
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
        ApiClient.apiService.getMunicipalTaxPaymentReport(
            bearerToken(),
            MunicipalTaxTransactionReportRequest(fromDate, toDate, status, subscriberNo, page, PER_PAGE)
        ).enqueue(object : Callback<General<List<MunicipalTaxTransactionReportDataItem>>> {
            override fun onResponse(
                call: Call<General<List<MunicipalTaxTransactionReportDataItem>>>,
                response: Response<General<List<MunicipalTaxTransactionReportDataItem>>>
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
                call: Call<General<List<MunicipalTaxTransactionReportDataItem>>>,
                t: Throwable
            ) {
                isLoading = false
                onError(t.localizedMessage ?: getString(R.string.errGeneric))
            }
        })
    }

    private fun mapToTransactionItem(item: MunicipalTaxTransactionReportDataItem): TransactionItem {
        val accountNo = item.houseNumber ?: item.subscriberNo ?: "--"
        return TransactionItem(
            mobileNumber     = Utility.maskNumber(accountNo),
            transactionId    = item.transactionId ?: "--",
            amount           = Utility.formatAmount(item.amount ?: item.totalPayable),
            status           = item.status ?: "--",
            categoryIconRes  = R.drawable.ic_tax,
            username         = item.customerName ?: "--",
            date             = item.createdAt ?: "--",
            platformFee      = Utility.formatAmount(item.platformFee),
            totalPayable     = Utility.formatAmount(item.totalPayable),
            referenceId      = item.transactionId ?: "--",
            userId           = item.id?.toString() ?: "--",
            accountNumber    = accountNo,
            companyName      = item.operatorName ?: item.serviceType ?: "--",
            isMobileCategory = false
        )
    }
}
