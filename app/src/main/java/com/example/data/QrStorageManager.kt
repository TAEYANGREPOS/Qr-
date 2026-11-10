package com.example.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class QrStorageManager(private val context: Context) {

    private val qrDir: File
        get() {
            val dir = File(context.filesDir, "qr_codes")
            if (!dir.exists()) {
                dir.mkdirs()
            }
            return dir
        }

    suspend fun saveImageFromUri(sourceUri: Uri): String = withContext(Dispatchers.IO) {
        val extension = when (context.contentResolver.getType(sourceUri)) {
            "image/jpeg", "image/jpg" -> ".jpg"
            "image/webp" -> ".webp"
            else -> ".png"
        }
        val fileName = "qr_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}$extension"
        val destinationFile = File(qrDir, fileName)

        context.contentResolver.openInputStream(sourceUri)?.use { input ->
            FileOutputStream(destinationFile).use { output ->
                input.copyTo(output)
            }
        } ?: throw IllegalStateException("Could not open input stream from uri: $sourceUri")

        fileName
    }

    fun getFile(fileName: String): File {
        return File(qrDir, fileName)
    }

    suspend fun deleteFile(fileName: String): Boolean = withContext(Dispatchers.IO) {
        val file = File(qrDir, fileName)
        if (file.exists()) {
            file.delete()
        } else {
            true
        }
    }

    fun shareQrCode(fileName: String, displayName: String) {
        val file = getFile(fileName)
        if (!file.exists()) return

        val contentUri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/*"
            putExtra(Intent.EXTRA_STREAM, contentUri)
            putExtra(Intent.EXTRA_SUBJECT, "UPI QR Code - $displayName")
            putExtra(Intent.EXTRA_TEXT, "Scan this QR code: $displayName")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooser = Intent.createChooser(shareIntent, "Share QR Code").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }
}
