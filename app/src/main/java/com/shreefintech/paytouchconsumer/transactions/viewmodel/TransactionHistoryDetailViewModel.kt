package com.shreefintech.paytouchconsumer.transactions.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.shreefintech.paytouchconsumer.R
import com.shreefintech.paytouchconsumer.retrofit.ApiClient
import com.shreefintech.paytouchconsumer.utill.Utility
import com.shreefintech.paytouchconsumer.retrofit.ApiHelper
import com.shreefintech.paytouchconsumer.retrofit.model.General
import com.shreefintech.paytouchconsumer.retrofit.model.transactions.TransactionHistoryDetailItem
import com.shreefintech.paytouchconsumer.retrofit.model.billpay.BillPayTransactionItem
import com.shreefintech.paytouchconsumer.retrofit.model.wallet.WalletTopupItem
import com.shreefintech.paytouchconsumer.retrofit.model.wallet.WalletWithdrawalItem
import com.shreefintech.paytouchconsumer.utill.bearerToken
import com.shreefintech.paytouchconsumer.utill.getString
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class TransactionHistoryDetailViewModel(application: Application) : AndroidViewModel(application) {

    fun loadDetail(
        transactionId: String,
        onLoading: () -> Unit,
        onSuccess: (TransactionHistoryDetailItem) -> Unit,
        onError: (String) -> Unit
    ) {
        if (!Utility.isInternetAvailable(getApplication())) { onError(getString(R.string.msgNoInternet)); return }
        onLoading()
        ApiClient.apiService.getTransactionHistoryDetail(bearerToken(), transactionId)
            .enqueue(object : Callback<General<TransactionHistoryDetailItem>> {
                override fun onResponse(
                    call: Call<General<TransactionHistoryDetailItem>>,
                    response: Response<General<TransactionHistoryDetailItem>>
                ) {
                    val body = response.body()
                    if (response.isSuccessful && body?.data != null) {
                        onSuccess(body.data!!)
                    } else {
                        onError(
                            ApiHelper.parseErrorMessage(
                                getApplication(), response.code(), response.errorBody()?.string()
                            )
                        )
                    }
                }

                override fun onFailure(
                    call: Call<General<TransactionHistoryDetailItem>>,
                    t: Throwable
                ) {
                    onError(t.localizedMessage ?: getString(R.string.errGeneric))
                }
            })
    }

    // ── Wallet history detail (row tapped on My Wallet) ──────────────────────

    fun loadBillPayStatus(
        clientReference: String,
        onLoading: () -> Unit,
        onSuccess: (BillPayTransactionItem) -> Unit,
        onNotFound: () -> Unit,
        onError: (String) -> Unit
    ) = loadWalletDetail(
        { ApiClient.apiService.getBillPayStatus(bearerToken(), clientReference) },
        onLoading, onSuccess, onNotFound, onError
    )

    fun loadTopup(
        clientReference: String,
        onLoading: () -> Unit,
        onSuccess: (WalletTopupItem) -> Unit,
        onNotFound: () -> Unit,
        onError: (String) -> Unit
    ) = loadWalletDetail(
        { ApiClient.apiService.getWalletTopup(bearerToken(), clientReference) },
        onLoading, onSuccess, onNotFound, onError
    )

    fun loadWithdrawal(
        clientReference: String,
        onLoading: () -> Unit,
        onSuccess: (WalletWithdrawalItem) -> Unit,
        onNotFound: () -> Unit,
        onError: (String) -> Unit
    ) = loadWalletDetail(
        { ApiClient.apiService.getWalletWithdrawal(bearerToken(), clientReference) },
        onLoading, onSuccess, onNotFound, onError
    )

    // 404 = that reference has no detail record → "No data found", not an error toast
    private fun <T> loadWalletDetail(
        request: () -> Call<General<T>>,
        onLoading: () -> Unit,
        onSuccess: (T) -> Unit,
        onNotFound: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (!Utility.isInternetAvailable(getApplication())) { onError(getString(R.string.msgNoInternet)); return }
        onLoading()
        request().enqueue(object : Callback<General<T>> {
            override fun onResponse(call: Call<General<T>>, response: Response<General<T>>) {
                val data = response.body()?.data
                when {
                    response.isSuccessful && data != null -> onSuccess(data)
                    response.code() == HTTP_NOT_FOUND -> onNotFound()
                    else -> onError(
                        ApiHelper.parseErrorMessage(getApplication(), response.code(), response.errorBody()?.string())
                    )
                }
            }

            override fun onFailure(call: Call<General<T>>, t: Throwable) {
                t.printStackTrace()
                onError(t.localizedMessage ?: getString(R.string.errGeneric))
            }
        })
    }

    companion object {
        private const val HTTP_NOT_FOUND = 404
    }
}
