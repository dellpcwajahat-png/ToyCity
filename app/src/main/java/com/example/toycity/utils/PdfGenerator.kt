package com.example.toycity.utils

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import com.example.toycity.data.FinancialRecord
import com.itextpdf.kernel.colors.ColorConstants
import com.itextpdf.kernel.colors.DeviceRgb
import com.itextpdf.kernel.geom.PageSize
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.borders.Border
import com.itextpdf.layout.element.Cell
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import com.itextpdf.layout.element.Text
import com.itextpdf.layout.properties.TextAlignment
import com.itextpdf.layout.properties.UnitValue
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*
import com.example.toycity.utils.Formatter

object PdfGenerator {
    private val PRIMARY_COLOR = DeviceRgb(63, 81, 181)
    private val SUCCESS_COLOR = DeviceRgb(76, 175, 80)
    private val ERROR_COLOR = DeviceRgb(244, 67, 54)
    private val TEXT_COLOR = DeviceRgb(51, 51, 51)

    fun generateFinancialReport(context: Context, record: FinancialRecord, pageSize58mm: Boolean) {
        val fileName = "ToyCity_Report_${record.id}.pdf"
        
        try {
            val outputStream: OutputStream?
            val filePathDisplay: String

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                outputStream = uri?.let { resolver.openOutputStream(it) }
                filePathDisplay = "Downloads/$fileName"
            } else {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val file = File(downloadsDir, fileName)
                outputStream = FileOutputStream(file)
                filePathDisplay = file.absolutePath
            }

            if (outputStream == null) throw Exception("Failed to open output stream")

            val writer = PdfWriter(outputStream)
            val pdf = PdfDocument(writer)
            
            val size = if (pageSize58mm) PageSize(164f, 842f) else PageSize.A4
            val document = Document(pdf, size)
            val margins = if (pageSize58mm) 10f else 36f
            document.setMargins(margins, margins, margins, margins)

            // Header
            document.add(Paragraph("TOY CITY")
                .setTextAlignment(TextAlignment.CENTER)
                .setFontColor(PRIMARY_COLOR)
                .simulateBold()
                .setFontSize(if (pageSize58mm) 14f else 24f))
            
            document.add(Paragraph("Monthly Financial Report")
                .setTextAlignment(TextAlignment.CENTER)
                .setFontSize(if (pageSize58mm) 8f else 14f))
            
            document.add(Paragraph("Period: ${record.id}")
                .setTextAlignment(TextAlignment.CENTER)
                .setFontSize(if (pageSize58mm) 7f else 11f)
                .setMarginBottom(10f))

            // Summary Table
            val summaryTable = Table(UnitValue.createPercentArray(if (pageSize58mm) floatArrayOf(1f) else floatArrayOf(1f, 1f, 1f))).useAllAvailableWidth()
            addMetricToGrid(summaryTable, "Total Sales", record.totalSales, PRIMARY_COLOR, pageSize58mm)
            addMetricToGrid(summaryTable, "Net Profit", record.netProfit, SUCCESS_COLOR, pageSize58mm)
            addMetricToGrid(summaryTable, "Cash in Drawer", record.cashInDrawer, PRIMARY_COLOR, pageSize58mm)
            document.add(summaryTable.setMarginBottom(15f))

            // Financial Details
            document.add(Paragraph("Financial Summary")
                .simulateBold()
                .setFontSize(if (pageSize58mm) 10f else 12f)
                .setMarginBottom(5f))

            val detailTable = Table(UnitValue.createPercentArray(floatArrayOf(1f, 1f))).useAllAvailableWidth()
            addDetailRow(detailTable, "Starting Cash", record.startingCash, pageSize58mm)
            addDetailRow(detailTable, "Total Sales Revenue", record.totalSales, pageSize58mm)
            addDetailRow(detailTable, "Cost of Goods (COGS)", record.inventoryData.cogs, pageSize58mm)
            addDetailRow(detailTable, "Operating Expenses", record.totalExpenses, pageSize58mm)
            addDetailRow(detailTable, "Restock Investment", record.inventoryData.restockInvestment, pageSize58mm)
            addDetailRow(detailTable, "Customer Receivables", record.customerReceivables, pageSize58mm)
            document.add(detailTable.setMarginBottom(15f))

            // Expenses Categories
            if (record.expenseCategories.isNotEmpty()) {
                document.add(Paragraph("Expenses by Category")
                    .simulateBold()
                    .setFontSize(if (pageSize58mm) 10f else 12f)
                    .setMarginBottom(5f))
                
                val expenseTable = Table(UnitValue.createPercentArray(floatArrayOf(1f, 1f))).useAllAvailableWidth()
                record.expenseCategories.forEach { (cat, amount) ->
                    addDetailRow(expenseTable, cat, amount, pageSize58mm)
                }
                document.add(expenseTable.setMarginBottom(15f))
            }

            // Loans Status
            if (record.loans.isNotEmpty()) {
                document.add(Paragraph("Loans & Debt Status")
                    .simulateBold()
                    .setFontSize(if (pageSize58mm) 10f else 12f)
                    .setMarginTop(10f)
                    .setMarginBottom(5f))
                
                val loanTable = Table(UnitValue.createPercentArray(floatArrayOf(2f, 2f, 2f, 2f))).useAllAvailableWidth()
                loanTable.addHeaderCell(createReceiptHeaderCell("Lender", pageSize58mm))
                loanTable.addHeaderCell(createReceiptHeaderCell("Principal", pageSize58mm))
                loanTable.addHeaderCell(createReceiptHeaderCell("Repaid", pageSize58mm))
                loanTable.addHeaderCell(createReceiptHeaderCell("Balance", pageSize58mm))
                
                record.loans.forEach { loan ->
                    val balance = (loan.principalAmount - loan.repaymentToDate).coerceAtLeast(0.0)
                    loanTable.addCell(createReceiptCell(loan.lenderName, pageSize58mm))
                    loanTable.addCell(createReceiptCell(formatValue(loan.principalAmount), pageSize58mm))
                    loanTable.addCell(createReceiptCell(formatValue(loan.repaymentToDate), pageSize58mm))
                    loanTable.addCell(createReceiptCell(formatValue(balance), pageSize58mm).setFontColor(if (balance > 0) ERROR_COLOR else SUCCESS_COLOR))
                }
                document.add(loanTable.setMarginBottom(10f))
                
                val totalDebtRow = Table(UnitValue.createPercentArray(floatArrayOf(1f, 1f))).useAllAvailableWidth()
                addDetailRow(totalDebtRow, "Total Outstanding Debt", record.totalDebt, pageSize58mm)
                document.add(totalDebtRow.setMarginBottom(15f))
            }

            // Recent Cash Transactions (Optional - for audit)
            val recentCash = record.cashTransactions.takeLast(10)
            if (recentCash.isNotEmpty() && !pageSize58mm) {
                document.add(Paragraph("Recent Cash Transactions")
                    .simulateBold()
                    .setFontSize(12f)
                    .setMarginTop(10f)
                    .setMarginBottom(5f))
                
                val cashTable = Table(UnitValue.createPercentArray(floatArrayOf(2f, 3f, 2f, 2f))).useAllAvailableWidth()
                cashTable.addHeaderCell(createReceiptHeaderCell("Date", false))
                cashTable.addHeaderCell(createReceiptHeaderCell("Note/Category", false))
                cashTable.addHeaderCell(createReceiptHeaderCell("Type", false))
                cashTable.addHeaderCell(createReceiptHeaderCell("Amount", false))

                recentCash.reversed().forEach { tx ->
                    cashTable.addCell(createReceiptCell(Formatter.formatDate(Date(tx.date)), false))
                    cashTable.addCell(createReceiptCell("${tx.note} (${tx.category})", false))
                    cashTable.addCell(createReceiptCell(if (tx.isCashIn) "IN" else "OUT", false).setFontColor(if (tx.isCashIn) SUCCESS_COLOR else ERROR_COLOR))
                    cashTable.addCell(createReceiptCell(formatValue(tx.amount), false))
                }
                document.add(cashTable.setMarginBottom(15f))
            }

            // Footer
            val sdf = SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault())
            document.add(Paragraph("Generated on: ${sdf.format(Date())}")
                .setTextAlignment(TextAlignment.CENTER)
                .setFontSize(if (pageSize58mm) 7f else 9f)
                .setFontColor(ColorConstants.GRAY))

