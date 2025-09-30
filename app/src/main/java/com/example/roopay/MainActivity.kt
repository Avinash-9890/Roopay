package com.example.roopay

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.util.Log
import android.view.View
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.razorpay.Checkout
import com.razorpay.PaymentResultListener
import com.tbuonomo.viewpagerdotsindicator.WormDotsIndicator
import org.json.JSONObject

class MainActivity : AppCompatActivity(), PaymentResultListener {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var bannerViewPager: ViewPager2
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var runnable: Runnable

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // ✅ Preload Razorpay
        Checkout.preload(applicationContext)

        // ✅ Drawer + Button Setup
        drawerLayout = findViewById(R.id.drawerLayout)
        val navigationView = findViewById<NavigationView>(R.id.navigationView)
        val btnDrawer = findViewById<ImageButton>(R.id.btn1)

        btnDrawer.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        // ✅ Header View - SharedPreferences se data set karo
        val headerView = navigationView.getHeaderView(0)
        val tvUserName = headerView.findViewById<TextView>(R.id.username)
        val tvMobileNumber = headerView.findViewById<TextView>(R.id.usermobile)

        val sharedPref = getSharedPreferences("UserData", MODE_PRIVATE)
        val savedName = sharedPref.getString("username", "Guest")
        val savedMobile = sharedPref.getString("mobile", "")

        tvUserName.text = savedName
        tvMobileNumber.text = savedMobile

