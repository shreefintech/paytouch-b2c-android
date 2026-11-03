package com.shreefintech.paytouchconsumer.loadwallet.viewmodel

import android.app.Application
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.AndroidViewModel
import com.shreefintech.paytouchconsumer.Constant
import com.shreefintech.paytouchconsumer.R
import com.shreefintech.paytouchconsumer.loadwallet.model.WalletTransactionItem
import com.shreefintech.paytouchconsumer.retrofit.ApiClient
import com.shreefintech.paytouchconsumer.retrofit.ApiHelper
import com.shreefintech.paytouchconsumer.retrofit.model.General
import com.shreefintech.paytouchconsumer.retrofit.model.WalletDataItem
import com.shreefintech.paytouchconsumer.retrofit.model.rewards.EarningWalletItem
import com.shreefintech.paytouchconsumer.retrofit.model.wallet.BonusWalletItem
import com.shreefintech.paytouchconsumer.retrofit.model.rewards.RewardsLevelItem
import com.shreefintech.paytouchconsumer.retrofit.model.wallet.WalletBalanceItem
import com.shreefintech.paytouchconsumer.retrofit.model.wallet.WalletHistoryPageItem
import com.shreefintech.paytouchconsumer.retrofit.model.wallet.WalletTopupItem
import com.shreefintech.paytouchconsumer.retrofit.model.wallet.WalletTopupRequest
import com.shreefintech.paytouchconsumer.retrofit.model.wallet.WalletWithdrawalItem
import com.shreefintech.paytouchconsumer.retrofit.model.wallet.WalletWithdrawalRequest
import com.shreefintech.paytouchconsumer.utill.Utility
import com.shreefintech.paytouchconsumer.utill.bearerToken
import com.shreefintech.paytouchconsumer.utill.getString
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

// NOTE: extends AndroidViewModel — wallet screen IS the balance source; BaseBillViewModel's balance-check methods do not apply here
class LoadWalletViewModel(application: Application) : AndroidViewModel(application) {

    private val pollHandler = Handler(Looper.getMainLooper())

    companion object {
        // TODO(B2C-298): G6 — backend gave no polling interval/timeout; ~12 s total, then the status screen shows pending
        private const val TOPUP_POLL_INTERVAL_MS = 3000L
        private const val TOPUP_POLL_MAX_ATTEMPTS = 5
    }

