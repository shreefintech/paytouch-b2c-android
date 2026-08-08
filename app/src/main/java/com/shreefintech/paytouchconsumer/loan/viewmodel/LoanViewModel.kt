package com.shreefintech.paytouchconsumer.loan.viewmodel

import android.app.Application
import com.shreefintech.paytouchconsumer.BaseBillViewModel
import com.shreefintech.paytouchconsumer.Constant
import com.shreefintech.paytouchconsumer.R
import com.shreefintech.paytouchconsumer.retrofit.ApiClient
import com.shreefintech.paytouchconsumer.retrofit.ApiHelper
import com.shreefintech.paytouchconsumer.retrofit.model.General
import com.shreefintech.paytouchconsumer.retrofit.model.loan.LoanBillItem
import com.shreefintech.paytouchconsumer.retrofit.model.loan.LoanFetchBillRequest
import com.shreefintech.paytouchconsumer.retrofit.model.loan.LoanOperatorItem
import com.shreefintech.paytouchconsumer.retrofit.model.loan.LoanOperatorsDataItem
import com.shreefintech.paytouchconsumer.retrofit.model.loan.LoanPaymentItem
import com.shreefintech.paytouchconsumer.retrofit.model.loan.LoanProcessPaymentRequest
import com.shreefintech.paytouchconsumer.utill.SharedPreferenceHelper
import com.shreefintech.paytouchconsumer.utill.Utility
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LoanViewModel(application: Application) : BaseBillViewModel(application) {

    fun loadOperators(
        onLoading: () -> Unit,
        onSuccess: (List<LoanOperatorItem>) -> Unit,
        onError: (String) -> Unit
    ) {
        if (!Utility.isInternetAvailable(getApplication())) {
            onError(getString(R.string.msgNoInternet))
            return
        }
        onLoading()
        ApiClient.apiService.getLoanOperators(bearerToken())
            .enqueue(object : Callback<General<LoanOperatorsDataItem>> {
                override fun onResponse(
                    call: Call<General<LoanOperatorsDataItem>>,
                    response: Response<General<LoanOperatorsDataItem>>
                ) {
                    if (response.isSuccessful && response.body()?.data != null) {
                        val operatorMap = response.body()!!.data!!.operators ?: emptyMap()
                        val operators = operatorMap.map { (id, name) -> LoanOperatorItem(id = id, name = name) }
                        onSuccess(operators)
                    } else {
                        onError(
                            ApiHelper.parseErrorMessage(
                                getApplication(), response.code(), response.errorBody()?.string()
                            )
                        )
                    }
                }

                override fun onFailure(call: Call<General<LoanOperatorsDataItem>>, t: Throwable) {
                    onError(t.localizedMessage ?: getString(R.string.errGeneric))
                }
            })
    }

    fun fetchBill(
        consumerNumber: String,
        operatorId: String,
        onLoading: () -> Unit,
        onSuccess: (LoanBillItem) -> Unit,
        onError: (String) -> Unit
    ) {
        if (!Utility.isInternetAvailable(getApplication())) {
            onError(getString(R.string.msgNoInternet))
            return
        }
        onLoading()
        ApiClient.apiService.fetchLoanBill(
            bearerToken(),
            LoanFetchBillRequest(
                consumerNumber = consumerNumber,
                operatorId = operatorId,
                circleId = Constant.LOAN_CIRCLE_ID
            )
        ).enqueue(object : Callback<General<List<LoanBillItem>>> {
            override fun onResponse(
                call: Call<General<List<LoanBillItem>>>,
                response: Response<General<List<LoanBillItem>>>
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

            override fun onFailure(call: Call<General<List<LoanBillItem>>>, t: Throwable) {
                onError(t.localizedMessage ?: getString(R.string.errGeneric))
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
        onSuccess: (LoanPaymentItem) -> Unit,
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

    private fun processPayment(
        consumerNumber: String,
        operatorId: String,
        amount: Double,
        fee: Double,
        total: Double,
        onSuccess: (LoanPaymentItem) -> Unit,
        onError: (String) -> Unit
    ) {
        if (!Utility.isInternetAvailable(getApplication())) {
            onError(getString(R.string.msgNoInternet))
            return
        }
        ApiClient.apiService.processLoanPayment(
            bearerToken(),
            LoanProcessPaymentRequest(
                consumerNumber = consumerNumber,
                operatorId = operatorId,
                circleId = Constant.LOAN_CIRCLE_ID,
                amount = amount,
                platformFee = fee,
                totalPayable = total
            )
        ).enqueue(object : Callback<LoanPaymentItem> {
            override fun onResponse(
                call: Call<LoanPaymentItem>,
                response: Response<LoanPaymentItem>
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

            override fun onFailure(call: Call<LoanPaymentItem>, t: Throwable) {
                onError(t.localizedMessage ?: getString(R.string.errGeneric))
            }
        })
    }
}
