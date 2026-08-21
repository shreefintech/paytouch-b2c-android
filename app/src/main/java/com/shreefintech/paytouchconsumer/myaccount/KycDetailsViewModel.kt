package com.shreefintech.paytouchconsumer.myaccount

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.shreefintech.paytouchconsumer.R
import com.shreefintech.paytouchconsumer.retrofit.ApiClient
import com.shreefintech.paytouchconsumer.retrofit.ApiHelper
import com.shreefintech.paytouchconsumer.retrofit.model.kyc.KycMyAccountItem
import com.shreefintech.paytouchconsumer.utill.Utility
import com.shreefintech.paytouchconsumer.utill.bearerToken
import com.shreefintech.paytouchconsumer.utill.getString
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class KycDetailsViewModel(application: Application) : AndroidViewModel(application) {

    fun fetchMyAccount(
        onLoading: () -> Unit,
        onReady: (KycMyAccountItem) -> Unit,
        onError: (String) -> Unit
    ) {
        if (!Utility.isInternetAvailable(getApplication())) {
            onError(getString(R.string.msgNoInternet))
            return
        }
        onLoading()
        ApiClient.apiService.getKycMyAccount(bearerToken())
            .enqueue(object : Callback<KycMyAccountItem> {
                override fun onResponse(
                    call: Call<KycMyAccountItem>,
                    response: Response<KycMyAccountItem>
                ) {
                    val body = response.body()
                    if (response.isSuccessful && body?.success == true) {
                        onReady(body)
                    } else {
                        onError(
                            ApiHelper.parseErrorMessage(
                                getApplication(), response.code(), response.errorBody()?.string()
                            )
                        )
                    }
                }

                override fun onFailure(call: Call<KycMyAccountItem>, t: Throwable) {
                    onError(t.localizedMessage ?: getString(R.string.errGeneric))
                }
            })
    }
}
