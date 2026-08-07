package com.shreefintech.paytouchconsumer.loadwallet.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.shreefintech.paytouchconsumer.R
import com.shreefintech.paytouchconsumer.loadwallet.model.WalletTransactionItem
import com.shreefintech.paytouchconsumer.retrofit.ApiClient
import com.shreefintech.paytouchconsumer.retrofit.ApiHelper
import com.shreefintech.paytouchconsumer.retrofit.model.General
import com.shreefintech.paytouchconsumer.retrofit.model.WalletDataItem
import com.shreefintech.paytouchconsumer.retrofit.model.hdfc.HdfcCreateOrderRequest
import com.shreefintech.paytouchconsumer.retrofit.model.hdfc.HdfcOrderItem
import com.shreefintech.paytouchconsumer.retrofit.model.hdfc.HdfcOrderResponseItem
import com.shreefintech.paytouchconsumer.retrofit.model.wallet.WalletHistoryItem
import com.shreefintech.paytouchconsumer.retrofit.model.wallet.WalletHistoryPageItem
import com.shreefintech.paytouchconsumer.utill.Utility
import com.shreefintech.paytouchconsumer.utill.bearerToken
import com.shreefintech.paytouchconsumer.utill.getString
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LoadWalletViewModel(application: Application) : AndroidViewModel(application) {

    fun fetchUserWalletData(
        onLoading: () -> Unit,
        onSuccess: (WalletDataItem) -> Unit,
        onError: (String) -> Unit
    ) {
        if (!Utility.isInternetAvailable(getApplication())) {
            onError(getString(R.string.msgNoInternet))
            return
        }
        onLoading()
        ApiClient.apiService.getUserWalletData(bearerToken())
            .enqueue(object : Callback<General<WalletDataItem>> {
                override fun onResponse(
                    call: Call<General<WalletDataItem>>,
                    response: Response<General<WalletDataItem>>
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

                override fun onFailure(call: Call<General<WalletDataItem>>, t: Throwable) {
                    onError(t.localizedMessage ?: getString(R.string.errGeneric))
                }
            })
    }

    fun fetchRecentHistory(
        onSuccess: (ArrayList<WalletTransactionItem>) -> Unit,
        onError: (String) -> Unit
    ) {
        if (!Utility.isInternetAvailable(getApplication())) {
            onError(getString(R.string.msgNoInternet))
            return
        }
        ApiClient.apiService.getWalletHistory(bearerToken(), page = 1, perPage = 2)
            .enqueue(object : Callback<General<WalletHistoryPageItem>> {
                override fun onResponse(
                    call: Call<General<WalletHistoryPageItem>>,
                    response: Response<General<WalletHistoryPageItem>>
                ) {
                    if (response.isSuccessful && response.body()?.data != null) {
                        val rawList = response.body()!!.data!!.data ?: emptyList()
                        val list = ArrayList<WalletTransactionItem>()
                        rawList.forEach { list.add(mapToWalletTransactionItem(it)) }
                        onSuccess(list)
                    } else {
                        onError(
                            ApiHelper.parseErrorMessage(
                                getApplication(), response.code(), response.errorBody()?.string()
                            )
                        )
                    }
                }

                override fun onFailure(call: Call<General<WalletHistoryPageItem>>, t: Throwable) {
                    onError(t.localizedMessage ?: getString(R.string.errGeneric))
                }
            })
    }

    fun createHdfcOrder(
        amount: Double,
        description: String,
        onLoading: () -> Unit,
        onSuccess: (HdfcOrderItem) -> Unit,
        onError: (String) -> Unit
    ) {
        if (!Utility.isInternetAvailable(getApplication())) {
            onError(getString(R.string.msgNoInternet))
            return
        }
        onLoading()
        ApiClient.apiService.createHdfcOrder(
            bearerToken(),
            HdfcCreateOrderRequest(amount = amount, description = description)
        ).enqueue(object : Callback<HdfcOrderResponseItem> {
            override fun onResponse(
                call: Call<HdfcOrderResponseItem>,
                response: Response<HdfcOrderResponseItem>
            ) {
                if (response.isSuccessful && response.body()?.success == true && response.body()?.data != null) {
                    onSuccess(response.body()!!.data!!)
                } else {
                    onError(
                        ApiHelper.parseErrorMessage(
                            getApplication(), response.code(), response.errorBody()?.string()
                        )
                    )
                }
            }

            override fun onFailure(call: Call<HdfcOrderResponseItem>, t: Throwable) {
                onError(t.localizedMessage ?: getString(R.string.errGeneric))
            }
        })
    }

    fun checkHdfcOrderStatus(
        orderId: String,
        onLoading: () -> Unit,
        onSuccess: (HdfcOrderItem) -> Unit,
        onError: (String) -> Unit
    ) {
        if (!Utility.isInternetAvailable(getApplication())) {
            onError(getString(R.string.msgNoInternet))
            return
        }
        onLoading()
        ApiClient.apiService.getHdfcOrderStatus(bearerToken(), orderId)
            .enqueue(object : Callback<HdfcOrderResponseItem> {
                override fun onResponse(
                    call: Call<HdfcOrderResponseItem>,
                    response: Response<HdfcOrderResponseItem>
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

                override fun onFailure(call: Call<HdfcOrderResponseItem>, t: Throwable) {
                    onError(t.localizedMessage ?: getString(R.string.errGeneric))
                }
            })
    }

    private fun mapToWalletTransactionItem(item: WalletHistoryItem): WalletTransactionItem {
        return WalletTransactionItem(
            title = item.serviceName ?: "--",
            date = Utility.formatDate(item.createdAt),
            amount = Utility.formatAmount(item.amount),
            isCredit = item.type?.uppercase() == "CREDIT"
        )
    }
}