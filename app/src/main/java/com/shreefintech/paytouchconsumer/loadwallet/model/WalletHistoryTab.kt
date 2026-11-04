package com.shreefintech.paytouchconsumer.loadwallet.model

import androidx.annotation.StringRes
import com.shreefintech.paytouchconsumer.R

// My Wallet history: Wallet tab (GET wallet/statement, `statementWallet` filter) and Rewards & Withdrawals tab
// (wallet/withdrawals, rewards/bonus, rewards/referral; Cashback has no endpoint yet)
enum class WalletHistoryTab(
    val group: Group,
    @StringRes val labelRes: Int,
    val statementWallet: String? = null
) {
    MAIN(Group.WALLET, R.string.labelTabMain, "main"),
    BONUS(Group.WALLET, R.string.labelTabBonus, "bonus"),
    REFERRAL(Group.WALLET, R.string.labelTabReferral, "referral"),
    EARNING(Group.WALLET, R.string.labelTabEarning, "earning"),
    WITHDRAWALS(Group.REWARDS, R.string.labelTabWithdrawals),
    REWARD_BONUS(Group.REWARDS, R.string.labelTabBonus),
    // TODO(B2C-298): no cashback history endpoint yet — always "No transactions found"
    CASHBACK(Group.REWARDS, R.string.labelCashback),
    REWARD_REFERRAL(Group.REWARDS, R.string.labelTabReferral);

    enum class Group { WALLET, REWARDS }

    companion object {
        fun of(group: Group) = entries.filter { it.group == group }
    }
}
