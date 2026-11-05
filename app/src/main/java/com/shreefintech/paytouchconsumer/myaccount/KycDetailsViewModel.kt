package com.shreefintech.paytouchconsumer.myaccount

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.shreefintech.paytouchconsumer.R
import com.shreefintech.paytouchconsumer.retrofit.ApiClient
import com.shreefintech.paytouchconsumer.retrofit.ApiHelper
import com.shreefintech.paytouchconsumer.retrofit.model.General
import com.shreefintech.paytouchconsumer.retrofit.model.auth.MeDataItem
import com.shreefintech.paytouchconsumer.retrofit.model.wallet.VirtualAccountItem
import com.shreefintech.paytouchconsumer.retrofit.model.wallet.WalletBankAccountItem
import com.shreefintech.paytouchconsumer.utill.Utility
import com.shreefintech.paytouchconsumer.utill.bearerToken
import com.shreefintech.paytouchconsumer.utill.getString
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class KycDetailsViewModel(application: Application) : AndroidViewModel(application) {

    /**
     * auth/me (identity + KYC state) first; once it succeeds GET wallet/bank-account (withdrawal bank) and
     * GET virtual-account (VA card) run alongside. Only an auth/me failure is an error — a bank failure still
     * shows identity with a null bank and `isBankFailed`; a VA failure only affects the VA card (`vaError`).
     */
    fun fetchDetails(
        onLoading: () -> Unit,
        onReady: (me: MeDataItem, bank: WalletBankAccountItem?, isBankFailed: Boolean) -> Unit,
        onVirtualAccount: (va: VirtualAccountItem?, vaError: String?) -> Unit,
        onError: (String) -> Unit
    ) {
        if (!Utility.isInternetAvailable(getApplication())) {
            onError(getString(R.string.msgNoInternet))
            return
        }
        onLoading()
        ApiClient.apiService.getMe(bearerToken())
            .enqueue(object : Callback<General<MeDataItem>> {
                override fun onResponse(call: Call<General<MeDataItem>>, response: Response<General<MeDataItem>>) {
                    val me = response.body()?.data
                    if (response.isSuccessful && me != null) {
                        fetchBank { bank, isFailed -> onReady(me, bank, isFailed) }
                        fetchVirtualAccount(onVirtualAccount)
                    } else {
                        onError(ApiHelper.parseErrorMessage(getApplication(), response.code(), response.errorBody()?.string()))
                    }
                }

                override fun onFailure(call: Call<General<MeDataItem>>, t: Throwable) {
                    onError(t.localizedMessage ?: getString(R.string.errGeneric))
                }
            })
    }

    private fun fetchBank(onDone: (bank: WalletBankAccountItem?, isFailed: Boolean) -> Unit) {
        ApiClient.apiService.getWalletBankAccount(bearerToken())
            .enqueue(object : Callback<General<WalletBankAccountItem>> {
                override fun onResponse(
                    call: Call<General<WalletBankAccountItem>>,
                    response: Response<General<WalletBankAccountItem>>
                ) {
                    val bank = if (response.isSuccessful) response.body()?.data else null
                    onDone(bank, bank == null)
                }

                override fun onFailure(call: Call<General<WalletBankAccountItem>>, t: Throwable) {
                    t.printStackTrace()
                    onDone(null, true)
                }
            })
    }

    private fun fetchVirtualAccount(onDone: (va: VirtualAccountItem?, error: String?) -> Unit) {
        ApiClient.apiService.getVirtualAccount(bearerToken())
            .enqueue(object : Callback<General<VirtualAccountItem>> {
                override fun onResponse(
                    call: Call<General<VirtualAccountItem>>,
                    response: Response<General<VirtualAccountItem>>
                ) {
                    val va = response.body()?.data
                    if (response.isSuccessful && va != null) {
                        onDone(va, null)
                    } else {
                        onDone(null, ApiHelper.parseErrorMessage(getApplication(), response.code(), response.errorBody()?.string()))
                    }
                }

                override fun onFailure(call: Call<General<VirtualAccountItem>>, t: Throwable) {
                    t.printStackTrace()
                    onDone(null, t.localizedMessage ?: getString(R.string.errGeneric))
                }
            })
    }
}
