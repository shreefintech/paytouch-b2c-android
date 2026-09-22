package com.shreefintech.paytouchconsumer.retrofit

import android.content.Context
import com.shreefintech.paytouchconsumer.Constant
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiAdminClient {

    private var appContext: Context? = null
    private var _apiService: ApiAdminService? = null

    val apiService: ApiAdminService
        get() = _apiService ?: buildService().also { _apiService = it }

    fun init(context: Context) {
        appContext = context.applicationContext
        if (_apiService == null) _apiService = buildService()
    }

    private fun buildService(): ApiAdminService {
        val clientBuilder = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)

        appContext?.let { clientBuilder.addInterceptor(SessionInterceptor(it)) }

        return Retrofit.Builder()
            .baseUrl(Constant.BASE_URL_ADMIN)
            .client(clientBuilder.build())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiAdminService::class.java)
    }
}
