package com.shreefintech.paytouchconsumer.electricity.viewmodel

import android.app.Application
import com.shreefintech.paytouchconsumer.BaseBillViewModel
import com.shreefintech.paytouchconsumer.Constant
import com.shreefintech.paytouchconsumer.R
import com.shreefintech.paytouchconsumer.retrofit.ApiClient
import com.shreefintech.paytouchconsumer.retrofit.ApiHelper
import com.shreefintech.paytouchconsumer.retrofit.model.General
import com.shreefintech.paytouchconsumer.retrofit.model.electricity.ElectricityBillItem
import com.shreefintech.paytouchconsumer.retrofit.model.electricity.ElectricityFetchBillRequest
import com.shreefintech.paytouchconsumer.retrofit.model.electricity.ElectricityOperatorItem
import com.shreefintech.paytouchconsumer.retrofit.model.electricity.ElectricityPaymentItem
import com.shreefintech.paytouchconsumer.retrofit.model.electricity.ElectricityProcessPaymentRequest
import com.shreefintech.paytouchconsumer.utill.SharedPreferenceHelper
import com.shreefintech.paytouchconsumer.utill.Utility
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ElectricityViewModel(application: Application) : BaseBillViewModel(application) {

    // ── Public API ────────────────────────────────────────────────────────────

    fun loadOperators(
        onLoading: () -> Unit,
        onSuccess: (List<ElectricityOperatorItem>) -> Unit,
        onError: (String) -> Unit
    ) {
        if (!Utility.isInternetAvailable(getApplication())) {
            onError(getString(R.string.msgNoInternet))
            return
        }
        onLoading()
        ApiClient.apiService.getElectricityOperators(bearerToken())
            .enqueue(object : Callback<General<List<ElectricityOperatorItem>>> {
                override fun onResponse(
                    call: Call<General<List<ElectricityOperatorItem>>>,
                    response: Response<General<List<ElectricityOperatorItem>>>
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

                override fun onFailure(call: Call<General<List<ElectricityOperatorItem>>>, t: Throwable) {
                    onError(t.localizedMessage ?: getString(R.string.errGeneric))
                }
            })
    }

    fun fetchBill(
        connectionNumber: String,
        operatorId: String,
        onLoading: () -> Unit,
        onSuccess: (ElectricityBillItem) -> Unit,
        onError: (String) -> Unit
    ) {
        if (!Utility.isInternetAvailable(getApplication())) {
            onError(getString(R.string.msgNoInternet))
            return
        }
        onLoading()
        ApiClient.apiService.fetchElectricityBill(
            bearerToken(),
            ElectricityFetchBillRequest(
                connectionNumber = connectionNumber,
                operatorId = operatorId,
                circleId = "00"
            )
        ).enqueue(object : Callback<General<List<ElectricityBillItem>>> {
            override fun onResponse(
                call: Call<General<List<ElectricityBillItem>>>,
                response: Response<General<List<ElectricityBillItem>>>
            ) {
                if (response.isSuccessful && response.body()?.data != null) {
                    val bill = response.body()!!.data!!.firstOrNull()
                    if (bill != null) onSuccess(bill)
                    else onError(getString(R.string.errGeneric))
                } else {
                    onError(
                        ApiHelper.parseErrorMessage(
                            getApplication(), response.code(), response.errorBody()?.string()
                        )
                    )
                }
            }

            override fun onFailure(call: Call<General<List<ElectricityBillItem>>>, t: Throwable) {
                onError(t.localizedMessage ?: getString(R.string.errGeneric))
            }
        })
    }

    fun verifyAndPay(
        connectionNumber: String,
        operatorId: String,
        amount: Double,
        fee: Double,
        total: Double,
        onLoading: () -> Unit,
        onSuccess: (ElectricityPaymentItem) -> Unit,
        onError: (String) -> Unit
    ) {
        if (!Utility.isInternetAvailable(getApplication())) {
            onError(getString(R.string.msgNoInternet))
            return
        }
        onLoading()
        val proceed = { processPayment(connectionNumber, operatorId, amount, fee, total, onSuccess, onError) }
        val fallback = { checkWalletBalance(total, proceed, onError) }
        val userId = SharedPreferenceHelper.getSharedPreferenceString(
            getApplication(), Constant.KEY_USER_ID, ""
        ) ?: ""
        checkVpsBalance(total, userId, onSufficient = proceed, onFallback = fallback)
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private fun processPayment(
        connectionNumber: String,
        operatorId: String,
        amount: Double,
        fee: Double,
        total: Double,
        onSuccess: (ElectricityPaymentItem) -> Unit,
        onError: (String) -> Unit
    ) {
        if (!Utility.isInternetAvailable(getApplication())) {
            onError(getString(R.string.msgNoInternet))
            return
        }
        ApiClient.apiService.processElectricityPayment(
            bearerToken(),
            ElectricityProcessPaymentRequest(
                connectionNumber = connectionNumber,
                operatorId = operatorId,
                circleId = "00",
                amount = amount,
                platformFee = fee,
                totalPayable = total
            )
        ).enqueue(object : Callback<ElectricityPaymentItem> {
            override fun onResponse(
                call: Call<ElectricityPaymentItem>,
                response: Response<ElectricityPaymentItem>
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

            override fun onFailure(call: Call<ElectricityPaymentItem>, t: Throwable) {
                onError(t.localizedMessage ?: getString(R.string.errGeneric))
            }
        })
    }
}
