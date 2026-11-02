package com.shreefintech.paytouchconsumer.kyc.bank.model

data class EditBankPassItem(
    val bankId: Int,
    val isPrimary: Boolean,
    val accountHolderName: String?,
    val accountNumber: String?,
    val bankName: String?,
    val ifsc: String?,
    val branchName: String?,
    val bankProofUrl: String?
) {
    // bank_id 0 = withdrawal bank → PUT wallet/bank-account (MPIN); 1–3 → legacy dashboard-kyc/bank-account
    val isWithdrawalBank: Boolean get() = bankId == WITHDRAWAL_BANK_ID

    companion object {
        const val WITHDRAWAL_BANK_ID = 0
    }
}
