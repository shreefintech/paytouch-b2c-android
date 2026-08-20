package com.shreefintech.paytouchconsumer.loadwallet

import android.app.Activity
import android.content.Context
import com.shreefintech.paytouchconsumer.Constant
import com.shreefintech.paytouchconsumer.utill.SharedPreferenceHelper

object HdfcPaymentHelper {

    private val failedStatuses = setOf(
        Constant.HDFC_STATUS_JUSPAY_DECLINED,
        Constant.HDFC_STATUS_AUTHENTICATION_FAILED,
        Constant.HDFC_STATUS_AUTHORIZATION_FAILED,
        Constant.HDFC_STATUS_AUTO_REFUNDED
    )

    fun isFailedStatus(status: String) = status.uppercase() in failedStatuses

    fun pendingOrderId(context: Context): String? =
        SharedPreferenceHelper.getSharedPreferenceString(context, Constant.KEY_HDFC_PENDING_ORDER_ID, "")
            .takeIf { it.isNotEmpty() }

    fun pendingAmount(context: Context): String? =
        SharedPreferenceHelper.getSharedPreferenceString(context, Constant.KEY_HDFC_PENDING_AMOUNT, "")
            .takeIf { it.isNotEmpty() }

    fun launchPayment(
        context: Activity,
        orderId: String,
        amount: String,
        payUrl: String,
        returnUrl: String
    ) {
        SharedPreferenceHelper.setSharedPreferenceString(context, Constant.KEY_HDFC_PENDING_ORDER_ID, orderId)
        SharedPreferenceHelper.setSharedPreferenceString(context, Constant.KEY_HDFC_PENDING_AMOUNT, amount)
        context.startActivity(HdfcWebViewActivity.newIntent(context, payUrl, returnUrl))
    }

    fun clearPendingState(context: Context) {
        SharedPreferenceHelper.setSharedPreferenceString(context, Constant.KEY_HDFC_PENDING_ORDER_ID, "")
        SharedPreferenceHelper.setSharedPreferenceString(context, Constant.KEY_HDFC_PENDING_AMOUNT, "")
    }
}
