package com.shreefintech.paytouchconsumer.prepaid.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.shreefintech.paytouchconsumer.R
import com.shreefintech.paytouchconsumer.retrofit.ApiClient
import com.shreefintech.paytouchconsumer.retrofit.ApiHelper
import com.shreefintech.paytouchconsumer.retrofit.model.General
import com.shreefintech.paytouchconsumer.retrofit.model.prepaid.PrepaidVerifyPaymentDataItem
import com.shreefintech.paytouchconsumer.utill.Utility
import com.shreefintech.paytouchconsumer.utill.bearerToken
import com.shreefintech.paytouchconsumer.utill.getString
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class PrepaidSmsReceiptViewModel(application: Application) : AndroidViewModel(application) {

    fun getLatestPayments(
        onLoading: () -> Unit,
        onSuccess: (PrepaidVerifyPaymentDataItem) -> Unit,
        onError: (String) -> Unit
    ) {
        if (!Utility.isInternetAvailable(getApplication())) {
            onError(getString(R.string.msgNoInternet))
            return
        }
        onLoading()
        ApiClient.apiService.getPrepaidLatestPayment(
            bearerToken()
        ).enqueue(object : Callback<General<PrepaidVerifyPaymentDataItem>> {
            override fun onResponse(
                call: Call<General<PrepaidVerifyPaymentDataItem>>,
                response: Response<General<PrepaidVerifyPaymentDataItem>>
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
                call: Call<General<PrepaidVerifyPaymentDataItem>>,
                t: Throwable
            ) {
                onError(t.localizedMessage ?: getString(R.string.errGeneric))
            }
        })
    }

}
