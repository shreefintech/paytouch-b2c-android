package com.shreefintech.paytouchconsumer.loadwallet.model

// History row → TransactionHistoryDetailActivity. `source` picks the detail call (keyed by clientReference);
// ROW has no call and shows the row's own fields (Transaction ID, Description, Balance After, Date)
data class WalletHistoryDetailPassItem(
    val source: Source,
    val clientReference: String?,
    val txnReference: String?,
    val description: String?,
    val amount: String?,
    val balanceAfter: String?,
    val createdAt: String?
) {
    enum class Source { BILLPAY, TOPUP, WITHDRAWAL, ROW }
}
