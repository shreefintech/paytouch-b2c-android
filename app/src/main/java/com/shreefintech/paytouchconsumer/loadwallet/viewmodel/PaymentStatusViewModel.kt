package com.shreefintech.paytouchconsumer.loadwallet.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.shreefintech.paytouchconsumer.R
import com.shreefintech.paytouchconsumer.retrofit.ApiClient
import com.shreefintech.paytouchconsumer.retrofit.ApiHelper
import com.shreefintech.paytouchconsumer.retrofit.model.hdfc.HdfcOrderItem
import com.shreefintech.paytouchconsumer.retrofit.model.hdfc.HdfcOrderResponseItem
import com.shreefintech.paytouchconsumer.utill.Utility
import com.shreefintech.paytouchconsumer.utill.bearerToken
import com.shreefintech.paytouchconsumer.utill.getString
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class PaymentStatusViewModel(application: Application) : AndroidViewModel(application) {

    fun recheckStatus(
        orderId: String,
        onLoading: () -> Unit,
        onSuccess: (HdfcOrderItem) -> Unit,
        onError: (String) -> Unit
    ) {
        if (!Utility.isInternetAvailable(getApplication())) {
            onError(getString(R.string.msgNoInternet))
            return
        }
        onLoading()
        ApiClient.apiService.getHdfcOrderStatus(bearerToken(), orderId)
            .enqueue(object : Callback<HdfcOrderResponseItem> {
                override fun onResponse(
                    call: Call<HdfcOrderResponseItem>,
                    response: Response<HdfcOrderResponseItem>
                ) {
                    // success flag intentionally not checked: the status endpoint may return
                    // success:false for declined/refunded states while still providing valid
                    // order data needed for display. data != null is the correct gate here.
                    if (response.isSuccessful && response.body()?.data != null) {
                        onSuccess(response.body()!!.data!!)
                    } else {
                        onError(
                            ApiHelper.parseErrorMessage(
                                getApplication(), response.code(), response.errorBody()?.string()
                            )
                        )
                    }
                }

                override fun onFailure(call: Call<HdfcOrderResponseItem>, t: Throwable) {
                    onError(t.localizedMessage ?: getString(R.string.errGeneric))
                }
            })
    }
}
