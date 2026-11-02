package com.shreefintech.paytouchconsumer.myaccount.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.shreefintech.paytouchconsumer.R
import com.shreefintech.paytouchconsumer.retrofit.ApiClient
import com.shreefintech.paytouchconsumer.retrofit.ApiHelper
import com.shreefintech.paytouchconsumer.retrofit.model.General
import com.shreefintech.paytouchconsumer.retrofit.model.myaccount.AccountInfoItem
import com.shreefintech.paytouchconsumer.retrofit.model.myaccount.EditProfileDataItem
import com.shreefintech.paytouchconsumer.retrofit.model.myaccount.EditProfileRequest
import com.shreefintech.paytouchconsumer.retrofit.model.myaccount.ReferralDataItem
import com.shreefintech.paytouchconsumer.retrofit.model.myaccount.ReferralInfoItem
import com.shreefintech.paytouchconsumer.utill.Utility
import com.shreefintech.paytouchconsumer.utill.bearerToken
import com.shreefintech.paytouchconsumer.utill.getString
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MyAccountViewModel(application: Application) : AndroidViewModel(application) {

    fun getAccountInfo(
        onLoading: () -> Unit,
        onSuccess: (AccountInfoItem) -> Unit,
        onError: (String) -> Unit
    ) {
        if (!Utility.isInternetAvailable(getApplication())) {
            onError(getString(R.string.msgNoInternet))
            return
        }
        onLoading()
        ApiClient.apiService.getAccountOverview(bearerToken())
            .enqueue(object : Callback<AccountInfoItem> {
                override fun onResponse(
                    call: Call<AccountInfoItem>,
                    response: Response<AccountInfoItem>
                ) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        val data = response.body()
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

    fun updateProfile(
        request: EditProfileRequest,
        onLoading: () -> Unit,
        onSuccess: (EditProfileDataItem, String) -> Unit,
        onError: (String) -> Unit
    ) {
        if (!Utility.isInternetAvailable(getApplication())) {
            onError(getString(R.string.msgNoInternet))
            return
        }
        onLoading()
        ApiClient.apiService.updateProfile(bearerToken(), request)
            .enqueue(object : Callback<General<EditProfileDataItem>> {
                override fun onResponse(
                    call: Call<General<EditProfileDataItem>>,
                    response: Response<General<EditProfileDataItem>>
                ) {
                    val body = response.body()
                    val data = body?.data
                    if (response.isSuccessful && body?.success == true && data != null) {
                        onSuccess(data, body.message ?: getString(R.string.msgProfileUpdated))
                    } else if (response.isSuccessful) {
                        // HTTP 200 with success:false — errorBody() is null, the reason is in body.message
                        onError(body?.message ?: getString(R.string.errGeneric))
                    } else {
                        onError(
                            ApiHelper.parseErrorMessage(
                                getApplication(), response.code(), response.errorBody()?.string()
                            )
                        )
                    }
                }

                override fun onFailure(call: Call<General<EditProfileDataItem>>, t: Throwable) {
                    onError(t.localizedMessage ?: getString(R.string.errGeneric))
                }
            })
    }
}
