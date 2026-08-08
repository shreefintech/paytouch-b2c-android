package com.shreefintech.paytouchconsumer.dth.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.shreefintech.paytouchconsumer.R
import com.shreefintech.paytouchconsumer.retrofit.ApiClient
import com.shreefintech.paytouchconsumer.retrofit.ApiHelper
import com.shreefintech.paytouchconsumer.retrofit.model.General
import com.shreefintech.paytouchconsumer.retrofit.model.dth.DthLatestPaymentDataItem
import com.shreefintech.paytouchconsumer.utill.Utility
import com.shreefintech.paytouchconsumer.utill.bearerToken
import com.shreefintech.paytouchconsumer.utill.getString
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class DthSmsReceiptViewModel(application: Application) : AndroidViewModel(application) {

    fun getLatestPayment(
        onLoading: () -> Unit,
        onSuccess: (DthLatestPaymentDataItem) -> Unit,
        onError: (String) -> Unit
    ) {
        if (!Utility.isInternetAvailable(getApplication())) {
            onError(getString(R.string.msgNoInternet))
            return
        }
        onLoading()
        ApiClient.apiService.getDthLatestPayment(bearerToken())
            .enqueue(object : Callback<General<DthLatestPaymentDataItem>> {
                override fun onResponse(
                    call: Call<General<DthLatestPaymentDataItem>>,
                    response: Response<General<DthLatestPaymentDataItem>>
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
                    call: Call<General<DthLatestPaymentDataItem>>,
                    t: Throwable
                ) {
                    onError(t.localizedMessage ?: getString(R.string.errGeneric))
                }
            })
    }
}
