package com.shreefintech.paytouchconsumer.retrofit

import android.content.Context
import android.content.Intent
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonParser
import com.google.gson.annotations.SerializedName
import com.shreefintech.paytouchconsumer.Constant
import com.shreefintech.paytouchconsumer.R
import com.shreefintech.paytouchconsumer.auth.LoginActivity
import com.shreefintech.paytouchconsumer.auth.NextStepRouter
import com.shreefintech.paytouchconsumer.enums.NextStep
import com.shreefintech.paytouchconsumer.utill.SharedPreferenceHelper

object ApiHelper {

    // Error envelope: `data` holds per-field messages on 422 ({"field": ["msg"]})
    private data class ErrorBodyItem(
        @field:SerializedName("message")    val message: String? = null,
        @field:SerializedName("error_code") val errorCode: String? = null,
        @field:SerializedName("data")       val data: JsonElement? = null
    )

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


    /**
     * The first 422 field error, else the backend `message`, else the HTTP status text.
     * A 403 also hands its `error_code` to [onForbidden].
     */
    fun parseErrorMessage(mContext: Context, statusCode: Int, errorBody: String?): String {
        // Step 1 : Try to parse backend `message` field (same as iOS GeneralResponseModel decode)
        var body: ErrorBodyItem? = null
        if (!errorBody.isNullOrEmpty()) {
            try {
                body = Gson().fromJson(errorBody, ErrorBodyItem::class.java)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        if (statusCode == 403) onForbidden(mContext, body?.errorCode)

        val fieldError = if (statusCode == 422) firstFieldError(body?.data) else null

        // Step 2 : Fallback to HTTP status description (same as iOS httpResponse.errorMessage)
        return fieldError
            ?: body?.message?.takeIf { it.isNotEmpty() }
            ?: getHttpErrorMessage(mContext, statusCode)
    }

    /**
     * Money-call 403s. Codes that name a KYC step (`AADHAAR_REQUIRED`, `KYC_RESUBMIT_REQUIRED`) open it via
     * [NextStepRouter]; `USER_BLOCKED` logs out like a 401. Others (`BANK_DETAILS_REQUIRED`, `LOCATION_REQUIRED`)
     * only show the message. NEW_TASK because most callers pass the Application context.
     */
    private fun onForbidden(context: Context, errorCode: String?) {
        if (errorCode.isNullOrEmpty() || !SharedPreferenceHelper.isLoggedIn(context)) return
        if (errorCode == Constant.ERROR_CODE_USER_BLOCKED) {
            SharedPreferenceHelper.clearSharedPreference(context)
            context.startActivity(Intent(context, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
            return
        }
        // error_code is the upper-case form of the matching next_step value
        val nextStep = errorCode.lowercase()
        if (NextStep.from(nextStep)?.isKycStep == true) {
            // CLEAR_TOP: reuses the Home/status screen already in the stack instead of stacking a second one
            context.startActivity(NextStepRouter.intentFor(context, nextStep, null).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            })
        }
    }

    private fun firstFieldError(data: JsonElement?): String? {
        if (data == null || !data.isJsonObject) return null
        val first = data.asJsonObject.entrySet().firstOrNull()?.value ?: return null
        val message = when {
            first.isJsonArray && first.asJsonArray.size() > 0 -> first.asJsonArray[0]
            else -> first
        }
        return message.takeIf { it.isJsonPrimitive }?.asString?.takeIf { it.isNotEmpty() }
    }

}
