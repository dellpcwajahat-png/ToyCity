package com.example.toycity.utils

import android.content.Context
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage

object BarcodeScanner {
    fun scanFromImage(
        context: Context,
        imageUri: android.net.Uri,
        onSuccess: (String) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        try {
            val image = InputImage.fromFilePath(context, imageUri)
            val options = BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
                .build()
            val scanner = BarcodeScanning.getClient(options)

            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    if (barcodes.isNotEmpty()) {
                        onSuccess(barcodes[0].rawValue ?: "")
                    } else {
                        onFailure(Exception("No barcode found in image"))
                    }
                }
                .addOnFailureListener {
                    onFailure(it)
                }
        } catch (e: Exception) {
            onFailure(e)
        }
    }
}
