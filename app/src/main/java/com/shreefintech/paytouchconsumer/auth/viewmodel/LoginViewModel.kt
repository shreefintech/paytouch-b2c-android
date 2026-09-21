package com.shreefintech.paytouchconsumer.auth.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.shreefintech.paytouchconsumer.Constant
import com.shreefintech.paytouchconsumer.R
import com.shreefintech.paytouchconsumer.enums.LoginMode
import com.shreefintech.paytouchconsumer.retrofit.ApiAdminClient
import com.shreefintech.paytouchconsumer.retrofit.ApiClient
import com.shreefintech.paytouchconsumer.retrofit.ApiHelper
import com.shreefintech.paytouchconsumer.retrofit.model.General
import com.shreefintech.paytouchconsumer.retrofit.model.auth.LoginItem
import com.shreefintech.paytouchconsumer.utill.SharedPreferenceHelper
import com.shreefintech.paytouchconsumer.utill.Utility
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LoginViewModel(application: Application) : AndroidViewModel(application) {

    fun login(
        mobile: String,
        credential: String,
        mode: LoginMode,
        onLoading: () -> Unit,
        onSuccess: (LoginItem?) -> Unit,
        onError: (String) -> Unit
    ) {
        if (!Utility.isInternetAvailable(getApplication())) {
            onError(getApplication<Application>().getString(R.string.msgNoInternet))
            return
        }
        onLoading()
        val type = if (mode == LoginMode.PASSWORD) "password" else "mpin"
        ApiClient.apiService.login(mobile, credential, type)
            .enqueue(object : Callback<General<LoginItem?>?> {
                override fun onResponse(
                    call: Call<General<LoginItem?>?>,
                    response: Response<General<LoginItem?>?>
                ) {
                    if (response.isSuccessful) {
                        val data = response.body()?.data
                        data?.let { saveSession(it) }
                        onSuccess(data)
                    } else {
                        onError(
                            ApiHelper.parseErrorMessage(
                                getApplication(), response.code(), response.errorBody()?.string()
                            )
                        )
                    }
                }

                override fun onFailure(call: Call<General<LoginItem?>?>, t: Throwable) {
                    onError(
                        t.localizedMessage
                            ?: getApplication<Application>().getString(R.string.errGeneric)
                    )
                }
            })
    }

    private fun saveSession(data: LoginItem) {
        SharedPreferenceHelper.setSharedPreferenceString(
            getApplication(), Constant.KEY_TOKEN, data.token ?: ""
        )
        SharedPreferenceHelper.setSharedPreferenceString(
            getApplication(), Constant.KEY_TOKEN_TYPE, data.tokenType ?: ""
        )
        data.user?.let { user ->
            SharedPreferenceHelper.setSharedPreferenceString(
                getApplication(), Constant.KEY_USER_ID, user.id?.toString() ?: ""
            )
            SharedPreferenceHelper.setSharedPreferenceString(
                getApplication(), Constant.KEY_MOBILE, user.mobile ?: ""
            )
            SharedPreferenceHelper.setSharedPreferenceString(
                getApplication(), Constant.KEY_EMAIL, user.email ?: ""
            )
            SharedPreferenceHelper.setSharedPreferenceString(
                getApplication(), Constant.KEY_REFERRAL_CODE, user.referralCode ?: ""
            )
            fireVpsRegistration(
                user.id ?: 0,
                user.mobile ?: "",
                user.email ?: "",
                user.referralCode ?: ""
            )
        }
    }

    private fun fireVpsRegistration(id: Int, mobile: String, email: String, referralCode: String) {
        if (!Utility.isInternetAvailable(getApplication())) return
        ApiAdminClient.apiService.registerUser(id, mobile, email, mobile, referralCode)
            .enqueue(object : Callback<Any> {
                override fun onResponse(call: Call<Any>, response: Response<Any>) {}
                override fun onFailure(call: Call<Any>, t: Throwable) { t.printStackTrace() }
            })
    }
}
