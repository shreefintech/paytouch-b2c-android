package com.shreefintech.paytouchconsumer.electricity.viewmodel

import android.app.Application
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import com.shreefintech.paytouchconsumer.Constant
import com.shreefintech.paytouchconsumer.R
import com.shreefintech.paytouchconsumer.electricity.model.TransactionItem
import com.shreefintech.paytouchconsumer.retrofit.ApiClient
import com.shreefintech.paytouchconsumer.retrofit.ApiHelper
import com.shreefintech.paytouchconsumer.retrofit.model.General
import com.shreefintech.paytouchconsumer.retrofit.model.electricity.ElectricityTransactionReportDataItem
import com.shreefintech.paytouchconsumer.retrofit.model.electricity.ElectricityTransactionReportRequest
import com.shreefintech.paytouchconsumer.utill.SharedPreferenceHelper
import com.shreefintech.paytouchconsumer.utill.Utility
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class TransactionReportViewModel(application: Application) : AndroidViewModel(application) {

    fun getTransactionReport(
        fromDate: String?,
        toDate: String?,
        status: String?,
        consumerNo: String?,
        onLoading: () -> Unit,
        onSuccess: (ArrayList<TransactionItem>) -> Unit,
        onError: (String) -> Unit
    ) {
        if (!Utility.isInternetAvailable(getApplication())) {
            onError(getString(R.string.msgNoInternet))
            return
        }
        onLoading()
        ApiClient.apiService.getElectricityPaymentReport(
            bearerToken(),
            ElectricityTransactionReportRequest(fromDate, toDate, status, consumerNo)
        ).enqueue(object : Callback<General<List<ElectricityTransactionReportDataItem>>> {
            override fun onResponse(
                call: Call<General<List<ElectricityTransactionReportDataItem>>>,
                response: Response<General<List<ElectricityTransactionReportDataItem>>>
            ) {
                if (response.isSuccessful && response.body()?.data != null) {
                    val list = ArrayList<TransactionItem>()
                    response.body()!!.data!!.forEachIndexed { index, item ->
                        list.add(mapToTransactionItem(item))
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
                onError(t.localizedMessage ?: getString(R.string.errGeneric))
            }
        })
    }

    private fun mapToTransactionItem(item: ElectricityTransactionReportDataItem): TransactionItem {
        return TransactionItem(
            mobileNumber    = item.consumerNo ?: "--",
            transactionId   = item.transactionId ?: "--",
            amount          = "₹%.2f".format(item.totalPayable ?: 0.0),
            status          = item.status ?: "--",
            categoryIconRes = R.drawable.ic_electricity,
            username        = item.customerName ?: "--",
            date            = item.createdAt ?: "--",
            platformFee     = "₹%.2f".format(item.platformFee ?: 0.0),
            totalPayable    = "₹%.2f".format(item.totalPayable ?: 0.0),
            referenceId     = item.transactionId ?: "--",
            userId          = item.id?.toString() ?: "--",
            accountNumber   = item.consumerNo ?: "--",
            companyName     = item.subservice?.takeIf { it.isNotEmpty() } ?: item.operatorId ?: "--"
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
