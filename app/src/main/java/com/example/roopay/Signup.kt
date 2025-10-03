package com.example.roopay

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.widget.*
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.io.IOException
import java.util.*

class SignupActivity : AppCompatActivity() {

    private lateinit var etName: EditText
    private lateinit var etEmail: EditText
    private lateinit var etMobile: EditText
    private lateinit var etAddress: EditText
    private lateinit var etUsername: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnSignup: Button

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback

    companion object {
        private const val TAG = "SignupActivity"
        private const val LOCATION_PERMISSION_CODE = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        Log.d(TAG, "🚀 onCreate called")

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        initViews()

        btnSignup.setOnClickListener {
            Log.d(TAG, "🔘 Signup button clicked")
            registerUser()
        }

        val txtSignin = findViewById<TextView>(R.id.txtSignin)
        txtSignin.setOnClickListener {
            startActivity(Intent(this, SigninActivity::class.java))
        }


        setupLocationUpdates()
        checkLocationPermissionAndStart()
    }

    private fun initViews() {
        Log.d(TAG, "🔧 Initializing views...")
        etName = findViewById(R.id.etName)
        etEmail = findViewById(R.id.etEmail)
        etMobile = findViewById(R.id.etMobile)
        etAddress = findViewById(R.id.etAddress)
        etUsername = findViewById(R.id.etUsername)
        etPassword = findViewById(R.id.etPassword)
        btnSignup = findViewById(R.id.btnSignup)
    }

