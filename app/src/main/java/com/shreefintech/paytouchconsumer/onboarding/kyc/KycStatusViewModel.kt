package com.shreefintech.paytouchconsumer.onboarding.kyc

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.shreefintech.paytouchconsumer.R
import com.shreefintech.paytouchconsumer.retrofit.ApiClient
import com.shreefintech.paytouchconsumer.retrofit.ApiHelper
import com.shreefintech.paytouchconsumer.retrofit.model.kyc.KycMyAccountItem
import com.shreefintech.paytouchconsumer.retrofit.model.kyc.KycStatusItem
import com.shreefintech.paytouchconsumer.utill.Utility
import com.shreefintech.paytouchconsumer.utill.bearerToken
import com.shreefintech.paytouchconsumer.utill.getString
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class KycStatusViewModel(application: Application) : AndroidViewModel(application) {

    fun fetchStatus(
        onLoading: () -> Unit,
        onReady: (KycStatusItem) -> Unit,
        onError: (String) -> Unit
    ) {
        if (!Utility.isInternetAvailable(getApplication())) {
            onError(getString(R.string.msgNoInternet))
            return
        }
        onLoading()
        ApiClient.apiService.getKycStatus(bearerToken())
            .enqueue(object : Callback<KycStatusItem> {
                override fun onResponse(call: Call<KycStatusItem>, response: Response<KycStatusItem>) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        onReady(response.body()!!)
                    } else {
                        onError(ApiHelper.parseErrorMessage(getApplication(), response.code(), response.errorBody()?.string()))
                    }
                }

                override fun onFailure(call: Call<KycStatusItem>, t: Throwable) {
                    onError(t.localizedMessage ?: getString(R.string.errGeneric))
                }
            })
    }

    fun fetchMyAccount(
        onLoading: () -> Unit,
        onReady: (KycMyAccountItem) -> Unit,
        onError: (String) -> Unit
    ) {
        if (!Utility.isInternetAvailable(getApplication())) {
            onError(getString(R.string.msgNoInternet))
            return
        }
        onLoading()
        ApiClient.apiService.getKycMyAccount(bearerToken())
            .enqueue(object : Callback<KycMyAccountItem> {
                override fun onResponse(call: Call<KycMyAccountItem>, response: Response<KycMyAccountItem>) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        onReady(response.body()!!)
                    } else {
                        onError(ApiHelper.parseErrorMessage(getApplication(), response.code(), response.errorBody()?.string()))
                    }
                }

                override fun onFailure(call: Call<KycMyAccountItem>, t: Throwable) {
                    onError(t.localizedMessage ?: getString(R.string.errGeneric))
                }
            })
    }
}
