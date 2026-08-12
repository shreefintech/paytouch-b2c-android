package com.shreefintech.paytouchconsumer.loadwallet

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.shreefintech.paytouchconsumer.Constant
import com.shreefintech.paytouchconsumer.R
import com.shreefintech.paytouchconsumer.loadwallet.model.PaymentStatusItem
import com.shreefintech.paytouchconsumer.retrofit.ApiClient
import com.shreefintech.paytouchconsumer.retrofit.ApiHelper
import com.shreefintech.paytouchconsumer.retrofit.model.hdfc.HdfcOrderResponseItem
import com.shreefintech.paytouchconsumer.utill.SharedPreferenceHelper
import com.shreefintech.paytouchconsumer.utill.Utility
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.lang.ref.WeakReference

object HdfcPaymentHelper {

    private var pendingOrderId: String? = null
    private var pendingAmount: String? = null
    private var pendingActivityRef: WeakReference<Activity>? = null
    private var resumeWatcher: Application.ActivityLifecycleCallbacks? = null

    val failedStatuses = setOf(
        "JUSPAY_DECLINED", "AUTHENTICATION_FAILED", "AUTHORIZATION_FAILED", "AUTO_REFUNDED"
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
        registerResumeWatcher(context)
    }

    private fun registerResumeWatcher(context: Activity) {
        pendingActivityRef = WeakReference(context)
        var webViewOpened = false

        resumeWatcher = object : Application.ActivityLifecycleCallbacks {
            override fun onActivityCreated(a: Activity, b: Bundle?) {}
            override fun onActivityStarted(a: Activity) {}
            override fun onActivityStopped(a: Activity) {}
            override fun onActivitySaveInstanceState(a: Activity, b: Bundle) {}

            override fun onActivityPaused(a: Activity) {
                if (a == pendingActivityRef?.get()) webViewOpened = true
            }

            override fun onActivityResumed(a: Activity) {
                if (webViewOpened && a == pendingActivityRef?.get()) {
                    a.application.unregisterActivityLifecycleCallbacks(this)
                    resumeWatcher = null
                    checkOrderStatus(a)
                }
            }

            override fun onActivityDestroyed(a: Activity) {
                if (a == pendingActivityRef?.get()) {
                    a.application.unregisterActivityLifecycleCallbacks(this)
                    resumeWatcher = null
                    clearPendingState()
                }
            }
        }

        context.application.registerActivityLifecycleCallbacks(resumeWatcher!!)
    }

    // ponytail: network call in Application.ActivityLifecycleCallbacks — ViewModel is scoped
    // to the same Activity and cannot outlive it to observe the watcher callback.
    private fun checkOrderStatus(context: Activity) {
        val orderId = pendingOrderId ?: run { clearPendingState(); return }

        if (!Utility.isInternetAvailable(context)) {
            openStatusActivity(context, orderId, pendingAmount ?: "", "NEW")
            clearPendingState()
            return
        }

        ApiClient.apiService.getHdfcOrderStatus(bearerToken(context), orderId)
            .enqueue(object : Callback<HdfcOrderResponseItem> {
                override fun onResponse(
                    call: Call<HdfcOrderResponseItem>,
                    response: Response<HdfcOrderResponseItem>
                ) {
                    if (context.isFinishing || context.isDestroyed) { clearPendingState(); return }
                    val data = if (response.isSuccessful && response.body()?.success == true) response.body()?.data else null
                    openStatusActivity(
                        context,
                        orderId = data?.orderId ?: orderId,
                        amount  = data?.amount ?: pendingAmount ?: "",
                        status  = data?.status ?: "NEW"
                    )
                    clearPendingState()
                }

                override fun onFailure(call: Call<HdfcOrderResponseItem>, t: Throwable) {
                    if (context.isFinishing || context.isDestroyed) { clearPendingState(); return }
                    openStatusActivity(context, orderId, pendingAmount ?: "", "NEW")
                    clearPendingState()
                }
            })
    }

    private fun openStatusActivity(
        context: Activity,
        orderId: String,
        amount: String,
        status: String
    ) {
        PaymentStatusActivity.start(
            context,
            PaymentStatusItem(orderId = orderId, amount = amount, status = status)
        )
    }

    private fun bearerToken(context: Activity): String {
        val token = SharedPreferenceHelper.getSharedPreferenceString(
            context, Constant.KEY_TOKEN, ""
        ) ?: ""
        return "Bearer $token"
    }

    private fun clearPendingState() {
        pendingOrderId    = null
        pendingAmount     = null
        pendingActivityRef = null
    }
}
