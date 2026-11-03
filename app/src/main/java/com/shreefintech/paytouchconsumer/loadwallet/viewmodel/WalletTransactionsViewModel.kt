package com.shreefintech.paytouchconsumer.loadwallet.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.shreefintech.paytouchconsumer.Constant
import com.shreefintech.paytouchconsumer.R
import com.shreefintech.paytouchconsumer.loadwallet.model.WalletTransactionItem
import com.shreefintech.paytouchconsumer.retrofit.ApiClient
import com.shreefintech.paytouchconsumer.retrofit.ApiHelper
import com.shreefintech.paytouchconsumer.retrofit.model.wallet.WalletStatementItem
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

    // wallet/statement pages by cursor; `page` is kept only so the screen can tell the first page from the rest
    private var currentPage = 0
    private var nextCursor: String? = null
    private var isLastPage = false
    private var isLoading  = false

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
            nextCursor  = null
            isLoading   = false
        }
        if (isLoading) return
        if (!Utility.isInternetAvailable(getApplication())) {
            onError(getString(R.string.msgNoInternet))
            return
        }
        isLoading = true
        onLoading()
        ApiClient.apiService.getWalletStatement(bearerToken(), Constant.WALLET_STATEMENT_MAIN, PER_PAGE, nextCursor)
            .enqueue(object : Callback<WalletStatementItem> {
                override fun onResponse(call: Call<WalletStatementItem>, response: Response<WalletStatementItem>) {
                    isLoading = false
                    val body = response.body()
                    if (response.isSuccessful && body?.success == true) {
                        nextCursor  = body.meta?.nextCursor
                        isLastPage  = nextCursor.isNullOrEmpty()
                        currentPage = page
                        val list = ArrayList<WalletTransactionItem>()
                        body.data?.forEach { list.add(WalletTransactionItem.from(it)) }
                        onSuccess(list)
                    } else {
                        onError(
                            ApiHelper.parseErrorMessage(
                                getApplication(), response.code(), response.errorBody()?.string()
                            )
                        )
                    }
                }

                override fun onFailure(call: Call<WalletStatementItem>, t: Throwable) {
                    isLoading = false
                    onError(t.localizedMessage ?: getString(R.string.errGeneric))
                }
            })
    }
}
