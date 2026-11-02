package com.shreefintech.paytouchconsumer.auth.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.shreefintech.paytouchconsumer.R
import com.shreefintech.paytouchconsumer.retrofit.ApiClient
import com.shreefintech.paytouchconsumer.retrofit.ApiHelper
import com.shreefintech.paytouchconsumer.retrofit.model.General
import com.shreefintech.paytouchconsumer.retrofit.model.auth.CreateMpinRequest
import com.shreefintech.paytouchconsumer.retrofit.model.auth.MeDataItem
import com.shreefintech.paytouchconsumer.retrofit.model.auth.MessageItem
import com.shreefintech.paytouchconsumer.retrofit.model.auth.ResetCredentialRequest
import com.shreefintech.paytouchconsumer.utill.Utility
import com.shreefintech.paytouchconsumer.utill.bearerToken
import com.shreefintech.paytouchconsumer.utill.saveOnboardingFlags
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ResetMpinViewModel(application: Application) : AndroidViewModel(application) {

    fun changeMpin(
        resetToken: String,
        newMpin: String,
        onLoading: () -> Unit,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (!Utility.isInternetAvailable(getApplication())) {
            onError(getApplication<Application>().getString(R.string.msgNoInternet))
            return
        }
        onLoading()
        ApiClient.apiService.resetCredential(ResetCredentialRequest(resetToken, "mpin", newMpin))
            .enqueue(object : Callback<MessageItem> {
                override fun onResponse(call: Call<MessageItem>, response: Response<MessageItem>) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        onSuccess()
                    } else {
                        onError(ApiHelper.parseErrorMessage(getApplication(), response.code(), response.errorBody()?.string()))
                    }
                }

                override fun onFailure(call: Call<MessageItem>, t: Throwable) {
                    onError(t.localizedMessage ?: getApplication<Application>().getString(R.string.errGeneric))
                }
            })
    }

    /** Response has the same shape as auth/me — the caller routes again from its `next_step`. */
    fun createMpin(
        mpin: String,
        onLoading: () -> Unit,
        onSuccess: (MeDataItem?) -> Unit,
        onError: (String) -> Unit
    ) {
        if (!Utility.isInternetAvailable(getApplication())) {
            onError(getApplication<Application>().getString(R.string.msgNoInternet))
            return
        }
        onLoading()
        ApiClient.apiService.createMpin(bearerToken(), CreateMpinRequest(mpin, mpin))
            .enqueue(object : Callback<General<MeDataItem>> {
                override fun onResponse(call: Call<General<MeDataItem>>, response: Response<General<MeDataItem>>) {
                    if (response.isSuccessful) {
                        val data = response.body()?.data
                        saveOnboardingFlags(data?.bankDetailsAdded, data?.panAdded, data?.nextStep)
                        onSuccess(data)
                    } else {
                        onError(ApiHelper.parseErrorMessage(getApplication(), response.code(), response.errorBody()?.string()))
                    }
                }

                override fun onFailure(call: Call<General<MeDataItem>>, t: Throwable) {
                    onError(t.localizedMessage ?: getApplication<Application>().getString(R.string.errGeneric))
                }
            })
    }
}
