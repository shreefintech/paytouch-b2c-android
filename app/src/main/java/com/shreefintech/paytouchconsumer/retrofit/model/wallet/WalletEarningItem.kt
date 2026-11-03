package com.shreefintech.paytouchconsumer.retrofit.model.wallet

import com.google.gson.annotations.SerializedName

// Response of GET wallet/earning
// TODO(B2C-298): no response sample shared — field names come from the B2C-253 field list, types are assumed; verify against a real response
data class WalletEarningItem(
    @field:SerializedName("opted_in")               val optedIn: Boolean?,
    @field:SerializedName("principal")              val principal: String?,
    @field:SerializedName("interest_accrued")       val interestAccrued: String?,
    @field:SerializedName("lifetime_interest_paid") val lifetimeInterestPaid: String?,
    @field:SerializedName("aging_started_at")       val agingStartedAt: String?,
    @field:SerializedName("accrual_starts_at")      val accrualStartsAt: String?,
    // TODO(B2C-298): shape unknown (amount or object) — kept untyped so a mismatch can't fail the whole parse
    @field:SerializedName("pending_withdrawal")     val pendingWithdrawal: Any?,
    @field:SerializedName("min_lock")               val minLock: String?,
    @field:SerializedName("min_withdraw")           val minWithdraw: String?,
    @field:SerializedName("pan_added")              val panAdded: Boolean?,
    @field:SerializedName("interest_eligible")      val interestEligible: Boolean?
) {
    // Reads pending_withdrawal as a plain amount or as an object { amount, available_at }
    val pendingWithdrawalAmount: Double
        get() = when (val p = pendingWithdrawal) {
            is Number    -> p.toDouble()
            is String    -> p.toDoubleOrNull() ?: 0.0
            is Map<*, *> -> p["amount"]?.toString()?.toDoubleOrNull() ?: 0.0
            else         -> 0.0
        }

    val pendingWithdrawalAvailableAt: String?
        get() = (pendingWithdrawal as? Map<*, *>)?.get("available_at") as? String
}
