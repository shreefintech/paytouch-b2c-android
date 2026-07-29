package com.shreefintech.paytouchconsumer.retrofit

import android.content.Context
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.shreefintech.paytouchconsumer.R
import com.shreefintech.paytouchconsumer.retrofit.model.General

object ApiHelper {

    fun getHttpErrorMessage(context: Context, statusCode: Int): String {
        return when (statusCode) {
            400 -> context.getString(R.string.apiErr400)
            401 -> context.getString(R.string.apiErr401)
            402 -> context.getString(R.string.apiErr402)
            403 -> context.getString(R.string.apiErr403)
            404 -> context.getString(R.string.apiErr404)
            405 -> context.getString(R.string.apiErr405)
            413 -> context.getString(R.string.apiErr413)
            415 -> context.getString(R.string.apiErr415)
            422 -> context.getString(R.string.apiErr422)
            425 -> context.getString(R.string.apiErr425)
            429 -> context.getString(R.string.apiErr429)
            500 -> context.getString(R.string.apiErr500)
            502 -> context.getString(R.string.apiErr502)
            503 -> context.getString(R.string.apiErr503)
            504 -> context.getString(R.string.apiErr504)
            508 -> context.getString(R.string.apiErr508)
            else -> context.getString(R.string.errGeneric)
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
