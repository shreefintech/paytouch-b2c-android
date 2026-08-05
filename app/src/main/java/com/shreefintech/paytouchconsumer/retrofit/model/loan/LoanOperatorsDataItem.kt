package com.shreefintech.paytouchconsumer.retrofit.model.loan

import com.google.gson.annotations.SerializedName

data class LoanOperatorsDataItem(
    @field:SerializedName("operators") val operators: Map<String, String>?
)
