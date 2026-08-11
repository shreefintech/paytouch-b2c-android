package com.shreefintech.paytouchconsumer.loadwallet

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.util.Log
import com.shreefintech.paytouchconsumer.Constant
import com.shreefintech.paytouchconsumer.R
import com.shreefintech.paytouchconsumer.loadwallet.model.PaymentStatusItem
import com.shreefintech.paytouchconsumer.retrofit.ApiClient
import com.shreefintech.paytouchconsumer.retrofit.ApiHelper
import com.shreefintech.paytouchconsumer.retrofit.model.hdfc.HdfcCreateOrderRequest
import com.shreefintech.paytouchconsumer.retrofit.model.hdfc.HdfcOrderResponseItem
import com.shreefintech.paytouchconsumer.utill.SharedPreferenceHelper
import com.shreefintech.paytouchconsumer.utill.ToastUtil
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
    private var pendingStatusLoading: (() -> Unit)? = null
    private var pendingStatusLoadingDone: (() -> Unit)? = null

    private val failedStatuses = setOf(
        "JUSPAY_DECLINED", "AUTHENTICATION_FAILED", "AUTHORIZATION_FAILED", "AUTO_REFUNDED"
    )

    fun payWalletTopup(
        context: Activity,
        amount: Double,
        description: String,
        onLoading: () -> Unit,
        onLoadingDone: () -> Unit,
        onBeforeWebView: () -> Unit = {},
        onStatusLoading: () -> Unit = {},
        onStatusLoadingDone: () -> Unit = {}
    ) {
        if (!Utility.isInternetAvailable(context)) {
            ToastUtil.showDelete(context, context.getString(R.string.msgNoInternet))
            return
        }
        onLoading()
        ApiClient.apiService.createHdfcOrder(
            authorization = bearerToken(context),
            request = HdfcCreateOrderRequest(amount = amount, description = description)
        ).enqueue(object : Callback<HdfcOrderResponseItem> {
            override fun onResponse(
                call: Call<HdfcOrderResponseItem>,
                response: Response<HdfcOrderResponseItem>
            ) {
                onLoadingDone()
                if (context.isFinishing || context.isDestroyed) return

                val body = response.body()
                if (response.isSuccessful && body?.success == true && body.data != null) {
                    val data = body.data!!
                    val payUrl = data.paymentLinks?.web.orEmpty()

                    if (payUrl.isEmpty()) {
                        ToastUtil.showDelete(
                            context,
                            body.message ?: context.getString(R.string.errGeneric)
                        )
                        return
                    }

                    val status = data.status?.uppercase().orEmpty()
                    if (status in failedStatuses) {
                        PaymentStatusActivity.start(
                            context,
                            PaymentStatusItem(
                                orderId = data.orderId ?: "",
                                amount  = data.amount ?: "",
                                status  = data.status ?: "NEW"
                            )
                        )
                        return
                    }

                    pendingOrderId = data.orderId
                    pendingAmount  = data.amount
                    onBeforeWebView()
                    context.startActivity(
                        HdfcWebViewActivity.newIntent(context, payUrl, data.returnUrl ?: "")
                    )
                    registerResumeWatcher(context, onStatusLoading, onStatusLoadingDone)
                } else {
                    val msg = body?.message
                        ?: ApiHelper.parseErrorMessage(
                            context, response.code(), response.errorBody()?.string()
                        )
                    ToastUtil.showDelete(context, msg)
                }
            }

            override fun onFailure(call: Call<HdfcOrderResponseItem>, t: Throwable) {
                onLoadingDone()
                if (context.isFinishing || context.isDestroyed) return
                ToastUtil.showDelete(
                    context,
                    t.localizedMessage ?: context.getString(R.string.errGeneric)
                )
            }
        })
    }

    private fun bearerToken(context: Activity): String {
        val token = SharedPreferenceHelper.getSharedPreferenceString(
            context, Constant.KEY_TOKEN, ""
        ) ?: ""
        return "Bearer $token"
    }

    private fun registerResumeWatcher(
        context: Activity,
        onStatusLoading: () -> Unit,
        onStatusLoadingDone: () -> Unit
    ) {
        pendingActivityRef       = WeakReference(context)
        pendingStatusLoading     = onStatusLoading
        pendingStatusLoadingDone = onStatusLoadingDone
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

    private fun checkOrderStatus(context: Activity) {
        val orderId = pendingOrderId ?: run { clearPendingState(); return }

        if (!Utility.isInternetAvailable(context)) {
            openStatusActivity(context, orderId, pendingAmount ?: "", "NEW")
            clearPendingState()
            return
        }

        pendingStatusLoading?.invoke()
        ApiClient.apiService.getHdfcOrderStatus(bearerToken(context), orderId)
            .enqueue(object : Callback<HdfcOrderResponseItem> {
                override fun onResponse(
                    call: Call<HdfcOrderResponseItem>,
                    response: Response<HdfcOrderResponseItem>
                ) {
                    pendingStatusLoadingDone?.invoke()
                    if (context.isFinishing || context.isDestroyed) { clearPendingState(); return }
                    val data = response.body()?.data
                    openStatusActivity(
                        context,
                        orderId = data?.orderId ?: orderId,
                        amount  = data?.amount ?: pendingAmount ?: "",
                        status  = data?.status ?: "NEW"
                    )
                    clearPendingState()
                }

                override fun onFailure(call: Call<HdfcOrderResponseItem>, t: Throwable) {
                    pendingStatusLoadingDone?.invoke()
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

    private fun clearPendingState() {
        pendingOrderId           = null
        pendingAmount            = null
        pendingActivityRef       = null
        pendingStatusLoading     = null
        pendingStatusLoadingDone = null
    }
}
