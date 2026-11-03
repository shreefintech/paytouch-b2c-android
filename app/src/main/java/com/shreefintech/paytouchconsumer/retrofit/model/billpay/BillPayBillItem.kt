package com.shreefintech.paytouchconsumer.retrofit.model.billpay

import com.google.gson.annotations.SerializedName

// POST billpay/fetch-bill — has_bill false means "No bill found"; enquiry_ref is not shown
data class BillPayBillItem(
    @field:SerializedName("has_bill")      val hasBill: Boolean? = null,
    @field:SerializedName("amount")        val amount: Double? = null,
    @field:SerializedName("due_date")      val dueDate: String? = null,
    @field:SerializedName("bill_date")     val billDate: String? = null,
    @field:SerializedName("customer_name") val customerName: String? = null,
    @field:SerializedName("enquiry_ref")   val enquiryRef: String? = null
)