    override fun onCleared() {
        pollHandler.removeCallbacksAndMessages(null)
        super.onCleared()
    }

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
                    val data = response.body()?.data
                    if (response.isSuccessful && data != null) {
                        onSuccess(data)
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

    // Balance shown on Load Wallet — the VA card still comes from fetchUserWalletData()
    fun fetchWalletBalance(
        onSuccess: (WalletBalanceItem) -> Unit,
        onError: (String) -> Unit
    ) {
        if (!Utility.isInternetAvailable(getApplication())) {
            onError(getString(R.string.msgNoInternet))
            return
        }
        ApiClient.apiService.getWalletBalance(bearerToken())
            .enqueue(object : Callback<General<WalletBalanceItem>> {
                override fun onResponse(
                    call: Call<General<WalletBalanceItem>>,
                    response: Response<General<WalletBalanceItem>>
                ) {
                    val data = response.body()?.data
                    if (response.isSuccessful && data != null) {
                        onSuccess(data)
                    } else {
                        onError(
                            ApiHelper.parseErrorMessage(
                                getApplication(), response.code(), response.errorBody()?.string()
                            )
                        )
                    }
                }

                override fun onFailure(call: Call<General<WalletBalanceItem>>, t: Throwable) {
                    onError(t.localizedMessage ?: getString(R.string.errGeneric))
                }
            })
    }

    fun createTopup(
        amount: Double,
        onLoading: () -> Unit,
        onSuccess: (WalletTopupItem) -> Unit,
        onError: (String) -> Unit
    ) {
        if (!Utility.isInternetAvailable(getApplication())) {
            onError(getString(R.string.msgNoInternet))
            return
        }
        onLoading()
        ApiClient.apiService.createWalletTopup(
            authorization = bearerToken(),
            request = WalletTopupRequest(amount = amount, flow = Constant.TOPUP_FLOW_PAYMENT_PAGE)
        ).enqueue(object : Callback<General<WalletTopupItem>> {
            override fun onResponse(
                call: Call<General<WalletTopupItem>>,
                response: Response<General<WalletTopupItem>>
            ) {
                val body = response.body()
                val data = body?.data
                if (response.isSuccessful && data != null) {
                    onSuccess(data)
                } else {
                    // TODO(B2C-298): G21/G22 — 403 error_code (LOCATION_REQUIRED, BANK_DETAILS_REQUIRED, ...) only shows the server message
                    onError(
                        body?.message ?: ApiHelper.parseErrorMessage(
                            getApplication(), response.code(), response.errorBody()?.string()
                        )
                    )
                }
            }

            override fun onFailure(call: Call<General<WalletTopupItem>>, t: Throwable) {
                onError(t.localizedMessage ?: getString(R.string.errGeneric))
            }
        })
    }

    // Polls GET wallet/topups/{client_reference} until is_final or the attempt cap — null when no status could be read
    fun pollTopupStatus(
        clientReference: String,
        onResult: (WalletTopupItem?) -> Unit,
        attempt: Int = 1
    ) {
        if (!Utility.isInternetAvailable(getApplication())) { onResult(null); return }
        ApiClient.apiService.getWalletTopup(bearerToken(), clientReference)
            .enqueue(object : Callback<General<WalletTopupItem>> {
                override fun onResponse(
                    call: Call<General<WalletTopupItem>>,
                    response: Response<General<WalletTopupItem>>
                ) {
                    val data = if (response.isSuccessful) response.body()?.data else null
                    if (data != null && data.isFinal != true && attempt < TOPUP_POLL_MAX_ATTEMPTS) {
                        pollHandler.postDelayed(
                            { pollTopupStatus(clientReference, onResult, attempt + 1) },
                            TOPUP_POLL_INTERVAL_MS
                        )
                    } else {
                        onResult(data)
                    }
                }

                override fun onFailure(call: Call<General<WalletTopupItem>>, t: Throwable) {
                    t.printStackTrace()
                    onResult(null)
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
                    val page = response.body()?.data
                    if (response.isSuccessful && page != null) {
                        val rawList = page.data ?: emptyList()
                        val list = ArrayList<WalletTransactionItem>()
                        rawList.forEach { list.add(WalletTransactionItem.from(it)) }
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

    fun fetchBonusWallet(
        onSuccess: (BonusWalletItem) -> Unit,
        onError: (String) -> Unit
    ) {
        if (!Utility.isInternetAvailable(getApplication())) {
            onError(getString(R.string.msgNoInternet))
            return
        }
        ApiClient.apiService.getBonusWallet(bearerToken())
            .enqueue(object : Callback<General<BonusWalletItem?>> {
                override fun onResponse(
                    call: Call<General<BonusWalletItem?>>,
                    response: Response<General<BonusWalletItem?>>
                ) {
                    val data = response.body()?.data
                    if (response.isSuccessful && data != null) {
                        onSuccess(data)
                    } else {
                        onError(
                            ApiHelper.parseErrorMessage(
                                getApplication(), response.code(), response.errorBody()?.string()
                            )
                        )
                    }
                }

                override fun onFailure(call: Call<General<BonusWalletItem?>>, t: Throwable) {
                    onError(t.localizedMessage ?: getString(R.string.errGeneric))
                }
            })
    }

    fun fetchEarningWallet(
        onSuccess: (EarningWalletItem) -> Unit,
        onError: (String) -> Unit
    ) {
        if (!Utility.isInternetAvailable(getApplication())) {
            onError(getString(R.string.msgNoInternet))
            return
        }
        ApiClient.apiService.getEarningWallet(bearerToken())
            .enqueue(object : Callback<General<EarningWalletItem?>> {
                override fun onResponse(
                    call: Call<General<EarningWalletItem?>>,
                    response: Response<General<EarningWalletItem?>>
                ) {
                    val data = response.body()?.data
                    if (response.isSuccessful && data != null) {
                        onSuccess(data)
                    } else {
                        onError(
                            ApiHelper.parseErrorMessage(
                                getApplication(), response.code(), response.errorBody()?.string()
                            )
                        )
                    }
                }

                override fun onFailure(call: Call<General<EarningWalletItem?>>, t: Throwable) {
                    onError(t.localizedMessage ?: getString(R.string.errGeneric))
                }
            })
    }

    fun fetchRewardsLevel(
        onSuccess: (RewardsLevelItem) -> Unit,
        onError: (String) -> Unit
    ) {
        if (!Utility.isInternetAvailable(getApplication())) {
            onError(getString(R.string.msgNoInternet))
            return
        }
        ApiClient.apiService.getRewardsLevel(bearerToken())
            .enqueue(object : Callback<General<RewardsLevelItem?>> {
                override fun onResponse(
                    call: Call<General<RewardsLevelItem?>>,
                    response: Response<General<RewardsLevelItem?>>
                ) {
                    val data = response.body()?.data
                    if (response.isSuccessful && data != null) {
                        onSuccess(data)
                    } else {
                        onError(
                            ApiHelper.parseErrorMessage(
                                getApplication(), response.code(), response.errorBody()?.string()
                            )
                        )
                    }
                }

                override fun onFailure(call: Call<General<RewardsLevelItem?>>, t: Throwable) {
                    onError(t.localizedMessage ?: getString(R.string.errGeneric))
                }
            })
    }

    fun withdraw(
        amount: Double,
        mpin: String,
        onLoading: () -> Unit,
        onSuccess: (WalletWithdrawalItem) -> Unit,
        onError: (String) -> Unit
    ) {
        if (!Utility.isInternetAvailable(getApplication())) {
            onError(getString(R.string.msgNoInternet))
            return
        }
        onLoading()
        ApiClient.apiService.createWalletWithdrawal(
            authorization = bearerToken(),
            request = WalletWithdrawalRequest(amount = amount, mpin = mpin)
        ).enqueue(object : Callback<General<WalletWithdrawalItem>> {
            override fun onResponse(
                call: Call<General<WalletWithdrawalItem>>,
                response: Response<General<WalletWithdrawalItem>>
            ) {
                val body = response.body()
                val data = body?.data
                if (response.isSuccessful && data != null) {
                    onSuccess(data)
                } else {
                    // TODO(B2C-298): G16 — no error samples (wrong MPIN, insufficient balance, limits); server message shown as-is
                    onError(
                        body?.message ?: ApiHelper.parseErrorMessage(
                            getApplication(), response.code(), response.errorBody()?.string()
                        )
                    )
                }
            }

            override fun onFailure(call: Call<General<WalletWithdrawalItem>>, t: Throwable) {
                onError(t.localizedMessage ?: getString(R.string.errGeneric))
            }
        })
    }

}
