package com.example.androidpowerinfer

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Environment
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.Log
import androidx.core.content.FileProvider
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ParsePDF : Application() {
    companion object {
        lateinit var instance: ParsePDF
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        PDFBoxResourceLoader.init(this)
    }

    private val tag = ParsePDF::class.java.simpleName

    suspend fun parsePdfToString(context: Context, pdfUri: Uri): String =
        withContext(Dispatchers.IO) {
            var textResult = ""
            try {
                context.contentResolver.openInputStream(pdfUri)?.use { inputStream: InputStream ->
                    PDDocument.load(inputStream).use { document ->
                        val stripper = PDFTextStripper()
                        textResult = stripper.getText(document)
                    }
                }
            } catch (e: Exception) {
                Log.e("PDFParse", "Failed to parse PDF: ${e.message}", e)
                textResult = ""
            }
            textResult
        }

    @SuppressLint("QueryPermissionsNeeded")
    fun exportPDF(messages: List<ChatMessage>, context: Context) {

        val document = PdfDocument()
        val x = 40f
        var y = 40f
        val pageWidth = 595
        val pageHeight = 842
        var pageNumber = 1
        val lineHeight = 20f

        val textPaint = TextPaint().apply {
            color = android.graphics.Color.BLACK
            textSize = 12f
            isAntiAlias = true
        }

        var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
        var page = document.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val maxWidth = pageWidth - 80

        try {
            for (message in messages) {
                val formattedMessage = "${message.sender}: ${message.content}"
                if (message.sender.equals("system", ignoreCase = true) ||
                    message.sender.equals("error", ignoreCase = true)
                ) {
                    continue
                }

                val staticLayout = StaticLayout.Builder.obtain(
                    formattedMessage,
                    0,
                    formattedMessage.length,
                    textPaint,
                    maxWidth
                )
                    .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                    .setLineSpacing(0.0f, 1.0f)
                    .setIncludePad(false)
                    .build()

                if (y + staticLayout.height > pageHeight - 40) {
                    document.finishPage(page)
                    pageNumber += 1
                    pageInfo =
                        PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                    page = document.startPage(pageInfo)
                    y = 40f
                }

                canvas.save()
                canvas.translate(x, y)
                staticLayout.draw(canvas)
                canvas.restore()

                y += staticLayout.height + lineHeight
            }
            document.finishPage(page)

            val pdfDir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "Chats")
            if (!pdfDir.exists()) {
                pdfDir.mkdirs()
            }
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "Chat_$timeStamp"
            val pdfFile = File(pdfDir, "$fileName.pdf")

            FileOutputStream(pdfFile).use { output ->
                document.writeTo(output)
            }
            document.close()
            Log.i(tag, "exportChatToPdf: PDF exported successfully")

            val pdfUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                pdfFile
            )

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(pdfUri, "application/pdf")
                flags = Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }

            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
            } else {
                Log.e(tag, "No application found to open PDF")
            }
        } catch (e: IOException) {
            Log.e(tag, "exportChatToPdf: Failed to write PDF", e)
        } finally {
            document.close()
        }
    }
}