            document.close()
            Toast.makeText(context, "Report Saved: $filePathDisplay", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun generateReceipt(context: Context, sale: com.example.toycity.data.Sale, pageSize58mm: Boolean) {
        val fileName = "Receipt_${sale.id.takeLast(6)}_${System.currentTimeMillis()}.pdf"
        val settings = SecurityManager.getReceiptSettings(context)
        val bizName = settings["name"]?.takeIf { it.isNotBlank() } ?: "TOY CITY"
        val bizAddress = settings["address"] ?: ""
        val bizPhone = settings["phone"] ?: ""
        val ntnNo = settings["ntnNo"] ?: ""
        val salesPerson = settings["salesPerson"] ?: ""
        val thankYouNote = settings["note"] ?: "Thank you for shopping at Toy City!"

        try {
            val outputStream: OutputStream?
            val filePathDisplay: String

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                outputStream = uri?.let { resolver.openOutputStream(it) }
                filePathDisplay = "Downloads/$fileName"
            } else {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val file = File(downloadsDir, fileName)
                outputStream = FileOutputStream(file)
                filePathDisplay = file.absolutePath
            }

            if (outputStream == null) throw Exception("Failed to open output stream")

            val writer = PdfWriter(outputStream)
            val pdf = PdfDocument(writer)
            
            val size = if (pageSize58mm) PageSize(164f, 842f) else PageSize.A4
            val document = Document(pdf, size)
            val margins = if (pageSize58mm) 10f else 36f
            document.setMargins(margins, margins, margins, margins)

            // Header
            document.add(Paragraph(bizName)
                .setTextAlignment(TextAlignment.CENTER)
                .setFontColor(PRIMARY_COLOR)
                .simulateBold()
                .setFontSize(if (pageSize58mm) 14f else 24f))

            if (bizAddress.isNotBlank()) {
                document.add(Paragraph(bizAddress)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setFontSize(if (pageSize58mm) 7f else 10f))
            }

            if (bizPhone.isNotBlank() || ntnNo.isNotBlank()) {
                val contactInfo = StringBuilder()
                if (bizPhone.isNotBlank()) contactInfo.append("Phone: $bizPhone")
                if (bizPhone.isNotBlank() && ntnNo.isNotBlank()) contactInfo.append(" | ")
                if (ntnNo.isNotBlank()) contactInfo.append("NTN: $ntnNo")
                
                document.add(Paragraph(contactInfo.toString())
                    .setTextAlignment(TextAlignment.CENTER)
                    .setFontSize(if (pageSize58mm) 7f else 10f))
            }
            
            document.add(Paragraph("Sales Receipt")
                .setTextAlignment(TextAlignment.CENTER)
                .setFontSize(if (pageSize58mm) 8f else 12f)
                .setMarginBottom(10f))

            // Sale Info
            val infoTable = Table(UnitValue.createPercentArray(floatArrayOf(1f, 1f))).useAllAvailableWidth()
            infoTable.addCell(createSummaryCell("Date: ${Formatter.formatDate(Date(sale.timestamp))}", TextAlignment.LEFT, pageSize58mm))
            infoTable.addCell(createSummaryCell("Receipt #: ${sale.id.takeLast(8).uppercase()}", TextAlignment.RIGHT, pageSize58mm))
            document.add(infoTable.setMarginBottom(10f))

            document.add(Paragraph("Customer: ${sale.customerName}")
                .setFontSize(if (pageSize58mm) 9f else 11f)
                .setMarginBottom(5f))

            if (salesPerson.isNotBlank()) {
                document.add(Paragraph("Served by: $salesPerson")
                    .setFontSize(if (pageSize58mm) 7f else 9f)
                    .setMarginBottom(10f))
            }

            // Items Table
            val itemColumns = if (pageSize58mm) floatArrayOf(3f, 1f, 2f) else floatArrayOf(4f, 1f, 2f, 2f)
            val itemTable = Table(UnitValue.createPercentArray(itemColumns)).useAllAvailableWidth()
            
            // Header Row
            itemTable.addHeaderCell(createReceiptHeaderCell("Item", pageSize58mm))
            itemTable.addHeaderCell(createReceiptHeaderCell("Qty", pageSize58mm))
            if (!pageSize58mm) itemTable.addHeaderCell(createReceiptHeaderCell("Price", pageSize58mm))
            itemTable.addHeaderCell(createReceiptHeaderCell("Total", pageSize58mm))

            sale.items.forEach { item ->
                itemTable.addCell(createReceiptCell(item.productName, pageSize58mm))
                itemTable.addCell(createReceiptCell(item.quantity.toString(), pageSize58mm).setTextAlignment(TextAlignment.CENTER))
                if (!pageSize58mm) itemTable.addCell(createReceiptCell(Formatter.formatCurrency(item.price), pageSize58mm))
                itemTable.addCell(createReceiptCell(Formatter.formatCurrency(item.price * item.quantity), pageSize58mm).setTextAlignment(TextAlignment.RIGHT))
            }
            document.add(itemTable.setMarginBottom(15f))

            // Totals
            val totalsTable = Table(UnitValue.createPercentArray(floatArrayOf(1f, 1f))).useAllAvailableWidth()
            totalsTable.addCell(Cell().add(Paragraph("GRAND TOTAL").simulateBold().setFontSize(if (pageSize58mm) 10f else 14f)).setBorder(Border.NO_BORDER))
            totalsTable.addCell(Cell().add(Paragraph(Formatter.formatCurrency(sale.totalAmount)).simulateBold().setFontSize(if (pageSize58mm) 10f else 14f).setFontColor(PRIMARY_COLOR)).setTextAlignment(TextAlignment.RIGHT).setBorder(Border.NO_BORDER))
            document.add(totalsTable)

            // Footer
            document.add(Paragraph("\n\n$thankYouNote")
                .setTextAlignment(TextAlignment.CENTER)
                .setFontSize(if (pageSize58mm) 8f else 10f))
            
            document.add(Paragraph("Items once sold are not returnable.")
                .setTextAlignment(TextAlignment.CENTER)
                .setFontSize(if (pageSize58mm) 7f else 9f)
                .setFontColor(ColorConstants.GRAY))

            document.close()
            Toast.makeText(context, "Receipt Saved: $filePathDisplay", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun createReceiptHeaderCell(text: String, isSmall: Boolean): Cell {
        return Cell().add(Paragraph(text).simulateBold().setFontSize(if (isSmall) 8f else 10f).setFontColor(ColorConstants.WHITE))
            .setBackgroundColor(PRIMARY_COLOR)
            .setPadding(if (isSmall) 2f else 5f)
    }

    private fun createReceiptCell(text: String, isSmall: Boolean): Cell {
        return Cell().add(Paragraph(text).setFontSize(if (isSmall) 8f else 10f))
            .setPadding(if (isSmall) 2f else 5f)
            .setBorder(Border.NO_BORDER)
            .setBorderBottom(com.itextpdf.layout.borders.SolidBorder(ColorConstants.LIGHT_GRAY, 0.5f))
    }

    private fun formatValue(value: Double): String {
        return Formatter.formatCurrency(value)
    }

    private fun createSummaryCell(text: String, alignment: TextAlignment, isSmall: Boolean): Cell {
        return Cell().add(Paragraph(text).setFontSize(if (isSmall) 7f else 10f))
            .setTextAlignment(alignment)
            .setBorder(Border.NO_BORDER)
    }

    private fun addMetricToGrid(table: Table, label: String, value: Double, color: DeviceRgb, isSmall: Boolean) {
        val cell = Cell().add(
            Paragraph(label).setFontSize(if (isSmall) 7f else 9f).setFontColor(ColorConstants.GRAY)
        ).add(
            Paragraph(formatValue(value)).setFontSize(if (isSmall) 9f else 14f).simulateBold().setFontColor(color)
        ).setPadding(if (isSmall) 2f else 8f)
        
        if (!isSmall) {
            cell.setBorder(Border.NO_BORDER).setBackgroundColor(DeviceRgb(245, 245, 245))
        } else {
            cell.setBorder(Border.NO_BORDER)
        }
        table.addCell(cell)
    }

    private fun addVisualBar(document: Document, label: String, value: Double, max: Double, color: DeviceRgb) {
        val percentage = (value / max).toFloat().coerceIn(0.01f, 1f)
        val barTable = Table(UnitValue.createPercentArray(floatArrayOf(2f, 8f))).useAllAvailableWidth().setMarginTop(5f)
        
        barTable.addCell(Cell().add(Paragraph(label).setFontSize(10f)).setBorder(Border.NO_BORDER).setVerticalAlignment(com.itextpdf.layout.properties.VerticalAlignment.MIDDLE))
        
        val progressContainer = Table(UnitValue.createPercentArray(floatArrayOf(percentage, 1f - percentage))).useAllAvailableWidth()
        progressContainer.addCell(Cell().setBackgroundColor(color).setHeight(12f).setBorder(Border.NO_BORDER))
        progressContainer.addCell(Cell().setBackgroundColor(DeviceRgb(230, 230, 230)).setHeight(12f).setBorder(Border.NO_BORDER))
        
        barTable.addCell(Cell().add(progressContainer).setBorder(Border.NO_BORDER).setVerticalAlignment(com.itextpdf.layout.properties.VerticalAlignment.MIDDLE))
        document.add(barTable)
    }

    private fun addDetailRow(table: Table, label: String, value: Double, isSmall: Boolean) {
        table.addCell(Cell().add(Paragraph(label).setFontSize(if (isSmall) 8f else 11f)).setBorder(Border.NO_BORDER))
        table.addCell(Cell().add(Paragraph(formatValue(value)).setFontSize(if (isSmall) 8f else 11f).setTextAlignment(TextAlignment.RIGHT)).setBorder(Border.NO_BORDER))
    }

    private fun createHeaderCell(text: String): Cell {
        return Cell().add(Paragraph(text).simulateBold().setFontColor(ColorConstants.WHITE))
            .setBackgroundColor(PRIMARY_COLOR)
            .setPadding(5f)
    }

    private fun createCell(text: String): Cell {
        return Cell().add(Paragraph(text).setFontSize(11f)).setPadding(5f)
    }

    private fun createLoanCell(text: String, alignment: TextAlignment, isSmall: Boolean): Cell {
        return Cell().add(Paragraph(text).setFontSize(if (isSmall) 8f else 10f))
            .setTextAlignment(alignment)
            .setBorder(Border.NO_BORDER)
    }
}
