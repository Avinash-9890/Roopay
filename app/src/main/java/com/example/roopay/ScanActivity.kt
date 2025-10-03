package com.example.roopay

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.zxing.integration.android.IntentIntegrator
import org.json.JSONObject

class ScanActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Immediately start ZXing scanner
        IntentIntegrator(this)
            .setDesiredBarcodeFormats(IntentIntegrator.QR_CODE)
            .setPrompt("Scan QR")
            .setBeepEnabled(true)
            .setCameraId(0)
            .initiateScan()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (result != null) {
            if (result.contents == null) {
                Toast.makeText(this, "Scan cancelled", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                handleScannedText(result.contents)
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun handleScannedText(text: String) {
        // Try JSON parse
        try {
            val json = JSONObject(text)
            if (json.optString("type") == "roopay_user") {
                val name = json.optString("name")
                val upi = json.optString("upi")
                showUserDialog(name, upi)
                return
            }
        } catch (e: Exception) {
            // not JSON
        }

        // If it's a UPI URI or contains pa= then treat as UPI direct
        if (text.startsWith("upi://") || text.contains("pa=")) {
            showUpiDialog(text)
            return
        }

        // Fallback
        AlertDialog.Builder(this)
            .setTitle("Scanned")
            .setMessage(text)
            .setPositiveButton("OK") { _, _ -> finish() }
            .show()
    }

    private fun showUserDialog(name: String, upi: String) {
        AlertDialog.Builder(this)
            .setTitle("Pay $name")
            .setMessage("UPI: $upi")
            .setPositiveButton("Pay") { _, _ ->
                launchUpiPayment(upi, name, "Payment via Roopay")
            }
            .setNegativeButton("Cancel") { _, _ -> finish() }
            .show()
    }

    private fun showUpiDialog(upiUri: String) {
        AlertDialog.Builder(this)
            .setTitle("UPI")
            .setMessage(upiUri)
            .setPositiveButton("Open") { _, _ ->
                try {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(upiUri)))
                } catch (e: Exception) {
                    Toast.makeText(this, "No app to handle UPI", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel") { _, _ -> finish() }
            .show()
    }

    private fun launchUpiPayment(upiId: String, name: String, note: String) {
        val uri = Uri.parse("upi://pay").buildUpon()
            .appendQueryParameter("pa", upiId)
            .appendQueryParameter("pn", name)
            .appendQueryParameter("tn", note)
            .appendQueryParameter("am", "")
            .appendQueryParameter("cu", "INR")
            .build()

        val intent = Intent(Intent.ACTION_VIEW, uri)
        try {
            startActivity(Intent.createChooser(intent, "Pay with"))
        } catch (e: Exception) {
            Toast.makeText(this, "No UPI app found", Toast.LENGTH_SHORT).show()
        } finally {
            finish()
        }
    }
}
