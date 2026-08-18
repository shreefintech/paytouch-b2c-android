package com.shreefintech.paytouchconsumer.loadwallet

import android.app.Activity
import com.shreefintech.paytouchconsumer.Constant

object HdfcPaymentHelper {

    var pendingOrderId: String? = null
        private set
    var pendingAmount: String? = null
        private set

    private val failedStatuses = setOf(
        Constant.HDFC_STATUS_JUSPAY_DECLINED,
        Constant.HDFC_STATUS_AUTHENTICATION_FAILED,
        Constant.HDFC_STATUS_AUTHORIZATION_FAILED,
        Constant.HDFC_STATUS_AUTO_REFUNDED
    )

    fun isFailedStatus(status: String) = status.uppercase() in failedStatuses

    fun launchPayment(
        context: Activity,
        orderId: String,
        amount: String,
        payUrl: String,
        returnUrl: String
    ) {
        pendingOrderId = orderId
        pendingAmount  = amount
        context.startActivity(HdfcWebViewActivity.newIntent(context, payUrl, returnUrl))
    }

    fun clearPendingState() {
        pendingOrderId = null
        pendingAmount  = null
    }
}
