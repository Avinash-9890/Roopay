package com.example.roopay

import com.google.gson.annotations.SerializedName

data class ModelClass(


    @SerializedName("member_id") val memberId: String,
    @SerializedName("api_password") val apiPassword: String,
    @SerializedName("api_pin") val apiPin: String,
    @SerializedName("number") val number: String
)





