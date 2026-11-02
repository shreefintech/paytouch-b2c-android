package com.shreefintech.paytouchconsumer.myaccount

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.shreefintech.paytouchconsumer.R
import com.shreefintech.paytouchconsumer.retrofit.ApiAuthClient
import com.shreefintech.paytouchconsumer.retrofit.ApiHelper
import com.shreefintech.paytouchconsumer.retrofit.model.General
import com.shreefintech.paytouchconsumer.retrofit.model.auth.MeDataItem
import com.shreefintech.paytouchconsumer.retrofit.model.wallet.WalletBankAccountItem
import com.shreefintech.paytouchconsumer.utill.Utility
import com.shreefintech.paytouchconsumer.utill.bearerToken
import com.shreefintech.paytouchconsumer.utill.getString
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class KycDetailsViewModel(application: Application) : AndroidViewModel(application) {

    /**
     * auth/me (identity + KYC state), then GET wallet/bank-account (withdrawal bank).
     * Only an auth/me failure is an error — a bank failure still shows identity with a null bank.
     */
    fun fetchDetails(
        onLoading: () -> Unit,
        onReady: (MeDataItem, WalletBankAccountItem?) -> Unit,
        onError: (String) -> Unit
    ) {
        if (!Utility.isInternetAvailable(getApplication())) {
            onError(getString(R.string.msgNoInternet))
            return
        }
        onLoading()
        ApiAuthClient.apiService.getMe(bearerToken())
            .enqueue(object : Callback<General<MeDataItem>> {
                override fun onResponse(call: Call<General<MeDataItem>>, response: Response<General<MeDataItem>>) {
                    val me = response.body()?.data
                    if (response.isSuccessful && me != null) {
                        fetchBank { bank -> onReady(me, bank) }
                    } else {
                        onError(ApiHelper.parseErrorMessage(getApplication(), response.code(), response.errorBody()?.string()))
                    }
                }

                override fun onFailure(call: Call<General<MeDataItem>>, t: Throwable) {
                    onError(t.localizedMessage ?: getString(R.string.errGeneric))
                }
            })
    }

    private fun fetchBank(onDone: (WalletBankAccountItem?) -> Unit) {
        ApiAuthClient.apiService.getWalletBankAccount(bearerToken())
            .enqueue(object : Callback<General<WalletBankAccountItem>> {
                override fun onResponse(
                    call: Call<General<WalletBankAccountItem>>,
                    response: Response<General<WalletBankAccountItem>>
                ) {
                    onDone(if (response.isSuccessful) response.body()?.data else null)
                }

                override fun onFailure(call: Call<General<WalletBankAccountItem>>, t: Throwable) {
                    t.printStackTrace()
                    onDone(null)
                }
            })
    }
}
