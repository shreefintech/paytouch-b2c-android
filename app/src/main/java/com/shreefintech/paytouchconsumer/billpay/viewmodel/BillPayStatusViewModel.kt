package com.shreefintech.paytouchconsumer.billpay.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.shreefintech.paytouchconsumer.R
import com.shreefintech.paytouchconsumer.retrofit.ApiClient
import com.shreefintech.paytouchconsumer.retrofit.model.billpay.BillPayTransactionItem
import com.shreefintech.paytouchconsumer.utill.Utility
import com.shreefintech.paytouchconsumer.utill.bearerToken
import com.shreefintech.paytouchconsumer.utill.envelopeCallback
import com.shreefintech.paytouchconsumer.utill.getString

/** GET billpay/status/{client_reference} — polled after pay, and loads the receipt. */
class BillPayStatusViewModel(application: Application) : AndroidViewModel(application) {

    fun loadStatus(
        clientReference: String,
        onLoading: () -> Unit,
        onSuccess: (BillPayTransactionItem) -> Unit,
        onError: (String) -> Unit
    ) {
        if (!Utility.isInternetAvailable(getApplication())) {
            onError(getString(R.string.msgNoInternet))
            return
        }
        onLoading()
        ApiClient.apiService.getBillPayStatus(bearerToken(), clientReference)
            .enqueue(envelopeCallback(onSuccess, onError))
    }
}
