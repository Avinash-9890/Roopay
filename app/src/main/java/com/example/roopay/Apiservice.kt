package com.example.roopay


import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Query

interface Apiservice {



    @Headers("Content-Type: application/json")
    @POST("recharge_api/recharge")
    fun postActivationData(
        @Body requestBody: ModelClass
    ): Call<ModelClass>

       }

