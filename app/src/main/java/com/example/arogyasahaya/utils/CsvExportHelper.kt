package com.example.arogyasahaya.utils

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.arogyasahaya.data.local.entity.Vital
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

object CsvExportHelper {
    fun exportVitalsToCsv(context: Context, vitals: List<Vital>, profileName: String) {
        val fileName = "Health_Report_${profileName.replace(" ", "_")}.csv"
        val file = File(context.cacheDir, fileName)
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

        try {
            file.printWriter().use { out ->
                out.println("Date,Systolic,Diastolic,HeartRate,Sugar,Notes")
                vitals.forEach { vital ->
                    out.println("${sdf.format(Date(vital.date))},${vital.systolic},${vital.diastolic},${vital.heartRate},${vital.sugar ?: ""},\"${vital.notes ?: ""}\"")
                }
            }

            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_SUBJECT, "Health Vitals Report: $profileName")
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Export CSV"))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
