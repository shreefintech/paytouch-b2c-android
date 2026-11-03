package com.shreefintech.paytouchconsumer.retrofit.model.wallet

import com.google.gson.annotations.SerializedName

// Response of GET wallet/balance — every wallet bucket in one call
data class WalletBalanceItem(
    @field:SerializedName("has_wallet")                     val hasWallet: Boolean?,
    @field:SerializedName("main_wallet")                    val mainWallet: String?,
    @field:SerializedName("qr_load_wallet")                 val qrLoadWallet: String?,
    @field:SerializedName("upi_intent_wallet")              val upiIntentWallet: String?,
    @field:SerializedName("lock_wallet")                    val lockWallet: String?,
    @field:SerializedName("upi_settlement_wallet")          val upiSettlementWallet: String?,
    @field:SerializedName("aeps_wallet")                    val aepsWallet: String?,
    @field:SerializedName("referral_wallet")                val referralWallet: String?,
    @field:SerializedName("bonus_wallet")                   val bonusWallet: String?,
    @field:SerializedName("earning_wallet_principal")       val earningWalletPrincipal: String?,
    @field:SerializedName("earning_wallet_interest_accrued") val earningWalletInterestAccrued: String?,
    @field:SerializedName("updated_at")                     val updatedAt: String?,
    @field:SerializedName("live_balance")                   val liveBalance: String?
)
