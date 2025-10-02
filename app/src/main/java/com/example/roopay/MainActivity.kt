package com.example.roopay

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.razorpay.Checkout
import com.razorpay.PaymentResultListener
import org.json.JSONObject
import androidx.appcompat.widget.Toolbar
import androidx.biometric.BiometricManager
import android.provider.Settings
import android.view.View
import retrofit2.Retrofit
import com.tbuonomo.viewpagerdotsindicator.WormDotsIndicator // (if you use it elsewhere)

class MainActivity : AppCompatActivity(), PaymentResultListener {

    private lateinit var drawerLayout: DrawerLayout

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // Razorpay preload
        Checkout.preload(applicationContext)

        // Drawer + navigation view
        drawerLayout = findViewById(R.id.drawerLayout)
        val navigationView = findViewById<NavigationView>(R.id.navigationView)
        val btnDrawer = findViewById<ImageButton>(R.id.btn1)

        btnDrawer.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        // Toolbar + bottom nav
        val bottomnav = findViewById<BottomNavigationView>(R.id.bottomnav)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        // Default fragment
        if (savedInstanceState == null) {
            loadFragment(HomeFragment())
        }

        bottomnav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    loadFragment(HomeFragment())
                    true
                }
                R.id.nav_wallet -> {
                    loadFragment(SearchFragment())
                    true
                }
                R.id.nav_offers -> {
                    loadFragment(PersonFragments())
                    true
                }
                R.id.nav_profile -> {
                    loadFragment(HistoryFragment())
                    true
                }
                else -> false
            }
        }

        // --- Biometric Switch in Drawer (safe handling) ---
        try {
            val menuItem = navigationView.menu.findItem(R.id.nav_biometric)
            val actionV: View? = menuItem.actionView
            val switchBiometric: Switch? = actionV?.findViewById(R.id.switchBiometric)

            if (switchBiometric == null) {
                Log.w("MainActivity", "Biometric switch view is null. Make sure menu item has actionLayout set.")
            } else {
                val prefs = getSharedPreferences("MyPrefs", MODE_PRIVATE)
                // set initial state first (before listener)
                switchBiometric.isChecked = prefs.getBoolean("useBiometric", false)

                switchBiometric.setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) {
                        val biometricManager = BiometricManager.from(this)
                        when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)) {
                            BiometricManager.BIOMETRIC_SUCCESS -> {
                                prefs.edit().putBoolean("useBiometric", true).apply()
                                Toast.makeText(this, "Biometric enabled", Toast.LENGTH_SHORT).show()
                            }
                            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                                Toast.makeText(this, "Please set up biometric in settings", Toast.LENGTH_LONG).show()
                                // Open biometric enroll screen
                                val enrollIntent = Intent(Settings.ACTION_BIOMETRIC_ENROLL).apply {
                                    putExtra(
                                        Settings.EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED,
                                        BiometricManager.Authenticators.BIOMETRIC_STRONG
                                    )
                                }
                                startActivity(enrollIntent)
                                // keep switch off until user actually enrolls
                                switchBiometric.isChecked = false
                            }
                            else -> {
                                Toast.makeText(this, "Biometric not supported on this device", Toast.LENGTH_SHORT).show()
                                switchBiometric.isChecked = false
                            }
                        }
                    } else {
                        // turned off
                        getSharedPreferences("MyPrefs", MODE_PRIVATE).edit()
                            .putBoolean("useBiometric", false).apply()
                        Toast.makeText(this, "Biometric disabled", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error setting up biometric switch: ${e.localizedMessage}", e)
        }

        // Drawer header (username/mobile)
        val headerView = navigationView.getHeaderView(0)
        val tvUserName = headerView.findViewById<TextView>(R.id.username)
        val tvMobileNumber = headerView.findViewById<TextView>(R.id.usermobile)

        val sharedPref = getSharedPreferences("UserData", MODE_PRIVATE)
        val savedName = sharedPref.getString("username", "Guest")
        val savedMobile = sharedPref.getString("mobile", "")

        tvUserName.text = savedName
        tvMobileNumber.text = savedMobile

        // Drawer item clicks
        navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.logout -> {
                    showLogoutDialog()
                    drawerLayout.closeDrawer(GravityCompat.START)
                    true
                }
                else -> {
                    drawerLayout.closeDrawer(GravityCompat.START)
                    true
                }
            }
        }
    }

    /** Fragment loader utility */
    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.toolbar_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_menu -> {
                Toast.makeText(this, "Notifications clicked", Toast.LENGTH_SHORT).show()
                true
            }
            R.id.action_notification -> {
                Toast.makeText(this, "Help clicked", Toast.LENGTH_SHORT).show()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    /** Logout dialog */
    private fun showLogoutDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialoge_logout, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        dialogView.findViewById<Button>(R.id.btnYes).setOnClickListener {
            dialog.dismiss()
            logoutUser()
        }
        dialogView.findViewById<Button>(R.id.btnNo).setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun logoutUser() {
        // clear shared prefs (be careful: you might want to keep some data)
        val sharedPref = getSharedPreferences("UserData", MODE_PRIVATE)
        sharedPref.edit().clear().apply()

        FirebaseAuth.getInstance().signOut()

        val intent = Intent(this, SigninActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    /** Razorpay helpers (unchanged) */
    fun openPaymentDialog() {
        showAmountDialogAndPay()
    }

    fun startPaymentFromFragment(amount: Double) {
        startRazorpayPayment(amount)
    }

    private fun showAmountDialogAndPay() {
        val input = EditText(this)
        input.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        input.hint = "Enter amount (INR)"

        AlertDialog.Builder(this)
            .setTitle("Enter Amount")
            .setView(input)
            .setPositiveButton("Pay") { _, _ ->
                val amount = input.text.toString().trim().toDoubleOrNull()
                if (amount == null || amount <= 0) {
                    Toast.makeText(this, "Enter a valid amount", Toast.LENGTH_SHORT).show()
                } else {
                    startRazorpayPayment(amount)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun startRazorpayPayment(amountInRupees: Double) {
        val checkout = Checkout()
        // Use your actual key (don't expose production key in client in real app)
        checkout.setKeyID("rzp_test_RNH706CURjoNRd")
        val amountInPaise = (amountInRupees * 100).toInt()

        try {
            val options = JSONObject()
            options.put("name", "Roopay")
            options.put("description", "Wallet Payout")
            options.put("currency", "INR")
            options.put("amount", amountInPaise)

            val prefill = JSONObject()
            prefill.put("email", "user@example.com")
            prefill.put("contact", "9999999999")
            options.put("prefill", prefill)

            checkout.open(this, options)
        } catch (e: Exception) {
            Toast.makeText(this, "Payment Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onPaymentSuccess(razorpayPaymentID: String?) {
        AlertDialog.Builder(this)
            .setTitle("Payment Successful ✅")
            .setMessage("Payment ID: $razorpayPaymentID")
            .setPositiveButton("OK", null)
            .show()
    }

    override fun onPaymentError(code: Int, response: String?) {
        AlertDialog.Builder(this)
            .setTitle("Payment Failed ❌")
            .setMessage("Error: $response")
            .setPositiveButton("OK", null)
            .show()
    }

    override fun onDestroy() {
        super.onDestroy()
        // nothing to remove here (banner runnable lives inside HomeFragment)
    }

    // Activation dialog and API call (kept same as your earlier code)
    private fun showActivationDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_activation, null)

        val etMemberId = dialogView.findViewById<EditText>(R.id.etmemberId)
        val etPassword = dialogView.findViewById<EditText>(R.id.etPassword)
        val etPin = dialogView.findViewById<EditText>(R.id.etpin)
        val etNumber = dialogView.findViewById<EditText>(R.id.etnumber)

        AlertDialog.Builder(this)
            .setTitle("Enter Activation Details")
            .setView(dialogView)
            .setPositiveButton("Submit") { _, _ ->
                val memberId = etMemberId.text.toString().trim()
                val apiPassword = etPassword.text.toString().trim()
                val apiPin = etPin.text.toString().trim()
                val number = etNumber.text.toString().trim()

                if (memberId.isEmpty() || apiPassword.isEmpty() || apiPin.isEmpty() || number.isEmpty()) {
                    Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                } else {
                    callActivationApi(memberId, apiPassword, apiPin, number)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun callActivationApi(memberId: String, apiPassword: String, apiPin: String, number: String) {
        val requestData = ModelClass(memberId, apiPassword, apiPin, number)

        val call = RetrofitClient.instance.postActivationData(requestData)

        call.enqueue(object : retrofit2.Callback<ModelClass> {
            override fun onResponse(
                call: retrofit2.Call<ModelClass>,
                response: retrofit2.Response<ModelClass>
            ) {
                if (response.isSuccessful && response.body() != null) {
                    val data = response.body()!!
                    Log.d("API_RESPONSE", "✅ Success: $data")

                    val gson = com.google.gson.GsonBuilder().setPrettyPrinting().create()
                    val fullJson = gson.toJson(data)

                    val intent = Intent(this@MainActivity, ApiResponseActivity::class.java)
                    intent.putExtra("api_response", fullJson)
                    startActivity(intent)

                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e("API_RESPONSE", "❌ Server Error: ${response.code()} | Body: $errorBody")

                    val intent = Intent(this@MainActivity, ApiResponseActivity::class.java)
                    intent.putExtra("api_response", "Server Error: ${response.code()}\n\n$errorBody")
                    startActivity(intent)
                }
            }

            override fun onFailure(call: retrofit2.Call<ModelClass>, t: Throwable) {
                Log.e("API_RESPONSE", "❌ Network Error", t)
                val intent = Intent(this@MainActivity, ApiResponseActivity::class.java)
                intent.putExtra("api_response", "Network Error: ${t.localizedMessage ?: "Unknown error"}")
                startActivity(intent)
            }
        })
    }
}
