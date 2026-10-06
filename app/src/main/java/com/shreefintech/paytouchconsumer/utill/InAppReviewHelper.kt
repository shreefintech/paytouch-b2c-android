package com.shreefintech.paytouchconsumer.utill

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import com.google.android.play.core.review.ReviewManagerFactory
import com.shreefintech.paytouchconsumer.Constant
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

/**
 * Google Play in-app review — asked once a bill payment receipt shows a successful payment.
 *
 * Rules (all must pass):
 * - payment status is Success, and this transaction has not been counted before
 * - user has 3+ successful bill payments on this device
 * - app installed 3+ days ago
 * - review not asked in the last 90 days
 *
 * Play decides on its own whether the sheet is actually shown and never reports whether the user rated,
 * so the ask is recorded as soon as the flow is launched.
 */
object InAppReviewHelper {

    /**
     * Call from a `*SmsReceiptActivity` once the receipt of a just-completed payment is populated.
     * Safe to call again for the same receipt (retry / recreation) — the transaction is counted only once.
     */
    fun onPaymentReceiptShown(activity: ComponentActivity, transactionId: String?, status: String?) {
        if (!status.equals(Constant.PAYMENT_STATUS_SUCCESS, ignoreCase = true)) return
        if (!recordSuccessfulPayment(activity, transactionId)) return
        if (!isEligible(activity)) return

        // lifecycleScope is cancelled on destroy — the delayed launch can never outlive the screen
        activity.lifecycleScope.launch {
            delay(Constant.REVIEW_DELAY_MS)
            // User left the receipt (share sheet, back, home) — skip; the next payment asks instead
            if (!activity.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) return@launch
            launchReview(activity)
        }
    }

    /** Counts a successful payment once per transaction. Returns false if it was already counted. */
    private fun recordSuccessfulPayment(context: Context, transactionId: String?): Boolean {
        // No ID means the same receipt cannot be told apart on a retry — skip rather than double count
        if (transactionId.isNullOrBlank()) return false
        val lastCounted = SharedPreferenceHelper.getSharedPreferenceString(context, Constant.KEY_REVIEW_LAST_TXN_ID, "")
        if (lastCounted == transactionId) return false

        val count = SharedPreferenceHelper.getSharedPreferenceInt(context, Constant.KEY_REVIEW_SUCCESS_COUNT, 0)
        SharedPreferenceHelper.setSharedPreferenceInt(context, Constant.KEY_REVIEW_SUCCESS_COUNT, count + 1)
        SharedPreferenceHelper.setSharedPreferenceString(context, Constant.KEY_REVIEW_LAST_TXN_ID, transactionId)
        return true
    }

    private fun isEligible(context: Context): Boolean {
        val count = SharedPreferenceHelper.getSharedPreferenceInt(context, Constant.KEY_REVIEW_SUCCESS_COUNT, 0)
        if (count < Constant.REVIEW_MIN_SUCCESS_PAYMENTS) return false

        val now = System.currentTimeMillis()
        val installedAt = firstInstallTime(context) ?: return false
        if (now - installedAt < TimeUnit.DAYS.toMillis(Constant.REVIEW_MIN_INSTALL_DAYS)) return false

        val lastAskedAt = SharedPreferenceHelper.getSharedPreferenceLong(context, Constant.KEY_REVIEW_LAST_ASKED_AT, 0L)
        return lastAskedAt == 0L || now - lastAskedAt >= TimeUnit.DAYS.toMillis(Constant.REVIEW_COOLDOWN_DAYS)
    }

    private fun firstInstallTime(context: Context): Long? {
        return try {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
            packageInfo.firstInstallTime
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
            null
        }
    }

    private fun launchReview(activity: ComponentActivity) {
        val reviewManager = ReviewManagerFactory.create(activity.applicationContext)
        // Activity-scoped listener — removed automatically in onStop, so it never fires on a dead screen
        reviewManager.requestReviewFlow().addOnCompleteListener(activity) { task ->
            if (!task.isSuccessful) {
                // Expected when Play Store is missing / outdated or the device is offline
                task.exception?.printStackTrace()
                return@addOnCompleteListener
            }
            if (activity.isFinishing || activity.isDestroyed) return@addOnCompleteListener
            SharedPreferenceHelper.setSharedPreferenceLong(activity, Constant.KEY_REVIEW_LAST_ASKED_AT, System.currentTimeMillis())
            reviewManager.launchReviewFlow(activity, task.result)
        }
    }
}
