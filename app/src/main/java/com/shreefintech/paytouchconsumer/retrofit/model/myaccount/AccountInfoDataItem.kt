package com.shreefintech.paytouchconsumer.retrofit.model.myaccount

import com.google.gson.annotations.SerializedName

data class AccountInfoDataItem(
    @field:SerializedName("id")                     val id: Int?,
    @field:SerializedName("member_id")              val memberId: String?,
    @field:SerializedName("member_no")              val memberNo: String?,
    @field:SerializedName("member_code")            val memberCode: String?,
    @field:SerializedName("mobile_no")              val mobileNo: String?,
    @field:SerializedName("member_name")            val memberName: String?,
    @field:SerializedName("birth_date")             val birthDate: String?,
    @field:SerializedName("age")                    val age: Int?,
    @field:SerializedName("home_address")           val homeAddress: String?,
    @field:SerializedName("city_name")              val cityName: String?,
    @field:SerializedName("email")                  val email: String?,
    @field:SerializedName("status")                 val status: String?,
    @field:SerializedName("pan_card_no")            val panCardNo: String?,
    @field:SerializedName("aadhaar_no")             val aadhaarNo: String?,
    @field:SerializedName("gst_no")                 val gstNo: String?,
    @field:SerializedName("registration_date")      val registrationDate: String?,
    @field:SerializedName("activation_date")        val activationDate: String?,
    @field:SerializedName("balance")                val balance: String?,
    @field:SerializedName("pan_card_photo")         val panCardPhoto: String?,
    @field:SerializedName("aadhaar_front_photo")    val aadhaarFrontPhoto: String?,
    @field:SerializedName("aadhaar_back_photo")     val aadhaarBackPhoto: String?,
    @field:SerializedName("virtual_account_number") val virtualAccountNumber: String?,
    @field:SerializedName("vpa")                    val vpa: String?
)
