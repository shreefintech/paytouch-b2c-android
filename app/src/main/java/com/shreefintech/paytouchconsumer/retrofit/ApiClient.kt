package com.shreefintech.paytouchconsumer.retrofit

import android.content.Context
import com.google.gson.GsonBuilder
import com.shreefintech.paytouchconsumer.BuildConfig
import com.shreefintech.paytouchconsumer.Constant
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {

    private const val DEFAULT_TIMEOUT_SECONDS = 30
    private const val UPLOAD_TIMEOUT_SECONDS = 120

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

    private fun buildRetrofit(): Retrofit {
        val clientBuilder = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(DEFAULT_TIMEOUT_SECONDS.toLong(), TimeUnit.SECONDS)
            .writeTimeout(DEFAULT_TIMEOUT_SECONDS.toLong(), TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("Accept", "application/json")
                    .build()
                // Multipart KYC uploads (ID images + selfie) need longer; every other call keeps
                // the default so a hung payment request does not spin for two minutes.
                val effectiveChain = if (request.body is MultipartBody) {
                    chain.withReadTimeout(UPLOAD_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                        .withWriteTimeout(UPLOAD_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                } else chain
                effectiveChain.proceed(request)
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
