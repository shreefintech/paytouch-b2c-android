package com.shreefintech.paytouchconsumer.utill

import android.app.Application
import android.location.Location
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import com.shreefintech.paytouchconsumer.Constant
import com.shreefintech.paytouchconsumer.R
import com.shreefintech.paytouchconsumer.enums.NextStep
import com.shreefintech.paytouchconsumer.retrofit.ApiClient
import com.shreefintech.paytouchconsumer.retrofit.ApiHelper
import com.shreefintech.paytouchconsumer.retrofit.model.CityItem
import com.shreefintech.paytouchconsumer.retrofit.model.General
import com.shreefintech.paytouchconsumer.retrofit.model.StateItem
import com.shreefintech.paytouchconsumer.retrofit.model.auth.MessageItem
import com.shreefintech.paytouchconsumer.retrofit.model.location.UserLocationRequest
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

fun AndroidViewModel.bearerToken(): String =
    SharedPreferenceHelper.bearerToken(getApplication())

fun AndroidViewModel.getString(@StringRes resId: Int): String =
    getApplication<Application>().getString(resId)

/** Sources: auth/login, auth/me, auth/mpin. Absent keys leave the saved value. */
fun AndroidViewModel.saveOnboardingFlags(bankDetailsAdded: Boolean?, panAdded: Boolean?, nextStep: String?) {
    // bank_details_required forces the Add Bank Details popup before the first money action
    val bankAdded = if (NextStep.from(nextStep) == NextStep.BANK_DETAILS_REQUIRED) false else bankDetailsAdded
    bankAdded?.let {
        SharedPreferenceHelper.setSharedPreferenceBoolean(getApplication(), Constant.KEY_BANK_DETAILS_ADDED, it)
    }
    panAdded?.let {
        SharedPreferenceHelper.setSharedPreferenceBoolean(getApplication(), Constant.KEY_PAN_ADDED, it)
    }
}

/**
 * POST location — required once per app open before billpay/pay (403 LOCATION_REQUIRED otherwise).
 * Sent from Home and resent by Bill Pay after that 403. Failures are not shown to the user.
 */
fun AndroidViewModel.sendLocation(location: Location) {
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

fun AndroidViewModel.loadAddressStates(
    onLoading: () -> Unit,
    onSuccess: (List<StateItem>) -> Unit,
    onError: (String) -> Unit
) {
    if (!Utility.isInternetAvailable(getApplication())) {
        onError(getString(R.string.msgNoInternet))
        return
    }
    onLoading()
    ApiClient.apiService.getAddressStates().enqueue(object : Callback<General<List<StateItem>>> {
        override fun onResponse(
            call: Call<General<List<StateItem>>>,
            response: Response<General<List<StateItem>>>
        ) {
            val states = response.body()?.data
            if (response.isSuccessful && states != null) {
                onSuccess(states)
            } else {
                onError(ApiHelper.parseErrorMessage(getApplication(), response.code(), response.errorBody()?.string()))
            }
        }

        override fun onFailure(call: Call<General<List<StateItem>>>, t: Throwable) {
            onError(t.localizedMessage ?: getString(R.string.errGeneric))
        }
    })
}

fun AndroidViewModel.loadAddressCities(
    stateId: String,
    onLoading: () -> Unit,
    onSuccess: (List<CityItem>) -> Unit,
    onError: (String) -> Unit
) {
    if (!Utility.isInternetAvailable(getApplication())) {
        onError(getString(R.string.msgNoInternet))
        return
    }
    onLoading()
    ApiClient.apiService.getAddressCities(stateId).enqueue(object : Callback<General<List<CityItem>>> {
        override fun onResponse(
            call: Call<General<List<CityItem>>>,
            response: Response<General<List<CityItem>>>
        ) {
            val cities = response.body()?.data
            if (response.isSuccessful && cities != null) {
                onSuccess(cities)
            } else {
                onError(ApiHelper.parseErrorMessage(getApplication(), response.code(), response.errorBody()?.string()))
            }
        }

        override fun onFailure(call: Call<General<List<CityItem>>>, t: Throwable) {
            onError(t.localizedMessage ?: getString(R.string.errGeneric))
        }
    })
}
