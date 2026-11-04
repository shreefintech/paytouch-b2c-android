package com.shreefintech.paytouchconsumer.retrofit.model.billpay

import com.google.gson.annotations.SerializedName

data class BillPayInputParamItem(
    @field:SerializedName("param_name") val paramName: String? = null,
    @field:SerializedName("name")       val name: String? = null,
    @field:SerializedName("key")        val key: String? = null,
    @field:SerializedName("label")      val label: String? = null
) {
    // Key read from param_name, else name / key
    val resolvedKey: String?
        get() = listOf(paramName, name, key).firstOrNull { !it.isNullOrBlank() }
}
