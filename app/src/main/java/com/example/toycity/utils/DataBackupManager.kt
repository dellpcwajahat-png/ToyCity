package com.example.toycity.utils

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import com.example.toycity.data.FinancialRecord
import com.example.toycity.data.FinancialRepository
import com.example.toycity.data.LogRepository
import com.example.toycity.data.UserLog
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*

object DataBackupManager {
    private val gson = Gson()

    suspend fun exportData(context: Context, userId: String) {
        withContext(Dispatchers.IO) {
            try {
                val repository = FinancialRepository()
                // Fetch ALL records from the collection to ensure nothing is missed
                val records = repository.getAbsoluteAllRecords().first()
                
                if (records.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Export failed: No data found to backup", Toast.LENGTH_LONG).show()
                    }
                    return@withContext
                }

                val jsonString = gson.toJson(records)
                
                val fileName = "ToyCity_Backup_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.json"
                
                val outputStream: OutputStream?
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val contentValues = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                        put(MediaStore.MediaColumns.MIME_TYPE, "application/json")
                        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                    }
                    val resolver = context.contentResolver
                    val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                    outputStream = uri?.let { resolver.openOutputStream(it) }
                } else {
                    val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    val file = java.io.File(downloadsDir, fileName)
                    outputStream = java.io.FileOutputStream(file)
                }

                outputStream?.use { it.write(jsonString.toByteArray()) }

                val logRepository = LogRepository()
                val auth = FirebaseAuth.getInstance()
                val user = auth.currentUser
                logRepository.addLog(
                    UserLog(
                        userId = user?.uid ?: "Anonymous",
                        userEmail = user?.email ?: "Anonymous",
                        action = "Data Export",
                        details = "Exported ${records.size} records to $fileName",
                        timestamp = System.currentTimeMillis()
                    )
                )
                
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Successfully exported ${records.size} records to Downloads", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    suspend fun importData(context: Context, uri: Uri, userId: String) {
        withContext(Dispatchers.IO) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val reader = BufferedReader(InputStreamReader(inputStream))
                val jsonString = reader.use { it.readText() }
                
                val listType = object : TypeToken<List<FinancialRecord>>() {}.type
                val records: List<FinancialRecord>? = gson.fromJson(jsonString, listType)
                
                if (records.isNullOrEmpty()) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Import failed: File is empty or invalid", Toast.LENGTH_LONG).show()
                    }
                    return@withContext
                }

                val repository = FinancialRepository()
                records.forEach { record ->
                    // Use SHARED_STORE_DATA instead of individual userId
                    // to ensure all users see the imported data
                    repository.saveRecord(record.copy(userId = "SHARED_STORE_DATA"))
                }

                val logRepository = LogRepository()
                val auth = FirebaseAuth.getInstance()
                val user = auth.currentUser
                logRepository.addLog(
                    UserLog(
                        userId = user?.uid ?: "Anonymous",
                        userEmail = user?.email ?: "Anonymous",
                        action = "Data Import",
                        details = "Imported ${records.size} records from backup file",
                        timestamp = System.currentTimeMillis()
                    )
                )
                
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Successfully imported ${records.size} records", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Import failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
