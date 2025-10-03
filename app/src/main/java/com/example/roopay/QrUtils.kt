package com.example.roopay

import android.graphics.Bitmap
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder

object QRUtils {
    fun generateQRCode(text: String, size: Int = 800): Bitmap? {
        return try {
            val barcodeEncoder = BarcodeEncoder()
            barcodeEncoder.encodeBitmap(text, BarcodeFormat.QR_CODE, size, size)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
