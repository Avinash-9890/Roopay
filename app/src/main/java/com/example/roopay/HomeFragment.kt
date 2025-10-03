package com.example.roopay

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.tbuonomo.viewpagerdotsindicator.WormDotsIndicator

class HomeFragment : Fragment() {

    private lateinit var bannerViewPager: ViewPager2
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var runnable: Runnable

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ✅ Banner setup
        val bannerImages = listOf(
            R.drawable.banner11,
            R.drawable.banner12,
            R.drawable.banner13,
            R.drawable.banner14
        )

        bannerViewPager = view.findViewById(R.id.bannerViewPager)
        bannerViewPager.adapter = BannerAdapter(bannerImages)

        val dotsIndicator = view.findViewById<WormDotsIndicator>(R.id.dotsIndicator)
        dotsIndicator.attachTo(bannerViewPager)

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

        // ✅ Setup Sections
        setupBankingItems(view)
        setupUtilityItems(view)
        setupTravelItems(view)
        setupTopSection(view)

    }

    private fun setupTopSection(root: View) {
        val itemTopup = root.findViewById<View>(R.id.itemTopup)
        itemTopup.findViewById<ImageView>(R.id.iconImage).setImageResource(R.drawable.topup)
        itemTopup.findViewById<TextView>(R.id.iconLabel).text = "Topup"

        val itemPayout = root.findViewById<View>(R.id.itemPayout)
        itemPayout.findViewById<ImageView>(R.id.iconImage).setImageResource(R.drawable.pay)
        itemPayout.findViewById<TextView>(R.id.iconLabel).text = "Payout"

        val itemQr = root.findViewById<View>(R.id.itemQr)
        itemQr.findViewById<ImageView>(R.id.iconImage).setImageResource(R.drawable.myqrcode)
        itemQr.findViewById<TextView>(R.id.iconLabel).text = "QR"

        val itemActivate = root.findViewById<View>(R.id.itemActivate)
        itemActivate.findViewById<ImageView>(R.id.iconImage).setImageResource(R.drawable.activate)
        itemActivate.findViewById<TextView>(R.id.iconLabel).text = "Activate"

        // ✅ Razorpay Payment on Payout Click
        itemPayout.setOnClickListener {
            (activity as? MainActivity)?.openPaymentDialog()

        }


        // ✅ API call for Activation
        itemTopup.setOnClickListener {
            val intent = Intent(requireContext(), ActivationActivity::class.java)
            startActivity(intent)


        }

        // ✅ Activate button click
        itemQr.setOnClickListener {
            val prefs = requireContext().getSharedPreferences("UserData", AppCompatActivity.MODE_PRIVATE)
            val upi = prefs.getString("upi", null)

            if (upi.isNullOrEmpty()) {
                // अगर UPI नहीं है → पहले ProfileActivity खोलो
                val intent = Intent(requireContext(), ProfileActivity::class.java)
                startActivity(intent)
            } else {
                // अगर पहले से UPI है → सीधा MyQrActivity खोलो
                val intent = Intent(requireContext(), MyQrActivity::class.java)
                startActivity(intent)
            }
        }





    }


    private fun setupBankingItems(root: View) {
        val item1 = root.findViewById<View>(R.id.item1)
        item1.findViewById<ImageView>(R.id.iconImage).setImageResource(R.drawable.icon1)
        item1.findViewById<TextView>(R.id.iconLabel).text = "Bank"

        val item2 = root.findViewById<View>(R.id.item2)
        item2.findViewById<ImageView>(R.id.iconImage).setImageResource(R.drawable.icon2)
        item2.findViewById<TextView>(R.id.iconLabel).text = "Transfer"

        val item3 = root.findViewById<View>(R.id.item3)
        item3.findViewById<ImageView>(R.id.iconImage).setImageResource(R.drawable.icon3)
        item3.findViewById<TextView>(R.id.iconLabel).text = "AEPS"

        val item4 = root.findViewById<View>(R.id.item4)
        item4.findViewById<ImageView>(R.id.iconImage).setImageResource(R.drawable.icon4)
        item4.findViewById<TextView>(R.id.iconLabel).text = "Mini Stmt"

        val item5 = root.findViewById<View>(R.id.item5)
        item5.findViewById<ImageView>(R.id.iconImage).setImageResource(R.drawable.icon5)
        item5.findViewById<TextView>(R.id.iconLabel).text = "Balance"

        val item6 = root.findViewById<View>(R.id.item6)
        item6.findViewById<ImageView>(R.id.iconImage).setImageResource(R.drawable.icon6)
        item6.findViewById<TextView>(R.id.iconLabel).text = "PAN Card"

        val item7 = root.findViewById<View>(R.id.item7)
        item7.findViewById<ImageView>(R.id.iconImage).setImageResource(R.drawable.icon7)
        item7.findViewById<TextView>(R.id.iconLabel).text = "Insurance"

        val item8 = root.findViewById<View>(R.id.item8)
        item8.findViewById<ImageView>(R.id.iconImage).setImageResource(R.drawable.icon8)
        item8.findViewById<TextView>(R.id.iconLabel).text = "Loan"
        
    }

    private fun setupUtilityItems(root: View) {
        val util1 = root.findViewById<View>(R.id.util1)
        util1.findViewById<ImageView>(R.id.iconImage).setImageResource(R.drawable.util1)
        util1.findViewById<TextView>(R.id.iconLabel).text = "Recharge"

        val util2 = root.findViewById<View>(R.id.util2)
        util2.findViewById<ImageView>(R.id.iconImage).setImageResource(R.drawable.util2)
        util2.findViewById<TextView>(R.id.iconLabel).text = "Electricity"

        val util3 = root.findViewById<View>(R.id.util3)
        util3.findViewById<ImageView>(R.id.iconImage).setImageResource(R.drawable.util3)
        util3.findViewById<TextView>(R.id.iconLabel).text = "Water"

        val util4 = root.findViewById<View>(R.id.util4)
        util4.findViewById<ImageView>(R.id.iconImage).setImageResource(R.drawable.util4)
        util4.findViewById<TextView>(R.id.iconLabel).text = "Gas"

        val util5 = root.findViewById<View>(R.id.util5)
        util5.findViewById<ImageView>(R.id.iconImage).setImageResource(R.drawable.util5)
        util5.findViewById<TextView>(R.id.iconLabel).text = "DTH"

        val util6 = root.findViewById<View>(R.id.util6)
        util6.findViewById<ImageView>(R.id.iconImage).setImageResource(R.drawable.util6)
        util6.findViewById<TextView>(R.id.iconLabel).text = "FASTag"
    }

    private fun setupTravelItems(root: View) {
        val bus = root.findViewById<View>(R.id.bus)
        bus.findViewById<ImageView>(R.id.iconImage).setImageResource(R.drawable.bus)
        bus.findViewById<TextView>(R.id.iconLabel).text = "Bus Booking"

        val train = root.findViewById<View>(R.id.train)
        train.findViewById<ImageView>(R.id.iconImage).setImageResource(R.drawable.train)
        train.findViewById<TextView>(R.id.iconLabel).text = "Train Booking"

        val flight = root.findViewById<View>(R.id.flight)
        flight.findViewById<ImageView>(R.id.iconImage).setImageResource(R.drawable.airplane)
        flight.findViewById<TextView>(R.id.iconLabel).text = "Flight Booking"

        val hotel = root.findViewById<View>(R.id.hotel)
        hotel.findViewById<ImageView>(R.id.iconImage).setImageResource(R.drawable.hotel)
        hotel.findViewById<TextView>(R.id.iconLabel).text = "Hotel Booking"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacks(runnable)
    }
}
