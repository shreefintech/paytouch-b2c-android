package com.shreefintech.paytouchconsumer.loadwallet

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import com.shreefintech.paytouchconsumer.Constant
import com.shreefintech.paytouchconsumer.utill.SharedPreferenceHelper

object HdfcPaymentHelper {

    private val failedStatuses = setOf(
        Constant.HDFC_STATUS_JUSPAY_DECLINED,
        Constant.HDFC_STATUS_AUTHENTICATION_FAILED,
        Constant.HDFC_STATUS_AUTHORIZATION_FAILED,
        Constant.HDFC_STATUS_AUTO_REFUNDED,
        Constant.TOPUP_STATUS_FAILED
    )

    fun isFailedStatus(status: String) = status.uppercase() in failedStatuses

    fun launchPayment(
        context: Activity,
        orderId: String,
        amount: String,
        payUrl: String,
        returnUrl: String
    ) {
        SharedPreferenceHelper.setSharedPreferenceString(context, Constant.KEY_PENDING_ORDER_ID, orderId)
        SharedPreferenceHelper.setSharedPreferenceString(context, Constant.KEY_PENDING_AMOUNT, amount)
        context.startActivity(HdfcWebViewActivity.newIntent(context, payUrl, returnUrl))
    }

    // Hands upi_intent_url (upi://pay?...) to the installed UPI app; the caller's onResume polls the status.
    // false = no app can open it — pending state is cleared so onResume doesn't poll an unpaid top-up.
    fun launchUpiIntent(context: Activity, orderId: String, amount: String, upiUrl: String): Boolean {
        SharedPreferenceHelper.setSharedPreferenceString(context, Constant.KEY_PENDING_ORDER_ID, orderId)
        SharedPreferenceHelper.setSharedPreferenceString(context, Constant.KEY_PENDING_AMOUNT, amount)
        return try {
            context.startActivity(Intent(Intent.ACTION_VIEW, upiUrl.toUri()))
            true
        } catch (e: ActivityNotFoundException) {
            e.printStackTrace()
            clearPendingState(context)
            false
        }
    }

    fun clearPendingState(context: Context) {
        SharedPreferenceHelper.setSharedPreferenceString(context, Constant.KEY_PENDING_ORDER_ID, "")
        SharedPreferenceHelper.setSharedPreferenceString(context, Constant.KEY_PENDING_AMOUNT, "")
    }
}
