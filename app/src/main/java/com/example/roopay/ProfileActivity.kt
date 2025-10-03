package com.example.roopay

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.util.*

class ProfileActivity : AppCompatActivity() {

    private lateinit var etName: EditText
    private lateinit var etUpi: EditText
    private lateinit var btnSave: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        etName = findViewById(R.id.etName)
        etUpi = findViewById(R.id.etUpi)
        btnSave = findViewById(R.id.btnSaveProfile)

        val prefs = getSharedPreferences("UserData", MODE_PRIVATE)

        // पहली बार UID generate करो
        if (!prefs.contains("uid")) {
            prefs.edit().putString("uid", UUID.randomUUID().toString()).apply()
        }

        // 👉 केवल तभी fields भरें जब value वाकई user ने save की हो
        val savedName = prefs.getString("username", null)
        val savedUpi = prefs.getString("upi", null)

        if (!savedName.isNullOrEmpty()) etName.setText(savedName)
        if (!savedUpi.isNullOrEmpty()) etUpi.setText(savedUpi)

        btnSave.setOnClickListener {
            val name = etName.text.toString().trim()
            val upi = etUpi.text.toString().trim()

            if (name.isEmpty() || upi.isEmpty()) {
                Toast.makeText(this, "Please enter name and UPI id", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            prefs.edit()
                .putString("username", name)
                .putString("upi", upi)
                .apply()

            Toast.makeText(this, "Profile saved", Toast.LENGTH_SHORT).show()

            // अब QR activity खोलो
            startActivity(Intent(this, MyQrActivity::class.java))
            finish()
        }
    }
}
