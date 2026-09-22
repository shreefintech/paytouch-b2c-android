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
import com.shreefintech.paytouchconsumer.retrofit.model.auth.LoginDataItem
import com.shreefintech.paytouchconsumer.retrofit.model.auth.LoginRequest
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
        onSuccess: (LoginDataItem?) -> Unit,
        onError: (String) -> Unit
    ) {
        if (!Utility.isInternetAvailable(getApplication())) {
            onError(getApplication<Application>().getString(R.string.msgNoInternet))
            return
        }
        onLoading()
        val type = if (mode == LoginMode.PASSWORD) "password" else "mpin"
        ApiClient.apiService.login(LoginRequest(mobile, credential, type))
            .enqueue(object : Callback<General<LoginDataItem>> {
                override fun onResponse(
                    call: Call<General<LoginDataItem>>,
                    response: Response<General<LoginDataItem>>
                ) {
                    if (response.isSuccessful && response.body()?.data != null) {
                        val data = response.body()!!.data
                        data?.let { saveSession(it) }
                        onSuccess(data)
                    } else {
                        val msg = ApiHelper.parseErrorMessage(
                            getApplication(), response.code(), response.errorBody()?.string()
                        )
                        onError(msg)
                    }
                }

                override fun onFailure(call: Call<General<LoginDataItem>>, t: Throwable) {
                    onError(
                        t.localizedMessage
                            ?: getApplication<Application>().getString(R.string.errGeneric)
                    )
                }
            })
    }

    private fun saveSession(data: LoginDataItem) {
        SharedPreferenceHelper.setSharedPreferenceString(
            getApplication(), Constant.KEY_LAST_INTERACTION, System.currentTimeMillis().toString()
        )
        SharedPreferenceHelper.setSharedPreferenceString(
            getApplication(), Constant.KEY_TOKEN, data.token ?: ""
        )
        SharedPreferenceHelper.setSharedPreferenceString(
            getApplication(), Constant.KEY_TOKEN_TYPE, data.tokenType ?: ""
        )
        SharedPreferenceHelper.setSharedPreferenceString(
            getApplication(), Constant.KEY_WALLET_BALANCE, "0.00"
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
                user.id ?: 0, user.mobile ?: "", user.email ?: "", user.referralCode ?: ""
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
