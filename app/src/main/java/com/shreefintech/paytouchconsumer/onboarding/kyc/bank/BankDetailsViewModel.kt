package com.shreefintech.paytouchconsumer.onboarding.kyc.bank

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import com.shreefintech.paytouchconsumer.R
import com.shreefintech.paytouchconsumer.utill.Utility

data class BankAccountInput(
    val accountNumber: String,
    val bankName: String,
    val ifscCode: String,
    val branchName: String,
    val proofType: String,
    val proofUri: Uri
)

class BankDetailsViewModel(application: Application) : AndroidViewModel(application) {

    fun submit(
        accounts: List<BankAccountInput>,
        onLoading: () -> Unit,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (!Utility.isInternetAvailable(getApplication())) {
            onError(getApplication<Application>().getString(R.string.msgNoInternet))
            return
        }
        onLoading()
        // TODO(PAYTOUCH-KYC): wire bank-details submit API call via ApiClient.apiService (multipart,
        //  one entry per BankAccountInput: accountNumber, bankName, ifscCode, branchName, proofType + proofUri)
        onSuccess()
    }
}
