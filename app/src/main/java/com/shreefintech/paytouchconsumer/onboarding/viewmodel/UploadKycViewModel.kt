package com.shreefintech.paytouchconsumer.onboarding.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.shreefintech.paytouchconsumer.R
import com.shreefintech.paytouchconsumer.utill.Utility

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
        // TODO(PAYTOUCH-KYC): wire upload-KYC API call via ApiClient.apiService (multipart:
        //  mobile, memberName, birthdate, age, address, city, email, panNumber, aadharNumber, gstNumber)
        onSuccess()
    }
}
