package com.shreefintech.paytouchconsumer.onboarding.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.shreefintech.paytouchconsumer.R
import com.shreefintech.paytouchconsumer.utill.Utility
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
        viewModelScope.launch {
            try {
                // TODO(PAYTOUCH-VA): wire create-virtual-account API call (multipart:
                //  fullName, mobile, state, city, district, panNumber, aadharNumber,
                //  ifscCode, bankAccount, vpa, branchName + the 4 document file parts)
                // val response = ApiClient.apiService.createVirtualAccount(...)
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
                    onError(e.message ?: getApplication<Application>().getString(R.string.errGeneric))
                }
            }
        }
    }
}