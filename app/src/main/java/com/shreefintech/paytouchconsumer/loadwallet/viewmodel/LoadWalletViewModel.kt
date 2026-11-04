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
import com.shreefintech.paytouchconsumer.retrofit.model.auth.MeDataItem
import com.shreefintech.paytouchconsumer.retrofit.model.rewards.RewardsLevelItem
import com.shreefintech.paytouchconsumer.retrofit.model.wallet.BonusRewardItem
import com.shreefintech.paytouchconsumer.retrofit.model.wallet.BonusWalletHistoryItem
import com.shreefintech.paytouchconsumer.retrofit.model.wallet.VirtualAccountItem
import com.shreefintech.paytouchconsumer.retrofit.model.wallet.WalletBalanceItem
import com.shreefintech.paytouchconsumer.retrofit.model.wallet.WalletEarningItem
import com.shreefintech.paytouchconsumer.retrofit.model.wallet.WalletTopupFeeItem
import com.shreefintech.paytouchconsumer.retrofit.model.wallet.WalletWithdrawalFeeItem
import com.shreefintech.paytouchconsumer.retrofit.model.wallet.WalletStatementItem
import com.shreefintech.paytouchconsumer.retrofit.model.wallet.WalletTopupItem
import com.shreefintech.paytouchconsumer.retrofit.model.wallet.WalletTopupRequest
import com.shreefintech.paytouchconsumer.retrofit.model.wallet.WalletWithdrawalItem
import com.shreefintech.paytouchconsumer.retrofit.model.wallet.WalletWithdrawalRequest
import com.shreefintech.paytouchconsumer.utill.MoneyRequestId
import com.shreefintech.paytouchconsumer.utill.Utility
import com.shreefintech.paytouchconsumer.utill.bearerToken
import com.shreefintech.paytouchconsumer.utill.getString
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

// NOTE: extends AndroidViewModel — wallet screen IS the balance source
class LoadWalletViewModel(application: Application) : AndroidViewModel(application) {

    private val pollHandler = Handler(Looper.getMainLooper())
    private val withdrawIdempotencyKey = MoneyRequestId()

    companion object {
        // TODO(B2C-298): G6 — backend gave no polling interval/timeout for top-ups or withdrawals; ~12 s total,
        //  then the status shows pending
        private const val STATUS_POLL_INTERVAL_MS = 3000L
        private const val STATUS_POLL_MAX_ATTEMPTS = 5
        private const val REFERRAL_PER_PAGE = 50
    }

    override fun onCleared() {
        pollHandler.removeCallbacksAndMessages(null)
        super.onCleared()
    }