        // ✅ Navigation Drawer item clicks (LOGOUT INCLUDED)
        navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.logout -> {
                    showLogoutDialog()
                    drawerLayout.closeDrawer(GravityCompat.START)
                    true
                }
                else -> false
            }
        }

        // ✅ Banner Images
        val bannerImages = listOf(
            R.drawable.banner11,
            R.drawable.banner12,
            R.drawable.banner13,
            R.drawable.banner14
        )

        // ✅ ViewPager2 + Adapter
        bannerViewPager = findViewById(R.id.bannerViewPager)
        bannerViewPager.adapter = BannerAdapter(bannerImages)

        // ✅ Dots Indicator
        val dotsIndicator = findViewById<WormDotsIndicator>(R.id.dotsIndicator)
        dotsIndicator.attachTo(bannerViewPager)

        // ✅ Auto Slide ViewPager2
        runnable = object : Runnable {
            override fun run() {
                val nextItem =
                    if (bannerViewPager.currentItem < bannerImages.size - 1) bannerViewPager.currentItem + 1
                    else 0
                bannerViewPager.setCurrentItem(nextItem, true)
                handler.postDelayed(this, 3000)
            }
        }
        handler.postDelayed(runnable, 3000)

        setupWalletItems()
        setupBankingItems()
        setupUtilityItems()
        setupTravelItems()
    }

    /** ✅ Custom Logout Dialog */
    @SuppressLint("MissingInflatedId")
    private fun showLogoutDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialoge_logout, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        // ✅ Handle buttons
        dialogView.findViewById<Button>(R.id.btnYes).setOnClickListener {
            dialog.dismiss()
            logoutUser()
        }

        dialogView.findViewById<Button>(R.id.btnNo).setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    /** ✅ Clear Data + Redirect to SigninActivity */
    private fun logoutUser() {
        val sharedPref = getSharedPreferences("UserData", MODE_PRIVATE)
        sharedPref.edit().clear().apply()

        FirebaseAuth.getInstance().signOut()

        val intent = Intent(this, SigninActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    /** ✅ Wallet Items Setup + Razorpay Payment on Payout */
    private fun setupWalletItems() {
        val itemTopup = findViewById<View>(R.id.itemTopup)
        val itemPayout = findViewById<View>(R.id.itemPayout)
        val itemQr = findViewById<View>(R.id.itemQr)
        val itemActivate = findViewById<View>(R.id.itemActivate)

        itemTopup.findViewById<ImageView>(R.id.iconImage).setImageResource(R.drawable.topup)
        itemTopup.findViewById<TextView>(R.id.iconLabel).text = "Topup"

        itemPayout.findViewById<ImageView>(R.id.iconImage).setImageResource(R.drawable.pay)
        itemPayout.findViewById<TextView>(R.id.iconLabel).text = "Payout"

        itemQr.findViewById<ImageView>(R.id.iconImage).setImageResource(R.drawable.myqrcode)
        itemQr.findViewById<TextView>(R.id.iconLabel).text = "My QR"

        itemActivate.findViewById<ImageView>(R.id.iconImage).setImageResource(R.drawable.activate)
        itemActivate.findViewById<TextView>(R.id.iconLabel).text = "Activate"

        itemActivate.setOnClickListener {
            callActivationApi()
        }

        // ✅ Razorpay Payment on Payout Click
        itemPayout.setOnClickListener {
            showAmountDialogAndPay()
        }
    }

    /** ✅ Show Dialog to Enter Amount */
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
                    Log.e("RAZORPAY", "❌ Invalid amount entered: $amount")
                    Toast.makeText(this, "Enter a valid amount", Toast.LENGTH_SHORT).show()
                } else {
                    Log.d("RAZORPAY", "✅ Amount entered: $amount")
                    startRazorpayPayment(amount)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    /** ✅ Start Razorpay Checkout */
    private fun startRazorpayPayment(amountInRupees: Double) {
        Log.d("RAZORPAY", "🟢 Starting Razorpay Payment...")

        val checkout = Checkout()

        // ❗ FIXED: You must only use KeyID here, not KeySecret
        checkout.setKeyID("rzp_test_RNH706CURjoNRd")
        Log.d("RAZORPAY", "🔑 KeyID set: rzp_test_RNH706CURjoNRd")

        val amountInPaise = (amountInRupees * 100).toInt()
        Log.d("RAZORPAY", "💰 Amount in paise: $amountInPaise")

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

            Log.d("RAZORPAY", "📦 Checkout Options JSON: $options")

            checkout.open(this, options)
            Log.d("RAZORPAY", "🚀 Checkout opened successfully!")
        } catch (e: Exception) {
            Log.e("RAZORPAY", "❌ Error starting Razorpay Checkout", e)
            Toast.makeText(this, "Payment Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    /** ✅ Razorpay Callbacks */
    override fun onPaymentSuccess(razorpayPaymentID: String?) {
        Log.d("RAZORPAY", "✅ Payment Success | PaymentID: $razorpayPaymentID")
        AlertDialog.Builder(this)
            .setTitle("Payment Successful ✅")
            .setMessage("Payment ID: $razorpayPaymentID")
            .setPositiveButton("OK", null)
            .show()
    }

    override fun onPaymentError(code: Int, response: String?) {
        Log.e("RAZORPAY", "❌ Payment Failed | Code: $code | Response: $response")
        AlertDialog.Builder(this)
            .setTitle("Payment Failed ❌")
            .setMessage("Error: $response")
            .setPositiveButton("OK", null)
            .show()
    }

    private fun callActivationApi() {
        val memberId = "9876543210"
        val apiPassword = "1234"
        val apiPin = "1234"
        val number = "998988200"

        val call = RetrofitClient.instance.getActivationData(memberId, apiPassword, apiPin, number)

        call.enqueue(object : retrofit2.Callback<ModelClass> {
            override fun onResponse(
                call: retrofit2.Call<ModelClass>,
                response: retrofit2.Response<ModelClass>
            ) {
                if (response.isSuccessful && response.body() != null) {
                    val data = response.body()!!

                    // Convert full response to string (use Gson if available)
                    val gson = com.google.gson.GsonBuilder().setPrettyPrinting().create()
                    val fullJson = gson.toJson(data)

                    Log.d("API_RESPONSE", "✅ Full Response:\n$fullJson")

                    // ✅ Open ApiResponseActivity
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

    private fun setupBankingItems() {
        val item1 = findViewById<View>(R.id.item1)
        val item2 = findViewById<View>(R.id.item2)
        val item3 = findViewById<View>(R.id.item3)
        val item4 = findViewById<View>(R.id.item4)
        val item5 = findViewById<View>(R.id.item5)
        val item6 = findViewById<View>(R.id.item6)
        val item7 = findViewById<View>(R.id.item7)
        val item8 = findViewById<View>(R.id.item8)

        item1.findViewById<ImageView>(R.id.iconImage).setImageResource(R.drawable.icon1)
        item1.findViewById<TextView>(R.id.iconLabel).text = "Bank"

        item2.findViewById<ImageView>(R.id.iconImage).setImageResource(R.drawable.icon2)
        item2.findViewById<TextView>(R.id.iconLabel).text = "Transfer"

        item3.findViewById<ImageView>(R.id.iconImage).setImageResource(R.drawable.icon3)
        item3.findViewById<TextView>(R.id.iconLabel).text = "AEPS"

        item4.findViewById<ImageView>(R.id.iconImage).setImageResource(R.drawable.icon4)
        item4.findViewById<TextView>(R.id.iconLabel).text = "Mini Stmt"

        item5.findViewById<ImageView>(R.id.iconImage).setImageResource(R.drawable.icon5)
        item5.findViewById<TextView>(R.id.iconLabel).text = "Balance"

        item6.findViewById<ImageView>(R.id.iconImage).setImageResource(R.drawable.icon6)
        item6.findViewById<TextView>(R.id.iconLabel).text = "PAN Card"

        item7.findViewById<ImageView>(R.id.iconImage).setImageResource(R.drawable.icon7)
        item7.findViewById<TextView>(R.id.iconLabel).text = "Insurance"

        item8.findViewById<ImageView>(R.id.iconImage).setImageResource(R.drawable.icon8)
        item8.findViewById<TextView>(R.id.iconLabel).text = "Loan"
    }

    private fun setupUtilityItems() {
        val util1 = findViewById<View>(R.id.util1)
        val util2 = findViewById<View>(R.id.util2)
        val util3 = findViewById<View>(R.id.util3)
        val util4 = findViewById<View>(R.id.util4)
        val util5 = findViewById<View>(R.id.util5)
        val util6 = findViewById<View>(R.id.util6)

        util1.findViewById<ImageView>(R.id.iconImage).setImageResource(R.drawable.util1)
        util1.findViewById<TextView>(R.id.iconLabel).text = "Recharge"

        util2.findViewById<ImageView>(R.id.iconImage).setImageResource(R.drawable.util2)
        util2.findViewById<TextView>(R.id.iconLabel).text = "Electricity"

        util3.findViewById<ImageView>(R.id.iconImage).setImageResource(R.drawable.util3)
        util3.findViewById<TextView>(R.id.iconLabel).text = "Water"

        util4.findViewById<ImageView>(R.id.iconImage).setImageResource(R.drawable.util4)
        util4.findViewById<TextView>(R.id.iconLabel).text = "Gas"

        util5.findViewById<ImageView>(R.id.iconImage).setImageResource(R.drawable.util5)
        util5.findViewById<TextView>(R.id.iconLabel).text = "DTH"

        util6.findViewById<ImageView>(R.id.iconImage).setImageResource(R.drawable.util6)
        util6.findViewById<TextView>(R.id.iconLabel).text = "FASTag"
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(runnable)
    }

    private fun setupTravelItems() {
        val itemBus = findViewById<View>(R.id.bus)
        val itemTrain = findViewById<View>(R.id.train)
        val itemFlight = findViewById<View>(R.id.flight)
        val itemHotel = findViewById<View>(R.id.hotel)

        itemBus.findViewById<ImageView>(R.id.iconImage).setImageResource(R.drawable.bus)
        itemBus.findViewById<TextView>(R.id.iconLabel).text = "Bus Booking"

        itemTrain.findViewById<ImageView>(R.id.iconImage).setImageResource(R.drawable.train)
        itemTrain.findViewById<TextView>(R.id.iconLabel).text = "Train Booking"

        itemFlight.findViewById<ImageView>(R.id.iconImage).setImageResource(R.drawable.airplane)
        itemFlight.findViewById<TextView>(R.id.iconLabel).text = "Flight Booking"

        itemHotel.findViewById<ImageView>(R.id.iconImage).setImageResource(R.drawable.hotel)
        itemHotel.findViewById<TextView>(R.id.iconLabel).text = "Hotel Booking"
    }
}
