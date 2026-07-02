package com.example.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Environment
import android.util.Log
import androidx.core.content.FileProvider
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

// Representing a rich domain model for Receipt containing products
data class ReceiptWithProducts(
    val receipt: ReceiptEntity,
    val products: List<ProductEntity>
)

class ReceiptRepository(private val context: Context) {
    private val db = AppDatabase.getDatabase(context)
    val receiptDao = db.receiptDao()
    val productDao = db.productDao()
    val customerDao = db.customerDao()
    val settingsDao = db.settingsDao()
    val templateDao = db.templateDao()

    // Flow lists
    val allReceipts: Flow<List<ReceiptEntity>> = receiptDao.getAllReceipts()
    val allCustomers: Flow<List<CustomerEntity>> = customerDao.getAllCustomers()
    val allTemplates: Flow<List<TemplateEntity>> = templateDao.getAllTemplates()
    val settings: Flow<SettingsEntity?> = settingsDao.getSettings()

    // Core Db Actions
    suspend fun getReceiptWithProducts(id: Long): ReceiptWithProducts? = withContext(Dispatchers.IO) {
        val receipt = receiptDao.getReceiptById(id) ?: return@withContext null
        val products = productDao.getProductsForReceipt(id)
        ReceiptWithProducts(receipt, products)
    }

    suspend fun insertCustomer(customer: CustomerEntity) = withContext(Dispatchers.IO) {
        customerDao.insertCustomer(customer)
    }

    suspend fun deleteCustomer(id: Long) = withContext(Dispatchers.IO) {
        customerDao.deleteCustomer(id)
    }

    suspend fun saveSettings(settingsEntity: SettingsEntity) = withContext(Dispatchers.IO) {
        settingsDao.insertSettings(settingsEntity)
    }

    suspend fun toggleFavoriteTemplate(templateId: String) = withContext(Dispatchers.IO) {
        val templates = templateDao.getAllTemplates().firstOrNull() ?: return@withContext
        val template = templates.find { it.id == templateId }
        if (template != null) {
            templateDao.updateTemplate(template.copy(isFavorite = !template.isFavorite))
        }
    }

    suspend fun deleteReceipt(receipt: ReceiptEntity) = withContext(Dispatchers.IO) {
        // Delete files
        try {
            if (receipt.pdfPath.isNotEmpty()) File(receipt.pdfPath).delete()
            if (receipt.imagePath.isNotEmpty()) File(receipt.imagePath).delete()
            if (receipt.jsonPath.isNotEmpty()) File(receipt.jsonPath).delete()
            val thumbPath = receipt.jsonPath.replace(".json", ".thumb")
            File(thumbPath).delete()
        } catch (e: Exception) {
            Log.e("ReceiptRepository", "Error deleting receipt files", e)
        }
        productDao.deleteProductsForReceipt(receipt.id)
        receiptDao.deleteReceipt(receipt)
    }

    // Initialize Default Settings and Templates
    suspend fun initDefaults() = withContext(Dispatchers.IO) {
        val currentSettings = settingsDao.getSettingsOnce()
        if (currentSettings == null) {
            settingsDao.insertSettings(SettingsEntity())
        }

        val templates = listOf(
            TemplateEntity("Modern", "Modern Theme", "Modern", false),
            TemplateEntity("Professional", "Professional Executive", "Professional", true),
            TemplateEntity("Restaurant", "Cafe & Restaurant", "Restaurant", false),
            TemplateEntity("Shop", "Retail Store", "Shop", false),
            TemplateEntity("Grocery", "Supermarket & Grocery", "Grocery", false),
            TemplateEntity("Pharmacy", "Medical Store", "Pharmacy", false),
            TemplateEntity("Electronics", "Tech & Digital", "Electronics", false)
        )
        templateDao.insertTemplates(templates)
    }

