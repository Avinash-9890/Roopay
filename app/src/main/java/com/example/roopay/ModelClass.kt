package com.example.roopay

import com.google.gson.annotations.SerializedName

data class ModelClass(


    @SerializedName("ERROR") val error: Int,
    @SerializedName("MESSAGE") val message: String,
    @SerializedName("ip") val ip: String,
    @SerializedName("request") val request: RequestData
)

data class RequestData(
    @SerializedName("member_id") val member_id: String,
    @SerializedName("api_password") val api_password: String,
    @SerializedName("a_pi_pin") val a_pi_pin: String,
    @SerializedName("number") val number: String
)




