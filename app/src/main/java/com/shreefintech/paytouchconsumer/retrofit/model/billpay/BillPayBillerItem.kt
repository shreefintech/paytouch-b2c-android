package com.shreefintech.paytouchconsumer.retrofit.model.billpay

import com.google.gson.annotations.SerializedName

// GET billpay/utility-billers — id is sent later as biller_id; input_params describe the form fields
data class BillPayBillerItem(
    @field:SerializedName("id")           val id: Int? = null,
    @field:SerializedName("name")         val name: String? = null,
    @field:SerializedName("input_params") val inputParams: List<BillPayInputParamItem>? = null
)
