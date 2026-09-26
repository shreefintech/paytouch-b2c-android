package com.shreefintech.paytouchconsumer.utill

import android.app.Application
import com.shreefintech.paytouchconsumer.fcm.NotificationHelper
import com.shreefintech.paytouchconsumer.retrofit.ApiAdminClient
import com.shreefintech.paytouchconsumer.retrofit.ApiClient

class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        ApiClient.init(this)
        ApiAdminClient.init(this)
        NotificationHelper.createChannel(this)
    }
}
