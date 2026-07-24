package com.shreefintech.paytouchconsumer.retrofit

import android.content.Context
import com.google.gson.GsonBuilder
import com.shreefintech.paytouchconsumer.BuildConfig
import com.shreefintech.paytouchconsumer.Constant
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {

    private var appContext: Context? = null
    private var _retrofit: Retrofit? = null
    private var _apiService: ApiService? = null

    val retrofit: Retrofit
        get() = _retrofit ?: buildRetrofit().also { _retrofit = it }

    val apiService: ApiService
        get() = _apiService ?: retrofit.create(ApiService::class.java).also { _apiService = it }

    fun init(context: Context) {
        appContext = context.applicationContext
        if (_retrofit == null) _retrofit = buildRetrofit()
        if (_apiService == null) _apiService = retrofit.create(ApiService::class.java)
    }

    fun resetWithNewUrl(context: Context) {
        _retrofit = null
        _apiService = null
    }

    private fun buildRetrofit(): Retrofit {
        val clientBuilder = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("Accept", "application/json")
                    .build()
                chain.proceed(request)
            }

        appContext?.let { clientBuilder.addInterceptor(SessionInterceptor(it)) }

        if (BuildConfig.DEBUG) {
            clientBuilder.addInterceptor(
                HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }
            )
            clientBuilder.addInterceptor(CurlInterceptor())
        }

        return Retrofit.Builder()
            .baseUrl(Constant.BASE_URL)
            .client(clientBuilder.build())
            .addConverterFactory(
                GsonConverterFactory.create(GsonBuilder().setLenient().create())
            )
            .build()
    }
}
