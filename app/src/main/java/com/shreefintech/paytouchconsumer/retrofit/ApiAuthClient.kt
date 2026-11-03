package com.shreefintech.paytouchconsumer.retrofit

import com.shreefintech.paytouchconsumer.Constant

/** Client for the B2C auth/onboarding backend. [ApiClient.init] must run first so the session interceptor is attached. */
object ApiAuthClient {

    private var _apiService: ApiAuthService? = null

    val apiService: ApiAuthService
        get() = _apiService ?: buildService().also { _apiService = it }

    fun init() {
        if (_apiService == null) _apiService = buildService()
    }

    private fun buildService(): ApiAuthService =
        ApiClient.buildRetrofit(Constant.BASE_URL_AUTH).create(ApiAuthService::class.java)
}
