package com.shreefintech.paytouchconsumer.myaccount.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.shreefintech.paytouchconsumer.Constant
import com.shreefintech.paytouchconsumer.R
import com.shreefintech.paytouchconsumer.retrofit.ApiClient
import com.shreefintech.paytouchconsumer.retrofit.ApiHelper
import com.shreefintech.paytouchconsumer.retrofit.model.myaccount.AccountInfoDataItem
import com.shreefintech.paytouchconsumer.retrofit.model.myaccount.AccountInfoItem
import com.shreefintech.paytouchconsumer.retrofit.model.myaccount.ReferralDataItem
import com.shreefintech.paytouchconsumer.retrofit.model.myaccount.ReferralInfoItem
import com.shreefintech.paytouchconsumer.utill.SharedPreferenceHelper
import com.shreefintech.paytouchconsumer.utill.Utility
import com.shreefintech.paytouchconsumer.utill.bearerToken
import com.shreefintech.paytouchconsumer.utill.getString
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MyAccountViewModel(application: Application) : AndroidViewModel(application) {

    fun getAccountInfo(
        onLoading: () -> Unit,
        onSuccess: (AccountInfoDataItem) -> Unit,
        onError: (String) -> Unit
    ) {
        if (!Utility.isInternetAvailable(getApplication())) {
            onError(getString(R.string.msgNoInternet))
            return
        }
        val userId = SharedPreferenceHelper.getSharedPreferenceString(
            getApplication(), Constant.KEY_USER_ID, null
        ) ?: ""
        onLoading()
        ApiClient.apiService.getKycAccountInfo(bearerToken(), userId)
            .enqueue(object : Callback<AccountInfoItem> {
                override fun onResponse(
                    call: Call<AccountInfoItem>,
                    response: Response<AccountInfoItem>
                ) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        val data = response.body()?.kycData
                        if (data != null) {
                            onSuccess(data)
                        } else {
                            onError(getString(R.string.errGeneric))
                        }
                    } else {
                        onError(
                            ApiHelper.parseErrorMessage(
                                getApplication(), response.code(), response.errorBody()?.string()
                            )
                        )
                    }
                }

                override fun onFailure(call: Call<AccountInfoItem>, t: Throwable) {
                    onError(t.localizedMessage ?: getString(R.string.errGeneric))
                }
            })
    }

    fun getReferralInfo(
        onLoading: () -> Unit,
        onSuccess: (ReferralDataItem) -> Unit,
        onError: (String) -> Unit
    ) {
        if (!Utility.isInternetAvailable(getApplication())) {
            onError(getString(R.string.msgNoInternet))
            return
        }
        onLoading()
        ApiClient.apiService.getReferralInfo(bearerToken())
            .enqueue(object : Callback<ReferralInfoItem> {
                override fun onResponse(
                    call: Call<ReferralInfoItem>,
                    response: Response<ReferralInfoItem>
                ) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        val data = response.body()?.data
                        if (data != null) {
                            onSuccess(data)
                        } else {
                            onError(getString(R.string.errGeneric))
                        }
                    } else {
                        onError(
                            ApiHelper.parseErrorMessage(
                                getApplication(), response.code(), response.errorBody()?.string()
                            )
                        )
                    }
                }

                override fun onFailure(call: Call<ReferralInfoItem>, t: Throwable) {
                    onError(t.localizedMessage ?: getString(R.string.errGeneric))
                }
            })
    }
}
