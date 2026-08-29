package com.shreefintech.paytouchconsumer.kyc.bank.model

import com.shreefintech.paytouchconsumer.enums.StatementPeriod

data class BankAccountInputItem(
    val accountNumber: String,
    val bankName: String,
    val ifscCode: String,
    val branchName: String,
    val proofType: String,
    val proofBytes: ByteArray,
    val statementPeriod: StatementPeriod?
)
