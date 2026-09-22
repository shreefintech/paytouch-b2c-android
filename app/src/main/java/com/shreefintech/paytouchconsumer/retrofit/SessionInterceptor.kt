package com.shreefintech.paytouchconsumer.retrofit

import android.content.Context
import android.content.Intent
import com.shreefintech.paytouchconsumer.Constant
import com.shreefintech.paytouchconsumer.auth.LoginActivity
import com.shreefintech.paytouchconsumer.utill.SharedPreferenceHelper
import okhttp3.Interceptor
import okhttp3.Response

class SessionInterceptor(private val context: Context) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        if (SharedPreferenceHelper.isLoggedIn(context)) {
            SharedPreferenceHelper.setSharedPreferenceString(
                context, Constant.KEY_LAST_INTERACTION, System.currentTimeMillis().toString()
            )
        }
        val response = chain.proceed(chain.request())
        if (response.code == 401) {
            SharedPreferenceHelper.clearSharedPreference(context)
            context.startActivity(Intent(context, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
        }
        return response
    }
}
