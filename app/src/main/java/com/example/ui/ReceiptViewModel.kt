package com.example.ui

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.CustomerEntity
import com.example.data.ProductEntity
import com.example.data.ReceiptEntity
import com.example.data.ReceiptRepository
import com.example.data.ReceiptWithProducts
import com.example.data.SettingsEntity
import com.example.data.TemplateEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class AppScreen {
    Splash,
    Intro,
    ProfileSetup,
    Dashboard,
    CreateReceipt,
    History,
    Reports,
    Settings
}

class ReceiptViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = ReceiptRepository(application.applicationContext)

    suspend fun getReceiptWithProducts(id: Long): ReceiptWithProducts? {
        return repository.getReceiptWithProducts(id)
    }

    // Current screen navigation state
    var currentScreen by mutableStateOf(AppScreen.Splash)
        private set

    // Setup Wizard Preferences
    var selectedCurrency by mutableStateOf("BDT")
    var selectedTheme by mutableStateOf("Dark")

    // Database Flows
    val settings: StateFlow<SettingsEntity?> = repository.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    val templates: StateFlow<List<TemplateEntity>> = repository.allTemplates.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Search query & combined Receipt flow
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    val receipts: StateFlow<List<ReceiptEntity>> = combine(
        repository.allReceipts,
        _searchQuery
    ) { all, query ->
        if (query.trim().isEmpty()) {
            all
        } else {
            all.filter {
                it.receiptNumber.lowercase().contains(query.lowercase()) ||
                it.customerName.lowercase().contains(query.lowercase()) ||
                it.customerPhone.lowercase().contains(query.lowercase()) ||
                it.date.lowercase().contains(query.lowercase()) ||
                it.total.toString().contains(query)
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val customers: StateFlow<List<CustomerEntity>> = repository.allCustomers.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Editing Receipt States (dynamic for Live Preview)
    var receiptNumber by mutableStateOf("")
    var customerName by mutableStateOf("")
    var customerPhone by mutableStateOf("")
    var customerEmail by mutableStateOf("")
    var customerAddress by mutableStateOf("")
    var dateStr by mutableStateOf("")
    var timeStr by mutableStateOf("")
    val editingProducts = mutableStateListOf<ProductEntity>()
    var discountStr by mutableStateOf("0")
    var vatRateStr by mutableStateOf("0")
    var templateName by mutableStateOf("Professional")
    var notes by mutableStateOf("Thank you for your business!")

    // Receipt details drawer or overlay
    var activeReceiptWithProducts by mutableStateOf<ReceiptWithProducts?>(null)

    // AI smart auto-filling status message
    var aiParsingStatus by mutableStateOf("")
        private set

    init {
        viewModelScope.launch {
            repository.initDefaults()
            
            // Check if First-Launch: Show intro if name is default or empty
            val currentSettings = repository.settingsDao.getSettingsOnce()
            if (currentSettings == null || currentSettings.businessName == "NexVora Labs") {
                // Not configured or default, let's proceed to splash first, then intro
                navigateToScreen(AppScreen.Splash)
            } else {
                selectedCurrency = currentSettings.currency
                selectedTheme = currentSettings.theme
                navigateToScreen(AppScreen.Splash)
            }
        }
    }

    fun navigateToScreen(screen: AppScreen) {
        currentScreen = screen
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    // Prepare Form for Create/Edit Receipt
    fun startNewReceipt(editingReceipt: ReceiptEntity? = null) {
        viewModelScope.launch {
            val currentSettings = settings.value ?: SettingsEntity()
            if (editingReceipt == null) {
                // Generate automated Receipt Number e.g. RCP-2026-0702-0001
                val sdfNum = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US)
                receiptNumber = "RCP-${sdfNum.format(Date())}"
                customerName = ""
                customerPhone = ""
                customerEmail = ""
                customerAddress = ""
                
                val sdfDate = SimpleDateFormat("dd/MM/yyyy", Locale.US)
                val sdfTime = SimpleDateFormat("HH:mm", Locale.US)
                dateStr = sdfDate.format(Date())
                timeStr = sdfTime.format(Date())
                
                editingProducts.clear()
                // Start with one empty item line
                editingProducts.add(ProductEntity(name = "", quantity = 1.0, price = 0.0, discount = 0.0, vat = 0.0, receiptId = 0))
                
                discountStr = "0"
                vatRateStr = currentSettings.taxRate.toString()
                templateName = currentSettings.theme // Default to professional/matching
                notes = "Thank you for your patronage!"
            } else {
                // Edit existing Receipt
                receiptNumber = editingReceipt.receiptNumber
                customerName = editingReceipt.customerName
                customerPhone = editingReceipt.customerPhone
                customerEmail = editingReceipt.customerEmail
                customerAddress = editingReceipt.customerAddress
                dateStr = editingReceipt.date
                timeStr = editingReceipt.time
                discountStr = editingReceipt.discount.toString()
                
                val sub = editingReceipt.subtotal
                // Calculate VAT rate from vat and sub
                val rate = if (sub > 0) (editingReceipt.vat / (sub - editingReceipt.discount)) * 100.0 else 0.0
                vatRateStr = String.format(Locale.US, "%.1f", rate)
                templateName = editingReceipt.templateName
                notes = editingReceipt.notes

                editingProducts.clear()
                val currentProds = repository.productDao.getProductsForReceipt(editingReceipt.id)
                if (currentProds.isNotEmpty()) {
                    editingProducts.addAll(currentProds)
                } else {
                    editingProducts.add(ProductEntity(name = "", quantity = 1.0, price = 0.0, discount = 0.0, vat = 0.0, receiptId = editingReceipt.id))
                }
            }
        }
    }

    fun addEmptyProduct() {
        editingProducts.add(ProductEntity(name = "", quantity = 1.0, price = 0.0, discount = 0.0, vat = 0.0, receiptId = 0))
    }

    fun removeProductAt(index: Int) {
        if (editingProducts.size > 1) {
            editingProducts.removeAt(index)
        }
    }

    // Save Setup Settings from wizard
    fun saveInitialSetup(businessName: String, address: String, phone: String, email: String) {
        viewModelScope.launch {
            val setupSettings = SettingsEntity(
                businessName = businessName,
                businessAddress = address,
                businessPhone = phone,
                businessEmail = email,
                currency = selectedCurrency,
                theme = selectedTheme
            )
            repository.saveSettings(setupSettings)
            navigateToScreen(AppScreen.Dashboard)
        }
    }

    // Save Settings from general settings screen
    fun updateGeneralSettings(updated: SettingsEntity) {
        viewModelScope.launch {
            repository.saveSettings(updated)
            selectedTheme = updated.theme
            selectedCurrency = updated.currency
        }
    }

    // Toggle favorite template
    fun toggleFavoriteTemplate(id: String) {
        viewModelScope.launch {
            repository.toggleFavoriteTemplate(id)
        }
    }

    // Save receipt to Room DB and write PDF, PNG files
    fun generateReceipt(onSuccess: (Long) -> Unit) {
        viewModelScope.launch {
            try {
                // Sanitize products
                val validProducts = editingProducts.filter { it.name.trim().isNotEmpty() && it.price > 0 }
                if (validProducts.isEmpty()) {
                    // add default if empty to prevent crash
                    return@launch
                }

                val discVal = discountStr.toDoubleOrNull() ?: 0.0
                val vatVal = vatRateStr.toDoubleOrNull() ?: 0.0

                val receiptId = repository.generateAndSaveReceipt(
                    receiptNumber = receiptNumber.trim(),
                    customerName = customerName.trim(),
                    customerPhone = customerPhone.trim(),
                    customerEmail = customerEmail.trim(),
                    customerAddress = customerAddress.trim(),
                    dateStr = dateStr.trim(),
                    timeStr = timeStr.trim(),
                    products = validProducts,
                    discount = discVal,
                    vatRate = vatVal,
                    templateName = templateName,
                    notes = notes.trim()
                )

                onSuccess(receiptId)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Delete Receipt
    fun deleteReceipt(receipt: ReceiptEntity) {
        viewModelScope.launch {
            repository.deleteReceipt(receipt)
            // If active receipt is deleted, dismiss details
            if (activeReceiptWithProducts?.receipt?.id == receipt.id) {
                activeReceiptWithProducts = null
            }
        }
    }

    // Duplicate Receipt
    fun duplicateReceipt(receipt: ReceiptEntity) {
        viewModelScope.launch {
            val prods = repository.productDao.getProductsForReceipt(receipt.id)
            val sdfNum = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US)
            val newNum = "RCP-${sdfNum.format(Date())}"
            
            repository.generateAndSaveReceipt(
                receiptNumber = newNum,
                customerName = receipt.customerName,
                customerPhone = receipt.customerPhone,
                customerEmail = receipt.customerEmail,
                customerAddress = receipt.customerAddress,
                dateStr = receipt.date,
                timeStr = receipt.time,
                products = prods,
                discount = receipt.discount,
                vatRate = if (receipt.subtotal > 0) (receipt.vat / (receipt.subtotal - receipt.discount)) * 100.0 else 0.0,
                templateName = receipt.templateName,
                notes = receipt.notes
            )
        }
    }

    // Backup ZIP exports
    var backupExportPath by mutableStateOf<String?>(null)
    fun createBackup(onResult: (String?) -> Unit) {
        viewModelScope.launch {
            try {
                val path = repository.exportBackup()
                backupExportPath = path
                onResult(path)
            } catch (e: Exception) {
                onResult(null)
            }
        }
    }

    // Restore ZIP import
    fun restoreBackup(uri: Uri, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = repository.restoreBackup(uri)
            onResult(success)
        }
    }

    // Share File Helper
    fun getShareUri(filePath: String): Uri? {
        return repository.getShareUri(filePath)
    }

    // Gemini Smart parse helper
    fun parseWithAI(rawText: String) {
        if (rawText.trim().isEmpty()) return
        aiParsingStatus = "Parsing with Gemini..."
        viewModelScope.launch {
            val result = repository.parseReceiptTextWithAI(rawText)
            if (result != null) {
                customerName = result.customerName
                customerPhone = result.customerPhone
                customerEmail = result.customerEmail
                customerAddress = result.customerAddress
                discountStr = String.format(Locale.US, "%.0f", result.discount)
                notes = result.notes.ifEmpty { notes }
                
                if (result.products.isNotEmpty()) {
                    editingProducts.clear()
                    result.products.forEach { p ->
                        editingProducts.add(
                            ProductEntity(
                                name = p.name,
                                quantity = p.quantity,
                                price = p.price,
                                discount = 0.0,
                                vat = 0.0,
                                receiptId = 0
                            )
                        )
                    }
                }
                aiParsingStatus = "Successfully parsed!"
            } else {
                aiParsingStatus = "AI Parsing failed. Check connection or key."
            }
        }
    }
}