    // Dynamic Directory structure: NexReceipt/Receipts/YYYY/MonthName/
    fun getReceiptFolder(dateStr: String): File {
        // Assume date format "dd/MM/yyyy" or similar
        var year = "2026"
        var month = "July"
        try {
            val date = SimpleDateFormat("dd/MM/yyyy", Locale.US).parse(dateStr)
            if (date != null) {
                val cal = Calendar.getInstance()
                cal.time = date
                year = cal.get(Calendar.YEAR).toString()
                month = SimpleDateFormat("MMMM", Locale.US).format(date)
            }
        } catch (e: Exception) {
            // fallback current year/month
            val cal = Calendar.getInstance()
            year = cal.get(Calendar.YEAR).toString()
            month = SimpleDateFormat("MMMM", Locale.US).format(Date())
        }

        val baseDir = context.getExternalFilesDir(null) ?: context.filesDir
        val receiptsFolder = File(baseDir, "NexReceipt/Receipts/$year/$month")
        if (!receiptsFolder.exists()) {
            receiptsFolder.mkdirs()
        }
        return receiptsFolder
    }

    // Generate Receipt files: PDF, Image, JSON, and Thumbnail
    suspend fun generateAndSaveReceipt(
        receiptNumber: String,
        customerName: String,
        customerPhone: String,
        customerEmail: String,
        customerAddress: String,
        dateStr: String,
        timeStr: String,
        products: List<ProductEntity>,
        discount: Double,
        vatRate: Double,
        templateName: String,
        notes: String
    ): Long = withContext(Dispatchers.IO) {
        val settings = settingsDao.getSettingsOnce() ?: SettingsEntity()
        
        // Calculate subtotal, VAT, total
        var subtotal = 0.0
        products.forEach {
            val itemTotal = it.price * it.quantity
            val itemDisc = it.discount
            val itemVat = (itemTotal - itemDisc) * (it.vat / 100.0)
            subtotal += itemTotal
        }
        
        val discountAmount = discount
        val activeVatAmount = (subtotal - discountAmount) * (vatRate / 100.0)
        val finalTotal = subtotal - discountAmount + activeVatAmount

        // Get organized folder
        val folder = getReceiptFolder(dateStr)
        val sanitizedReceiptNum = receiptNumber.replace("/", "_").replace("\\", "_")
        val pdfFile = File(folder, "Receipt_$sanitizedReceiptNum.pdf")
        val imageFile = File(folder, "Receipt_$sanitizedReceiptNum.png")
        val jsonFile = File(folder, "Receipt_$sanitizedReceiptNum.json")
        val thumbFile = File(folder, "Receipt_$sanitizedReceiptNum.thumb")

        // Draw and Generate Bitmap
        val receiptBitmap = drawReceiptBitmap(
            settings = settings,
            receiptNumber = receiptNumber,
            customerName = customerName,
            customerPhone = customerPhone,
            customerEmail = customerEmail,
            customerAddress = customerAddress,
            dateStr = dateStr,
            timeStr = timeStr,
            products = products,
            subtotal = subtotal,
            discount = discountAmount,
            vatRate = vatRate,
            vatAmount = activeVatAmount,
            total = finalTotal,
            templateName = templateName,
            notes = notes
        )

        // Save PNG Image
        try {
            FileOutputStream(imageFile).use { out ->
                receiptBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
        } catch (e: Exception) {
            Log.e("ReceiptRepository", "Error saving PNG", e)
        }

        // Save PDF using standard Android PdfDocument drawing our Bitmap
        try {
            val pdfDocument = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(receiptBitmap.width, receiptBitmap.height, 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val pdfCanvas = page.canvas
            pdfCanvas.drawBitmap(receiptBitmap, 0f, 0f, null)
            pdfDocument.finishPage(page)
            FileOutputStream(pdfFile).use { out ->
                pdfDocument.writeTo(out)
            }
            pdfDocument.close()
        } catch (e: Exception) {
            Log.e("ReceiptRepository", "Error saving PDF", e)
        }

        // Save JSON File representing metadata
        try {
            val json = JSONObject().apply {
                put("receiptNumber", receiptNumber)
                put("date", dateStr)
                put("time", timeStr)
                put("template", templateName)
                put("notes", notes)
                put("discount", discountAmount)
                put("vatRate", vatRate)
                put("total", finalTotal)
                
                val customerJson = JSONObject().apply {
                    put("name", customerName)
                    put("phone", customerPhone)
                    put("email", customerEmail)
                    put("address", customerAddress)
                }
                put("customer", customerJson)

                val productsArray = JSONArray()
                products.forEach { p ->
                    val prodJson = JSONObject().apply {
                        put("name", p.name)
                        put("quantity", p.quantity)
                        put("price", p.price)
                        put("discount", p.discount)
                        put("vat", p.vat)
                    }
                    productsArray.put(prodJson)
                }
                put("products", productsArray)
            }
            
            FileOutputStream(jsonFile).use { out ->
                OutputStreamWriter(out).use { writer ->
                    writer.write(json.toString(4))
                }
            }
        } catch (e: Exception) {
            Log.e("ReceiptRepository", "Error saving JSON", e)
        }

        // Generate tiny Thumbnail for Receipt Card
        try {
            val thumbBitmap = Bitmap.createScaledBitmap(receiptBitmap, 120, 170, true)
            FileOutputStream(thumbFile).use { out ->
                thumbBitmap.compress(Bitmap.CompressFormat.PNG, 80, out)
            }
        } catch (e: Exception) {
            Log.e("ReceiptRepository", "Error saving Thumbnail", e)
        }

        // Insert / Update in Database
        val existingReceipt = receiptDao.getReceiptByNumber(receiptNumber)
        val receiptId = if (existingReceipt != null) {
            val updated = existingReceipt.copy(
                customerName = customerName,
                customerPhone = customerPhone,
                customerEmail = customerEmail,
                customerAddress = customerAddress,
                date = dateStr,
                time = timeStr,
                subtotal = subtotal,
                discount = discountAmount,
                vat = activeVatAmount,
                total = finalTotal,
                templateName = templateName,
                notes = notes,
                pdfPath = pdfFile.absolutePath,
                imagePath = imageFile.absolutePath,
                jsonPath = jsonFile.absolutePath
            )
            receiptDao.updateReceipt(updated)
            productDao.deleteProductsForReceipt(existingReceipt.id)
            existingReceipt.id
        } else {
            val newReceipt = ReceiptEntity(
                receiptNumber = receiptNumber,
                customerName = customerName,
                customerPhone = customerPhone,
                customerEmail = customerEmail,
                customerAddress = customerAddress,
                date = dateStr,
                time = timeStr,
                subtotal = subtotal,
                discount = discountAmount,
                vat = activeVatAmount,
                total = finalTotal,
                templateName = templateName,
                notes = notes,
                pdfPath = pdfFile.absolutePath,
                imagePath = imageFile.absolutePath,
                jsonPath = jsonFile.absolutePath,
                isFavorite = false
            )
            receiptDao.insertReceipt(newReceipt)
        }

        // Insert Products in DB
        val dbProducts = products.map { it.copy(receiptId = receiptId) }
        productDao.insertProducts(dbProducts)

        // Save Customer Entity for future autocomplete suggestions
        if (customerName.trim().isNotEmpty()) {
            val existingCustList = customerDao.getAllCustomers().firstOrNull() ?: emptyList()
            val match = existingCustList.find { it.name.lowercase() == customerName.trim().lowercase() }
            if (match == null) {
                customerDao.insertCustomer(
                    CustomerEntity(
                        name = customerName.trim(),
                        phone = customerPhone.trim(),
                        email = customerEmail.trim(),
                        address = customerAddress.trim()
                    )
                )
            }
        }

        receiptId
    }

    // A helper method to draw the entire Receipt details beautifully on a custom bitmap canvas
    fun drawReceiptBitmap(
        settings: SettingsEntity,
        receiptNumber: String,
        customerName: String,
        customerPhone: String,
        customerEmail: String,
        customerAddress: String,
        dateStr: String,
        timeStr: String,
        products: List<ProductEntity>,
        subtotal: Double,
        discount: Double,
        vatRate: Double,
        vatAmount: Double,
        total: Double,
        templateName: String,
        notes: String
    ): Bitmap {
        val width = settings.receiptWidth.coerceAtLeast(380)
        // Calculate required dynamic height based on product count
        val rowHeight = 45
        val baseHeight = 650
        val height = baseHeight + (products.size * rowHeight)

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Select styles based on template
        val bgPaint = Paint().apply { color = Color.WHITE }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // Paints
        val primaryPaint = Paint().apply {
            color = when (templateName) {
                "Restaurant" -> Color.parseColor("#E65100") // Deep Orange
                "Shop" -> Color.parseColor("#1565C0") // Blue
                "Grocery" -> Color.parseColor("#2E7D32") // Green
                "Pharmacy" -> Color.parseColor("#C62828") // Red
                "Electronics" -> Color.parseColor("#37474F") // Slate Grey
                "Professional" -> Color.parseColor("#1A237E") // Royal Blue
                else -> Color.parseColor("#212121") // Charcoal
            }
            isAntiAlias = true
        }

        val textPaint = Paint().apply {
            color = Color.parseColor("#2D3748")
            textSize = 14f
            isAntiAlias = true
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }

        val titlePaint = Paint().apply {
            color = primaryPaint.color
            textSize = 28f
            isAntiAlias = true
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }

        val headerPaint = Paint().apply {
            color = primaryPaint.color
            textSize = 15f
            isAntiAlias = true
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }

        val linePaint = Paint().apply {
            color = Color.parseColor("#E2E8F0")
            strokeWidth = 1.5f
            style = Paint.Style.STROKE
        }

        val secondaryTextPaint = Paint().apply {
            color = Color.parseColor("#718096")
            textSize = 12f
            isAntiAlias = true
        }

        // Draw header
        var currentY = 50f
        
        // Business Name
        canvas.drawText(settings.businessName, 30f, currentY, titlePaint)
        currentY += 25f
        
        // Business Address & Contact Info
        canvas.drawText(settings.businessAddress, 30f, currentY, secondaryTextPaint)
        currentY += 18f
        canvas.drawText("Phone: ${settings.businessPhone} | Email: ${settings.businessEmail}", 30f, currentY, secondaryTextPaint)
        
        if (settings.businessTaxId.isNotEmpty()) {
            currentY += 18f
            canvas.drawText("TAX ID: ${settings.businessTaxId}", 30f, currentY, secondaryTextPaint)
        }

        // Invoice/Receipt design top band
        currentY += 30f
        canvas.drawRect(30f, currentY, (width - 30).toFloat(), currentY + 3f, primaryPaint)
        currentY += 25f

        // Receipt details
        canvas.drawText("Receipt No: $receiptNumber", 30f, currentY, headerPaint)
        
        // Date & Time
        val dtPaint = Paint(secondaryTextPaint).apply {
            textAlign = Paint.Align.RIGHT
        }
        canvas.drawText("Date: $dateStr  $timeStr", (width - 30).toFloat(), currentY, dtPaint)
        currentY += 30f

        // Customer details
        canvas.drawText("Billed To:", 30f, currentY, headerPaint)
        currentY += 18f
        canvas.drawText(customerName.ifEmpty { "Guest Customer" }, 30f, currentY, textPaint)
        currentY += 16f
        if (customerPhone.isNotEmpty() || customerEmail.isNotEmpty()) {
            canvas.drawText("Phone: ${customerPhone.ifEmpty { "N/A" }} | Email: ${customerEmail.ifEmpty { "N/A" }}", 30f, currentY, secondaryTextPaint)
            currentY += 16f
        }
        if (customerAddress.isNotEmpty()) {
            canvas.drawText("Address: $customerAddress", 30f, currentY, secondaryTextPaint)
            currentY += 16f
        }

        currentY += 15f
        // Draw Table Headings
        canvas.drawRect(30f, currentY, (width - 30).toFloat(), currentY + 30f, Paint().apply { color = Color.parseColor("#F7FAFC") })
        val tableHeaderPaint = Paint(textPaint).apply {
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        canvas.drawText("Item Description", 40f, currentY + 20f, tableHeaderPaint)
        canvas.drawText("Qty", (width * 0.55).toFloat(), currentY + 20f, tableHeaderPaint)
        canvas.drawText("Price", (width * 0.7).toFloat(), currentY + 20f, tableHeaderPaint)
        
        val priceAlignPaint = Paint(tableHeaderPaint).apply {
            textAlign = Paint.Align.RIGHT
        }
        canvas.drawText("Total (${settings.currency})", (width - 40).toFloat(), currentY + 20f, priceAlignPaint)
        
        currentY += 30f

        // Draw Product Rows
        products.forEach { prod ->
            canvas.drawText(prod.name, 40f, currentY + 25f, textPaint)
            canvas.drawText(String.format(Locale.US, "%.1f", prod.quantity), (width * 0.55).toFloat(), currentY + 25f, textPaint)
            canvas.drawText(String.format(Locale.US, "%.2f", prod.price), (width * 0.7).toFloat(), currentY + 25f, textPaint)
            
            val itemTotal = prod.price * prod.quantity
            val itemDisc = prod.discount
            val itemVat = (itemTotal - itemDisc) * (prod.vat / 100.0)
            val lineTotal = itemTotal - itemDisc + itemVat

            val rightAlignText = Paint(textPaint).apply { textAlign = Paint.Align.RIGHT }
            canvas.drawText(String.format(Locale.US, "%.2f", lineTotal), (width - 40).toFloat(), currentY + 25f, rightAlignText)
            
            currentY += rowHeight
            canvas.drawLine(30f, currentY, (width - 30).toFloat(), currentY, linePaint)
        }

        currentY += 20f

        // Summary Calculations (Subtotal, Discount, VAT, Grand Total)
        val summaryLabelPaint = Paint(textPaint).apply {
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val summaryValPaint = Paint(textPaint).apply {
            textAlign = Paint.Align.RIGHT
        }
        
        canvas.drawText("Subtotal:", (width * 0.6).toFloat(), currentY, summaryLabelPaint)
        canvas.drawText(String.format(Locale.US, "${settings.currency} %.2f", subtotal), (width - 40).toFloat(), currentY, summaryValPaint)
        
        if (discount > 0) {
            currentY += 22f
            canvas.drawText("Discount:", (width * 0.6).toFloat(), currentY, summaryLabelPaint)
            canvas.drawText(String.format(Locale.US, "-${settings.currency} %.2f", discount), (width - 40).toFloat(), currentY, summaryValPaint)
        }

        if (vatAmount > 0) {
            currentY += 22f
            canvas.drawText("VAT (${String.format(Locale.US, "%.1f", vatRate)}%):", (width * 0.6).toFloat(), currentY, summaryLabelPaint)
            canvas.drawText(String.format(Locale.US, "${settings.currency} %.2f", vatAmount), (width - 40).toFloat(), currentY, summaryValPaint)
        }

        currentY += 15f
        canvas.drawLine((width * 0.5).toFloat(), currentY, (width - 30).toFloat(), currentY, linePaint)
        currentY += 25f

        // GRAND TOTAL
        val totalLabelPaint = Paint(titlePaint).apply {
            textSize = 18f
        }
        val totalValPaint = Paint(titlePaint).apply {
            textSize = 20f
            textAlign = Paint.Align.RIGHT
        }
        canvas.drawText("Grand Total:", (width * 0.5).toFloat(), currentY, totalLabelPaint)
        canvas.drawText(String.format(Locale.US, "${settings.currency} %.2f", total), (width - 40).toFloat(), currentY, totalValPaint)

        currentY += 40f
        
        // Draw Notes & Terms
        if (notes.isNotEmpty()) {
            canvas.drawText("Notes / Terms:", 30f, currentY, headerPaint)
            currentY += 18f
            canvas.drawText(notes, 30f, currentY, secondaryTextPaint)
            currentY += 30f
        }

        // Simple Signature line
        canvas.drawText("Authorized Signature", (width - 150).toFloat(), currentY + 30f, secondaryTextPaint)
        canvas.drawLine((width - 170).toFloat(), currentY + 12f, (width - 30).toFloat(), currentY + 12f, linePaint)
        
        // Footer message
        currentY += 70f
        val footerPaint = Paint(secondaryTextPaint).apply {
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("Thank you for your business!", (width / 2).toFloat(), currentY, footerPaint)

        return bitmap
    }

    // Share action - generates file uri securely using FileProvider
    fun getShareUri(filePath: String): Uri? {
        val file = File(filePath)
        if (!file.exists()) return null
        return FileProvider.getUriForFile(
            context,
            "com.aistudio.infinityreceipt.fshkwa.fileprovider",
            file
        )
    }

    // Export complete ZIP Backup
    suspend fun exportBackup(): String = withContext(Dispatchers.IO) {
        val backupDir = context.getExternalFilesDir(null) ?: context.filesDir
        val backupFile = File(backupDir, "NexReceipt_Backup.zip")
        if (backupFile.exists()) {
            backupFile.delete()
        }

        // Close db connection momentarily for safe copy, or perform export copy
        db.close()

        val dbFile = context.getDatabasePath("infinity_receipt_db")
        val baseFolder = File(backupDir, "NexReceipt")

        ZipOutputStream(BufferedOutputStream(FileOutputStream(backupFile))).use { zos ->
            // Add Db
            if (dbFile.exists()) {
                addFileToZip(zos, dbFile, "infinity_receipt_db")
                // include journaling files if they exist
                val walFile = File(dbFile.absolutePath + "-wal")
                val shmFile = File(dbFile.absolutePath + "-shm")
                if (walFile.exists()) addFileToZip(zos, walFile, "infinity_receipt_db-wal")
                if (shmFile.exists()) addFileToZip(zos, shmFile, "infinity_receipt_db-shm")
            }

            // Recursively Add Receipts
            if (baseFolder.exists()) {
                addFolderToZip(zos, baseFolder, "NexReceipt")
            }
        }

        // Re-init db by accessing it
        AppDatabase.getDatabase(context)

        backupFile.absolutePath
    }

    private fun addFileToZip(zos: ZipOutputStream, file: File, entryPath: String) {
        val buffer = ByteArray(1024)
        FileInputStream(file).use { fis ->
            zos.putNextEntry(ZipEntry(entryPath))
            var len: Int
            while (fis.read(buffer).also { len = it } > 0) {
                zos.write(buffer, 0, len)
            }
            zos.closeEntry()
        }
    }

    private fun addFolderToZip(zos: ZipOutputStream, folder: File, baseName: String) {
        val files = folder.listFiles() ?: return
        for (file in files) {
            val entryPath = if (baseName.isEmpty()) file.name else "$baseName/${file.name}"
            if (file.isDirectory) {
                addFolderToZip(zos, file, entryPath)
            } else {
                addFileToZip(zos, file, entryPath)
            }
        }
    }

    // Restore ZIP Backup
    suspend fun restoreBackup(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            db.close() // Close DB safely
            val baseDir = context.getExternalFilesDir(null) ?: context.filesDir
            val dbFile = context.getDatabasePath("infinity_receipt_db")

            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                ZipInputStream(BufferedInputStream(inputStream)).use { zis ->
                    var entry: ZipEntry? = zis.nextEntry
                    val buffer = ByteArray(1024)

                    while (entry != null) {
                        val name = entry.name
                        if (name.startsWith("infinity_receipt_db")) {
                            // Extract Db
                            val targetFile = if (name == "infinity_receipt_db") dbFile else File(dbFile.parentFile, name)
                            if (!targetFile.parentFile.exists()) {
                                targetFile.parentFile.mkdirs()
                            }
                            FileOutputStream(targetFile).use { fos ->
                                var len: Int
                                while (zis.read(buffer).also { len = it } > 0) {
                                    fos.write(buffer, 0, len)
                                }
                            }
                        } else if (name.startsWith("NexReceipt")) {
                            // Extract Receipt resources
                            val targetFile = File(baseDir, name)
                            if (!targetFile.parentFile.exists()) {
                                targetFile.parentFile.mkdirs()
                            }
                            FileOutputStream(targetFile).use { fos ->
                                var len: Int
                                while (zis.read(buffer).also { len = it } > 0) {
                                    fos.write(buffer, 0, len)
                                }
                            }
                        }
                        zis.closeEntry()
                        entry = zis.nextEntry
                    }
                }
            }
            // Re-init db
            AppDatabase.getDatabase(context)
            true
        } catch (e: Exception) {
            Log.e("ReceiptRepository", "Error restoring backup", e)
            // Re-init db to prevent crash
            AppDatabase.getDatabase(context)
            false
        }
    }

    // Gemini API integration to intelligently analyze text and autocomplete the receipt form
    suspend fun parseReceiptTextWithAI(inputText: String): AIReceiptResult? = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.e("ReceiptRepository", "Gemini API key is not configured.")
            return@withContext null
        }

        try {
            val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey"
            val url = URL(endpoint)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json; utf-8")
            connection.doOutput = true

            val systemInstruction = """
                You are a business receipt parser. Parse the user's raw text and extract receipt info into JSON format:
                {
                  "customerName": "string",
                  "customerPhone": "string",
                  "customerEmail": "string",
                  "customerAddress": "string",
                  "products": [
                     { "name": "string", "quantity": 1.0, "price": 10.0 }
                  ],
                  "discount": 0.0,
                  "notes": "string"
                }
                Provide raw JSON only without markdown formatting. If some fields are missing, provide default values or empty strings.
            """.trimIndent()

            val requestBody = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", "System: $systemInstruction\nUser Text: $inputText")
                            })
                        })
                    })
                })
            }

            connection.outputStream.use { os ->
                val inputBytes = requestBody.toString().toByteArray(Charsets.UTF_8)
                os.write(inputBytes, 0, inputBytes.size)
            }

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val responseReader = BufferedReader(InputStreamReader(connection.inputStream, "utf-8"))
                val response = StringBuilder()
                var responseLine: String?
                while (responseReader.readLine().also { responseLine = it } != null) {
                    response.append(responseLine?.trim())
                }
                
                val responseJson = JSONObject(response.toString())
                val candidates = responseJson.getJSONArray("candidates")
                val parts = candidates.getJSONObject(0).getJSONObject("content").getJSONArray("parts")
                var aiResponseText = parts.getJSONObject(0).getString("text")

                // Clean potential markdown blocks
                if (aiResponseText.contains("```json")) {
                    aiResponseText = aiResponseText.substringAfter("```json").substringBefore("```")
                } else if (aiResponseText.contains("```")) {
                    aiResponseText = aiResponseText.substringAfter("```").substringBefore("```")
                }

                val parsedObj = JSONObject(aiResponseText.trim())
                val cName = parsedObj.optString("customerName", "")
                val cPhone = parsedObj.optString("customerPhone", "")
                val cEmail = parsedObj.optString("customerEmail", "")
                val cAddress = parsedObj.optString("customerAddress", "")
                val disc = parsedObj.optDouble("discount", 0.0)
                val aiNotes = parsedObj.optString("notes", "")

                val prodList = mutableListOf<AIProduct>()
                val prodArray = parsedObj.optJSONArray("products")
                if (prodArray != null) {
                    for (i in 0 until prodArray.length()) {
                        val pObj = prodArray.getJSONObject(i)
                        prodList.add(
                            AIProduct(
                                name = pObj.optString("name", "Item"),
                                quantity = pObj.optDouble("quantity", 1.0),
                                price = pObj.optDouble("price", 0.0)
                            )
                        )
                    }
                }

                return@withContext AIReceiptResult(
                    customerName = cName,
                    customerPhone = cPhone,
                    customerEmail = cEmail,
                    customerAddress = cAddress,
                    products = prodList,
                    discount = disc,
                    notes = aiNotes
                )
            } else {
                Log.e("ReceiptRepository", "HTTP error response from Gemini: $responseCode")
            }
        } catch (e: Exception) {
            Log.e("ReceiptRepository", "Error parsing receipt with Gemini API", e)
        }
        null
    }
}

// Data models for AI parsing
data class AIProduct(
    val name: String,
    val quantity: Double,
    val price: Double
)

data class AIReceiptResult(
    val customerName: String,
    val customerPhone: String,
    val customerEmail: String,
    val customerAddress: String,
    val products: List<AIProduct>,
    val discount: Double,
    val notes: String
)
