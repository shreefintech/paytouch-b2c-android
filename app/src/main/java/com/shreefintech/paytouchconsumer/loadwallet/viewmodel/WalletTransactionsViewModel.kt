package com.shreefintech.paytouchconsumer.loadwallet.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.shreefintech.paytouchconsumer.R
import com.shreefintech.paytouchconsumer.loadwallet.model.WalletHistoryTab
import com.shreefintech.paytouchconsumer.loadwallet.model.WalletTransactionItem
import com.shreefintech.paytouchconsumer.retrofit.ApiClient
import com.shreefintech.paytouchconsumer.retrofit.ApiHelper
import com.shreefintech.paytouchconsumer.retrofit.model.General
import com.shreefintech.paytouchconsumer.retrofit.model.wallet.BonusRewardItem
import com.shreefintech.paytouchconsumer.retrofit.model.wallet.WalletStatementItem
import com.shreefintech.paytouchconsumer.retrofit.model.wallet.WalletWithdrawalPageItem
import com.shreefintech.paytouchconsumer.utill.Utility
import com.shreefintech.paytouchconsumer.utill.bearerToken
import com.shreefintech.paytouchconsumer.utill.getString
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

// One loader for every My Wallet history sub tab — My Wallet preview sends perPage 2, View All 20
class WalletTransactionsViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        const val PER_PAGE = 20
    }

    // wallet/statement pages by cursor, wallet/withdrawals by page number; rewards are first page only
    private var currentPage = 0
    private var nextCursor: String? = null
    private var isLastPage = false
    private var isLoading  = false
    // Bumped on every page-1 load so a reply for a tab the user already left is dropped
    private var generation = 0

    fun canLoadMore() = !isLoading && !isLastPage

    fun nextPage() = currentPage + 1

    fun loadHistory(
        tab: WalletHistoryTab,
        page: Int,
        perPage: Int = PER_PAGE,
        onLoading: () -> Unit,
        onSuccess: (ArrayList<WalletTransactionItem>) -> Unit,
        onError: (String) -> Unit
    ) {
        if (page == 1) {
            isLastPage  = false
            currentPage = 0
            nextCursor  = null
            isLoading   = false
            generation++
        }
        if (isLoading) return
        if (!Utility.isInternetAvailable(getApplication())) {
            onError(getString(R.string.msgNoInternet))
            return
        }
        val requestGeneration = generation
        val deliver: (ArrayList<WalletTransactionItem>, Boolean) -> Unit = { list, lastPage ->
            if (requestGeneration == generation) {
                isLoading   = false
                isLastPage  = lastPage
                currentPage = page
                onSuccess(list)
            }
        }
        val fail: (String) -> Unit = { msg ->
            if (requestGeneration == generation) {
                isLoading = false
                onError(msg)
            }
        }

        val statementWallet = tab.statementWallet
        if (tab == WalletHistoryTab.CASHBACK) {
            deliver(ArrayList(), true)
            return
        }
        isLoading = true
        onLoading()
        when {
            statementWallet != null -> loadStatement(statementWallet, perPage, deliver, fail)
            tab == WalletHistoryTab.WITHDRAWALS -> loadWithdrawals(page, perPage, deliver, fail)
            else -> loadRewards(tab == WalletHistoryTab.REWARD_REFERRAL, perPage, deliver, fail)
        }
    }

    private fun loadStatement(
        wallet: String,
        perPage: Int,
        deliver: (ArrayList<WalletTransactionItem>, Boolean) -> Unit,
        fail: (String) -> Unit
    ) {
        ApiClient.apiService.getWalletStatement(bearerToken(), wallet, perPage, nextCursor)
            .enqueue(object : Callback<WalletStatementItem> {
                override fun onResponse(call: Call<WalletStatementItem>, response: Response<WalletStatementItem>) {
                    val body = response.body()
                    if (response.isSuccessful && body?.success == true) {
                        nextCursor = body.meta?.nextCursor
                        val list = ArrayList<WalletTransactionItem>()
                        body.data?.forEach { list.add(WalletTransactionItem.from(it)) }
                        deliver(list, nextCursor.isNullOrEmpty())
                    } else {
                        fail(errorMessage(response.code(), response.errorBody()))
                    }
                }

                override fun onFailure(call: Call<WalletStatementItem>, t: Throwable) {
                    t.printStackTrace()
                    fail(t.localizedMessage ?: getString(R.string.errGeneric))
                }
            })
    }

    private fun loadWithdrawals(
        page: Int,
        perPage: Int,
        deliver: (ArrayList<WalletTransactionItem>, Boolean) -> Unit,
        fail: (String) -> Unit
    ) {
        ApiClient.apiService.getWalletWithdrawals(bearerToken(), page, perPage)
            .enqueue(object : Callback<WalletWithdrawalPageItem> {
                override fun onResponse(
                    call: Call<WalletWithdrawalPageItem>,
                    response: Response<WalletWithdrawalPageItem>
                ) {
                    val body = response.body()
                    if (response.isSuccessful && body?.success == true) {
                        val list = ArrayList<WalletTransactionItem>()
                        body.data?.forEach { row ->
                            val title = getApplication<Application>().getString(
                                R.string.labelWithdrawalTo, row.bank?.bankName ?: "--"
                            )
                            list.add(WalletTransactionItem.from(row, title))
                        }
                        val current = body.meta?.currentPage ?: page
                        val last = body.meta?.lastPage ?: current
                        deliver(list, current >= last)
                    } else {
                        fail(errorMessage(response.code(), response.errorBody()))
                    }
                }

                override fun onFailure(call: Call<WalletWithdrawalPageItem>, t: Throwable) {
                    t.printStackTrace()
                    fail(t.localizedMessage ?: getString(R.string.errGeneric))
                }
            })
    }

    // rewards/bonus and rewards/referral — first page only (doc); lastPage is always true
    private fun loadRewards(
        isReferral: Boolean,
        perPage: Int,
        deliver: (ArrayList<WalletTransactionItem>, Boolean) -> Unit,
        fail: (String) -> Unit
    ) {
        val call = if (isReferral) {
            ApiClient.apiService.getReferralHistory(bearerToken(), page = 1, perPage = perPage)
        } else {
            ApiClient.apiService.getBonusRewards(bearerToken(), page = 1, perPage = perPage)
        }
        call.enqueue(object : Callback<General<List<BonusRewardItem>>> {
            override fun onResponse(
                call: Call<General<List<BonusRewardItem>>>,
                response: Response<General<List<BonusRewardItem>>>
            ) {
                val data = response.body()?.data
                if (response.isSuccessful && data != null) {
                    deliver(ArrayList(data.map { WalletTransactionItem.from(it) }), true)
                } else {
                    fail(errorMessage(response.code(), response.errorBody()))
                }
            }

            override fun onFailure(call: Call<General<List<BonusRewardItem>>>, t: Throwable) {
                t.printStackTrace()
                fail(t.localizedMessage ?: getString(R.string.errGeneric))
            }
        })
    }

    private fun errorMessage(code: Int, errorBody: ResponseBody?): String =
        ApiHelper.parseErrorMessage(getApplication(), code, errorBody?.string())
}
