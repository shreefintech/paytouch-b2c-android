package com.shreefintech.paytouchconsumer.fastag.viewmodel

import android.app.Application
import com.shreefintech.paytouchconsumer.BaseBillViewModel
import com.shreefintech.paytouchconsumer.Constant
import com.shreefintech.paytouchconsumer.R
import com.shreefintech.paytouchconsumer.retrofit.ApiClient
import com.shreefintech.paytouchconsumer.retrofit.ApiHelper
import com.shreefintech.paytouchconsumer.retrofit.model.General
import com.shreefintech.paytouchconsumer.retrofit.model.fastag.FastagOperatorItem
import com.shreefintech.paytouchconsumer.retrofit.model.fastag.FastagPaymentItem
import com.shreefintech.paytouchconsumer.retrofit.model.fastag.FastagProcessPaymentRequest
import com.shreefintech.paytouchconsumer.utill.SharedPreferenceHelper
import com.shreefintech.paytouchconsumer.utill.Utility
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class FastagViewModel(application: Application) : BaseBillViewModel(application) {

    fun loadOperators(
        onLoading: () -> Unit,
        onSuccess: (List<FastagOperatorItem>) -> Unit,
        onError: (String) -> Unit
    ) {
        if (!Utility.isInternetAvailable(getApplication())) {
            onError(getString(R.string.msgNoInternet))
            return
        }
        onLoading()
        ApiClient.apiService.getFastagOperators(bearerToken())
            .enqueue(object : Callback<General<List<FastagOperatorItem>>> {
                override fun onResponse(
                    call: Call<General<List<FastagOperatorItem>>>,
                    response: Response<General<List<FastagOperatorItem>>>
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

                override fun onFailure(call: Call<General<List<FastagOperatorItem>>>, t: Throwable) {
                    onError(t.localizedMessage ?: getString(R.string.errGeneric))
                }
            })
    }

    fun verifyAndPay(
        vehicleNumber: String,
        operator: String,
        operatorName: String,
        circle: String,
        amount: Double,
        fee: Double,
        total: Double,
        onLoading: () -> Unit,
        onSuccess: (FastagPaymentItem) -> Unit,
        onError: (String) -> Unit
    ) {
        if (!Utility.isInternetAvailable(getApplication())) {
            onError(getString(R.string.msgNoInternet))
            return
        }
        onLoading()
        val proceed = { processPayment(vehicleNumber, operator, operatorName, circle, amount, fee, total, onSuccess, onError) }
        val fallback = { checkWalletBalance(total, proceed, onError) }
        val userId = SharedPreferenceHelper.getSharedPreferenceString(
            getApplication(), Constant.KEY_USER_ID, ""
        ) ?: ""
        checkVpsBalance(total, userId, onSufficient = proceed, onFallback = fallback)
    }

    private fun processPayment(
        vehicleNumber: String,
        operator: String,
        operatorName: String,
        circle: String,
        amount: Double,
        fee: Double,
        total: Double,
        onSuccess: (FastagPaymentItem) -> Unit,
        onError: (String) -> Unit
    ) {
        if (!Utility.isInternetAvailable(getApplication())) {
            onError(getString(R.string.msgNoInternet))
            return
        }
        ApiClient.apiService.processFastagPayment(
            bearerToken(),
            FastagProcessPaymentRequest(
                vehicleNumber = vehicleNumber,
                operator = operator,
                operatorName = operatorName,
                circle = circle,
                amount = amount,
                platformFee = fee,
                totalPayable = total
            )
        ).enqueue(object : Callback<FastagPaymentItem> {
            override fun onResponse(
                call: Call<FastagPaymentItem>,
                response: Response<FastagPaymentItem>
            ) {
                val body = response.body()
                if (response.isSuccessful && body?.success == true) {
                    onSuccess(body)
                } else {
                    onError(
                        body?.message ?: ApiHelper.parseErrorMessage(
                            getApplication(), response.code(), response.errorBody()?.string()
                        )
                    )
                }
            }

            override fun onFailure(call: Call<FastagPaymentItem>, t: Throwable) {
                onError(t.localizedMessage ?: getString(R.string.errGeneric))
            }
        })
    }
}
