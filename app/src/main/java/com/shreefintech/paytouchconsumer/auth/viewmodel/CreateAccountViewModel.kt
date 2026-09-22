package com.shreefintech.paytouchconsumer.auth.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import com.shreefintech.paytouchconsumer.Constant
import com.shreefintech.paytouchconsumer.R
import com.shreefintech.paytouchconsumer.retrofit.ApiClient
import com.shreefintech.paytouchconsumer.retrofit.ApiHelper
import com.shreefintech.paytouchconsumer.retrofit.model.General
import com.shreefintech.paytouchconsumer.retrofit.model.auth.LoginDataItem
import com.shreefintech.paytouchconsumer.retrofit.model.auth.RegisterRequest
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
        ApiClient.apiService.register(
            RegisterRequest(name, mobile, email, password, passwordConfirmation, referralCode)
        ).enqueue(object : Callback<General<LoginDataItem>> {
            override fun onResponse(
                call: Call<General<LoginDataItem>>,
                response: Response<General<LoginDataItem>>
            ) {
                if (response.isSuccessful) {
                    response.body()?.data?.let { data ->
                        SharedPreferenceHelper.setSharedPreferenceString(context, Constant.KEY_TOKEN, data.token ?: "")
                        SharedPreferenceHelper.setSharedPreferenceString(context, Constant.KEY_TOKEN_TYPE, data.tokenType ?: "")
                        SharedPreferenceHelper.setSharedPreferenceString(context, Constant.KEY_WALLET_BALANCE, "0.00")
                        data.user?.let { user ->
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

            override fun onFailure(call: Call<General<LoginDataItem>>, t: Throwable) {
                onError(t.localizedMessage ?: context.getString(R.string.errGeneric))
            }
        })
    }
}
