package com.shreefintech.paytouchconsumer.auth.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.shreefintech.paytouchconsumer.Constant
import com.shreefintech.paytouchconsumer.R
import com.shreefintech.paytouchconsumer.enums.LoginMode
import com.shreefintech.paytouchconsumer.retrofit.ApiClient
import com.shreefintech.paytouchconsumer.retrofit.ApiHelper
import com.shreefintech.paytouchconsumer.retrofit.model.LoginItem
import com.shreefintech.paytouchconsumer.utill.SharedPreferenceHelper
import com.shreefintech.paytouchconsumer.utill.Utility
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody

class LoginViewModel(application: Application) : AndroidViewModel(application) {

    fun login(
        mobile: String,
        credential: String,
        mode: LoginMode,
        onLoading: () -> Unit,
        onSuccess: (LoginItem?) -> Unit,
        onError: (String) -> Unit
    ) {
        val error = validate(mobile, credential, mode)
        if (error != null) { onError(error); return }
        if (!Utility.isInternetAvailable(getApplication())) {
            onError(getApplication<Application>().getString(R.string.msgNoInternet))
            return
        }
        onLoading()
        viewModelScope.launch {
            try {
                val json = Gson().toJson(
                    when (mode) {
                        LoginMode.PASSWORD -> mapOf("mobile" to mobile, "password" to credential)
                        LoginMode.MPIN     -> mapOf("mobile" to mobile, "mpin" to credential)
                    }
                )
                val body = json.toRequestBody("application/json".toMediaTypeOrNull())
                val response = withContext(Dispatchers.IO) {
                    ApiClient.apiService.login(body)
                }
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        val data = response.body()?.data
                        data?.let {
                            SharedPreferenceHelper.setSharedPreferenceString(
                                getApplication(), Constant.KEY_TOKEN, it.token ?: ""
                            )
                            SharedPreferenceHelper.setSharedPreferenceString(
                                getApplication(), Constant.KEY_TOKEN_TYPE, it.tokenType ?: ""
                            )
                            it.user?.let { user ->
                                SharedPreferenceHelper.setSharedPreferenceString(
                                    getApplication(), Constant.KEY_USER_ID, user.id?.toString() ?: ""
                                )
                                SharedPreferenceHelper.setSharedPreferenceString(
                                    getApplication(), Constant.KEY_MOBILE, user.mobile ?: ""
                                )
                                SharedPreferenceHelper.setSharedPreferenceString(
                                    getApplication(), Constant.KEY_EMAIL, user.email ?: ""
                                )
                            }
                        }
                        onSuccess(data)
                    } else {
                        val msg = ApiHelper.parseErrorMessage(
                            getApplication(),
                            response.code(),
                            response.errorBody()?.string()
                        )
                        onError(msg)
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onError(
                        e.localizedMessage
                            ?: getApplication<Application>().getString(R.string.errGeneric)
                    )
                }
            }
        }
    }

    private fun validate(mobile: String, credential: String, mode: LoginMode): String? {
        val app = getApplication<Application>()
        if (mobile.isBlank()) return app.getString(R.string.msgMobileEmpty)
        if (mobile.length != 10 || !mobile.matches(Regex("[6-9][0-9]{9}"))) {
            return app.getString(R.string.msgMobileInvalid)
        }
        return when (mode) {
            LoginMode.PASSWORD -> when {
                credential.isBlank()    -> app.getString(R.string.msgPasswordEmpty)
                credential.length < 8   -> app.getString(R.string.msgPasswordShort)
                else                    -> null
            }
            LoginMode.MPIN -> when {
                credential.isBlank()                          -> app.getString(R.string.msgMpinEmpty)
                !credential.matches(Regex("[0-9]{4}"))        -> app.getString(R.string.msgMpinInvalid)
                else                                          -> null
            }
        }
    }
}
