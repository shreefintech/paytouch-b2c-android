package com.shreefintech.paytouchconsumer.billpay.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.shreefintech.paytouchconsumer.R
import com.shreefintech.paytouchconsumer.retrofit.ApiClient
import com.shreefintech.paytouchconsumer.retrofit.ApiHelper
import com.shreefintech.paytouchconsumer.retrofit.model.General
import com.shreefintech.paytouchconsumer.retrofit.model.billpay.BillPayTransactionItem
import com.shreefintech.paytouchconsumer.utill.Utility
import com.shreefintech.paytouchconsumer.utill.bearerToken
import com.shreefintech.paytouchconsumer.utill.getString
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

/** GET billpay/status/{client_reference} — polled after pay, and loads the receipt. */
class BillPayStatusViewModel(application: Application) : AndroidViewModel(application) {

    fun loadStatus(
        clientReference: String,
        onLoading: () -> Unit,
        onSuccess: (BillPayTransactionItem) -> Unit,
        onError: (String) -> Unit
    ) {
        if (!Utility.isInternetAvailable(getApplication())) {
            onError(getString(R.string.msgNoInternet))
            return
        }
        onLoading()
        ApiClient.apiService.getBillPayStatus(bearerToken(), clientReference)
            .enqueue(object : Callback<General<BillPayTransactionItem>> {
                override fun onResponse(
                    call: Call<General<BillPayTransactionItem>>,
                    response: Response<General<BillPayTransactionItem>>
                ) {
                    val item = response.body()?.data
                    if (response.isSuccessful && item != null) {
                        onSuccess(item)
                    } else {
                        onError(
                            ApiHelper.parseErrorMessage(
                                getApplication(), response.code(), response.errorBody()?.string()
                            )
                        )
                    }
                }

                override fun onFailure(call: Call<General<BillPayTransactionItem>>, t: Throwable) {
                    onError(t.localizedMessage ?: getString(R.string.errGeneric))
                }
            })
    }
}
