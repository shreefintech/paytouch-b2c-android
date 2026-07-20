package com.shreefintech.paytouchconsumer.onboarding.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.shreefintech.paytouchconsumer.R
import com.shreefintech.paytouchconsumer.utill.Utility
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class UploadKycViewModel(application: Application) : AndroidViewModel(application) {

    fun submitKyc(
        mobile: String,
        memberName: String,
        birthdate: String,
        age: Int,
        address: String,
        city: String,
        email: String,
        panNumber: String,
        aadharNumber: String,
        gstNumber: String,
        onLoading: () -> Unit,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (!Utility.isInternetAvailable(getApplication())) {
            onError(getApplication<Application>().getString(R.string.msgNoInternet))
            return
        }
        onLoading()
        viewModelScope.launch {
            try {
                // TODO(PAYTOUCH-KYC): wire upload-KYC API call
                // val body = JsonObject().apply {
                //     addProperty("mobile", mobile)
                //     addProperty("member_name", memberName)
                //     addProperty("birthdate", birthdate)
                //     addProperty("age", age)
                //     addProperty("address", address)
                //     addProperty("city", city)
                //     addProperty("email", email)
                //     addProperty("pan_number", panNumber)
                //     addProperty("aadhar_number", aadharNumber)
                //     addProperty("gst_number", gstNumber)
                // }
                // val response = ApiClient.apiService.uploadKyc(body)
                // withContext(Dispatchers.Main) {
                //     if (response.isSuccessful && response.body()?.success == true) {
                //         onSuccess()
                //     } else {
                //         onError(ApiHelper.parseErrorMessage(response))
                //     }
                // }
                withContext(Dispatchers.Main) { onSuccess() }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onError(e.message ?: getApplication<Application>().getString(R.string.msgSomethingWentWrong))
                }
            }
        }
    }
}