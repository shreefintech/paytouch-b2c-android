package com.shreefintech.paytouchconsumer.utill

import android.app.Application
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import com.shreefintech.paytouchconsumer.Constant

fun AndroidViewModel.bearerToken(): String {
    val token = SharedPreferenceHelper.getSharedPreferenceString(
        getApplication(), Constant.KEY_TOKEN, ""
    ) ?: ""
    return "Bearer $token"
}

fun AndroidViewModel.getString(@StringRes resId: Int): String =
    getApplication<Application>().getString(resId)
