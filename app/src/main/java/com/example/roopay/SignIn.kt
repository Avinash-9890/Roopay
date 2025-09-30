package com.example.roopay

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SigninActivity : AppCompatActivity() {

    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnSignin: Button
    private lateinit var tvSignup: TextView

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    // ✅ Progress Dialog
    private var progressDialog: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_in)

        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        btnSignin = findViewById(R.id.btnSignin)
        tvSignup = findViewById(R.id.tvSignup)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        btnSignin.setOnClickListener { loginUser() }

        tvSignup.setOnClickListener {
            startActivity(Intent(this, SignupActivity::class.java))
        }
    }

    private fun loginUser() {
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show()
            return
        }

        showProgressDialog()

        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                val userId = result.user?.uid ?: return@addOnSuccessListener

                firestore.collection("users").document(userId).get()
                    .addOnSuccessListener { document ->
                        hideProgressDialog()

                        if (document.exists()) {
                            val name = document.getString("name") ?: ""
                            val mobile = document.getString("mobile") ?: ""

                            // ✅ Save data in SharedPreferences
                            val sharedPref = getSharedPreferences("UserData", MODE_PRIVATE)
                            val editor = sharedPref.edit()
                            editor.putString("username", name)
                            editor.putString("mobile", mobile)
                            editor.apply()

                            Toast.makeText(this, "Welcome $name!", Toast.LENGTH_LONG).show()

                            // ✅ Navigate to MainActivity
                            startActivity(Intent(this, MainActivity::class.java))
                            finish()
                        } else {
                            Toast.makeText(this, "User data not found", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .addOnFailureListener { e ->
                        hideProgressDialog()
                        Toast.makeText(this, "Failed to fetch user data: ${e.message}", Toast.LENGTH_LONG).show()
                    }
            }
            .addOnFailureListener { e ->
                hideProgressDialog()
                Toast.makeText(this, "Login failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun showProgressDialog() {
        if (progressDialog == null) {
            val builder = AlertDialog.Builder(this)
            val view = LayoutInflater.from(this).inflate(R.layout.dialog_progress, null)
            builder.setView(view)
            builder.setCancelable(false)
            progressDialog = builder.create()
        }
        progressDialog?.show()
    }

    private fun hideProgressDialog() {
        progressDialog?.dismiss()
    }
}
