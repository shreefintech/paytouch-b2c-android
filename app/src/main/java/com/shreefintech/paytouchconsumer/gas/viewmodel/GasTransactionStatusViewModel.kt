package com.shreefintech.paytouchconsumer.gas.viewmodel

import android.app.Application
import com.shreefintech.paytouchconsumer.BaseBillViewModel
import com.shreefintech.paytouchconsumer.R
import com.shreefintech.paytouchconsumer.transactions.model.TransactionItem
import com.shreefintech.paytouchconsumer.retrofit.ApiClient
import com.shreefintech.paytouchconsumer.retrofit.ApiHelper
import com.shreefintech.paytouchconsumer.retrofit.model.General
import com.shreefintech.paytouchconsumer.retrofit.model.gas.GasTransactionReportDataItem
import com.shreefintech.paytouchconsumer.retrofit.model.gas.GasTransactionStatusRequest
import com.shreefintech.paytouchconsumer.utill.Utility
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class GasTransactionStatusViewModel(application: Application) : BaseBillViewModel(application) {

    fun searchTransactionStatus(
        query: String?,
        onLoading: () -> Unit,
        onSuccess: (ArrayList<TransactionItem>) -> Unit,
        onError: (String) -> Unit
    ) {
        if (!Utility.isInternetAvailable(getApplication())) {
            onError(getString(R.string.msgNoInternet))
            return
        }
        onLoading()
        ApiClient.apiService.getGasTransactionStatus(
            bearerToken(),
            GasTransactionStatusRequest(transactionId = query)
        ).enqueue(object : Callback<General<List<GasTransactionReportDataItem>>> {
            override fun onResponse(
                call: Call<General<List<GasTransactionReportDataItem>>>,
                response: Response<General<List<GasTransactionReportDataItem>>>
            ) {
                if (response.isSuccessful && response.body()?.data != null) {
                    val list = ArrayList<TransactionItem>()
                    response.body()!!.data!!.forEachIndexed { index, item ->
                        list.add(mapToTransactionItem(index, item))
                    }
                    onSuccess(list)
                } else {
                    onError(
                        ApiHelper.parseErrorMessage(
                            getApplication(), response.code(), response.errorBody()?.string()
                        )
                    )
                }
            }

            override fun onFailure(
                call: Call<General<List<GasTransactionReportDataItem>>>,
                t: Throwable
            ) {
                onError(t.localizedMessage ?: getString(R.string.errGeneric))
            }
        })
    }

    private fun mapToTransactionItem(index: Int, item: GasTransactionReportDataItem): TransactionItem {
        return TransactionItem(
            mobileNumber    = item.connectionNumber ?: "--",
            transactionId   = item.transactionId ?: "--",
            amount          = "₹${item.billAmount ?: "0.00"}",
            status          = item.status ?: "--",
            categoryIconRes = R.drawable.ic_gas,
            username        = item.customerName ?: "--",
            date            = item.createdAt ?: "--",
            platformFee     = "₹${item.platformFee ?: "0.00"}",
            totalPayable    = "₹${item.totalPayable ?: "0.00"}",
            referenceId     = item.transactionId ?: "--",
            userId          = (index + 1).toString(),
            accountNumber   = item.connectionNumber ?: "--",
            companyName     = item.operatorName?.takeIf { it.isNotEmpty() } ?: item.subservice ?: "--"
        )
    }

}
