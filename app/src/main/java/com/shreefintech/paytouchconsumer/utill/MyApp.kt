package com.shreefintech.paytouchconsumer.utill

import android.app.Application
import com.shreefintech.paytouchconsumer.retrofit.ApiClient

class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        ApiClient.init(this)
    }
}
