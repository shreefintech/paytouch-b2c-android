package com.shreefintech.paytouchconsumer.onboarding.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import com.shreefintech.paytouchconsumer.R
import com.shreefintech.paytouchconsumer.utill.Utility

class CreateVirtualAccountViewModel(application: Application) : AndroidViewModel(application) {

    fun submitVirtualAccount(
        fullName: String,
        mobile: String,
        state: String,
        city: String,
        district: String,
        panNumber: String,
        aadharNumber: String,
        ifscCode: String,
        bankAccount: String,
        vpa: String,
        branchName: String,
        aadharFrontUri: Uri,
        aadharBackUri: Uri,
        panUri: Uri,
        proofUri: Uri,
        onLoading: () -> Unit,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (!Utility.isInternetAvailable(getApplication())) {
            onError(getApplication<Application>().getString(R.string.msgNoInternet))
            return
        }
        onLoading()
        // TODO(PAYTOUCH-VA): wire create-virtual-account API call via ApiClient.apiService (multipart:
        //  fullName, mobile, state, city, district, panNumber, aadharNumber, ifscCode,
        //  bankAccount, vpa, branchName + aadharFrontUri, aadharBackUri, panUri, proofUri)
        onSuccess()
    }
}