    fun fetchVirtualAccount(
        onLoading: () -> Unit,
        onSuccess: (VirtualAccountItem) -> Unit,
        onError: (String) -> Unit
    ) {
        if (!Utility.isInternetAvailable(getApplication())) {
            onError(getString(R.string.msgNoInternet))
            return
        }
        onLoading()
        ApiClient.apiService.getVirtualAccount(bearerToken())
            .enqueue(object : Callback<General<VirtualAccountItem>> {
                override fun onResponse(
                    call: Call<General<VirtualAccountItem>>,
                    response: Response<General<VirtualAccountItem>>
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

                override fun onFailure(call: Call<General<VirtualAccountItem>>, t: Throwable) {
                    onError(t.localizedMessage ?: getString(R.string.errGeneric))
                }
            })
    }

    // virtual-account has no holder name — read it from auth/me; silent on failure (card keeps "--")
    fun fetchAccountHolderName(onSuccess: (String) -> Unit) {
        if (!Utility.isInternetAvailable(getApplication())) return
        ApiClient.apiService.getMe(bearerToken())
            .enqueue(object : Callback<General<MeDataItem>> {
                override fun onResponse(
                    call: Call<General<MeDataItem>>,
                    response: Response<General<MeDataItem>>
                ) {
                    val name = if (response.isSuccessful) response.body()?.data?.user?.name else null
                    if (!name.isNullOrBlank()) onSuccess(name)
                }

                override fun onFailure(call: Call<General<MeDataItem>>, t: Throwable) {
                    t.printStackTrace()
                }
            })
    }

    // Every wallet bucket (main, bonus, ...) shown on Load Wallet
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
        flow: String,
        remark: String?,
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
            request = WalletTopupRequest(amount = amount, flow = flow, remark = remark)
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
    fun pollTopupStatus(clientReference: String, onResult: (WalletTopupItem?) -> Unit) {
        pollStatus(
            request = { ApiClient.apiService.getWalletTopup(bearerToken(), clientReference) },
            isFinal = { it.isFinal == true },
            onResult = onResult
        )
    }

    // Shared by the top-up and withdrawal status polls — a fresh Call per attempt (a Call can't be re-enqueued)
    private fun <T> pollStatus(
        request: () -> Call<General<T>>,
        isFinal: (T) -> Boolean,
        onResult: (T?) -> Unit,
        attempt: Int = 1
    ) {
        if (!Utility.isInternetAvailable(getApplication())) { onResult(null); return }
        request().enqueue(object : Callback<General<T>> {
            override fun onResponse(call: Call<General<T>>, response: Response<General<T>>) {
                val data = if (response.isSuccessful) response.body()?.data else null
                if (data != null && !isFinal(data) && attempt < STATUS_POLL_MAX_ATTEMPTS) {
                    pollHandler.postDelayed(
                        { pollStatus(request, isFinal, onResult, attempt + 1) },
                        STATUS_POLL_INTERVAL_MS
                    )
                } else {
                    onResult(data)
                }
            }

            override fun onFailure(call: Call<General<T>>, t: Throwable) {
                t.printStackTrace()
                onResult(null)
            }
        })
    }

    // Fee previews are display-only — null on any failure, the server still applies the real fee
    // One call per payment method (UPI / CARD / NB) — the user picks the method on HDFC's page
    fun fetchTopupFee(amount: Double, paymentMethodType: String, onResult: (WalletTopupFeeItem?) -> Unit) {
        if (!Utility.isInternetAvailable(getApplication())) { onResult(null); return }
        ApiClient.apiService.getTopupFeePreview(bearerToken(), amount, paymentMethodType)
            .enqueue(object : Callback<General<WalletTopupFeeItem>> {
                override fun onResponse(
                    call: Call<General<WalletTopupFeeItem>>,
                    response: Response<General<WalletTopupFeeItem>>
                ) {
                    onResult(if (response.isSuccessful) response.body()?.data else null)
                }

                override fun onFailure(call: Call<General<WalletTopupFeeItem>>, t: Throwable) {
                    t.printStackTrace()
                    onResult(null)
                }
            })
    }

    fun fetchWithdrawalFee(amount: Double, onResult: (WalletWithdrawalFeeItem?) -> Unit) {
        if (!Utility.isInternetAvailable(getApplication())) { onResult(null); return }
        ApiClient.apiService.getWithdrawalFeePreview(bearerToken(), amount)
            .enqueue(object : Callback<General<WalletWithdrawalFeeItem>> {
                override fun onResponse(
                    call: Call<General<WalletWithdrawalFeeItem>>,
                    response: Response<General<WalletWithdrawalFeeItem>>
                ) {
                    onResult(if (response.isSuccessful) response.body()?.data else null)
                }

                override fun onFailure(call: Call<General<WalletWithdrawalFeeItem>>, t: Throwable) {
                    t.printStackTrace()
                    onResult(null)
                }
            })
    }

    // Polls GET wallet/withdrawals/{client_reference} until is_final or the attempt cap — null when no status could be read
    fun pollWithdrawalStatus(clientReference: String, onResult: (WalletWithdrawalItem?) -> Unit) {
        pollStatus(
            request = { ApiClient.apiService.getWalletWithdrawal(bearerToken(), clientReference) },
            isFinal = { it.isFinal == true },
            onResult = onResult
        )
    }

    fun fetchRecentHistory(
        onSuccess: (ArrayList<WalletTransactionItem>) -> Unit,
        onError: (String) -> Unit
    ) {
        if (!Utility.isInternetAvailable(getApplication())) {
            onError(getString(R.string.msgNoInternet))
            return
        }
        // Recent list: first 2 rows of the main-wallet statement
        ApiClient.apiService.getWalletStatement(bearerToken(), Constant.WALLET_STATEMENT_MAIN, perPage = 2, cursor = null)
            .enqueue(object : Callback<WalletStatementItem> {
                override fun onResponse(
                    call: Call<WalletStatementItem>,
                    response: Response<WalletStatementItem>
                ) {
                    val body = response.body()
                    if (response.isSuccessful && body?.success == true) {
                        val list = ArrayList<WalletTransactionItem>()
                        body.data?.forEach { list.add(WalletTransactionItem.from(it)) }
                        onSuccess(list)
                    } else {
                        onError(
                            ApiHelper.parseErrorMessage(
                                getApplication(), response.code(), response.errorBody()?.string()
                            )
                        )
                    }
                }

                override fun onFailure(call: Call<WalletStatementItem>, t: Throwable) {
                    onError(t.localizedMessage ?: getString(R.string.errGeneric))
                }
            })
    }

    // GET rewards/referral rows, mapped to the row shape ReferralHistoryAdp renders
    fun fetchReferralHistory(
        onSuccess: (List<BonusWalletHistoryItem>) -> Unit,
        onError: (String) -> Unit
    ) {
        if (!Utility.isInternetAvailable(getApplication())) {
            onError(getString(R.string.msgNoInternet))
            return
        }
        // TODO(B2C-298): first page only — add paging when users hold more than REFERRAL_PER_PAGE referral rows
        ApiClient.apiService.getReferralHistory(bearerToken(), page = 1, perPage = REFERRAL_PER_PAGE)
            .enqueue(object : Callback<General<List<BonusRewardItem>>> {
                override fun onResponse(
                    call: Call<General<List<BonusRewardItem>>>,
                    response: Response<General<List<BonusRewardItem>>>
                ) {
                    val data = response.body()?.data
                    if (response.isSuccessful && data != null) {
                        onSuccess(data.map { it.toHistoryItem() })
                    } else {
                        onError(
                            ApiHelper.parseErrorMessage(
                                getApplication(), response.code(), response.errorBody()?.string()
                            )
                        )
                    }
                }

                override fun onFailure(call: Call<General<List<BonusRewardItem>>>, t: Throwable) {
                    onError(t.localizedMessage ?: getString(R.string.errGeneric))
                }
            })
    }

    fun fetchEarningWallet(
        onSuccess: (WalletEarningItem) -> Unit,
        onError: (String) -> Unit
    ) {
        if (!Utility.isInternetAvailable(getApplication())) {
            onError(getString(R.string.msgNoInternet))
            return
        }
        ApiClient.apiService.getWalletEarning(bearerToken())
            .enqueue(object : Callback<General<WalletEarningItem>> {
                override fun onResponse(
                    call: Call<General<WalletEarningItem>>,
                    response: Response<General<WalletEarningItem>>
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

                override fun onFailure(call: Call<General<WalletEarningItem>>, t: Throwable) {
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
        ApiClient.apiService.getCustomerLevel(bearerToken())
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

    // onPending: timeout / 5xx — outcome unknown, so a retry of the same amount + mode + narration reuses the request_id
    fun withdraw(
        amount: Double,
        mpin: String,
        transferMode: String,
        narration: String?,
        onLoading: () -> Unit,
        onSuccess: (WalletWithdrawalItem) -> Unit,
        onError: (String) -> Unit,
        onPending: (String) -> Unit
    ) {
        if (!Utility.isInternetAvailable(getApplication())) {
            onError(getString(R.string.msgNoInternet))
            return
        }
        onLoading()
        ApiClient.apiService.createWalletWithdrawal(
            authorization = bearerToken(),
            request = WalletWithdrawalRequest(
                amount = amount,
                mpin = mpin,
                requestId = withdrawIdempotencyKey.forAttempt("$amount|$transferMode|$narration"),
                transferMode = transferMode,
                remark = narration
            )
        ).enqueue(object : Callback<General<WalletWithdrawalItem>> {
            override fun onResponse(
                call: Call<General<WalletWithdrawalItem>>,
                response: Response<General<WalletWithdrawalItem>>
            ) {
                if (response.code() >= 500) {
                    onPending(getString(R.string.msgMoneyRequestPending))
                    return
                }
                withdrawIdempotencyKey.clear()
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
                t.printStackTrace()
                onPending(getString(R.string.msgMoneyRequestPending))
            }
        })
    }

}
