package com.shreefintech.paytouchconsumer.earningwallet.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.shreefintech.paytouchconsumer.R
import com.shreefintech.paytouchconsumer.retrofit.ApiClient
import com.shreefintech.paytouchconsumer.retrofit.ApiHelper
import com.shreefintech.paytouchconsumer.retrofit.model.General
import com.shreefintech.paytouchconsumer.retrofit.model.auth.MessageItem
import com.shreefintech.paytouchconsumer.retrofit.model.rewards.RewardsLevelItem
import com.shreefintech.paytouchconsumer.retrofit.model.wallet.WalletBalanceItem
import com.shreefintech.paytouchconsumer.retrofit.model.wallet.WalletEarningAmountRequest
import com.shreefintech.paytouchconsumer.retrofit.model.wallet.WalletEarningItem
import com.shreefintech.paytouchconsumer.utill.MoneyRequestId
import com.shreefintech.paytouchconsumer.utill.Utility
import com.shreefintech.paytouchconsumer.utill.bearerToken
import com.shreefintech.paytouchconsumer.utill.getString
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class EarningWalletViewModel(application: Application) : AndroidViewModel(application) {

    private val moneyRequestId = MoneyRequestId()

    fun fetchEarningWallet(
        onLoading: () -> Unit,
        onSuccess: (WalletEarningItem) -> Unit,
        onError: (String) -> Unit
    ) {
        if (!Utility.isInternetAvailable(getApplication())) { onError(getString(R.string.msgNoInternet)); return }
        onLoading()
        ApiClient.apiService.getWalletEarning(bearerToken())
            .enqueue(object : Callback<General<WalletEarningItem>> {
                override fun onResponse(call: Call<General<WalletEarningItem>>, response: Response<General<WalletEarningItem>>) {
                    val data = response.body()?.data
                    if (response.isSuccessful && data != null) onSuccess(data)
                    else onError(ApiHelper.parseErrorMessage(getApplication(), response.code(), response.errorBody()?.string()))
                }
                override fun onFailure(call: Call<General<WalletEarningItem>>, t: Throwable) {
                    onError(t.localizedMessage ?: getString(R.string.errGeneric))
                }
            })
    }

    // Main wallet balance — the amount available to lock
    fun fetchWalletBalance(
        onSuccess: (WalletBalanceItem) -> Unit,
        onError: (String) -> Unit
    ) {
        if (!Utility.isInternetAvailable(getApplication())) { onError(getString(R.string.msgNoInternet)); return }
        ApiClient.apiService.getWalletBalance(bearerToken())
            .enqueue(object : Callback<General<WalletBalanceItem>> {
                override fun onResponse(call: Call<General<WalletBalanceItem>>, response: Response<General<WalletBalanceItem>>) {
                    val data = response.body()?.data
                    if (response.isSuccessful && data != null) onSuccess(data)
                    else onError(ApiHelper.parseErrorMessage(getApplication(), response.code(), response.errorBody()?.string()))
                }
                override fun onFailure(call: Call<General<WalletBalanceItem>>, t: Throwable) {
                    onError(t.localizedMessage ?: getString(R.string.errGeneric))
                }
            })
    }

    fun fetchLevel(
        onSuccess: (RewardsLevelItem) -> Unit,
        onError: (String) -> Unit
    ) {
        if (!Utility.isInternetAvailable(getApplication())) { onError(getString(R.string.msgNoInternet)); return }
        ApiClient.apiService.getCustomerLevel(bearerToken())
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
        ApiClient.apiService.postWalletEarningOptIn(bearerToken()).enqueue(actionCallback(onSuccess, onError))
    }

    // onPending: timeout / 5xx — outcome unknown, so a retry of the same amount reuses the request_id
    fun postLock(
        amount: Double,
        mpin: String,
        onLoading: () -> Unit,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit,
        onPending: (String) -> Unit
    ) {
        if (!Utility.isInternetAvailable(getApplication())) { onError(getString(R.string.msgNoInternet)); return }
        onLoading()
        val requestId = moneyRequestId.forAttempt("lock:$amount")
        ApiClient.apiService.postWalletEarningLock(bearerToken(), WalletEarningAmountRequest(amount, mpin, requestId))
            .enqueue(actionCallback(onSuccess, onError, onPending))
    }

    fun postWithdraw(
        amount: Double,
        mpin: String,
        onLoading: () -> Unit,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit,
        onPending: (String) -> Unit
    ) {
        if (!Utility.isInternetAvailable(getApplication())) { onError(getString(R.string.msgNoInternet)); return }
        onLoading()
        val requestId = moneyRequestId.forAttempt("withdraw:$amount")
        ApiClient.apiService.postWalletEarningWithdraw(bearerToken(), WalletEarningAmountRequest(amount, mpin, requestId))
            .enqueue(actionCallback(onSuccess, onError, onPending))
    }

    // opt-in / lock / withdraw all answer { success, message } — server message is shown either way
    private fun actionCallback(
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit,
        onPending: ((String) -> Unit)? = null
    ) = object : Callback<MessageItem> {
        override fun onResponse(call: Call<MessageItem>, response: Response<MessageItem>) {
            val body = response.body()
            if (onPending != null && response.code() >= 500) {
                onPending(getString(R.string.msgMoneyRequestPending))
                return
            }
            moneyRequestId.clear()
            if (response.isSuccessful && body?.success == true) {
                onSuccess(body.message ?: "")
            } else {
                onError(body?.message ?: ApiHelper.parseErrorMessage(getApplication(), response.code(), response.errorBody()?.string()))
            }
        }
        override fun onFailure(call: Call<MessageItem>, t: Throwable) {
            t.printStackTrace()
            if (onPending != null) onPending(getString(R.string.msgMoneyRequestPending))
            else onError(t.localizedMessage ?: getString(R.string.errGeneric))
        }
    }
}
