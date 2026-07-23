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

    private var _retrofit: Retrofit? = null
    private var _apiService: ApiService? = null

    val retrofit: Retrofit
        get() = _retrofit ?: buildRetrofit().also { _retrofit = it }

    val apiService: ApiService
        get() = _apiService ?: retrofit.create(ApiService::class.java).also { _apiService = it }

    /** Call once from Application.onCreate() to pre-warm the Retrofit instance. */
    fun init(context: Context) {
        if (_retrofit == null) _retrofit = buildRetrofit()
        if (_apiService == null) _apiService = retrofit.create(ApiService::class.java)
    }

    /** Call after a base-URL change (e.g. environment switch) to force a full rebuild. */
    fun resetWithNewUrl(context: Context) {
        _retrofit = null
        _apiService = null
    }

    private fun buildRetrofit(): Retrofit {
        val clientBuilder = OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("Accept", "application/json")
                    .addHeader("Content-Type", "application/json")
                    .build()
                chain.proceed(request)
            }

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
