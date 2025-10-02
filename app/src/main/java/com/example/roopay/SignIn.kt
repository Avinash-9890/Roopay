package com.example.roopay

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SigninActivity : AppCompatActivity() {

    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnSignin: Button
    private lateinit var tvSignup: TextView
    private lateinit var emailPasswordLayout: LinearLayout

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var prefs: SharedPreferences

    private var progressDialog: AlertDialog? = null

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_in)

        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        btnSignin = findViewById(R.id.btnSignin)
        tvSignup = findViewById(R.id.tvSignup)
        emailPasswordLayout = findViewById(R.id.emailPasswordLayout)


        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        prefs = getSharedPreferences("MyPrefs", MODE_PRIVATE)

        // ✅ Check if biometric is enabled
        val useBiometric = prefs.getBoolean("useBiometric", false)
        if (useBiometric) {
            showBiometricPrompt()
        } else {
            showEmailPasswordLogin()
        }

        btnSignin.setOnClickListener { loginUser() }

        tvSignup.setOnClickListener {
            startActivity(Intent(this, SignupActivity::class.java))
        }
    }

    private fun showBiometricPrompt() {
        val biometricPrompt = BiometricPrompt(
            this,
            ContextCompat.getMainExecutor(this),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    // ✅ Success → direct home
                    startActivity(Intent(this@SigninActivity, MainActivity::class.java))
                    finish()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    // ❌ fallback to email/password
                    showEmailPasswordLogin()
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    Toast.makeText(this@SigninActivity, "Biometric failed", Toast.LENGTH_SHORT).show()
                }
            }
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Login with Biometrics")
            .setSubtitle("Use your fingerprint or face")
            // ✅ Face + Fingerprint + PIN/Pattern/Password allow karo
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG
                        or BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    private fun showEmailPasswordLogin() {
        emailPasswordLayout.visibility = View.VISIBLE
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

                            val sharedPref = getSharedPreferences("UserData", MODE_PRIVATE)
                            val editor = sharedPref.edit()
                            editor.putString("username", name)
                            editor.putString("mobile", mobile)
                            editor.apply()

                            // ✅ Email+Password login successful → enable biometric
                            prefs.edit().putBoolean("useBiometric", true).apply()

                            Toast.makeText(this, "Welcome $name!", Toast.LENGTH_LONG).show()

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

