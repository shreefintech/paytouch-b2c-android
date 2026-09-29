package com.shreefintech.paytouchconsumer

import android.app.Application
import android.location.Location
import androidx.lifecycle.AndroidViewModel
import com.shreefintech.paytouchconsumer.fcm.NotificationHelper
import com.shreefintech.paytouchconsumer.retrofit.ApiClient
import com.shreefintech.paytouchconsumer.retrofit.ApiHelper
import com.shreefintech.paytouchconsumer.retrofit.model.auth.MessageItem
import com.shreefintech.paytouchconsumer.retrofit.model.location.UserLocationRequest
import com.shreefintech.paytouchconsumer.utill.Utility
import com.shreefintech.paytouchconsumer.utill.bearerToken
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    fun logout(onLoading: () -> Unit, onComplete: () -> Unit, onError: (String) -> Unit) {
        if (!Utility.isInternetAvailable(getApplication())) {
            onError(getApplication<Application>().getString(R.string.msgNoInternet))
            return
        }
        onLoading()
        // Unregister push token first — it needs the bearer token that logout invalidates
        NotificationHelper.removeToken(getApplication()) {
            callLogout(onComplete, onError)
        }
    }

    /** Fire-and-forget — used for payment risk checks; failures are not shown to the user. */
    fun sendLocation(location: Location) {
        if (!Utility.isInternetAvailable(getApplication())) return
        val body = UserLocationRequest(
            latitude = location.latitude,
            longitude = location.longitude,
            accuracy = if (location.hasAccuracy()) location.accuracy.toDouble() else null
        )
        ApiClient.apiService.sendLocation(bearerToken(), body)
            .enqueue(object : Callback<MessageItem> {
                override fun onResponse(call: Call<MessageItem>, response: Response<MessageItem>) {
                    if (!response.isSuccessful) {
                        IllegalStateException("sendLocation failed: HTTP ${response.code()}").printStackTrace()
                    }
                }

                override fun onFailure(call: Call<MessageItem>, t: Throwable) {
                    t.printStackTrace()
                }
            })
    }

    private fun callLogout(onComplete: () -> Unit, onError: (String) -> Unit) {
        ApiClient.apiService.logout(bearerToken())
            .enqueue(object : Callback<MessageItem> {
                override fun onResponse(call: Call<MessageItem>, response: Response<MessageItem>) {
                    // /logout response body only ever contains "message" (no "success" key),
                    // so checking body?.success == true would always be false. isSuccessful alone is correct here.
                    if (response.isSuccessful) {
                        onComplete()
                    } else {
                        onError(ApiHelper.parseErrorMessage(getApplication(), response.code(), response.errorBody()?.string()))
                    }
                }
                override fun onFailure(call: Call<MessageItem>, t: Throwable) {
                    onError(t.localizedMessage ?: getApplication<Application>().getString(R.string.errGeneric))
                }
            })
    }
}
