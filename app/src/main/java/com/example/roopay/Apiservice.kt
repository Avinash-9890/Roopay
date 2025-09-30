package com.example.roopay


import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface Apiservice {


    @GET("recharge_api/recharge")
    fun getActivationData(
        @Query("member_id") memberId: String,
        @Query("api_password") apiPassword: String,
        @Query("a_pi_pin") apiPin: String,
        @Query("number") number: String
    ): Call<ModelClass>


       }

