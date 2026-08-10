package com.shreefintech.paytouchconsumer.municipaltax.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.shreefintech.paytouchconsumer.R
import com.shreefintech.paytouchconsumer.retrofit.ApiClient
import com.shreefintech.paytouchconsumer.retrofit.ApiHelper
import com.shreefintech.paytouchconsumer.retrofit.model.General
import com.shreefintech.paytouchconsumer.retrofit.model.municipaltax.MunicipalTaxLatestPaymentDataItem
import com.shreefintech.paytouchconsumer.utill.Utility
import com.shreefintech.paytouchconsumer.utill.bearerToken
import com.shreefintech.paytouchconsumer.utill.getString
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MunicipalTaxSmsReceiptViewModel(application: Application) : AndroidViewModel(application) {

    fun getLatestPayments(
        onLoading: () -> Unit,
        onSuccess: (MunicipalTaxLatestPaymentDataItem) -> Unit,
        onError: (String) -> Unit
    ) {
        if (!Utility.isInternetAvailable(getApplication())) {
            onError(getString(R.string.msgNoInternet))
            return
        }
        onLoading()
        ApiClient.apiService.getMunicipalTaxLatestPayment(
            bearerToken()
        ).enqueue(object : Callback<General<MunicipalTaxLatestPaymentDataItem>> {
            override fun onResponse(
                call: Call<General<MunicipalTaxLatestPaymentDataItem>>,
                response: Response<General<MunicipalTaxLatestPaymentDataItem>>
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
                call: Call<General<MunicipalTaxLatestPaymentDataItem>>,
                t: Throwable
            ) {
                onError(t.localizedMessage ?: getString(R.string.errGeneric))
            }
        })
    }
}
