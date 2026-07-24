package com.shreefintech.paytouchconsumer.retrofit

import android.content.Context
import android.content.Intent
import com.shreefintech.paytouchconsumer.auth.LoginActivity
import com.shreefintech.paytouchconsumer.utill.SharedPreferenceHelper
import okhttp3.Interceptor
import okhttp3.Response

class SessionInterceptor(private val context: Context) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())
        if (response.code == 401) {
            SharedPreferenceHelper.clearSharedPreference(context)
            val intent = Intent(context, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            context.startActivity(intent)
        }
        return response
    }
}
