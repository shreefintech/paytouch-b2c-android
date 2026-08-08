package com.shreefintech.paytouchconsumer.dth.viewmodel

import android.app.Application
import com.shreefintech.paytouchconsumer.BaseBillViewModel
import com.shreefintech.paytouchconsumer.R
import com.shreefintech.paytouchconsumer.retrofit.ApiClient
import com.shreefintech.paytouchconsumer.retrofit.ApiHelper
import com.shreefintech.paytouchconsumer.retrofit.model.dth.DthPlanItem
import com.shreefintech.paytouchconsumer.retrofit.model.dth.DthPlansListItem
import com.shreefintech.paytouchconsumer.utill.Utility
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class DthPlanSelectionViewModel(application: Application) : BaseBillViewModel(application) {

    fun loadPlans(
        operatorId: String,
        onLoading: () -> Unit,
        onSuccess: (List<DthPlanItem>) -> Unit,
        onError: (String) -> Unit
    ) {
        if (!Utility.isInternetAvailable(getApplication())) {
            onError(getString(R.string.msgNoInternet))
            return
        }
        onLoading()
        ApiClient.apiService.getDthPlans(bearerToken(), operatorId)
            .enqueue(object : Callback<DthPlansListItem> {
                override fun onResponse(
                    call: Call<DthPlansListItem>,
                    response: Response<DthPlansListItem>
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

                override fun onFailure(call: Call<DthPlansListItem>, t: Throwable) {
                    onError(t.localizedMessage ?: getString(R.string.errGeneric))
                }
            })
    }
}