    private fun setupLocationUpdates() {
        Log.d(TAG, "📍 Setting up location updates...")

        locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
            .setMinUpdateIntervalMillis(2000)
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                Log.d(TAG, "📡 onLocationResult called")
                val location = locationResult.lastLocation ?: run {
                    Log.d(TAG, "⚠️ Location is null in callback")
                    return
                }
                resolveAddress(location.latitude, location.longitude)
            }
        }
    }

    private fun resolveAddress(lat: Double, lng: Double) {
        try {
            val geocoder = Geocoder(this, Locale.getDefault())
            val addresses = geocoder.getFromLocation(lat, lng, 1)
            if (!addresses.isNullOrEmpty()) {
                val fullAddress = addresses[0].getAddressLine(0)
                etAddress.setText(fullAddress)

                Log.d(TAG, "✅ Location: $lat, $lng")
                Log.d(TAG, "✅ Auto-filled Address: $fullAddress")

                // stop updates after getting address once
                fusedLocationClient.removeLocationUpdates(locationCallback)
                Log.d(TAG, "🛑 Location updates stopped after first fetch")
            } else {
                Log.d(TAG, "⚠️ Geocoder returned empty list")
            }
        } catch (e: IOException) {
            Log.e(TAG, "❌ Error fetching address: ${e.message}", e)
        }
    }

    private fun checkLocationPermissionAndStart() {
        Log.d(TAG, "🔎 Checking location permissions...")
        if (ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.d(TAG, "⚠️ Permission not granted, requesting...")
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                LOCATION_PERMISSION_CODE
            )
        } else {
            Log.d(TAG, "✅ Permission already granted, starting location fetch...")
            getUserLocation()
        }
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    private fun getUserLocation() {
        Log.d(TAG, "🚀 Trying last known location first...")

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                Log.d(TAG, "📍 Last known location found: lat=${location.latitude}, lng=${location.longitude}")
                resolveAddress(location.latitude, location.longitude)
            } else {
                Log.d(TAG, "⚠️ Last known location is null → requesting fresh location update")

                val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000)
                    .setMaxUpdates(1) // only once
                    .build()

                fusedLocationClient.requestLocationUpdates(
                    request,
                    object : LocationCallback() {
                        override fun onLocationResult(result: LocationResult) {
                            val freshLoc = result.lastLocation
                            if (freshLoc != null) {
                                Log.d(TAG, "✅ Fresh location received: lat=${freshLoc.latitude}, lng=${freshLoc.longitude}")
                                resolveAddress(freshLoc.latitude, freshLoc.longitude)
                            } else {
                                Log.d(TAG, "❌ Fresh location also NULL!")
                            }
                            fusedLocationClient.removeLocationUpdates(this)
                        }
                    },
                    mainLooper
                )
            }
        }.addOnFailureListener { e ->
            Log.e(TAG, "🚨 Failed to fetch last known location: ${e.message}")
        }
    }


    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    private fun startLocationUpdates() {
        Log.d(TAG, "🚀 Requesting location updates...")

        // 🔹 try last known location first
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                Log.d(TAG, "📍 Last known location: ${location.latitude}, ${location.longitude}")
                resolveAddress(location.latitude, location.longitude)
            } else {
                Log.d(TAG, "⚠️ Last known location is null, waiting for updates...")
            }
        }

        // 🔹 then request new updates
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, mainLooper)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_CODE &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            Log.d(TAG, "✅ Permission granted by user, starting location updates")
            startLocationUpdates()
        } else {
            Log.d(TAG, "❌ Location permission denied by user")
        }
    }

    private fun registerUser() {
        val name = etName.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val mobile = etMobile.text.toString().trim()
        val address = etAddress.text.toString().trim()
        val username = etUsername.text.toString().trim()
        val password = etPassword.text.toString().trim()

        Log.d(TAG, "📝 Register user with data: name=$name, email=$email, mobile=$mobile, address=$address, username=$username")

        // 🔹 Validation
        when {
            name.isEmpty() -> { etName.error = "Name required"; etName.requestFocus(); Log.d(TAG, "⚠️ Name empty"); return }
            email.isEmpty() -> { etEmail.error = "Email required"; etEmail.requestFocus(); Log.d(TAG, "⚠️ Email empty"); return }
            !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> { etEmail.error = "Invalid email"; etEmail.requestFocus(); Log.d(TAG, "⚠️ Invalid email format"); return }
            mobile.isEmpty() -> { etMobile.error = "Mobile required"; etMobile.requestFocus(); Log.d(TAG, "⚠️ Mobile empty"); return }
            mobile.length != 10 -> { etMobile.error = "Enter valid 10-digit mobile"; etMobile.requestFocus(); Log.d(TAG, "⚠️ Invalid mobile length"); return }
            username.isEmpty() -> { etUsername.error = "Username required"; etUsername.requestFocus(); Log.d(TAG, "⚠️ Username empty"); return }
            password.isEmpty() -> { etPassword.error = "Password required"; etPassword.requestFocus(); Log.d(TAG, "⚠️ Password empty"); return }
            password.length < 6 -> { etPassword.error = "Password must be at least 6 characters"; etPassword.requestFocus(); Log.d(TAG, "⚠️ Password too short"); return }
        }

        Log.d(TAG, "✅ Validation passed, creating user in Firebase...")

        // 🔹 Firebase Signup
        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                val userId = result.user?.uid ?: return@addOnSuccessListener
                Log.d(TAG, "✅ Firebase signup success, userId=$userId")
                saveUserToFirestore(userId, name, email, mobile, address, username)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "❌ Signup failed: ${e.message}", e)
                Toast.makeText(this, "❌ Signup failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }



    private fun saveUserToFirestore(
        userId: String,
        name: String,
        email: String,
        mobile: String,
        address: String,
        username: String
    ) {
        Log.d(TAG, "💾 Saving user data to Firestore for userId=$userId")

        val user = hashMapOf(
            "name" to name,
            "email" to email,
            "mobile" to mobile,
            "address" to address,
            "username" to username
        )

        firestore.collection("users").document(userId)
            .set(user)
            .addOnSuccessListener {
                Log.d(TAG, "✅ User saved to Firestore successfully")
                Toast.makeText(this, "✅ User registered successfully!", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, SigninActivity::class.java))
                finish()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "❌ Failed to save user data: ${e.message}", e)
                Toast.makeText(this, "Failed to save user data: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }
}
