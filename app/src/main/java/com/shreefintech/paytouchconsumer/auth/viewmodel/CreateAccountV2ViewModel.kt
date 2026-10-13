package com.shreefintech.paytouchconsumer.auth.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.shreefintech.paytouchconsumer.Constant
import com.shreefintech.paytouchconsumer.R
import com.shreefintech.paytouchconsumer.retrofit.ApiClient
import com.shreefintech.paytouchconsumer.retrofit.ApiHelper
import com.shreefintech.paytouchconsumer.retrofit.model.CityItem
import com.shreefintech.paytouchconsumer.retrofit.model.General
import com.shreefintech.paytouchconsumer.retrofit.model.StateItem
import com.shreefintech.paytouchconsumer.retrofit.model.auth.RegisterItem
import com.shreefintech.paytouchconsumer.utill.Utility
import com.shreefintech.paytouchconsumer.utill.getString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class CreateAccountV2ViewModel(application: Application) : AndroidViewModel(application) {

    fun loadStates(
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

    fun loadCities(
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

    fun register(
        name: String,
        mobile: String,
        email: String,
        password: String,
        passwordConfirmation: String,
        aadhaarNumber: String,
        aadhaarUri: Uri,
        stateId: String,
        cityId: String,
        referralCode: String,
        onLoading: () -> Unit,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (!Utility.isInternetAvailable(getApplication())) {
            onError(getString(R.string.msgNoInternet))
            return
        }
        onLoading()
        viewModelScope.launch {
            val photoPart = withContext(Dispatchers.IO) {
                Utility.uriToFilePart(getApplication(), aadhaarUri, "aadhaar_photo", "aadhaar")
            }
            if (photoPart == null) {
                onError(getString(R.string.errGeneric))
                return@launch
            }

            ApiClient.apiService.registerV2(
                name.toTextBody(),
                mobile.toTextBody(),
                email.toTextBody(),
                password.toTextBody(),
                passwordConfirmation.toTextBody(),
                aadhaarNumber.toTextBody(),
                photoPart,
                stateId.toTextBody(),
                cityId.toTextBody(),
                referralCode.toTextBody(),
                Constant.FCM_PLATFORM_ANDROID.toTextBody()
            ).enqueue(object : Callback<RegisterItem> {
                override fun onResponse(call: Call<RegisterItem>, response: Response<RegisterItem>) {
                    if (response.isSuccessful) {
                        onSuccess()
                    } else {
                        onError(ApiHelper.parseErrorMessage(getApplication(), response.code(), response.errorBody()?.string()))
                    }
                }

                override fun onFailure(call: Call<RegisterItem>, t: Throwable) {
                    onError(t.localizedMessage ?: getString(R.string.errGeneric))
                }
            })
        }
    }

    private fun String.toTextBody(): RequestBody = toRequestBody("text/plain".toMediaTypeOrNull())
}
