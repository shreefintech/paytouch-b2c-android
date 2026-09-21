package com.shreefintech.paytouchconsumer.auth.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.shreefintech.paytouchconsumer.R
import com.shreefintech.paytouchconsumer.retrofit.ApiClient
import com.shreefintech.paytouchconsumer.retrofit.ApiHelper
import com.shreefintech.paytouchconsumer.retrofit.model.General
import com.shreefintech.paytouchconsumer.retrofit.model.auth.MeDataItem
import com.shreefintech.paytouchconsumer.utill.Utility
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SplashViewModel(application: Application) : AndroidViewModel(application) {

    fun validateSession(
        authorization: String,
        onSuccess: (MeDataItem?) -> Unit,
        onError: (String) -> Unit
    ) {
        if (!Utility.isInternetAvailable(getApplication())) {
            onError(getApplication<Application>().getString(R.string.msgNoInternet))
            return
        }
        ApiClient.apiService.getMe(authorization)
            .enqueue(object : Callback<General<MeDataItem?>?> {
                override fun onResponse(
                    call: Call<General<MeDataItem?>?>,
                    response: Response<General<MeDataItem?>?>
                ) {
                    if (response.isSuccessful) {
                        onSuccess(response.body()?.data)
                    } else {
                        onError(
                            ApiHelper.parseErrorMessage(
                                getApplication(), response.code(), response.errorBody()?.string()
                            )
                        )
                    }
                }

                override fun onFailure(call: Call<General<MeDataItem?>?>, t: Throwable) {
                    onError(t.localizedMessage ?: getApplication<Application>().getString(R.string.errGeneric))
                }
            })
    }
}
