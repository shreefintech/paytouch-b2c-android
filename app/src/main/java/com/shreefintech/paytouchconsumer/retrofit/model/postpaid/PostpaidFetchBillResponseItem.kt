package com.shreefintech.paytouchconsumer.retrofit.model.postpaid

import com.google.gson.annotations.SerializedName

data class PostpaidFetchBillResponseItem(
    @field:SerializedName("success")
    val success: Boolean? = null,

    @field:SerializedName("data")
    val data: PostpaidFetchBillDataWrapperItem? = null,

    @field:SerializedName("bill_id")
    val billId: String? = null,

    @field:SerializedName("message")
    val message: PostpaidFetchBillMessageItem? = null
)

data class PostpaidFetchBillDataWrapperItem(
    @field:SerializedName("success")
    val success: Boolean? = null,

    @field:SerializedName("data")
    val data: List<PostpaidFetchBillDataItem>? = null,

    @field:SerializedName("message")
    val message: PostpaidFetchBillMessageItem? = null
)

data class PostpaidFetchBillMessageItem(
    @field:SerializedName("code")
    val code: String? = null,

    @field:SerializedName("text")
    val text: String? = null
)

data class PostpaidFetchBillDataItem(
    @field:SerializedName("billAmount")
    val billAmount: String? = null,

    @field:SerializedName("billnetamount")
    val billNetAmount: String? = null,

    @field:SerializedName("billdate")
    val billDate: String? = null,

    @field:SerializedName("dueDate")
    val dueDate: String? = null,

    @field:SerializedName("acceptPayment")
    val acceptPayment: Boolean? = null,

    @field:SerializedName("acceptPartPay")
    val acceptPartPay: Boolean? = null,

    @field:SerializedName("cellNumber")
    val cellNumber: String? = null,

    @field:SerializedName("userName")
    val userName: String? = null,

    @field:SerializedName("additionalDetails")
    val additionalDetails: PostpaidAdditionalDetailsItem? = null
)

data class PostpaidAdditionalDetailsItem(
    @field:SerializedName("Circle")
    val circle: String? = null
)
