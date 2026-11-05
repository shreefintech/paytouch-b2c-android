package com.shreefintech.paytouchconsumer.loadwallet.model

import androidx.annotation.StringRes
import com.shreefintech.paytouchconsumer.Constant
import com.shreefintech.paytouchconsumer.R

// My Wallet history: Wallet tab (GET wallet/statement, `statementWallet` filter) and Rewards & Withdrawals tab
// (wallet/withdrawals, rewards/bonus, rewards/referral; Cashback has no endpoint yet)
enum class WalletHistoryTab(
    val group: Group,
    @StringRes val labelRes: Int,
    val statementWallet: String? = null
) {
    MAIN(Group.WALLET, R.string.labelTabMain, Constant.WALLET_STATEMENT_MAIN),
    BONUS(Group.WALLET, R.string.labelTabBonus, Constant.WALLET_STATEMENT_BONUS),
    REFERRAL(Group.WALLET, R.string.labelTabReferral, Constant.WALLET_STATEMENT_REFERRAL),
    EARNING(Group.WALLET, R.string.labelTabEarning, Constant.WALLET_STATEMENT_EARNING),
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
