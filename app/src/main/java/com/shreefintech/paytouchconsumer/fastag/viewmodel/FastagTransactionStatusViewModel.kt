package com.shreefintech.paytouchconsumer.fastag.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.shreefintech.paytouchconsumer.R
import com.shreefintech.paytouchconsumer.retrofit.ApiClient
import com.shreefintech.paytouchconsumer.retrofit.ApiHelper
import com.shreefintech.paytouchconsumer.retrofit.model.General
import com.shreefintech.paytouchconsumer.retrofit.model.fastag.FastagTransactionReportDataItem
import com.shreefintech.paytouchconsumer.retrofit.model.fastag.FastagTransactionStatusRequest
import com.shreefintech.paytouchconsumer.transactions.model.TransactionItem
import com.shreefintech.paytouchconsumer.utill.Utility
import com.shreefintech.paytouchconsumer.utill.bearerToken
import com.shreefintech.paytouchconsumer.utill.getString
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class FastagTransactionStatusViewModel(application: Application) : AndroidViewModel(application) {

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

    fun loadStatus(
        query: String?,
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
        ApiClient.apiService.getFastagTransactionStatus(
            bearerToken(),
            FastagTransactionStatusRequest(vehicleNumber = null, transactionId = query, page = page, perPage = PER_PAGE)
        ).enqueue(object : Callback<General<List<FastagTransactionReportDataItem>>> {
            override fun onResponse(
                call: Call<General<List<FastagTransactionReportDataItem>>>,
                response: Response<General<List<FastagTransactionReportDataItem>>>
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
                call: Call<General<List<FastagTransactionReportDataItem>>>,
                t: Throwable
            ) {
                isLoading = false
                onError(t.localizedMessage ?: getString(R.string.errGeneric))
            }
        })
    }

    private fun mapToTransactionItem(item: FastagTransactionReportDataItem): TransactionItem {
        return TransactionItem(
            mobileNumber      = item.vehicleNumber ?: "--",
            transactionId     = item.transactionId ?: "--",
            amount            = Utility.formatAmount(item.amount),
            status            = item.status ?: "--",
            categoryIconRes   = R.drawable.ic_fastag,
            username          = item.vehicleNumber ?: "--",
            date              = item.createdAt ?: "--",
            platformFee       = Utility.formatAmount(item.platformFee),
            totalPayable      = Utility.formatAmount(item.totalPayable),
            referenceId       = item.transactionId ?: "--",
            userId            = item.id?.toString() ?: "--",
            accountNumber     = item.vehicleNumber ?: "--",
            companyName       = item.operatorName?.takeIf { it.isNotEmpty() } ?: item.operator ?: "--",
            isMobileCategory  = false,
            isVehicleCategory = true
        )
    }
}
