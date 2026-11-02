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
)
