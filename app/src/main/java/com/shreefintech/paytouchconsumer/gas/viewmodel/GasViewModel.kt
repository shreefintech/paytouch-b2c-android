package com.shreefintech.paytouchconsumer.gas.viewmodel

import android.app.Application
import com.shreefintech.paytouchconsumer.BaseBillViewModel
import com.shreefintech.paytouchconsumer.Constant
import com.shreefintech.paytouchconsumer.R
import com.shreefintech.paytouchconsumer.retrofit.ApiClient
import com.shreefintech.paytouchconsumer.retrofit.ApiHelper
import com.shreefintech.paytouchconsumer.retrofit.model.General
import com.shreefintech.paytouchconsumer.retrofit.model.gas.GasBillItem
import com.shreefintech.paytouchconsumer.retrofit.model.gas.GasFetchBillRequest
import com.shreefintech.paytouchconsumer.retrofit.model.gas.GasOperatorItem
import com.shreefintech.paytouchconsumer.retrofit.model.gas.GasPaymentItem
import com.shreefintech.paytouchconsumer.retrofit.model.gas.GasProcessPaymentRequest
import com.shreefintech.paytouchconsumer.utill.SharedPreferenceHelper
import com.shreefintech.paytouchconsumer.utill.Utility
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class GasViewModel(application: Application) : BaseBillViewModel(application) {

    // ── Public API ────────────────────────────────────────────────────────────

    fun loadOperators(
        onLoading: () -> Unit,
        onSuccess: (List<GasOperatorItem>) -> Unit,
        onError: (String) -> Unit
    ) {
        if (!Utility.isInternetAvailable(getApplication())) {
            onError(getString(R.string.msgNoInternet))
            return
        }
        onLoading()
        ApiClient.apiService.getGasOperators(bearerToken())
            .enqueue(object : Callback<General<List<GasOperatorItem>>> {
                override fun onResponse(
                    call: Call<General<List<GasOperatorItem>>>,
                    response: Response<General<List<GasOperatorItem>>>
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

                override fun onFailure(call: Call<General<List<GasOperatorItem>>>, t: Throwable) {
                    onError(t.localizedMessage ?: getString(R.string.err_generic))
                }
            })
    }

    fun fetchBill(
        consumerNumber: String,
        operatorId: String,
        onLoading: () -> Unit,
        onSuccess: (GasBillItem) -> Unit,
        onError: (String) -> Unit
    ) {
        if (!Utility.isInternetAvailable(getApplication())) {
            onError(getString(R.string.msgNoInternet))
            return
        }
        onLoading()
        ApiClient.apiService.fetchGasBill(
            bearerToken(),
            GasFetchBillRequest(
                consumerNumber = consumerNumber,
                operatorId = operatorId,
                circleId = "0"
            )
        ).enqueue(object : Callback<General<List<GasBillItem>>> {
            override fun onResponse(
                call: Call<General<List<GasBillItem>>>,
                response: Response<General<List<GasBillItem>>>
            ) {
                if (response.isSuccessful && response.body()?.data != null) {
                    val bill = response.body()!!.data!!.firstOrNull()
                    if (bill != null) onSuccess(bill)
                    else onError(getString(R.string.err_generic))
                } else {
                    onError(
                        ApiHelper.parseErrorMessage(
                            getApplication(), response.code(), response.errorBody()?.string()
                        )
                    )
                }
            }

            override fun onFailure(call: Call<General<List<GasBillItem>>>, t: Throwable) {
                onError(t.localizedMessage ?: getString(R.string.err_generic))
            }
        })
    }

    fun verifyAndPay(
        consumerNumber: String,
        operatorId: String,
        amount: Double,
        fee: Double,
        total: Double,
        onLoading: () -> Unit,
        onSuccess: (GasPaymentItem) -> Unit,
        onError: (String) -> Unit
    ) {
        if (!Utility.isInternetAvailable(getApplication())) {
            onError(getString(R.string.msgNoInternet))
            return
        }
        onLoading()
        val proceed = { processPayment(consumerNumber, operatorId, amount, fee, total, onSuccess, onError) }
        val fallback = { checkWalletBalance(total, proceed, onError) }
        val userId = SharedPreferenceHelper.getSharedPreferenceString(
            getApplication(), Constant.KEY_USER_ID, ""
        ) ?: ""
        checkVpsBalance(total, userId, onSufficient = proceed, onFallback = fallback)
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private fun processPayment(
        consumerNumber: String,
        operatorId: String,
        amount: Double,
        fee: Double,
        total: Double,
        onSuccess: (GasPaymentItem) -> Unit,
        onError: (String) -> Unit
    ) {
        if (!Utility.isInternetAvailable(getApplication())) {
            onError(getString(R.string.msgNoInternet))
            return
        }
        val transactionId = Utility.generateTransactionId()
        ApiClient.apiService.processGasPayment(
            bearerToken(),
            GasProcessPaymentRequest(
                consumerNumber = consumerNumber,
                operatorId = operatorId,
                circleId = "0",
                amount = amount,
                platformFee = fee,
                totalPayable = total,
                transactionId = transactionId
            )
        ).enqueue(object : Callback<GasPaymentItem> {
            override fun onResponse(
                call: Call<GasPaymentItem>,
                response: Response<GasPaymentItem>
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

            override fun onFailure(call: Call<GasPaymentItem>, t: Throwable) {
                onError(t.localizedMessage ?: getString(R.string.err_generic))
            }
        })
    }
}
