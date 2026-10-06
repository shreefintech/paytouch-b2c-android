package com.shreefintech.paytouchconsumer.earningwallet

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.shreefintech.paytouchconsumer.R
import com.shreefintech.paytouchconsumer.retrofit.ApiClient
import com.shreefintech.paytouchconsumer.retrofit.ApiHelper
import com.shreefintech.paytouchconsumer.retrofit.model.General
import com.shreefintech.paytouchconsumer.retrofit.model.WalletDataItem
import com.shreefintech.paytouchconsumer.retrofit.model.rewards.EarningWalletActionDataItem
import com.shreefintech.paytouchconsumer.retrofit.model.rewards.EarningWalletAmountRequest
import com.shreefintech.paytouchconsumer.retrofit.model.rewards.EarningWalletItem
import com.shreefintech.paytouchconsumer.retrofit.model.rewards.RewardsLevelItem
import com.shreefintech.paytouchconsumer.utill.Utility
import com.shreefintech.paytouchconsumer.utill.bearerToken
import com.shreefintech.paytouchconsumer.utill.getString
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class EarningWalletViewModel(application: Application) : AndroidViewModel(application) {

    fun fetchEarningWallet(
        onLoading: () -> Unit,
        onSuccess: (EarningWalletItem) -> Unit,
        onError: (String) -> Unit
    ) {
        if (!Utility.isInternetAvailable(getApplication())) { onError(getString(R.string.msgNoInternet)); return }
        onLoading()
        ApiClient.apiService.getEarningWallet(bearerToken())
            .enqueue(object : Callback<General<EarningWalletItem?>> {
                override fun onResponse(call: Call<General<EarningWalletItem?>>, response: Response<General<EarningWalletItem?>>) {
                    val data = response.body()?.data
                    if (response.isSuccessful && data != null) onSuccess(data)
                    else onError(ApiHelper.parseErrorMessage(getApplication(), response.code(), response.errorBody()?.string()))
                }
                override fun onFailure(call: Call<General<EarningWalletItem?>>, t: Throwable) {
                    onError(t.localizedMessage ?: getString(R.string.errGeneric))
                }
            })
    }

    fun fetchWalletData(
        onSuccess: (WalletDataItem) -> Unit,
        onError: (String) -> Unit
    ) {
        if (!Utility.isInternetAvailable(getApplication())) { onError(getString(R.string.msgNoInternet)); return }
        ApiClient.apiService.getUserWalletData(bearerToken())
            .enqueue(object : Callback<General<WalletDataItem>> {
                override fun onResponse(call: Call<General<WalletDataItem>>, response: Response<General<WalletDataItem>>) {
                    val data = response.body()?.data
                    if (response.isSuccessful && data != null) onSuccess(data)
                    else onError(ApiHelper.parseErrorMessage(getApplication(), response.code(), response.errorBody()?.string()))
                }
                override fun onFailure(call: Call<General<WalletDataItem>>, t: Throwable) {
                    onError(t.localizedMessage ?: getString(R.string.errGeneric))
                }
            })
    }

    fun fetchLevel(
        onSuccess: (RewardsLevelItem) -> Unit,
        onError: (String) -> Unit
    ) {
        if (!Utility.isInternetAvailable(getApplication())) { onError(getString(R.string.msgNoInternet)); return }
        ApiClient.apiService.getRewardsLevel(bearerToken())
            .enqueue(object : Callback<General<RewardsLevelItem?>> {
                override fun onResponse(call: Call<General<RewardsLevelItem?>>, response: Response<General<RewardsLevelItem?>>) {
                    val data = response.body()?.data
                    if (response.isSuccessful && data != null) onSuccess(data)
                    else onError(ApiHelper.parseErrorMessage(getApplication(), response.code(), response.errorBody()?.string()))
                }
                override fun onFailure(call: Call<General<RewardsLevelItem?>>, t: Throwable) {
                    onError(t.localizedMessage ?: getString(R.string.errGeneric))
                }
            })
    }

    fun postOptIn(
        onLoading: () -> Unit,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        if (!Utility.isInternetAvailable(getApplication())) { onError(getString(R.string.msgNoInternet)); return }
        onLoading()
        ApiClient.apiService.postEarningWalletOptIn(bearerToken())
            .enqueue(object : Callback<General<EarningWalletActionDataItem>> {
                override fun onResponse(call: Call<General<EarningWalletActionDataItem>>, response: Response<General<EarningWalletActionDataItem>>) {
                    val body = response.body()
                    if (response.isSuccessful && body?.success == true) {
                        onSuccess(body.message ?: "")
                    } else {
                        onError(body?.message ?: ApiHelper.parseErrorMessage(getApplication(), response.code(), response.errorBody()?.string()))
                    }
                }
                override fun onFailure(call: Call<General<EarningWalletActionDataItem>>, t: Throwable) {
                    onError(t.localizedMessage ?: getString(R.string.errGeneric))
                }
            })
    }

    fun postLock(
        amount: Double,
        onLoading: () -> Unit,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        if (!Utility.isInternetAvailable(getApplication())) { onError(getString(R.string.msgNoInternet)); return }
        onLoading()
        ApiClient.apiService.postEarningWalletLock(bearerToken(), EarningWalletAmountRequest(amount))
            .enqueue(object : Callback<General<EarningWalletActionDataItem>> {
                override fun onResponse(call: Call<General<EarningWalletActionDataItem>>, response: Response<General<EarningWalletActionDataItem>>) {
                    val body = response.body()
                    if (response.isSuccessful && body?.success == true) {
                        onSuccess(body.message ?: "")
                    } else {
                        onError(body?.message ?: ApiHelper.parseErrorMessage(getApplication(), response.code(), response.errorBody()?.string()))
                    }
                }
                override fun onFailure(call: Call<General<EarningWalletActionDataItem>>, t: Throwable) {
                    onError(t.localizedMessage ?: getString(R.string.errGeneric))
                }
            })
    }

    fun postWithdraw(
        amount: Double,
        onLoading: () -> Unit,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        if (!Utility.isInternetAvailable(getApplication())) { onError(getString(R.string.msgNoInternet)); return }
        onLoading()
        ApiClient.apiService.postEarningWalletWithdraw(bearerToken(), EarningWalletAmountRequest(amount))
            .enqueue(object : Callback<General<EarningWalletActionDataItem>> {
                override fun onResponse(call: Call<General<EarningWalletActionDataItem>>, response: Response<General<EarningWalletActionDataItem>>) {
                    val body = response.body()
                    if (response.isSuccessful && body?.success == true) {
                        onSuccess(body.message ?: "")
                    } else {
                        onError(body?.message ?: ApiHelper.parseErrorMessage(getApplication(), response.code(), response.errorBody()?.string()))
                    }
                }
                override fun onFailure(call: Call<General<EarningWalletActionDataItem>>, t: Throwable) {
                    onError(t.localizedMessage ?: getString(R.string.errGeneric))
                }
            })
    }
}
