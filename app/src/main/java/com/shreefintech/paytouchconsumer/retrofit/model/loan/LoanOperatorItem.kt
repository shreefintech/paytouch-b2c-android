package com.shreefintech.paytouchconsumer.retrofit.model.loan

import com.google.gson.annotations.SerializedName

data class LoanOperatorItem(
    @field:SerializedName("id")   val id: String?,
    @field:SerializedName("name") val name: String?
)
