package com.example.roopay

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class ActivationActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_activation)

        val etMemberId = findViewById<EditText>(R.id.etMemberId)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val etPin = findViewById<EditText>(R.id.etPin)
        val etNumber = findViewById<EditText>(R.id.etNumber)
        val btnSubmit = findViewById<Button>(R.id.btnSubmit)

        btnSubmit.setOnClickListener {
            val memberId = etMemberId.text.toString().trim()
            val password = etPassword.text.toString().trim()
            val pin = etPin.text.toString().trim()
            val number = etNumber.text.toString().trim()

            if (memberId.isEmpty() || password.isEmpty() || pin.isEmpty() || number.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Your account activated 🎉", Toast.LENGTH_LONG).show()
                finish()

            }
        }
    }
}
