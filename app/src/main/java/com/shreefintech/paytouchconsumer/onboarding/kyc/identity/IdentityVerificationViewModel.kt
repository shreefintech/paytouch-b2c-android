package com.shreefintech.paytouchconsumer.onboarding.kyc.identity

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.shreefintech.paytouchconsumer.R
import com.shreefintech.paytouchconsumer.utill.Utility

class IdentityVerificationViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        const val TOTAL_STEPS = 4
    }

    val currentStep = MutableLiveData(0)

    // Data accumulated across steps
    var mobile = ""
    var email = ""

    var aadhaarNumber = ""
    var aadhaarFrontUri: Uri? = null
    var aadhaarBackUri: Uri? = null

    var panNumber = ""
    var panFrontUri: Uri? = null

    var selfieUri: Uri? = null

    fun goToNextStep() {
        val step = currentStep.value ?: 0
        if (step < TOTAL_STEPS - 1) currentStep.value = step + 1
    }

    /** @return false when already at the first step (caller should exit the flow instead) */
    fun goToPreviousStep(): Boolean {
        val step = currentStep.value ?: 0
        if (step == 0) return false
        currentStep.value = step - 1
        return true
    }

    fun submitIdentity(
        onLoading: () -> Unit,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (!Utility.isInternetAvailable(getApplication())) {
            onError(getApplication<Application>().getString(R.string.msgNoInternet))
            return
        }
        onLoading()
        // TODO(PAYTOUCH-KYC): wire identity-verification submit API call via ApiClient.apiService (multipart:
        //  mobile, email, aadhaarNumber, panNumber + aadhaarFrontUri, aadhaarBackUri, panFrontUri, selfieUri)
        onSuccess()
    }
}
