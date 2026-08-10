package com.shreefintech.paytouchconsumer.municipaltax.viewmodel

import android.app.Application
import com.shreefintech.paytouchconsumer.BaseBillViewModel
import com.shreefintech.paytouchconsumer.Constant
import com.shreefintech.paytouchconsumer.R
import com.shreefintech.paytouchconsumer.retrofit.ApiClient
import com.shreefintech.paytouchconsumer.retrofit.ApiHelper
import com.shreefintech.paytouchconsumer.retrofit.model.General
import com.shreefintech.paytouchconsumer.retrofit.model.municipaltax.MunicipalTaxFetchBillDataItem
import com.shreefintech.paytouchconsumer.retrofit.model.municipaltax.MunicipalTaxFetchBillRequest
import com.shreefintech.paytouchconsumer.retrofit.model.municipaltax.MunicipalTaxOperatorItem
import com.shreefintech.paytouchconsumer.retrofit.model.municipaltax.MunicipalTaxPaymentItem
import com.shreefintech.paytouchconsumer.retrofit.model.municipaltax.MunicipalTaxProcessPaymentRequest
import com.shreefintech.paytouchconsumer.utill.SharedPreferenceHelper
import com.shreefintech.paytouchconsumer.utill.Utility
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MunicipalTaxViewModel(application: Application) : BaseBillViewModel(application) {

    fun loadOperators(
        onLoading: () -> Unit,
        onSuccess: (List<MunicipalTaxOperatorItem>) -> Unit,
        onError: (String) -> Unit
    ) {
        if (!Utility.isInternetAvailable(getApplication())) {
            onError(getString(R.string.msgNoInternet))
            return
        }
        onLoading()
        ApiClient.apiService.getMunicipalTaxOperators(bearerToken())
            .enqueue(object : Callback<General<List<MunicipalTaxOperatorItem>>> {
                override fun onResponse(
                    call: Call<General<List<MunicipalTaxOperatorItem>>>,
                    response: Response<General<List<MunicipalTaxOperatorItem>>>
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

                override fun onFailure(call: Call<General<List<MunicipalTaxOperatorItem>>>, t: Throwable) {
                    onError(t.localizedMessage ?: getString(R.string.errGeneric))
                }
            })
    }

    fun fetchBill(
        houseNumber: String,
        cir: String,
        op: String,
        onLoading: () -> Unit,
        onSuccess: (MunicipalTaxFetchBillDataItem) -> Unit,
        onError: (String) -> Unit
    ) {
        if (!Utility.isInternetAvailable(getApplication())) {
            onError(getString(R.string.msgNoInternet))
            return
        }
        onLoading()
        ApiClient.apiService.fetchMunicipalTaxBill(
            bearerToken(),
            MunicipalTaxFetchBillRequest(houseNumber = houseNumber, cir = cir, op = op)
        ).enqueue(object : Callback<General<List<MunicipalTaxFetchBillDataItem>>> {
            override fun onResponse(
                call: Call<General<List<MunicipalTaxFetchBillDataItem>>>,
                response: Response<General<List<MunicipalTaxFetchBillDataItem>>>
            ) {
                val first = response.body()?.data?.firstOrNull()
                if (response.isSuccessful && first != null) {
                    onSuccess(first)
                } else {
                    onError(
                        ApiHelper.parseErrorMessage(
                            getApplication(), response.code(), response.errorBody()?.string()
                        )
                    )
                }
            }

            override fun onFailure(call: Call<General<List<MunicipalTaxFetchBillDataItem>>>, t: Throwable) {
                onError(t.localizedMessage ?: getString(R.string.errGeneric))
            }
        })
    }

    fun verifyAndPay(
        houseNumber: String,
        op: String,
        cir: String,
        amount: Double,
        total: Double,
        customerName: String,
        dueDate: String,
        opName: String,
        onLoading: () -> Unit,
        onSuccess: (MunicipalTaxPaymentItem) -> Unit,
        onError: (String) -> Unit
    ) {
        if (!Utility.isInternetAvailable(getApplication())) {
            onError(getString(R.string.msgNoInternet))
            return
        }
        onLoading()
        val proceed = { processPayment(houseNumber, op, cir, amount, customerName, dueDate, opName, onSuccess, onError) }
        val fallback = { checkWalletBalance(total, proceed, onError) }
        val userId = SharedPreferenceHelper.getSharedPreferenceString(
            getApplication(), Constant.KEY_USER_ID, ""
        ) ?: ""
        checkVpsBalance(total, userId, onSufficient = proceed, onFallback = fallback)
    }

    private fun processPayment(
        houseNumber: String,
        op: String,
        cir: String,
        amount: Double,
        customerName: String,
        dueDate: String,
        opName: String,
        onSuccess: (MunicipalTaxPaymentItem) -> Unit,
        onError: (String) -> Unit
    ) {
        if (!Utility.isInternetAvailable(getApplication())) {
            onError(getString(R.string.msgNoInternet))
            return
        }
        ApiClient.apiService.processMunicipalTaxPayment(
            bearerToken(),
            MunicipalTaxProcessPaymentRequest(
                houseNumber  = houseNumber,
                op           = op,
                cir          = cir,
                amount       = amount,
                customerName = customerName,
                dueDate      = dueDate,
                opName       = opName
            )
        ).enqueue(object : Callback<MunicipalTaxPaymentItem> {
            override fun onResponse(
                call: Call<MunicipalTaxPaymentItem>,
                response: Response<MunicipalTaxPaymentItem>
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

            override fun onFailure(call: Call<MunicipalTaxPaymentItem>, t: Throwable) {
                onError(t.localizedMessage ?: getString(R.string.errGeneric))
            }
        })
    }
}
