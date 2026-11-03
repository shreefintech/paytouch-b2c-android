package com.shreefintech.paytouchconsumer.kyc

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.shreefintech.paytouchconsumer.R
import com.shreefintech.paytouchconsumer.retrofit.ApiClient
import com.shreefintech.paytouchconsumer.retrofit.ApiHelper
import com.shreefintech.paytouchconsumer.retrofit.model.General
import com.shreefintech.paytouchconsumer.retrofit.model.auth.MeDataItem
import com.shreefintech.paytouchconsumer.retrofit.model.kyc.KycMyAccountItem
import com.shreefintech.paytouchconsumer.utill.Utility
import com.shreefintech.paytouchconsumer.utill.bearerToken
import com.shreefintech.paytouchconsumer.utill.getString
import com.shreefintech.paytouchconsumer.utill.saveOnboardingFlags
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class KycStatusViewModel(application: Application) : AndroidViewModel(application) {

    /** auth/me is the KYC source of truth — the status screen redraws from its `next_step`. */
    fun fetchStatus(
        onLoading: () -> Unit,
        onReady: (MeDataItem) -> Unit,
        onError: (String) -> Unit
    ) {
        if (!Utility.isInternetAvailable(getApplication())) {
            onError(getString(R.string.msgNoInternet))
            return
        }
        onLoading()
        ApiClient.apiService.getMe(bearerToken())
            .enqueue(object : Callback<General<MeDataItem>> {
                override fun onResponse(call: Call<General<MeDataItem>>, response: Response<General<MeDataItem>>) {
                    val data = response.body()?.data
                    if (response.isSuccessful && data != null) {
                        saveOnboardingFlags(data.bankDetailsAdded, data.panAdded, data.nextStep)
                        onReady(data)
                    } else {
                        onError(ApiHelper.parseErrorMessage(getApplication(), response.code(), response.errorBody()?.string()))
                    }
                }

                override fun onFailure(call: Call<General<MeDataItem>>, t: Throwable) {
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
