package com.shreefintech.paytouchconsumer.loadwallet.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.shreefintech.paytouchconsumer.R
import com.shreefintech.paytouchconsumer.loadwallet.model.WalletTransactionItem
import com.shreefintech.paytouchconsumer.retrofit.ApiClient
import com.shreefintech.paytouchconsumer.retrofit.ApiHelper
import com.shreefintech.paytouchconsumer.retrofit.model.General
import com.shreefintech.paytouchconsumer.retrofit.model.wallet.WalletHistoryPageItem
import com.shreefintech.paytouchconsumer.utill.Utility
import com.shreefintech.paytouchconsumer.utill.bearerToken
import com.shreefintech.paytouchconsumer.utill.getString
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class WalletTransactionsViewModel(application: Application) : AndroidViewModel(application) {

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

    fun loadHistory(
        page: Int,
        onLoading: () -> Unit,
        onSuccess: (ArrayList<WalletTransactionItem>) -> Unit,
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
        ApiClient.apiService.getWalletHistory(bearerToken(), page, PER_PAGE)
            .enqueue(object : Callback<General<WalletHistoryPageItem>> {
                override fun onResponse(
                    call: Call<General<WalletHistoryPageItem>>,
                    response: Response<General<WalletHistoryPageItem>>
                ) {
                    isLoading = false
                    if (response.isSuccessful && response.body()?.data != null) {
                        val pageData = response.body()!!.data!!
                        val rawList  = pageData.data ?: emptyList()
                        isLastPage  = pageData.lastPage?.let { page >= it } ?: false
                        currentPage = page
                        val list = ArrayList<WalletTransactionItem>()
                        rawList.forEach { list.add(WalletTransactionItem.from(it)) }
                        onSuccess(list)
                    } else {
                        onError(
                            ApiHelper.parseErrorMessage(
                                getApplication(), response.code(), response.errorBody()?.string()
                            )
                        )
                    }
                }

                override fun onFailure(call: Call<General<WalletHistoryPageItem>>, t: Throwable) {
                    isLoading = false
                    onError(t.localizedMessage ?: getString(R.string.errGeneric))
                }
            })
    }
}