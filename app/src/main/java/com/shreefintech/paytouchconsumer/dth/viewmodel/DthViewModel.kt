package com.shreefintech.paytouchconsumer.dth.viewmodel

import android.app.Application
import com.shreefintech.paytouchconsumer.BaseBillViewModel
import com.shreefintech.paytouchconsumer.Constant
import com.shreefintech.paytouchconsumer.R
import com.shreefintech.paytouchconsumer.retrofit.ApiClient
import com.shreefintech.paytouchconsumer.retrofit.ApiHelper
import com.shreefintech.paytouchconsumer.retrofit.model.General
import com.shreefintech.paytouchconsumer.retrofit.model.dth.DthOperatorItem
import com.shreefintech.paytouchconsumer.retrofit.model.dth.DthPaymentItem
import com.shreefintech.paytouchconsumer.retrofit.model.dth.DthProcessPaymentRequest
import com.shreefintech.paytouchconsumer.utill.SharedPreferenceHelper
import com.shreefintech.paytouchconsumer.utill.Utility
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class DthViewModel(application: Application) : BaseBillViewModel(application) {

    fun loadOperators(
        onLoading: () -> Unit,
        onSuccess: (List<DthOperatorItem>) -> Unit,
        onError: (String) -> Unit
    ) {
        if (!Utility.isInternetAvailable(getApplication())) {
            onError(getString(R.string.msgNoInternet))
            return
        }
        onLoading()
        ApiClient.apiService.getDthOperators(bearerToken())
            .enqueue(object : Callback<General<List<DthOperatorItem>>> {
                override fun onResponse(
                    call: Call<General<List<DthOperatorItem>>>,
                    response: Response<General<List<DthOperatorItem>>>
                ) {
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

                override fun onFailure(call: Call<General<List<DthOperatorItem>>>, t: Throwable) {
                    onError(t.localizedMessage ?: getString(R.string.errGeneric))
                }
            })
    }

    fun verifyAndPay(
        mobileNo: String,
        operatorId: String,
        amount: Double,
        fee: Double,
        total: Double,
        onLoading: () -> Unit,
        onSuccess: (DthPaymentItem) -> Unit,
        onError: (String) -> Unit
    ) {
        if (!Utility.isInternetAvailable(getApplication())) {
            onError(getString(R.string.msgNoInternet))
            return
        }
        onLoading()
        val proceed = { processPayment( mobileNo, operatorId, amount, onSuccess, onError) }
        val fallback = { checkWalletBalance(total, proceed, onError) }
        val userId = SharedPreferenceHelper.getSharedPreferenceString(
            getApplication(), Constant.KEY_USER_ID, ""
        ) ?: ""
        checkVpsBalance(total, userId, onSufficient = proceed, onFallback = fallback)
    }

    private fun processPayment(
        mobileNo: String,
        operatorId: String,
        amount: Double,
        onSuccess: (DthPaymentItem) -> Unit,
        onError: (String) -> Unit
    ) {
        if (!Utility.isInternetAvailable(getApplication())) {
            onError(getString(R.string.msgNoInternet))
            return
        }
        ApiClient.apiService.processDthPayment(
            bearerToken(),
            DthProcessPaymentRequest(
                cn     = mobileNo,
                op     = operatorId,
                amount = "%.2f".format(amount)
            )
        ).enqueue(object : Callback<DthPaymentItem> {
            override fun onResponse(call: Call<DthPaymentItem>, response: Response<DthPaymentItem>) {
                val body = response.body()
                if (response.isSuccessful && body?.success == true) {
                    onSuccess(body)
                } else {
                    onError(
                        ApiHelper.parseErrorMessage(
                            getApplication(), response.code(), response.errorBody()?.string()
                        )
                    )
                }
            }

            override fun onFailure(call: Call<DthPaymentItem>, t: Throwable) {
                onError(t.localizedMessage ?: getString(R.string.errGeneric))
            }
        })
    }
}
