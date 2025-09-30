package com.example.roopay

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.widget.TextView
import android.widget.Button



class ApiResponseActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_api_response)

        val textViewResponse = findViewById<TextView>(R.id.textViewResponse)
        val btnClose = findViewById<Button>(R.id.btnClose)

        val apiResponse = intent.getStringExtra("api_response") ?: "No Response"
        textViewResponse.text = apiResponse

        btnClose.setOnClickListener { finish() }

    }
}