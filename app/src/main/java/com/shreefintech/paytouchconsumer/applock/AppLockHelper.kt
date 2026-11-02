package com.shreefintech.paytouchconsumer.applock

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import com.shreefintech.paytouchconsumer.BaseActivity
import com.shreefintech.paytouchconsumer.Constant
import com.shreefintech.paytouchconsumer.auth.CreateAccountActivity
import com.shreefintech.paytouchconsumer.auth.LoginActivity
import com.shreefintech.paytouchconsumer.auth.OtpVerificationActivity
import com.shreefintech.paytouchconsumer.auth.ResetMpinActivity
import com.shreefintech.paytouchconsumer.auth.ResetPasswordActivity
import com.shreefintech.paytouchconsumer.auth.SplashActivity
import com.shreefintech.paytouchconsumer.utill.SharedPreferenceHelper

/**
 * GPay-style app lock — asks for the phone's own screen lock (fingerprint / face / PIN / pattern)
 * via [AppLockActivity]:
 * - on a cold start that already has a saved session, and
 * - when the app returns from the background after more than [Constant.APP_LOCK_GRACE_MS]
 *   ([Constant.APP_LOCK_EXTERNAL_GRACE_MS] when the app itself opened the other app).
 *
 * Lifecycle callbacks always run on the main thread, so the state needs no synchronisation.
 */
object AppLockHelper : Application.ActivityLifecycleCallbacks {

    // The lock itself, plus the auth flow — the user proves identity with credentials there.
    private val exemptActivities = setOf(
        AppLockActivity::class.java,
        SplashActivity::class.java,
        LoginActivity::class.java,
        CreateAccountActivity::class.java,
        OtpVerificationActivity::class.java,
        ResetPasswordActivity::class.java,
        ResetMpinActivity::class.java
    )

    private var isLocked = false
    private var startedCount = 0
    private var backgroundAt = 0L
    private var isExternalLaunch = false

    fun init(app: Application) {
        isLocked = SharedPreferenceHelper.isLoggedIn(app)
        app.registerActivityLifecycleCallbacks(this)
    }

    /** The app is opening another app — camera, file picker, UPI app, payment gateway, share, browser. */
    fun onExternalLaunch() {
        isExternalLaunch = true
    }

    /** The user proved identity — device lock or credential login. */
    fun unlock() {
        isLocked = false
    }

    override fun onActivityStarted(activity: Activity) {
        if (startedCount++ == 0 && backgroundAt > 0L) {
            val awayMs = SystemClock.elapsedRealtime() - backgroundAt
            val graceMs = if (isExternalLaunch) Constant.APP_LOCK_EXTERNAL_GRACE_MS else Constant.APP_LOCK_GRACE_MS
            if (awayMs > graceMs) isLocked = true
            backgroundAt = 0L
            isExternalLaunch = false
        }
        if (!SharedPreferenceHelper.isLoggedIn(activity)) {
            // Logged out (logout / 401) — nothing to protect, and no stale lock after the next login.
            isLocked = false
            return
        }
        if (isLocked && activity is BaseActivity && activity.javaClass !in exemptActivities) {
            // REORDER_TO_FRONT reuses a lock screen already in the task instead of stacking a second one.
            activity.startActivity(
                Intent(activity, AppLockActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            )
        }
    }

    override fun onActivityResumed(activity: Activity) {
        // Back in the app without going to the background (e.g. share chooser dismissed).
        isExternalLaunch = false
    }

    override fun onActivityStopped(activity: Activity) {
        startedCount = maxOf(startedCount - 1, 0)
        // Rotation stops and restarts the Activity — that is not the app leaving the screen.
        if (startedCount == 0 && !activity.isChangingConfigurations) {
            backgroundAt = SystemClock.elapsedRealtime()
        }
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
    override fun onActivityPaused(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
    override fun onActivityDestroyed(activity: Activity) {}
}
