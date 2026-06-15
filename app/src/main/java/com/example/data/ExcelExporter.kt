package com.example.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileWriter

object ExcelExporter {
    
    fun exportSalesToCsvAndShare(context: Context, sales: List<SaleItem>, month: String) {
        if (sales.isEmpty()) {
            Toast.makeText(context, "Tiada data jualan untuk dieksport pada bulan ini.", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            // Re-format name e.g. "2026-06" to "Laporan_Jualan_2026_06"
            val formattedMonth = month.replace("-", "_")
            val fileName = "Laporan_Jualan_$formattedMonth.csv"
            
            // Create in cache dir for safe shareable URI access
            val file = File(context.cacheDir, fileName)
            val writer = FileWriter(file)
            
            // CSV Header (Standard compatible with Microsoft Excel)
            writer.append("ID,Tarikh,Usahawan,Kategori,Jualan Kasar (RM),Kos Sediaan (RM),Untung Bersih (RM),Keterangan\n")
            
            // Write each row
            for (sale in sales) {
                val escapedDesc = sale.description.replace("\"", "\"\"")
                writer.append("${sale.id},")
                writer.append("${sale.date},")
                writer.append("\"${sale.entrepreneurName}\",")
                writer.append("\"${sale.category}\",")
                writer.append("${sale.amount},")
                writer.append("${sale.cost},")
                writer.append("${sale.profit},")
                writer.append("\"$escapedDesc\"\n")
            }
            
            writer.flush()
            writer.close()
            
            // Get content Uri via FileProvider configuration
            val authority = "${context.packageName}.fileprovider"
            val uri: Uri = FileProvider.getUriForFile(context, authority, file)
            
            // Create share intent
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/comma-separated-values"
                putExtra(Intent.EXTRA_SUBJECT, "Laporan Jualan Sale AI Binari - Bulan $month")
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            val chooserIntent = Intent.createChooser(shareIntent, "Kongsi Laporan Excel / CSV").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooserIntent)
            
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Gagal mengeksport data: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }
}
