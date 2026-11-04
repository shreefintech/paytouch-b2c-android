package com.shreefintech.paytouchconsumer.billpay.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.shreefintech.paytouchconsumer.R
import com.shreefintech.paytouchconsumer.retrofit.ApiClient
import com.shreefintech.paytouchconsumer.retrofit.ApiHelper
import com.shreefintech.paytouchconsumer.retrofit.model.General
import com.shreefintech.paytouchconsumer.retrofit.model.billpay.BillPayPlanItem
import com.shreefintech.paytouchconsumer.utill.Utility
import com.shreefintech.paytouchconsumer.utill.bearerToken
import com.shreefintech.paytouchconsumer.utill.getString
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

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
            .enqueue(object : Callback<General<List<BillPayPlanItem>>> {
                override fun onResponse(
                    call: Call<General<List<BillPayPlanItem>>>,
                    response: Response<General<List<BillPayPlanItem>>>
                ) {
                    val plans = response.body()?.data
                    if (response.isSuccessful && plans != null) {
                        onSuccess(plans)
                    } else {
                        onError(
                            ApiHelper.parseErrorMessage(
                                getApplication(), response.code(), response.errorBody()?.string()
                            )
                        )
                    }
                }

                override fun onFailure(call: Call<General<List<BillPayPlanItem>>>, t: Throwable) {
                    onError(t.localizedMessage ?: getString(R.string.errGeneric))
                }
            })
    }
}
