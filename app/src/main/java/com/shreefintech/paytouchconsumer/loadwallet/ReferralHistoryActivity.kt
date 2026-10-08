package com.shreefintech.paytouchconsumer.loadwallet

import android.content.Context
import android.os.Bundle
import com.shreefintech.paytouchconsumer.BaseActivity

// Flow replaced by WalletTransactionsActivity with TAB_REFERRAL.
// Kept as a redirect shim so any existing call-site still compiles.
class ReferralHistoryActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WalletTransactionsActivity.start(this, WalletTransactionsActivity.TAB_REFERRAL)
        finish()
    }

    companion object {
        fun start(context: Context) {
            WalletTransactionsActivity.start(context, WalletTransactionsActivity.TAB_REFERRAL)
        }
    }
}
