package com.shreefintech.paytouchconsumer.myaccount.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.shreefintech.paytouchconsumer.R
import com.shreefintech.paytouchconsumer.retrofit.ApiClient
import com.shreefintech.paytouchconsumer.retrofit.ApiHelper
import com.shreefintech.paytouchconsumer.retrofit.model.General
import com.shreefintech.paytouchconsumer.retrofit.model.auth.AuthUserItem
import com.shreefintech.paytouchconsumer.retrofit.model.auth.MeDataItem
import com.shreefintech.paytouchconsumer.retrofit.model.auth.MessageItem
import com.shreefintech.paytouchconsumer.retrofit.model.myaccount.EditProfileRequest
import com.shreefintech.paytouchconsumer.retrofit.model.myaccount.ReferralDataItem
import com.shreefintech.paytouchconsumer.retrofit.model.myaccount.ReferralRewardsItem
import com.shreefintech.paytouchconsumer.retrofit.model.wallet.WalletBalanceItem
import com.shreefintech.paytouchconsumer.utill.Utility
import com.shreefintech.paytouchconsumer.utill.bearerToken
import com.shreefintech.paytouchconsumer.utill.getString
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MyAccountViewModel(application: Application) : AndroidViewModel(application) {

    fun getProfile(
        onLoading: () -> Unit,
        onSuccess: (AuthUserItem) -> Unit,
        onError: (String) -> Unit
    ) {
        if (!Utility.isInternetAvailable(getApplication())) {
            onError(getString(R.string.msgNoInternet))
            return
        }
        onLoading()
        ApiClient.apiService.getMe(bearerToken())
            .enqueue(object : Callback<General<MeDataItem>> {
                override fun onResponse(
                    call: Call<General<MeDataItem>>,
                    response: Response<General<MeDataItem>>
                ) {
                    val user = response.body()?.data?.user
                    if (response.isSuccessful && user != null) {
                        onSuccess(user)
                    } else if (response.isSuccessful) {
                        onError(response.body()?.message ?: getString(R.string.errGeneric))
                    } else {
                        onError(
                            ApiHelper.parseErrorMessage(
                                getApplication(), response.code(), response.errorBody()?.string()
                            )
                        )
                    }
                }

                override fun onFailure(call: Call<General<MeDataItem>>, t: Throwable) {
                    onError(t.localizedMessage ?: getString(R.string.errGeneric))
                }
            })
    }

    fun getWalletBalance(
        onSuccess: (WalletBalanceItem) -> Unit,
        onError: (String) -> Unit
    ) {
        if (!Utility.isInternetAvailable(getApplication())) {
            onError(getString(R.string.msgNoInternet))
            return
        }
        ApiClient.apiService.getWalletBalance(bearerToken())
            .enqueue(object : Callback<General<WalletBalanceItem>> {
                override fun onResponse(
                    call: Call<General<WalletBalanceItem>>,
                    response: Response<General<WalletBalanceItem>>
                ) {
                    val data = response.body()?.data
                    if (response.isSuccessful && data != null) {
                        onSuccess(data)
                    } else if (response.isSuccessful) {
                        onError(response.body()?.message ?: getString(R.string.errGeneric))
                    } else {
                        onError(
                            ApiHelper.parseErrorMessage(
                                getApplication(), response.code(), response.errorBody()?.string()
                            )
                        )
                    }
                }

                override fun onFailure(call: Call<General<WalletBalanceItem>>, t: Throwable) {
                    onError(t.localizedMessage ?: getString(R.string.errGeneric))
                }
            })
    }

    fun getReferralRewards(
        onLoading: () -> Unit,
        onSuccess: (ReferralDataItem) -> Unit,
        onError: (String) -> Unit
    ) {
        if (!Utility.isInternetAvailable(getApplication())) {
            onError(getString(R.string.msgNoInternet))
            return
        }
        onLoading()
        ApiClient.apiService.getReferralRewards(bearerToken())
            .enqueue(object : Callback<ReferralRewardsItem> {
                override fun onResponse(
                    call: Call<ReferralRewardsItem>,
                    response: Response<ReferralRewardsItem>
                ) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        val data = response.body()?.meta?.summary
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

                override fun onFailure(call: Call<ReferralRewardsItem>, t: Throwable) {
                    onError(t.localizedMessage ?: getString(R.string.errGeneric))
                }
            })
    }

    fun updateProfile(
        request: EditProfileRequest,
        onLoading: () -> Unit,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        if (!Utility.isInternetAvailable(getApplication())) {
            onError(getString(R.string.msgNoInternet))
            return
        }
        onLoading()
        ApiClient.apiService.updateMe(bearerToken(), request)
            .enqueue(object : Callback<MessageItem> {
                override fun onResponse(
                    call: Call<MessageItem>,
                    response: Response<MessageItem>
                ) {
                    val body = response.body()
                    // Success flag decides — the screen refetches auth/me for the updated fields
                    if (response.isSuccessful && body?.success == true) {
                        onSuccess(body.message ?: getString(R.string.msgProfileUpdated))
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

                override fun onFailure(call: Call<MessageItem>, t: Throwable) {
                    onError(t.localizedMessage ?: getString(R.string.errGeneric))
                }
            })
    }
}
