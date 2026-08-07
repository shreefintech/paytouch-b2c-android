package com.shreefintech.paytouchconsumer.postpaid.viewmodel

import android.app.Application
import com.shreefintech.paytouchconsumer.BaseBillViewModel
import com.shreefintech.paytouchconsumer.Constant
import com.shreefintech.paytouchconsumer.R
import com.shreefintech.paytouchconsumer.retrofit.ApiClient
import com.shreefintech.paytouchconsumer.retrofit.ApiHelper
import com.shreefintech.paytouchconsumer.retrofit.model.General
import com.shreefintech.paytouchconsumer.retrofit.model.postpaid.PostpaidOperatorItem
import com.shreefintech.paytouchconsumer.retrofit.model.postpaid.PostpaidPaymentItem
import com.shreefintech.paytouchconsumer.retrofit.model.postpaid.PostpaidProcessPaymentRequest
import com.shreefintech.paytouchconsumer.utill.SharedPreferenceHelper
import com.shreefintech.paytouchconsumer.utill.Utility
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class PostpaidViewModel(application: Application) : BaseBillViewModel(application) {

    fun loadOperators(
        onLoading: () -> Unit,
        onSuccess: (List<PostpaidOperatorItem>) -> Unit,
        onError: (String) -> Unit
    ) {
        if (!Utility.isInternetAvailable(getApplication())) {
            onError(getString(R.string.msgNoInternet))
            return
        }
        onLoading()
        ApiClient.apiService.getPostpaidOperators(bearerToken())
            .enqueue(object : Callback<General<List<PostpaidOperatorItem>>> {
                override fun onResponse(
                    call: Call<General<List<PostpaidOperatorItem>>>,
                    response: Response<General<List<PostpaidOperatorItem>>>
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

                override fun onFailure(call: Call<General<List<PostpaidOperatorItem>>>, t: Throwable) {
                    onError(t.localizedMessage ?: getString(R.string.errGeneric))
                }
            })
    }

    fun verifyAndPay(
        mobileNumber: String,
        operatorId: String,
        circleId: String,
        amount: Double,
        fee: Double,
        total: Double,
        onLoading: () -> Unit,
        onSuccess: (PostpaidPaymentItem) -> Unit,
        onError: (String) -> Unit
    ) {
        if (!Utility.isInternetAvailable(getApplication())) {
            onError(getString(R.string.msgNoInternet))
            return
        }
        onLoading()
        val proceed = { processPayment(mobileNumber, operatorId, circleId, amount, fee, total, onSuccess, onError) }
        val fallback = { checkWalletBalance(total, proceed, onError) }
        val userId = SharedPreferenceHelper.getSharedPreferenceString(
            getApplication(), Constant.KEY_USER_ID, ""
        ) ?: ""
        checkVpsBalance(total, userId, onSufficient = proceed, onFallback = fallback)
    }

    private fun processPayment(
        mobileNumber: String,
        operatorId: String,
        circleId: String,
        amount: Double,
        fee: Double,
        total: Double,
        onSuccess: (PostpaidPaymentItem) -> Unit,
        onError: (String) -> Unit
    ) {
        if (!Utility.isInternetAvailable(getApplication())) {
            onError(getString(R.string.msgNoInternet))
            return
        }
        ApiClient.apiService.processPostpaidPayment(
            bearerToken(),
            PostpaidProcessPaymentRequest(
                mobileNumber = mobileNumber,
                operatorId = operatorId,
                circleId = circleId,
                amount = amount,
                platformFee = fee,
                totalPayable = total
            )
        ).enqueue(object : Callback<PostpaidPaymentItem> {
            override fun onResponse(
                call: Call<PostpaidPaymentItem>,
                response: Response<PostpaidPaymentItem>
            ) {
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

            override fun onFailure(call: Call<PostpaidPaymentItem>, t: Throwable) {
                onError(t.localizedMessage ?: getString(R.string.errGeneric))
            }
        })
    }
}
