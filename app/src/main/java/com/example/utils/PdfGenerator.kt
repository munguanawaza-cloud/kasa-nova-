package com.example.utils

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.R
import com.example.data.local.CurrencyCode
import com.example.data.local.CustomerEntity
import com.example.data.local.SaleEntity
import com.example.data.local.SaleType
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

object PdfGenerator {

    private const val A4_WIDTH = 595
    private const val A4_HEIGHT = 842

    fun generateSaleInvoicePdf(
        context: Context,
        sale: SaleEntity,
        customer: CustomerEntity?
    ): Uri {
        val doc = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(A4_WIDTH, A4_HEIGHT, 1).create()
        val page = doc.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val headerBgPaint = Paint().apply { color = Color.parseColor("#0B3D2E") }
        val goldPaint = Paint().apply {
            color = Color.parseColor("#D4AF37")
            textSize = 22f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val whiteSubtitlePaint = Paint().apply {
            color = Color.WHITE
            textSize = 10.5f
            isAntiAlias = true
        }
        val titlePaint = Paint().apply {
            color = Color.parseColor("#0B3D2E")
            textSize = 14f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val bodyPaint = Paint().apply {
            color = Color.parseColor("#1E293B")
            textSize = 11.5f
            isAntiAlias = true
        }
        val boldBodyPaint = Paint().apply {
            color = Color.parseColor("#0F172A")
            textSize = 12.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val linePaint = Paint().apply {
            color = Color.parseColor("#CBD5E1")
            strokeWidth = 1.5f
        }
        val boxStrokePaint = Paint().apply {
            color = Color.parseColor("#0B3D2E")
            style = Paint.Style.STROKE
            strokeWidth = 1.5f
        }

        // Top Brand Banner with KASA NOVA Logo
        canvas.drawRect(0f, 0f, A4_WIDTH.toFloat(), 124f, headerBgPaint)
        runCatching {
            val logoBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.img_kasa_nova_icon)
            if (logoBitmap != null) {
                val dest = RectF(32f, 22f, 108f, 98f)
                canvas.drawBitmap(logoBitmap, null, dest, null)
                logoBitmap.recycle()
            }
        }
        canvas.drawText("KASA NOVA · HAMADA GROUP", 126f, 46f, goldPaint)
        canvas.drawText("Kolwezi, Lualaba · DRC | Africa/Lubumbashi", 126f, 68f, whiteSubtitlePaint)
        canvas.drawText("WhatsApp: +243 992 120 772 / +243 970 903 085", 126f, 86f, whiteSubtitlePaint)
        canvas.drawText("Facture / Reçu Officiel #KN-${sale.id}", 126f, 105f, whiteSubtitlePaint)

        // Date & Payment Mode
        var y = 158f
        canvas.drawText("Date (Kolwezi): ${TimeUtils.formatDateTime(sale.createdAt)}", 40f, y, bodyPaint)
        canvas.drawText("Type: ${sale.type}  |  Devise: ${sale.currency}", 335f, y, boldBodyPaint)
        if (sale.type == SaleType.INSTALLMENT && sale.dueDate > 0L) {
            y += 18f
            canvas.drawText("Échéance / Due: ${TimeUtils.formatDateOnly(sale.dueDate)}", 40f, y, boldBodyPaint)
        }

        y += 20f
        canvas.drawLine(40f, y, 555f, y, linePaint)

        // Customer Block
        y += 26f
        canvas.drawText("CLIENT / CUSTOMER", 40f, y, titlePaint)
        y += 20f
        canvas.drawText("Nom: ${customer?.name ?: sale.customerName}", 40f, y, boldBodyPaint)
        y += 18f
        canvas.drawText("Téléphone: ${customer?.phone ?: "-"}   |   Source: ${sale.customerSource}", 40f, y, bodyPaint)
        y += 18f
        canvas.drawText(
            "Adresse: ${customer?.address ?: "Kolwezi"}   |   Points fidélité: ${customer?.loyaltyPoints ?: 0}",
            40f,
            y,
            bodyPaint
        )

        y += 22f
        canvas.drawLine(40f, y, 555f, y, linePaint)

        // Line Item Block
        y += 26f
        canvas.drawText("DÉTAILS DE LA TRANSACTION (${sale.currency})", 40f, y, titlePaint)
        y += 22f
        canvas.drawText("Article: ${sale.productName}", 40f, y, boldBodyPaint)
        y += 18f
        canvas.drawText("Quantité: ${sale.quantity}", 40f, y, bodyPaint)
        if (sale.isGiveawayEntry) {
            canvas.drawText("Tombola / Giveaway: INSCRIT (Oui)", 300f, y, boldBodyPaint)
        }
        y += 24f
        canvas.drawText("Total Facturé: ${formatMoney(sale.totalPrice, sale.currency)}", 40f, y, boldBodyPaint)
        y += 20f
        canvas.drawText("Montant Payé: ${formatMoney(sale.paidAmount, sale.currency)}", 40f, y, bodyPaint)
        y += 20f
        canvas.drawText("Reste à Payer: ${formatMoney(sale.remainingAmount, sale.currency)}", 40f, y, boldBodyPaint)

        y += 24f
        canvas.drawLine(40f, y, 555f, y, linePaint)

        // Digital Signature Box (if available)
        y += 22f
        canvas.drawText("SIGNATURE CLIENT", 40f, y, titlePaint)
        canvas.drawText("VÉRIFICATION QR", 355f, y, titlePaint)

        val sigRect = Rect(40, (y + 12).toInt(), 290, (y + 132).toInt())
        canvas.drawRect(sigRect, boxStrokePaint)
        val sigFile = sale.signaturePath.takeIf { it.isNotBlank() }?.let { File(it) }
        if (sigFile != null && sigFile.exists()) {
            val sigBmp = BitmapFactory.decodeFile(sigFile.absolutePath)
            if (sigBmp != null) {
                canvas.drawBitmap(sigBmp, null, sigRect, null)
                sigBmp.recycle()
            }
        } else {
            canvas.drawText("Aucune signature enregistrée", 68f, y + 76f, bodyPaint)
        }

        // QR Verification Code
        val qrContent = "KASA-NOVA|SALE:${sale.id}|CUR:${sale.currency}|TOT:${sale.totalPrice}|PAID:${sale.paidAmount}|REM:${sale.remainingAmount}|GW:${sale.isGiveawayEntry}|TS:${sale.createdAt}"
        val qrBitmap = createQrBitmap(qrContent, 140)
        canvas.drawBitmap(qrBitmap, 355f, y + 10f, null)

        val footerY = y + 165f
        canvas.drawText("Livre ${sale.currency} strictement isolé — Aucune conversion auto.", 40f, footerY, bodyPaint)
        canvas.drawText("Service Client WhatsApp : +243 992 120 772 · HAMADA GROUP Kolwezi", 40f, footerY + 18f, bodyPaint)

        doc.finishPage(page)

        val dir = File(context.cacheDir, "invoices").apply { mkdirs() }
        val file = File(dir, "kasa_nova_invoice_${sale.id}.pdf")
        FileOutputStream(file).use { out ->
            doc.writeTo(out)
        }
        doc.close()

        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }

    fun buildSharePdfIntent(uri: Uri, title: String = "KASA NOVA Invoice"): Intent {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, title)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return Intent.createChooser(sendIntent, title)
    }

    private fun createQrBitmap(text: String, sizePx: Int): Bitmap {
        val bitMatrix = QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, sizePx, sizePx)
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.RGB_565)
        for (x in 0 until sizePx) {
            for (y in 0 until sizePx) {
                bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
            }
        }
        return bitmap
    }

    private fun formatMoney(amount: Double, currency: String): String {
        return if (currency == CurrencyCode.USD) {
            "$" + String.format(Locale.US, "%.2f USD", amount)
        } else {
            String.format(Locale.US, "%,.0f CDF", amount)
        }
    }
}
