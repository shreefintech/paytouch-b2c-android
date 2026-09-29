package com.shreefintech.paytouchconsumer.utill

import android.app.Application
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel

fun AndroidViewModel.bearerToken(): String =
    SharedPreferenceHelper.bearerToken(getApplication())

fun AndroidViewModel.getString(@StringRes resId: Int): String =
    getApplication<Application>().getString(resId)
