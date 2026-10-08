package com.shreefintech.paytouchconsumer

import android.app.Application
import android.location.Location
import androidx.lifecycle.AndroidViewModel
import com.shreefintech.paytouchconsumer.Constant
import com.shreefintech.paytouchconsumer.retrofit.ApiClient
import com.shreefintech.paytouchconsumer.retrofit.ApiHelper
import com.shreefintech.paytouchconsumer.retrofit.model.General
import com.shreefintech.paytouchconsumer.retrofit.model.auth.LogoutRequest
import com.shreefintech.paytouchconsumer.retrofit.model.auth.MessageItem
import com.shreefintech.paytouchconsumer.retrofit.model.location.UserLocationRequest
import com.shreefintech.paytouchconsumer.retrofit.model.rewards.RewardsLevelItem
import com.shreefintech.paytouchconsumer.rewards.RankTier
import com.shreefintech.paytouchconsumer.utill.SharedPreferenceHelper
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
        callLogout(onComplete, onError)
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

    /**
     * Silently checks /api/level on every Dashboard appear.
     * Saves the level first so each level shows exactly once.
     * No toast on failure — matches iOS "the level check is silent" spec.
     */
    fun checkLevelUp(onLevelUp: (RewardsLevelItem) -> Unit) {
        if (!Utility.isInternetAvailable(getApplication())) return
        ApiClient.apiService.getRewardsLevel(bearerToken())
            .enqueue(object : Callback<General<RewardsLevelItem?>> {
                override fun onResponse(
                    call: Call<General<RewardsLevelItem?>>,
                    response: Response<General<RewardsLevelItem?>>
                ) {
                    val data = if (response.isSuccessful) response.body()?.data else null
                    if (data == null) return
                    val newLevel = data.level ?: return
                    val saved = SharedPreferenceHelper.getSharedPreferenceString(
                        getApplication(), Constant.KEY_LAST_SEEN_LEVEL, ""
                    ).orEmpty()
                    SharedPreferenceHelper.setSharedPreferenceString(
                        getApplication(), Constant.KEY_LAST_SEEN_LEVEL, newLevel
                    )
                    if (RankTier.shouldShowLevelUp(newLevel, saved)) onLevelUp(data)
                }

                override fun onFailure(call: Call<General<RewardsLevelItem?>>, t: Throwable) {
                    t.printStackTrace()
                }
            })
    }

    private fun callLogout(onComplete: () -> Unit, onError: (String) -> Unit) {
        val fcmToken = SharedPreferenceHelper.getSharedPreferenceString(
            getApplication(), Constant.KEY_FCM_TOKEN, ""
        )?.ifEmpty { null }
        ApiClient.apiService.logout(bearerToken(), LogoutRequest(fcmToken))
            .enqueue(object : Callback<MessageItem> {
                override fun onResponse(call: Call<MessageItem>, response: Response<MessageItem>) {
                    // /logout response body only ever contains "message" (no "success" key),
                    // so checking body?.success == true would always be false. isSuccessful alone is correct here.
                    if (response.isSuccessful) {
                        SharedPreferenceHelper.setSharedPreferenceString(
                            getApplication(), Constant.KEY_FCM_TOKEN, ""
                        )
                        SharedPreferenceHelper.setSharedPreferenceBoolean(
                            getApplication(), Constant.KEY_FCM_TOKEN_AUTHED, false
                        )
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
