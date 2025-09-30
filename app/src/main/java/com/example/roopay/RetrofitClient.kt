package com.example.roopay

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {

    private const val BASE_URL = "https://supay.in/" // 👈 BASE URL

    // Apiservice ka sahi instance create kar rahe hain
    val instance: Apiservice by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        retrofit.create(Apiservice::class.java)
    }
}
