package com.shreefintech.paytouchconsumer.fcm

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessaging
import com.shreefintech.paytouchconsumer.Constant
import com.shreefintech.paytouchconsumer.R
import com.shreefintech.paytouchconsumer.auth.SplashActivity
import com.shreefintech.paytouchconsumer.retrofit.ApiClient
import com.shreefintech.paytouchconsumer.retrofit.model.auth.MessageItem
import com.shreefintech.paytouchconsumer.retrofit.model.notification.DeviceTokenRemoveRequest
import com.shreefintech.paytouchconsumer.retrofit.model.notification.DeviceTokenRequest
import com.shreefintech.paytouchconsumer.utill.SharedPreferenceHelper
import com.shreefintech.paytouchconsumer.utill.SharedPreferenceHelper.bearerToken
import com.shreefintech.paytouchconsumer.utill.Utility
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.concurrent.atomic.AtomicInteger

object NotificationHelper {

    // Token currently being registered — prevents duplicate calls when login and
    // HomeActivity both trigger a sync before the first call has completed.
    @Volatile
    private var pendingToken: String? = null

    private val notificationIdCounter = AtomicInteger(System.currentTimeMillis().toInt())

    /** Creates the push channel. Must run before any notification is posted — call from MyApp. */
    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            context.getString(R.string.labelNotificationChannelId),
            context.getString(R.string.labelNotificationChannel),
            NotificationManager.IMPORTANCE_HIGH
        )
        context.getSystemService(NotificationManager::class.java)
            ?.createNotificationChannel(channel)
    }

    fun showNotification(context: Context, title: String, message: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ActivityCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) return

        // Launcher-style intent: resumes the existing task if the app is open, otherwise starts from Splash
        val launchIntent = Intent(context, SplashActivity::class.java).apply {
            action = Intent.ACTION_MAIN
            addCategory(Intent.CATEGORY_LAUNCHER)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(
            context, context.getString(R.string.labelNotificationChannelId)
        )
            .setSmallIcon(R.drawable.img_paytouch)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context)
            .notify(notificationIdCounter.incrementAndGet(), notification)
    }

    /**
     * Registers the device's FCM token with the backend for the logged-in user.
     * Skips silently when logged out, offline, or when this token is already registered.
     *
     * @param token pass the token from onNewToken; when null the current token is fetched from Firebase.
     */
    fun syncToken(context: Context, token: String? = null) {
        val appContext = context.applicationContext
        if (token != null) {
            registerToken(appContext, token)
            return
        }
        FirebaseMessaging.getInstance().token
            .addOnSuccessListener { fcmToken -> registerToken(appContext, fcmToken) }
            .addOnFailureListener { it.printStackTrace() }
    }

    // Synchronized: onNewToken runs on an FCM worker thread while Login/Home sync on main
    @Synchronized
    private fun registerToken(context: Context, token: String) {
        if (token.isEmpty() || token == pendingToken) return
        if (!Utility.isInternetAvailable(context)) return
        val isLoggedIn = SharedPreferenceHelper.isLoggedIn(context)
        val registered = SharedPreferenceHelper.getSharedPreferenceString(
            context, Constant.KEY_FCM_TOKEN, ""
        )
        val registeredAuthed = SharedPreferenceHelper.getSharedPreferenceBoolean(
            context, Constant.KEY_FCM_TOKEN_AUTHED, false
        )
        // Skip only if the token is already registered for the current login state.
        // A token registered while logged out must be re-sent after login so the
        // server can link this device to the user's account.
        if (token == registered && (registeredAuthed || !isLoggedIn)) return

        pendingToken = token
        val bearer = if (isLoggedIn) bearerToken(context) else null
        val body = DeviceTokenRequest(
            fcmToken = token,
            platform = Constant.FCM_PLATFORM_ANDROID,
            deviceId = deviceId(context),
            language = Constant.FCM_LANGUAGE_DEFAULT
        )
        ApiClient.apiService.registerDeviceToken(bearer, body)
            .enqueue(object : Callback<MessageItem> {
                override fun onResponse(call: Call<MessageItem>, response: Response<MessageItem>) {
                    pendingToken = null
                    if (response.isSuccessful && response.body()?.success == true) {
                        SharedPreferenceHelper.setSharedPreferenceString(
                            context, Constant.KEY_FCM_TOKEN, token
                        )
                        SharedPreferenceHelper.setSharedPreferenceBoolean(
                            context, Constant.KEY_FCM_TOKEN_AUTHED, isLoggedIn
                        )
                    } else {
                        // Background call — no toast, but keep the failure visible for debugging
                        Utility.logError(IllegalStateException("registerDeviceToken failed: HTTP ${response.code()}"))
                    }
                }

                override fun onFailure(call: Call<MessageItem>, t: Throwable) {
                    pendingToken = null
                    t.printStackTrace()
                }
            })
    }

    /**
     * Unlinks this device's FCM token from the logged-in user (DELETE /api/device-token).
     * Must run before the logout API — logout invalidates the bearer token this call needs.
     * [onDone] always runs (success, failure, offline, no token) so logout is never blocked.
     */
    fun removeToken(context: Context, onDone: () -> Unit) {
        val appContext = context.applicationContext
        val token = SharedPreferenceHelper.getSharedPreferenceString(
            appContext, Constant.KEY_FCM_TOKEN, ""
        ).orEmpty()
        if (token.isEmpty() || !Utility.isInternetAvailable(appContext)) {
            onDone()
            return
        }
        ApiClient.apiService.removeDeviceToken(bearerToken(appContext), DeviceTokenRemoveRequest(token))
            .enqueue(object : Callback<MessageItem> {
                override fun onResponse(call: Call<MessageItem>, response: Response<MessageItem>) {
                    if (!response.isSuccessful) {
                        // Background call — no toast, but keep the failure visible for debugging
                        Utility.logError(IllegalStateException("removeDeviceToken failed: HTTP ${response.code()}"))
                    }
                    onDone()
                }

                override fun onFailure(call: Call<MessageItem>, t: Throwable) {
                    t.printStackTrace()
                    onDone()
                }
            })
    }

    // ANDROID_ID is stable per app-signing key + device user and survives SharedPreferences clears
    @SuppressLint("HardwareIds")
    private fun deviceId(context: Context): String =
        Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: ""
}
