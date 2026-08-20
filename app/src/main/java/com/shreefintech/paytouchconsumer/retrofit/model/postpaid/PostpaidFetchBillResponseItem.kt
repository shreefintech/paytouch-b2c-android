package com.shreefintech.paytouchconsumer.retrofit.model.postpaid

import com.google.gson.annotations.SerializedName

data class PostpaidFetchBillResponseItem (
    @field:SerializedName("data")
    val data: PostpaidFetchBillDataItem? = null,

    @field:SerializedName("success")
    val success: Boolean? = null,

    @field:SerializedName("details")
    val details: String? = null,

    @field:SerializedName("message")
    val message: String? = null
)
data class PostpaidFetchBillDataItem(

    @field:SerializedName("bill_amount")
    val billAmount: Int? = null,

    @field:SerializedName("bill_period")
    val billPeriod: String? = null,

    @field:SerializedName("bill_date")
    val billDate: String? = null,

    @field:SerializedName("due_date")
    val dueDate: String? = null,

    @field:SerializedName("customer_name")
    val customerName: String? = null,

    @field:SerializedName("circle_name")
    val circleName: String? = null,

    @field:SerializedName("message")
    val message: String? = null
)
