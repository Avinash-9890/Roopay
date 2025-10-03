package com.example.roopay

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

class MyQrActivity : AppCompatActivity() {

    private lateinit var ivQr: ImageView
    private lateinit var btnShare: Button
    private lateinit var btnEditProfile: Button
    private var currentBitmap: Bitmap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_qr)

        ivQr = findViewById(R.id.ivQr)
        btnShare = findViewById(R.id.btnShareQr)
        btnEditProfile = findViewById(R.id.btnEditProfile)

        val prefs = getSharedPreferences("UserData", MODE_PRIVATE)
        val name = prefs.getString("username", null)
        val upi = prefs.getString("upi", null)
        val uid = prefs.getString("uid", "unknown_uid") ?: "unknown_uid"

        // 🔴 Important Fix → अगर पहली बार profile empty है तो QR मत दिखाओ
        if (name.isNullOrEmpty() || upi.isNullOrEmpty()) {
            // QR hide करो
            ivQr.setImageBitmap(null)
            ivQr.setImageDrawable(null)
            Toast.makeText(this, "No profile found. Please set Name & UPI first.", Toast.LENGTH_LONG).show()

            // सीधे ProfileActivity खोलो
            startActivity(Intent(this, ProfileActivity::class.java))
            finish()
            return
        }

        // ✅ अब QR सिर्फ तब बनेगा जब profile भरा हो
        val json = JSONObject().apply {
            put("type", "roopay_user")
            put("uid", uid)
            put("name", name)
            put("upi", upi)
        }

        val payload = json.toString()
        val bmp = QRUtils.generateQRCode(payload, 900)
        currentBitmap = bmp
        bmp?.let { ivQr.setImageBitmap(it) }

        btnShare.setOnClickListener {
            bmp?.let { shareBitmap(it) }
                ?: Toast.makeText(this, "QR not generated", Toast.LENGTH_SHORT).show()
        }

        btnEditProfile.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
            finish()
        }
    }

    private fun shareBitmap(bitmap: Bitmap) {
        try {
            val cachePath = File(cacheDir, "images")
            cachePath.mkdirs()
            val file = File(cachePath, "qr.png")
            val fos = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
            fos.flush()
            fos.close()

            val contentUri: Uri =
                FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
            val share = Intent(Intent.ACTION_SEND).apply {
                type = "image/*"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(share, "Share QR"))
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Share failed: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }
}
