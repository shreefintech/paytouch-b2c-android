package com.shreefintech.paytouchconsumer

import android.app.Application
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import com.shreefintech.paytouchconsumer.retrofit.ApiAdminClient
import com.shreefintech.paytouchconsumer.retrofit.ApiClient
import com.shreefintech.paytouchconsumer.retrofit.ApiHelper
import com.shreefintech.paytouchconsumer.retrofit.model.General
import com.shreefintech.paytouchconsumer.retrofit.model.VpsBalanceItem
import com.shreefintech.paytouchconsumer.retrofit.model.WalletDataItem
import com.shreefintech.paytouchconsumer.utill.SharedPreferenceHelper
import com.shreefintech.paytouchconsumer.utill.Utility
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

abstract class BaseBillViewModel(application: Application) : AndroidViewModel(application) {

    protected fun checkVpsBalance(
        totalPayable: Double,
        userId: String,
        onSufficient: () -> Unit,
        onFallback: () -> Unit
    ) {
        if (!Utility.isInternetAvailable(getApplication())) { onFallback(); return }
        ApiAdminClient.apiService.getVpsBalance(userId)
            .enqueue(object : Callback<VpsBalanceItem> {
                override fun onResponse(call: Call<VpsBalanceItem>, response: Response<VpsBalanceItem>) {
                    val balance = response.body()?.balance?.toDoubleOrNull() ?: 0.0
                    if (response.isSuccessful && balance >= totalPayable) onSufficient()
                    else onFallback()
                }

                override fun onFailure(call: Call<VpsBalanceItem>, t: Throwable) {
                    t.printStackTrace()
                    onFallback()
                }
            })
    }

    protected fun checkWalletBalance(
        totalPayable: Double,
        onSufficient: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (!Utility.isInternetAvailable(getApplication())) {
            onError(getString(R.string.msgNoInternet))
            return
        }
        ApiClient.apiService.getUserWalletData(bearerToken())
            .enqueue(object : Callback<General<WalletDataItem>> {
                override fun onResponse(
                    call: Call<General<WalletDataItem>>,
                    response: Response<General<WalletDataItem>>
                ) {
                    if (response.isSuccessful && response.body()?.data != null) {
                        val balance = response.body()!!.data!!.walletBalance?.toDoubleOrNull() ?: 0.0
                        if (balance >= totalPayable) onSufficient()
                        else onError(getString(R.string.msgInsufficientBalance))
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

    protected fun bearerToken(): String {
        val token = SharedPreferenceHelper.getSharedPreferenceString(
            getApplication(), Constant.KEY_TOKEN, ""
        ) ?: ""
        return "Bearer $token"
    }

    protected fun getString(@StringRes resId: Int): String = getApplication<Application>().getString(resId)
}
