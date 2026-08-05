package com.shreefintech.paytouchconsumer.prepaid.viewmodel

import android.app.Application
import com.shreefintech.paytouchconsumer.BaseBillViewModel
import com.shreefintech.paytouchconsumer.Constant
import com.shreefintech.paytouchconsumer.R
import com.shreefintech.paytouchconsumer.retrofit.ApiClient
import com.shreefintech.paytouchconsumer.retrofit.ApiHelper
import com.shreefintech.paytouchconsumer.retrofit.model.General
import com.shreefintech.paytouchconsumer.retrofit.model.prepaid.PrepaidOperatorItem
import com.shreefintech.paytouchconsumer.retrofit.model.prepaid.PrepaidPaymentItem
import com.shreefintech.paytouchconsumer.retrofit.model.prepaid.PrepaidProcessDirectRequest
import com.shreefintech.paytouchconsumer.utill.SharedPreferenceHelper
import com.shreefintech.paytouchconsumer.utill.Utility
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class PrepaidViewModel(application: Application) : BaseBillViewModel(application) {

    // ── Public API ────────────────────────────────────────────────────────────

    fun loadOperators(
        onLoading: () -> Unit,
        onSuccess: (List<PrepaidOperatorItem>) -> Unit,
        onError: (String) -> Unit
    ) {
        if (!Utility.isInternetAvailable(getApplication())) {
            onError(getString(R.string.msgNoInternet))
            return
        }
        onLoading()
        ApiClient.apiService.getPrepaidOperators(bearerToken())
            .enqueue(object : Callback<General<List<PrepaidOperatorItem>>> {
                override fun onResponse(
                    call: Call<General<List<PrepaidOperatorItem>>>,
                    response: Response<General<List<PrepaidOperatorItem>>>
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

                override fun onFailure(call: Call<General<List<PrepaidOperatorItem>>>, t: Throwable) {
                    onError(t.localizedMessage ?: getString(R.string.errGeneric))
                }
            })
    }

    fun verifyAndPay(
        mobileNo: String,
        operatorCode: String,
        circleCode: String,
        amount: Double,
        fee: Double,
        total: Double,
        onLoading: () -> Unit,
        onSuccess: (PrepaidPaymentItem) -> Unit,
        onError: (String) -> Unit
    ) {
        if (!Utility.isInternetAvailable(getApplication())) {
            onError(getString(R.string.msgNoInternet))
            return
        }
        onLoading()
        val proceed = { processPayment(mobileNo, operatorCode, circleCode, amount, fee, total, onSuccess, onError) }
        val fallback = { checkWalletBalance(total, proceed, onError) }
        val userId = SharedPreferenceHelper.getSharedPreferenceString(
            getApplication(), Constant.KEY_USER_ID, ""
        ) ?: ""
        checkVpsBalance(total, userId, onSufficient = proceed, onFallback = fallback)
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private fun processPayment(
        mobileNo: String,
        operatorCode: String,
        circleCode: String,
        amount: Double,
        fee: Double,
        total: Double,
        onSuccess: (PrepaidPaymentItem) -> Unit,
        onError: (String) -> Unit
    ) {
        if (!Utility.isInternetAvailable(getApplication())) {
            onError(getString(R.string.msgNoInternet))
            return
        }
        ApiClient.apiService.processPrepaidPayment(
            bearerToken(),
            PrepaidProcessDirectRequest(
                mobileNo = mobileNo,
                operatorCode = operatorCode,
                circleCode = circleCode,
                amount = amount,
                platformFee = fee,
                totalPayable = total
            )
        ).enqueue(object : Callback<PrepaidPaymentItem> {
            override fun onResponse(
                call: Call<PrepaidPaymentItem>,
                response: Response<PrepaidPaymentItem>
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

            override fun onFailure(call: Call<PrepaidPaymentItem>, t: Throwable) {
                onError(t.localizedMessage ?: getString(R.string.errGeneric))
            }
        })
    }
}
