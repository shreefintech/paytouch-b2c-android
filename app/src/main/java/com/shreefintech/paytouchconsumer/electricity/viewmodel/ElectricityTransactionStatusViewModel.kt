package com.shreefintech.paytouchconsumer.electricity.viewmodel

import android.app.Application
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import com.shreefintech.paytouchconsumer.Constant
import com.shreefintech.paytouchconsumer.R
import com.shreefintech.paytouchconsumer.transactions.model.TransactionItem
import com.shreefintech.paytouchconsumer.retrofit.ApiClient
import com.shreefintech.paytouchconsumer.retrofit.ApiHelper
import com.shreefintech.paytouchconsumer.retrofit.model.General
import com.shreefintech.paytouchconsumer.retrofit.model.electricity.ElectricityTransactionReportDataItem
import com.shreefintech.paytouchconsumer.retrofit.model.electricity.ElectricityTransactionStatusRequest
import com.shreefintech.paytouchconsumer.utill.SharedPreferenceHelper
import com.shreefintech.paytouchconsumer.utill.Utility
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ElectricityTransactionStatusViewModel(application: Application) : AndroidViewModel(application) {

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
        ApiClient.apiService.getElectricityTransactionStatus(
            bearerToken(),
            ElectricityTransactionStatusRequest(transactionId = query)
        ).enqueue(object : Callback<General<List<ElectricityTransactionReportDataItem>>> {
            override fun onResponse(
                call: Call<General<List<ElectricityTransactionReportDataItem>>>,
                response: Response<General<List<ElectricityTransactionReportDataItem>>>
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
                call: Call<General<List<ElectricityTransactionReportDataItem>>>,
                t: Throwable
            ) {
                onError(t.localizedMessage ?: getString(R.string.err_generic))
            }
        })
    }

    private fun mapToTransactionItem(index: Int, item: ElectricityTransactionReportDataItem): TransactionItem {
        return TransactionItem(
            mobileNumber    = item.subscriberNo ?: "--",
            transactionId   = item.transactionId ?: "--",
            amount          = "₹%.2f".format(item.amount ?: 0.0),
            status          = item.status ?: "--",
            categoryIconRes = R.drawable.ic_electricity,
            username        = item.customerName ?: "--",
            date            = item.createdAt ?: "--",
            platformFee     = "₹%.2f".format(item.platformFee ?: 0.0),
            totalPayable    = "₹%.2f".format(item.totalPayable ?: 0.0),
            referenceId     = item.transactionId ?: "--",
            userId          = (index + 1).toString(),
            accountNumber   = item.subscriberNo ?: "--",
            companyName     = item.operatorId ?: "--"
        )
    }

    private fun bearerToken(): String {
        val token = SharedPreferenceHelper.getSharedPreferenceString(
            getApplication(), Constant.KEY_TOKEN, ""
        ) ?: ""
        return "Bearer $token"
    }

    private fun getString(@StringRes resId: Int): String = getApplication<Application>().getString(resId)
}
