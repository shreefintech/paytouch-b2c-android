package com.shreefintech.paytouchconsumer.billpay.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.shreefintech.paytouchconsumer.R
import com.shreefintech.paytouchconsumer.retrofit.ApiClient
import com.shreefintech.paytouchconsumer.retrofit.ApiHelper
import com.shreefintech.paytouchconsumer.retrofit.model.General
import com.shreefintech.paytouchconsumer.retrofit.model.billpay.BillPayBillItem
import com.shreefintech.paytouchconsumer.retrofit.model.billpay.BillPayBillerItem
import com.shreefintech.paytouchconsumer.retrofit.model.billpay.BillPayCircleItem
import com.shreefintech.paytouchconsumer.retrofit.model.billpay.BillPayFeePreviewItem
import com.shreefintech.paytouchconsumer.retrofit.model.billpay.BillPayFetchBillRequest
import com.shreefintech.paytouchconsumer.retrofit.model.billpay.BillPayPayRequest
import com.shreefintech.paytouchconsumer.retrofit.model.billpay.BillPayTransactionItem
import com.shreefintech.paytouchconsumer.utill.MoneyRequestId
import com.shreefintech.paytouchconsumer.utill.Utility
import com.shreefintech.paytouchconsumer.utill.bearerToken
import com.shreefintech.paytouchconsumer.utill.envelopeCallback
import com.shreefintech.paytouchconsumer.utill.getString
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

/** Shared Bill Pay form for every category: billers → (circle / plans) or fetch bill → fee preview → pay. */
class BillPayViewModel(application: Application) : AndroidViewModel(application) {

    private val moneyRequestId = MoneyRequestId()

    // Last fee-preview call — a slower reply for an older amount must not overwrite the current one
    private var feePreviewCall: Call<General<BillPayFeePreviewItem>>? = null

    fun loadBillers(
        category: String,
        onLoading: () -> Unit,
        onSuccess: (List<BillPayBillerItem>) -> Unit,
        onError: (String) -> Unit
    ) {
        if (!Utility.isInternetAvailable(getApplication())) { onError(getString(R.string.msgNoInternet)); return }
        onLoading()
        ApiClient.apiService.getBillPayBillers(bearerToken(), category).enqueue(envelopeCallback(onSuccess, onError))
    }

    fun loadCircles(
        onLoading: () -> Unit,
        onSuccess: (List<BillPayCircleItem>) -> Unit,
        onError: (String) -> Unit
    ) {
        if (!Utility.isInternetAvailable(getApplication())) { onError(getString(R.string.msgNoInternet)); return }
        onLoading()
        ApiClient.apiService.getBillPayCircles(bearerToken()).enqueue(envelopeCallback(onSuccess, onError))
    }

    fun fetchBill(
        billerId: String,
        inputParams: Map<String, String>,
        onLoading: () -> Unit,
        onSuccess: (BillPayBillItem) -> Unit,
        onError: (String) -> Unit
    ) {
        if (!Utility.isInternetAvailable(getApplication())) { onError(getString(R.string.msgNoInternet)); return }
        onLoading()
        ApiClient.apiService.fetchBillPayBill(bearerToken(), BillPayFetchBillRequest(billerId, inputParams))
            .enqueue(envelopeCallback(onSuccess, onError))
    }

    fun loadFeePreview(
        amount: String,
        onLoading: () -> Unit,
        onSuccess: (BillPayFeePreviewItem) -> Unit,
        onError: (String) -> Unit
    ) {
        if (!Utility.isInternetAvailable(getApplication())) { onError(getString(R.string.msgNoInternet)); return }
        feePreviewCall?.cancel()
        onLoading()
        feePreviewCall = ApiClient.apiService.getBillPayFeePreview(bearerToken(), amount).also { call ->
            call.enqueue(envelopeCallback<BillPayFeePreviewItem>(onSuccess) { msg ->
                // Cancelled because a newer amount was asked for — that call reports instead
                if (!call.isCanceled) onError(msg)
            })
        }
    }

    fun cancelFeePreview() {
        feePreviewCall?.cancel()
        feePreviewCall = null
    }

    /**
     * POST billpay/pay. [onError] gets the message plus the 403 `error_code` (null otherwise).
     * [onNotConfirmed]: timeout / 5xx / no reply — outcome unknown, never shown as failed. The same
     * request_id is reused while the same attempt (biller + params + amount) is retried, so it can't charge twice.
     */
    fun pay(
        billerId: String,
        inputParams: Map<String, String>,
        amount: String,
        mpin: String,
        onLoading: () -> Unit,
        onSuccess: (BillPayTransactionItem) -> Unit,
        onError: (message: String, errorCode: String?) -> Unit,
        onNotConfirmed: (String) -> Unit
    ) {
        if (!Utility.isInternetAvailable(getApplication())) { onError(getString(R.string.msgNoInternet), null); return }
        onLoading()
        val requestId = moneyRequestId.forAttempt("$billerId:${inputParams.toSortedMap()}:$amount")
        ApiClient.apiService.payBillPay(bearerToken(), BillPayPayRequest(billerId, inputParams, amount, mpin, requestId))
            .enqueue(object : Callback<General<BillPayTransactionItem>> {
                override fun onResponse(
                    call: Call<General<BillPayTransactionItem>>,
                    response: Response<General<BillPayTransactionItem>>
                ) {
                    if (response.code() >= 500) {
                        onNotConfirmed(getString(R.string.msgBillPayNotConfirmed))
                        return
                    }
                    // Definite answer from the server — the next attempt gets a new request_id
                    moneyRequestId.clear()
                    val item = response.body()?.data
                    if (response.isSuccessful && item != null) {
                        onSuccess(item)
                    } else {
                        val errorBody = response.errorBody()?.string()
                        onError(
                            ApiHelper.parseErrorMessage(getApplication(), response.code(), errorBody),
                            ApiHelper.parseErrorCode(errorBody)
                        )
                    }
                }

                override fun onFailure(call: Call<General<BillPayTransactionItem>>, t: Throwable) {
                    t.printStackTrace()
                    onNotConfirmed(getString(R.string.msgBillPayNotConfirmed))
                }
            })
    }
}
