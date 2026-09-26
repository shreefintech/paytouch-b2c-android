package com.shreefintech.paytouchconsumer.fcm

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
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
import com.shreefintech.paytouchconsumer.utill.Utility
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

object NotificationHelper {

    const val NOTIFICATION_PERMISSION_CODE = 1001

    // Token currently being registered — prevents duplicate calls when login and
    // HomeActivity both trigger a sync before the first call has completed.
    @Volatile
    private var pendingToken: String? = null

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

    /** Asks for POST_NOTIFICATIONS on Android 13+. No-op on older versions or when already granted. */
    fun requestPermission(activity: Activity) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        if (ActivityCompat.checkSelfPermission(
                activity, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        ) return
        ActivityCompat.requestPermissions(
            activity,
            arrayOf(Manifest.permission.POST_NOTIFICATIONS),
            NOTIFICATION_PERMISSION_CODE
        )
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
            .notify(System.currentTimeMillis().toInt(), notification)
    }

    /**
     * Registers the device's FCM token with the backend for the logged-in user.
     * Skips silently when logged out, offline, or when this token is already registered.
     *
     * @param token pass the token from onNewToken; when null the current token is fetched from Firebase.
     */
    fun syncToken(context: Context, token: String? = null) {
        val appContext = context.applicationContext
        if (!SharedPreferenceHelper.isLoggedIn(appContext)) return
        if (token != null) {
            registerToken(appContext, token)
            return
        }
        FirebaseMessaging.getInstance().token
            .addOnSuccessListener { fcmToken -> registerToken(appContext, fcmToken) }
            .addOnFailureListener { it.printStackTrace() }
    }

    /**
     * Unregisters this device's token so a logged-out device stops receiving the user's pushes.
     * Must run before the logout API (it needs a valid bearer token). [onDone] is always invoked,
     * on success, failure, or skip — logout must never be blocked by this call.
     */
    fun removeToken(context: Context, onDone: () -> Unit) {
        val appContext = context.applicationContext
        val token = SharedPreferenceHelper.getSharedPreferenceString(
            appContext, Constant.KEY_FCM_TOKEN, ""
        ) ?: ""
        if (token.isEmpty() || !Utility.isInternetAvailable(appContext)) {
            onDone()
            return
        }
        ApiClient.apiService.removeDeviceToken(bearerToken(appContext), DeviceTokenRemoveRequest(token))
            .enqueue(object : Callback<MessageItem> {
                override fun onResponse(call: Call<MessageItem>, response: Response<MessageItem>) {
                    onDone()
                }

                override fun onFailure(call: Call<MessageItem>, t: Throwable) {
                    t.printStackTrace()
                    onDone()
                }
            })
    }

    // Synchronized: onNewToken runs on an FCM worker thread while Login/Home sync on main
    @Synchronized
    private fun registerToken(context: Context, token: String) {
        if (token.isEmpty() || token == pendingToken) return
        if (!Utility.isInternetAvailable(context)) return
        val registered = SharedPreferenceHelper.getSharedPreferenceString(
            context, Constant.KEY_FCM_TOKEN, ""
        )
        if (token == registered) return

        pendingToken = token
        val body = DeviceTokenRequest(
            fcmToken = token,
            platform = Constant.FCM_PLATFORM_ANDROID,
            deviceId = deviceId(context)
        )
        ApiClient.apiService.registerDeviceToken(bearerToken(context), body)
            .enqueue(object : Callback<MessageItem> {
                override fun onResponse(call: Call<MessageItem>, response: Response<MessageItem>) {
                    pendingToken = null
                    if (response.isSuccessful && response.body()?.success == true) {
                        SharedPreferenceHelper.setSharedPreferenceString(
                            context, Constant.KEY_FCM_TOKEN, token
                        )
                    }
                }

                override fun onFailure(call: Call<MessageItem>, t: Throwable) {
                    pendingToken = null
                    t.printStackTrace()
                }
            })
    }

    private fun bearerToken(context: Context): String {
        val token = SharedPreferenceHelper.getSharedPreferenceString(
            context, Constant.KEY_TOKEN, ""
        ) ?: ""
        return "Bearer $token"
    }

    // ANDROID_ID is stable per app-signing key + device user and survives SharedPreferences clears
    @SuppressLint("HardwareIds")
    private fun deviceId(context: Context): String =
        Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: ""
}
