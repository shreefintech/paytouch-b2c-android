package com.shreefintech.paytouchconsumer.auth.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.shreefintech.paytouchconsumer.R
import com.shreefintech.paytouchconsumer.retrofit.ApiClient
import com.shreefintech.paytouchconsumer.retrofit.ApiHelper
import com.shreefintech.paytouchconsumer.retrofit.model.UserResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SplashViewModel(application: Application) : AndroidViewModel(application) {

    fun validateSession(
        authorization: String,
        onSuccess: (UserResponse?) -> Unit,
        onError: (String) -> Unit
    ) {
        ApiClient.apiService.getUser(authorization)
            .enqueue(object : Callback<UserResponse> {
                override fun onResponse(call: Call<UserResponse>, response: Response<UserResponse>) {
                    if (response.isSuccessful) {
                        onSuccess(response.body())
                    } else {
                        onError(
                            ApiHelper.parseErrorMessage(
                                getApplication(), response.code(), response.errorBody()?.string()
                            )
                        )
                    }
                }

                override fun onFailure(call: Call<UserResponse>, t: Throwable) {
                    onError(t.localizedMessage ?: getApplication<Application>().getString(R.string.err_generic))
                }
            })
    }
}
