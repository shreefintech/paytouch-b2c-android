package com.shreefintech.paytouchconsumer.prepaid.viewmodel

import android.app.Application
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import com.shreefintech.paytouchconsumer.Constant
import com.shreefintech.paytouchconsumer.R
import com.shreefintech.paytouchconsumer.retrofit.ApiClient
import com.shreefintech.paytouchconsumer.retrofit.ApiHelper
import com.shreefintech.paytouchconsumer.retrofit.model.General
import com.shreefintech.paytouchconsumer.retrofit.model.electricity.UnifiedTransactionItem
import com.shreefintech.paytouchconsumer.retrofit.model.prepaid.PrepaidOperatorItem
import com.shreefintech.paytouchconsumer.transactions.model.RecentTransactionItem
import com.shreefintech.paytouchconsumer.utill.SharedPreferenceHelper
import com.shreefintech.paytouchconsumer.utill.Utility
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.NumberFormat
import java.util.Locale

class PrepaidRecentTransactionViewModel(application: Application) : AndroidViewModel(application) {

    private val operatorMap = HashMap<String, String>()

    var currentPage = 1
        private set
    var hasMore = false
        private set
    var loading = false
        private set

    companion object {
        private const val PAGE_SIZE = 20
    }

    fun loadOperatorsThenData(
        onLoading: () -> Unit,
        onSuccess: (List<RecentTransactionItem>) -> Unit,
        onError: (String) -> Unit
    ) {
        if (!Utility.isInternetAvailable(getApplication())) {
            onError(getString(R.string.msgNoInternet))
            return
        }
        loading = true
        currentPage = 1
        hasMore = false
        operatorMap.clear()
        onLoading()

        ApiClient.apiService.getPrepaidOperators(bearerToken())
            .enqueue(object : Callback<General<List<PrepaidOperatorItem>>> {
                override fun onResponse(
                    call: Call<General<List<PrepaidOperatorItem>>>,
                    response: Response<General<List<PrepaidOperatorItem>>>
                ) {
                    response.body()?.data?.forEach { op ->
                        val name = op.name ?: return@forEach
                        op.code?.let { operatorMap[it] = name }
                        op.id?.let { operatorMap[it] = name }
                    }
                    fetchTransactions(onSuccess, onError)
                }

                override fun onFailure(
                    call: Call<General<List<PrepaidOperatorItem>>>,
                    t: Throwable
                ) {
                    fetchTransactions(onSuccess, onError)
                }
            })
    }

    fun loadNextPage(
        onLoading: () -> Unit,
        onSuccess: (List<RecentTransactionItem>) -> Unit,
        onError: (String) -> Unit
    ) {
        if (!Utility.isInternetAvailable(getApplication())) {
            onError(getString(R.string.msgNoInternet))
            return
        }
        if (!hasMore || loading) return
        loading = true
        currentPage++
        onLoading()
        fetchTransactions(onSuccess, onError)
    }

    private fun fetchTransactions(
        onSuccess: (List<RecentTransactionItem>) -> Unit,
        onError: (String) -> Unit
    ) {
        ApiClient.apiService.getTransactions(bearerToken(), "mobile_recharge", currentPage, PAGE_SIZE)
            .enqueue(object : Callback<General<List<UnifiedTransactionItem>>> {
                override fun onResponse(
                    call: Call<General<List<UnifiedTransactionItem>>>,
                    response: Response<General<List<UnifiedTransactionItem>>>
                ) {
                    loading = false
                    if (response.isSuccessful && response.body()?.data != null) {
                        val data = response.body()!!.data!!
                        hasMore = data.size == PAGE_SIZE
                        onSuccess(data.map { mapToDisplayItem(it) })
                    } else {
                        hasMore = false
                        onError(
                            ApiHelper.parseErrorMessage(
                                getApplication(), response.code(), response.errorBody()?.string()
                            )
                        )
                    }
                }

                override fun onFailure(
                    call: Call<General<List<UnifiedTransactionItem>>>,
                    t: Throwable
                ) {
                    loading = false
                    hasMore = false
                    onError(t.localizedMessage ?: getString(R.string.errGeneric))
                }
            })
    }

    private fun mapToDisplayItem(item: UnifiedTransactionItem): RecentTransactionItem {
        val operatorCode = item.extra?.operator
        val service = when {
            operatorCode != null && operatorMap.containsKey(operatorCode) -> operatorMap[operatorCode]!!
            operatorCode != null -> operatorCode
            else -> getString(R.string.categoryPrepaid)
        }
        return RecentTransactionItem(
            categoryName      = service,
            accountHolderName = item.extra?.customerName ?: "-",
            date              = if (item.createdAt.isNullOrBlank()) "-" else Utility.formatDate(item.createdAt),
            status            = item.status ?: "-",
            amount            = formatAmount(item.amount ?: "--"),
            accountNumber     = item.identifier ?: "-",
            reference         = item.referenceId ?: "-",
            categoryIconRes   = R.drawable.ic_prepaid
        )
    }

    private fun formatAmount(raw: String?): String {
        if (raw.isNullOrBlank()) return "-"
        return try {
            val number = raw.toDouble()
            val fmt = NumberFormat.getNumberInstance(Locale("en", "IN")).apply {
                maximumFractionDigits = 2
                minimumFractionDigits = 2
            }
            "₹${fmt.format(number)}"
        } catch (e: Exception) {
            "₹$raw"
        }
    }

    private fun bearerToken(): String {
        val token = SharedPreferenceHelper.getSharedPreferenceString(
            getApplication(), Constant.KEY_TOKEN, ""
        ) ?: ""
        return "Bearer $token"
    }

    private fun getString(@StringRes resId: Int): String =
        getApplication<Application>().getString(resId)
}
