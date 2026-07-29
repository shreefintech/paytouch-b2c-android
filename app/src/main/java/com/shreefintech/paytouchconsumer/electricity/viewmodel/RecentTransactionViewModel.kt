package com.shreefintech.paytouchconsumer.electricity.viewmodel

import android.app.Application
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import com.shreefintech.paytouchconsumer.Constant
import com.shreefintech.paytouchconsumer.R
import com.shreefintech.paytouchconsumer.electricity.model.RecentTransactionItem
import com.shreefintech.paytouchconsumer.retrofit.ApiClient
import com.shreefintech.paytouchconsumer.retrofit.ApiHelper
import com.shreefintech.paytouchconsumer.retrofit.model.General
import com.shreefintech.paytouchconsumer.retrofit.model.electricity.ElectricityOperatorItem
import com.shreefintech.paytouchconsumer.retrofit.model.electricity.UnifiedTransactionItem
import com.shreefintech.paytouchconsumer.utill.SharedPreferenceHelper
import com.shreefintech.paytouchconsumer.utill.Utility
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

class RecentTransactionViewModel(application: Application) : AndroidViewModel(application) {

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

    // ── Public API ────────────────────────────────────────────────────────────

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

        ApiClient.apiService.getElectricityOperators(bearerToken())
            .enqueue(object : Callback<General<List<ElectricityOperatorItem>>> {
                override fun onResponse(
                    call: Call<General<List<ElectricityOperatorItem>>>,
                    response: Response<General<List<ElectricityOperatorItem>>>
                ) {
                    response.body()?.data?.forEach { op ->
                        val id   = op.id   ?: return@forEach
                        val name = op.name ?: return@forEach
                        operatorMap[id] = name
                    }
                    fetchTransactions(onSuccess, onError)
                }

                override fun onFailure(
                    call: Call<General<List<ElectricityOperatorItem>>>,
                    t: Throwable
                ) {
                    // Operator fetch failed — proceed with empty map; transactions still load
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

    // ── Private helpers ───────────────────────────────────────────────────────

    private fun fetchTransactions(
        onSuccess: (List<RecentTransactionItem>) -> Unit,
        onError: (String) -> Unit
    ) {
        ApiClient.apiService.getTransactions(bearerToken(), "electricity", currentPage, PAGE_SIZE)
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
                    onError(t.localizedMessage ?: getString(R.string.err_generic))
                }
            })
    }

    private fun mapToDisplayItem(item: UnifiedTransactionItem): RecentTransactionItem {
        val operatorId = item.extra?.operatorId
        val service = when {
            operatorId != null && operatorMap.containsKey(operatorId) -> operatorMap[operatorId]!!
            operatorId != null -> operatorId
            else -> getString(R.string.categoryElectricity)
        }
        return RecentTransactionItem(
            categoryName      = service,
            accountHolderName = item.extra?.customerName ?: "-",
            date              = formatDate(item.createdAt),
            status            = item.status ?: "-",
            amount            = formatAmount(item.amount ?: "--"),
            accountNumber     = item.identifier ?: "-",
            reference         = item.referenceId ?: "-",
            categoryIconRes   = R.drawable.ic_electricity
        )
    }

    private fun formatDate(raw: String?): String {
        if (raw.isNullOrBlank()) return "-"
        return try {
            // Truncate microseconds: "2026-07-25T14:32:10.000000Z" → "2026-07-25T14:32:10"
            val truncated = raw.substringBefore(".")
            val inputFmt  = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
            val outputFmt = SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.US)
            val date = inputFmt.parse(truncated)
            if (date != null) outputFmt.format(date) else raw
        } catch (e: Exception) {
            raw
        }
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
