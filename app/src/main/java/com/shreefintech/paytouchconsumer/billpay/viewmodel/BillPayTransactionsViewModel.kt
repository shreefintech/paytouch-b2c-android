package com.shreefintech.paytouchconsumer.billpay.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.shreefintech.paytouchconsumer.Constant
import com.shreefintech.paytouchconsumer.R
import com.shreefintech.paytouchconsumer.enums.CategoryType
import com.shreefintech.paytouchconsumer.retrofit.ApiClient
import com.shreefintech.paytouchconsumer.retrofit.ApiHelper
import com.shreefintech.paytouchconsumer.retrofit.model.billpay.BillPayTransactionItem
import com.shreefintech.paytouchconsumer.retrofit.model.billpay.BillPayTransactionPageItem
import com.shreefintech.paytouchconsumer.transactions.model.RecentTransactionItem
import com.shreefintech.paytouchconsumer.transactions.model.TransactionItem
import com.shreefintech.paytouchconsumer.utill.Utility
import com.shreefintech.paytouchconsumer.utill.bearerToken
import com.shreefintech.paytouchconsumer.utill.getString
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

/**
 * GET transactions?category= — shared by Recent Transactions, Transaction Status, Transaction Report and the
 * SMS Receipt "latest payment" lookup. Paged by meta.current_page < meta.last_page.
 */
class BillPayTransactionsViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        const val PER_PAGE = 20
        // Screens that filter rows in the app stop auto-loading empty pages after this many in a row
        const val MAX_AUTO_PAGES = 5
    }

    private var currentPage = 0
    private var lastPage = 1

    // Only this call may update paging or reach the screen — a page-1 reload (tab / filter change) cancels the older one
    private var activeCall: Call<BillPayTransactionPageItem>? = null

    var isLoading = false
        private set

    fun canLoadMore() = !isLoading && currentPage < lastPage

    fun nextPage() = currentPage + 1

    /** [status]: server filter (success / failed), null for all. Page 1 resets paging. */
    fun loadPage(
        category: CategoryType,
        status: String?,
        page: Int,
        onLoading: () -> Unit,
        onSuccess: (List<BillPayTransactionItem>) -> Unit,
        onError: (String) -> Unit,
        perPage: Int = PER_PAGE
    ) {
        if (page == 1) {
            activeCall?.cancel()
            activeCall = null
            currentPage = 0
            lastPage = 1
            isLoading = false
        }
        if (isLoading) return
        if (!Utility.isInternetAvailable(getApplication())) {
            onError(getString(R.string.msgNoInternet))
            return
        }
        isLoading = true
        onLoading()
        val newCall = ApiClient.apiService.getBillPayTransactions(bearerToken(), category.value, page, perPage, status)
        activeCall = newCall
        newCall.enqueue(object : Callback<BillPayTransactionPageItem> {
            override fun onResponse(
                call: Call<BillPayTransactionPageItem>,
                response: Response<BillPayTransactionPageItem>
            ) {
                if (call != activeCall) return
                activeCall = null
                isLoading = false
                val body = response.body()
                if (response.isSuccessful && body?.success == true) {
                    currentPage = body.meta?.currentPage ?: page
                    lastPage = body.meta?.lastPage ?: currentPage
                    onSuccess(body.data.orEmpty())
                } else {
                    onError(
                        ApiHelper.parseErrorMessage(
                            getApplication(), response.code(), response.errorBody()?.string()
                        )
                    )
                }
            }

            override fun onFailure(call: Call<BillPayTransactionPageItem>, t: Throwable) {
                // Cancelled by a newer page-1 load — that call reports instead
                if (call != activeCall) return
                activeCall = null
                isLoading = false
                onError(t.localizedMessage ?: getString(R.string.errGeneric))
            }
        })
    }

    override fun onCleared() {
        activeCall?.cancel()
        activeCall = null
    }

    // ── Mapping (shared by every Bill Pay list) ───────────────────────────────

    fun mapToDisplayItem(item: BillPayTransactionItem, category: CategoryType): RecentTransactionItem =
        RecentTransactionItem(
            categoryName      = item.billerName ?: getString(category.titleRes),
            accountHolderName = "-",
            date              = Utility.formatDate(item.createdAt, "dd MMM yyyy"),
            status            = item.status ?: "-",
            amount            = Utility.formatAmount(item.amount),
            accountNumber     = accountNumber(item),
            reference         = txnReferenceOf(item),
            categoryIconRes   = category.iconRes,
            isMobileCategory  = category.isMobileCategory,
            isVehicleCategory = category.isVehicleCategory,
            identifierLabel   = item.identifierLabel?.takeIf { it.isNotBlank() }
        )

    fun mapToTransactionItem(item: BillPayTransactionItem, category: CategoryType): TransactionItem {
        val fee = (item.charge ?: 0.0) + (item.gstAmount ?: 0.0)
        return TransactionItem(
            mobileNumber      = accountNumber(item),
            transactionId     = txnReferenceOf(item),
            amount            = Utility.formatAmount(item.amount),
            status            = item.status ?: "--",
            categoryIconRes   = category.iconRes,
            username          = item.billerName ?: "--",
            date              = item.createdAt ?: "--",
            platformFee       = Utility.formatAmount(fee),
            totalPayable      = Utility.formatAmount(item.amount?.plus(fee)),
            referenceId       = referenceOf(item),
            // No numeric id in this API — client_reference is the server identity
            userId            = item.clientReference ?: "--",
            accountNumber     = accountNumber(item),
            companyName       = item.billerName ?: "--",
            isMobileCategory  = category.isMobileCategory,
            isVehicleCategory = category.isVehicleCategory,
            identifierLabel   = item.identifierLabel?.takeIf { it.isNotBlank() },
            categoryType      = category
        )
    }

    /**
     * The account the bill was paid for. Imported old payments send {source: "old_b2c", identifier: "…"};
     * new ones send the biller's own key ({mobile_number: "…"}). Order: identifier → the category's default key →
     * any other value except circle_code and source.
     */
    fun accountNumber(item: BillPayTransactionItem): String {
        val params = item.inputParams.orEmpty()
        val category = CategoryType.from(item.category)
        return listOfNotNull(Constant.BILLPAY_PARAM_IDENTIFIER, category.defaultParamName)
            .firstNotNullOfOrNull { key -> params[key]?.takeIf { it.isNotBlank() } }
            ?: params.filterKeys { it != Constant.BILLPAY_PARAM_CIRCLE_CODE && it != Constant.BILLPAY_PARAM_SOURCE }
                .values.firstOrNull { it.isNotBlank() }
            ?: "--"
    }

    // Transaction ID shown on every Bill Pay screen: txn_reference, else client_reference (old/imported payments)
    fun txnReferenceOf(item: BillPayTransactionItem): String =
        item.txnReference?.takeIf { it.isNotBlank() } ?: item.clientReference ?: "--"

    // Reference row: provider_ref, else txn_reference, else client_reference
    fun referenceOf(item: BillPayTransactionItem): String =
        item.providerRef?.takeIf { it.isNotBlank() }
            ?: item.txnReference?.takeIf { it.isNotBlank() }
            ?: item.clientReference ?: "--"
}
