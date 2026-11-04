package com.shreefintech.paytouchconsumer.billpay.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.shreefintech.paytouchconsumer.R
import com.shreefintech.paytouchconsumer.retrofit.ApiClient
import com.shreefintech.paytouchconsumer.retrofit.model.billpay.BillPayPlanItem
import com.shreefintech.paytouchconsumer.utill.Utility
import com.shreefintech.paytouchconsumer.utill.bearerToken
import com.shreefintech.paytouchconsumer.utill.envelopeCallback
import com.shreefintech.paytouchconsumer.utill.getString

class BillPayPlanSelectionViewModel(application: Application) : AndroidViewModel(application) {

    // circleCode: required for prepaid, null for DTH
    fun loadPlans(
        billerId: String,
        circleCode: String?,
        onLoading: () -> Unit,
        onSuccess: (List<BillPayPlanItem>) -> Unit,
        onError: (String) -> Unit
    ) {
        if (!Utility.isInternetAvailable(getApplication())) {
            onError(getString(R.string.msgNoInternet))
            return
        }
        onLoading()
        ApiClient.apiService.getBillPayPlans(bearerToken(), billerId, circleCode)
            .enqueue(envelopeCallback(onSuccess, onError))
    }
}
