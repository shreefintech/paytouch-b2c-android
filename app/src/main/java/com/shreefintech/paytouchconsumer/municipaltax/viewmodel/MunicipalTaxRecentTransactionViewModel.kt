package com.shreefintech.paytouchconsumer.municipaltax.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.shreefintech.paytouchconsumer.R
import com.shreefintech.paytouchconsumer.retrofit.ApiClient
import com.shreefintech.paytouchconsumer.retrofit.ApiHelper
import com.shreefintech.paytouchconsumer.retrofit.model.municipaltax.MunicipalTaxRecentDataItem
import com.shreefintech.paytouchconsumer.retrofit.model.municipaltax.MunicipalTaxRecentPageItem
import com.shreefintech.paytouchconsumer.transactions.model.RecentTransactionItem
import com.shreefintech.paytouchconsumer.utill.Utility
import com.shreefintech.paytouchconsumer.utill.bearerToken
import com.shreefintech.paytouchconsumer.utill.getString
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MunicipalTaxRecentTransactionViewModel(application: Application) : AndroidViewModel(application) {

    var currentPage = 1
        private set
    var hasMore = false
        private set
    var isLoading = false
        private set

    companion object {
        private const val PAGE_SIZE = 20
    }

    fun loadData(
        onLoading: () -> Unit,
        onSuccess: (List<RecentTransactionItem>) -> Unit,
        onError: (String) -> Unit
    ) {
        if (!Utility.isInternetAvailable(getApplication())) {
            onError(getString(R.string.msgNoInternet))
            return
        }
        isLoading = true
        currentPage = 1
        hasMore = false
        onLoading()
        fetchTransactions(onSuccess, onError)
    }

    fun loadNextPage(
        onLoading: () -> Unit,
        onSuccess: (List<RecentTransactionItem>) -> Unit,
        onError: (String) -> Unit
    ) {
        if (!Utility.isInternetAvailable(getApplication())) {
            onError(getString(R.string.msgNoInternet))
            return
        }
        if (!hasMore || isLoading) return
        isLoading = true
        currentPage++
        onLoading()
        fetchTransactions(onSuccess, onError)
    }

    private fun fetchTransactions(
        onSuccess: (List<RecentTransactionItem>) -> Unit,
        onError: (String) -> Unit
    ) {
        ApiClient.apiService.getMunicipalTaxRecentTransactions(bearerToken(), currentPage, PAGE_SIZE)
            .enqueue(object : Callback<MunicipalTaxRecentPageItem> {
                override fun onResponse(
                    call: Call<MunicipalTaxRecentPageItem>,
                    response: Response<MunicipalTaxRecentPageItem>
                ) {
                    isLoading = false
                    val body = response.body()
                    if (response.isSuccessful && body?.success == true) {
                        val items = body.transactions ?: emptyList()
                        val lastPage = body.pagination?.lastPage ?: 1
                        hasMore = currentPage < lastPage
                        onSuccess(items.map { mapToDisplayItem(it) })
                    } else {
                        hasMore = false
                        onError(
                            ApiHelper.parseErrorMessage(
                                getApplication(), response.code(), response.errorBody()?.string()
                            )
                        )
                    }
                }

                override fun onFailure(call: Call<MunicipalTaxRecentPageItem>, t: Throwable) {
                    isLoading = false
                    hasMore = false
                    onError(t.localizedMessage ?: getString(R.string.errGeneric))
                }
            })
    }

    private fun mapToDisplayItem(item: MunicipalTaxRecentDataItem): RecentTransactionItem {
        return RecentTransactionItem(
            categoryName      = item.operatorName ?: "--",
            accountHolderName = item.customerName ?: "--",
            date              = Utility.formatDate(item.createdAt, "dd MMM yyyy"),
            status            = item.status ?: "--",
            amount            = Utility.formatAmount(item.totalPayable ?: item.amount),
            accountNumber     = Utility.maskNumber(item.houseNumber ?: "--"),
            reference         = item.transactionId?:"--",
            categoryIconRes   = R.drawable.ic_tax,
            isMobileCategory  = false
        )
    }
}
