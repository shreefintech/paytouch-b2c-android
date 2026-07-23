package com.shreefintech.paytouchconsumer.retrofit

import android.content.Context
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.shreefintech.paytouchconsumer.R
import com.shreefintech.paytouchconsumer.retrofit.model.General

object ApiHelper {

    fun getHttpErrorMessage(context: Context, statusCode: Int): String {
        return when (statusCode) {
            400 -> context.getString(R.string.api_err_400)
            401 -> context.getString(R.string.api_err_401)
            402 -> context.getString(R.string.api_err_402)
            403 -> context.getString(R.string.api_err_403)
            404 -> context.getString(R.string.api_err_404)
            405 -> context.getString(R.string.api_err_405)
            413 -> context.getString(R.string.api_err_413)
            415 -> context.getString(R.string.api_err_415)
            422 -> context.getString(R.string.api_err_422)
            425 -> context.getString(R.string.api_err_425)
            429 -> context.getString(R.string.api_err_429)
            500 -> context.getString(R.string.api_err_500)
            502 -> context.getString(R.string.api_err_502)
            503 -> context.getString(R.string.api_err_503)
            504 -> context.getString(R.string.api_err_504)
            508 -> context.getString(R.string.api_err_508)
            else -> context.getString(R.string.err_generic)
        }
    }


    fun parseErrorMessage(mContext: Context, statusCode: Int, errorBody: String?): String {
        // Step 1 : Try to parse backend `message` field (same as iOS GeneralResponseModel decode)
        if (!errorBody.isNullOrEmpty()) {
            try {
                val model = Gson().fromJson(errorBody, General::class.java)
                if (!model.message.isNullOrEmpty()) {
                    return model.message
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Step 2 : Fallback to HTTP status description (same as iOS httpResponse.errorMessage)
        return getHttpErrorMessage(mContext,statusCode)
    }

}
