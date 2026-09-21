package com.shreefintech.paytouchconsumer.auth.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import com.shreefintech.paytouchconsumer.Constant
import com.shreefintech.paytouchconsumer.R
import com.shreefintech.paytouchconsumer.retrofit.ApiClient
import com.shreefintech.paytouchconsumer.retrofit.ApiHelper
import com.shreefintech.paytouchconsumer.retrofit.model.General
import com.shreefintech.paytouchconsumer.retrofit.model.auth.LoginItem
import com.shreefintech.paytouchconsumer.utill.SharedPreferenceHelper
import com.shreefintech.paytouchconsumer.utill.Utility
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class CreateAccountViewModel : ViewModel() {

    fun register(
        context: Context,
        name: String,
        mobile: String,
        email: String,
        referralCode: String,
        password: String,
        passwordConfirmation: String,
        onLoading: () -> Unit,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (!Utility.isInternetAvailable(context)) {
            onError(context.getString(R.string.msgNoInternet))
            return
        }
        onLoading()
        ApiClient.apiService.register(name, mobile, email, password, passwordConfirmation, referralCode)
            .enqueue(object : Callback<General<LoginItem?>?> {
                override fun onResponse(
                    call: Call<General<LoginItem?>?>,
                    response: Response<General<LoginItem?>?>
                ) {
                    if (response.isSuccessful) {
                        val data = response.body()?.data
                        data?.let {
                            SharedPreferenceHelper.setSharedPreferenceString(context, Constant.KEY_TOKEN, it.token ?: "")
                            SharedPreferenceHelper.setSharedPreferenceString(context, Constant.KEY_TOKEN_TYPE, it.tokenType ?: "")
                            it.user?.let { user ->
                                SharedPreferenceHelper.setSharedPreferenceString(context, Constant.KEY_USER_ID, user.id?.toString() ?: "")
                                SharedPreferenceHelper.setSharedPreferenceString(context, Constant.KEY_MOBILE, user.mobile ?: "")
                                SharedPreferenceHelper.setSharedPreferenceString(context, Constant.KEY_EMAIL, user.email ?: "")
                                SharedPreferenceHelper.setSharedPreferenceString(context, Constant.KEY_REFERRAL_CODE, user.referralCode ?: "")
                            }
                        }
                        onSuccess()
                    } else {
                        onError(ApiHelper.parseErrorMessage(context, response.code(), response.errorBody()?.string()))
                    }
                }

                override fun onFailure(call: Call<General<LoginItem?>?>, t: Throwable) {
                    onError(t.localizedMessage ?: context.getString(R.string.errGeneric))
                }
            })
    }
}
