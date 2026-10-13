package com.shreefintech.paytouchconsumer.utill

import android.app.Application
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import com.shreefintech.paytouchconsumer.R
import com.shreefintech.paytouchconsumer.retrofit.ApiClient
import com.shreefintech.paytouchconsumer.retrofit.ApiHelper
import com.shreefintech.paytouchconsumer.retrofit.model.CityItem
import com.shreefintech.paytouchconsumer.retrofit.model.General
import com.shreefintech.paytouchconsumer.retrofit.model.StateItem
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

fun AndroidViewModel.bearerToken(): String =
    SharedPreferenceHelper.bearerToken(getApplication())

fun AndroidViewModel.getString(@StringRes resId: Int): String =
    getApplication<Application>().getString(resId)

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
