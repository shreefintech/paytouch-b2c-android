package com.shreefintech.paytouchconsumer.auth.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import com.shreefintech.paytouchconsumer.Constant
import com.shreefintech.paytouchconsumer.R
import com.shreefintech.paytouchconsumer.retrofit.ApiClient
import com.shreefintech.paytouchconsumer.retrofit.ApiHelper
import com.shreefintech.paytouchconsumer.retrofit.model.auth.RegisterItem
import com.shreefintech.paytouchconsumer.utill.SharedPreferenceHelper
import com.shreefintech.paytouchconsumer.utill.Utility
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class CreateAccountViewModel : ViewModel() {

    fun register(
        context: Context,
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
        ApiClient.apiService.register(mobile, email, password, passwordConfirmation, referralCode)
            .enqueue(object : Callback<RegisterItem> {
                override fun onResponse(call: Call<RegisterItem>, response: Response<RegisterItem>) {
                    if (response.isSuccessful) {
                        val data = response.body()
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
                        val msg = ApiHelper.parseErrorMessage(context, response.code(), response.errorBody()?.string())
                        onError(msg)
                    }
                }

                override fun onFailure(call: Call<RegisterItem>, t: Throwable) {
                    onError(t.localizedMessage ?: context.getString(R.string.err_generic))
                }
            })
    }
}
