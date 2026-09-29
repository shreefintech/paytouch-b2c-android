package com.shreefintech.paytouchconsumer.retrofit.model.location

import com.google.gson.annotations.SerializedName

// Named UserLocationRequest to avoid clashing with com.google.android.gms.location.LocationRequest
data class UserLocationRequest(
    @field:SerializedName("latitude") val latitude: Double,
    @field:SerializedName("longitude") val longitude: Double,
    @field:SerializedName("accuracy") val accuracy: Double?
)
