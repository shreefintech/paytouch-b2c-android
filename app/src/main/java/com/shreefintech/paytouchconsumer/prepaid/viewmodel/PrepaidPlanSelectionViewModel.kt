package com.shreefintech.paytouchconsumer.prepaid.viewmodel

import android.app.Application
import com.shreefintech.paytouchconsumer.BaseBillViewModel
import com.shreefintech.paytouchconsumer.R
import com.shreefintech.paytouchconsumer.retrofit.ApiClient
import com.shreefintech.paytouchconsumer.retrofit.ApiHelper
import com.shreefintech.paytouchconsumer.retrofit.model.prepaid.PrepaidPlanItem
import com.shreefintech.paytouchconsumer.retrofit.model.prepaid.PrepaidPlansListItem
import com.shreefintech.paytouchconsumer.utill.Utility
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class PrepaidPlanSelectionViewModel(application: Application) : BaseBillViewModel(application) {

    fun loadPlans(
        operatorId: String,
        circleId: String,
        onLoading: () -> Unit,
        onSuccess: (List<PrepaidPlanItem>) -> Unit,
        onError: (String) -> Unit
    ) {
        if (!Utility.isInternetAvailable(getApplication())) {
            onError(getString(R.string.msgNoInternet))
            return
        }
        onLoading()
        ApiClient.apiService.getPrepaidPlans(bearerToken(), operatorId, circleId)
            .enqueue(object : Callback<PrepaidPlansListItem> {
                override fun onResponse(
                    call: Call<PrepaidPlansListItem>,
                    response: Response<PrepaidPlansListItem>
                ) {
                    val body = response.body()
                    if (response.isSuccessful && body?.success == true) {
                        onSuccess(body.plans ?: emptyList())
                    } else {
                        onError(
                            ApiHelper.parseErrorMessage(
                                getApplication(), response.code(), response.errorBody()?.string()
                            )
                        )
                    }
                }

                override fun onFailure(call: Call<PrepaidPlansListItem>, t: Throwable) {
                    onError(t.localizedMessage ?: getString(R.string.errGeneric))
                }
            })
    }
}
