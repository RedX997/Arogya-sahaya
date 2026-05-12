package com.example.arogyasahaya.utils

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Environment
import android.widget.Toast
import com.example.arogyasahaya.data.local.entity.Appointment
import com.example.arogyasahaya.data.local.entity.Medicine
import com.example.arogyasahaya.data.local.entity.Symptom
import com.example.arogyasahaya.data.local.entity.Vital
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object PdfReportGenerator {

    fun generateReport(
        context: Context,
        patientName: String,
        medicines: List<Medicine>,
        vitals: List<Vital>,
        symptoms: List<Symptom>
    ) {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 size
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas
        val paint = Paint()
        val titlePaint = Paint()
        
        var yPos = 50f

        // Header
        titlePaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        titlePaint.textSize = 24f
        titlePaint.color = Color.rgb(46, 125, 50) // HealthGreen
        canvas.drawText("AROGYA SAHAYA - HEALTH REPORT", 50f, yPos, titlePaint)
        
        yPos += 30f
        paint.textSize = 12f
        paint.color = Color.BLACK
        val dateStr = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date())
        canvas.drawText("Generated on: $dateStr", 50f, yPos, paint)
        
        yPos += 40f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 16f
        canvas.drawText("PATIENT: $patientName", 50f, yPos, paint)
        
        // Medicines Section
        yPos += 50f
        paint.color = Color.rgb(46, 125, 50)
        canvas.drawText("ACTIVE MEDICATIONS", 50f, yPos, paint)
        yPos += 20f
        paint.color = Color.BLACK
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 12f
        
        if (medicines.isEmpty()) {
            canvas.drawText("No active medications listed.", 70f, yPos, paint)
            yPos += 20f
        } else {
            for (med in medicines) {
                canvas.drawText("• ${med.name} (${med.dosage}) - ${med.frequency}", 70f, yPos, paint)
                yPos += 20f
                if (yPos > 750) break
            }
        }

        // Vitals Section
        yPos += 30f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 16f
        paint.color = Color.rgb(46, 125, 50)
        canvas.drawText("RECENT VITALS (Last 5 Readings)", 50f, yPos, paint)
        yPos += 20f
        paint.color = Color.BLACK
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 12f
        
        val recentVitals = vitals.take(5)
        if (recentVitals.isEmpty()) {
            canvas.drawText("No vitals recorded yet.", 70f, yPos, paint)
            yPos += 20f
        } else {
            for (v in recentVitals) {
                val vDate = SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(v.date))
                canvas.drawText("• $vDate: BP ${v.systolic}/${v.diastolic}, HR ${v.heartRate} bpm", 70f, yPos, paint)
                yPos += 20f
                if (yPos > 750) break
            }
        }

        // Symptoms Section
        yPos += 30f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 16f
        paint.color = Color.rgb(46, 125, 50)
        canvas.drawText("SYMPTOM LOG", 50f, yPos, paint)
        yPos += 20f
        paint.color = Color.BLACK
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 12f
        
        val recentSymptoms = symptoms.take(5)
        if (recentSymptoms.isEmpty()) {
            canvas.drawText("No symptoms recorded recently.", 70f, yPos, paint)
            yPos += 20f
        } else {
            for (s in recentSymptoms) {
                val sDate = SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(s.date))
                canvas.drawText("• $sDate: ${s.tags}. Note: ${s.notes}", 70f, yPos, paint)
                yPos += 20f
                if (yPos > 750) break
            }
        }

        // Footer
        paint.textSize = 10f
        paint.color = Color.GRAY
        canvas.drawText("This report is for informational purposes. Please consult a qualified doctor for medical advice.", 50f, 800f, paint)

        pdfDocument.finishPage(page)

        val fileName = "Health_Report_${System.currentTimeMillis()}.pdf"
        val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName)

        try {
            pdfDocument.writeTo(FileOutputStream(file))
            Toast.makeText(context, "Report saved: ${file.absolutePath}", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Failed to save report.", Toast.LENGTH_SHORT).show()
        } finally {
            pdfDocument.close()
        }
    }
}
