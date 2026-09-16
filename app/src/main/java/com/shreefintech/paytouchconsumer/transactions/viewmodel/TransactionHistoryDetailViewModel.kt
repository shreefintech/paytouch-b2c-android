package com.shreefintech.paytouchconsumer.transactions.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.shreefintech.paytouchconsumer.R
import com.shreefintech.paytouchconsumer.retrofit.ApiClient
import com.shreefintech.paytouchconsumer.retrofit.ApiHelper
import com.shreefintech.paytouchconsumer.retrofit.model.General
import com.shreefintech.paytouchconsumer.retrofit.model.transactions.TransactionHistoryDetailItem
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
        onLoading()
        ApiClient.apiService.getTransactionHistoryDetail(bearerToken(), transactionId)
            .enqueue(object : Callback<General<TransactionHistoryDetailItem>> {
                override fun onResponse(
                    call: Call<General<TransactionHistoryDetailItem>>,
                    response: Response<General<TransactionHistoryDetailItem>>
                ) {
                    if (response.isSuccessful && response.body()?.data != null) {
                        onSuccess(response.body()!!.data!!)
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
}
