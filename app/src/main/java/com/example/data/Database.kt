package com.example.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

// 1. Receipts Entity
@Entity(tableName = "receipts")
data class ReceiptEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val receiptNumber: String,
    val customerName: String,
    val customerPhone: String,
    val customerEmail: String,
    val customerAddress: String,
    val date: String,
    val time: String,
    val subtotal: Double,
    val discount: Double,
    val vat: Double,
    val total: Double,
    val templateName: String,
    val notes: String,
    val pdfPath: String,
    val imagePath: String,
    val jsonPath: String,
    val isFavorite: Boolean = false
)

// 2. Customers Entity
@Entity(tableName = "customers")
data class CustomerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String,
    val email: String,
    val address: String
)

// 3. Products Entity
@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val receiptId: Long,
    val name: String,
    val quantity: Double,
    val price: Double,
    val discount: Double,
    val vat: Double
)

// 4. Settings Entity
@Entity(tableName = "settings")
data class SettingsEntity(
    @PrimaryKey val id: Int = 1,
    val businessName: String = "NexVora Labs",
    val businessLogo: String = "", // Base64 or Path
    val businessAddress: String = "Dhaka, Bangladesh",
    val businessPhone: String = "+880123456789",
    val businessEmail: String = "info@nexvora.com",
    val businessTaxId: String = "",
    val currency: String = "BDT", // BDT or USD
    val taxRate: Double = 0.0,
    val theme: String = "Dark", // Light or Dark
    val signaturePath: String = "",
    val printerSize: String = "80mm",
    val receiptWidth: Int = 380,
    val dateFormat: String = "dd MMM yyyy"
)

// 5. Templates Entity
@Entity(tableName = "templates")
data class TemplateEntity(
    @PrimaryKey val id: String,
    val name: String,
    val category: String, // Restaurant, Shop, Grocery, etc.
    val isFavorite: Boolean = false
)

// --- DAOs ---

@Dao
interface ReceiptDao {
    @Query("SELECT * FROM receipts ORDER BY id DESC")
    fun getAllReceipts(): Flow<List<ReceiptEntity>>

    @Query("SELECT * FROM receipts WHERE id = :id")
    suspend fun getReceiptById(id: Long): ReceiptEntity?

    @Query("SELECT * FROM receipts WHERE receiptNumber = :number")
    suspend fun getReceiptByNumber(number: String): ReceiptEntity?

    @Query("SELECT * FROM receipts WHERE customerName LIKE '%' || :query || '%' OR customerPhone LIKE '%' || :query || '%' OR receiptNumber LIKE '%' || :query || '%'")
    fun searchReceipts(query: String): Flow<List<ReceiptEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReceipt(receipt: ReceiptEntity): Long

    @Update
    suspend fun updateReceipt(receipt: ReceiptEntity)

    @Delete
    suspend fun deleteReceipt(receipt: ReceiptEntity)
}

@Dao
interface CustomerDao {
    @Query("SELECT * FROM customers ORDER BY name ASC")
    fun getAllCustomers(): Flow<List<CustomerEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomer(customer: CustomerEntity): Long

    @Query("DELETE FROM customers WHERE id = :id")
    suspend fun deleteCustomer(id: Long)
}

@Dao
interface ProductDao {
    @Query("SELECT * FROM products WHERE receiptId = :receiptId")
    suspend fun getProductsForReceipt(receiptId: Long): List<ProductEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProducts(products: List<ProductEntity>)

    @Query("DELETE FROM products WHERE receiptId = :receiptId")
    suspend fun deleteProductsForReceipt(receiptId: Long)
}

@Dao
interface SettingsDao {
    @Query("SELECT * FROM settings WHERE id = 1")
    fun getSettings(): Flow<SettingsEntity?>

    @Query("SELECT * FROM settings WHERE id = 1")
    suspend fun getSettingsOnce(): SettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSettings(settings: SettingsEntity)
}

@Dao
interface TemplateDao {
    @Query("SELECT * FROM templates ORDER BY name ASC")
    fun getAllTemplates(): Flow<List<TemplateEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplates(templates: List<TemplateEntity>)

    @Update
    suspend fun updateTemplate(template: TemplateEntity)
}

// --- AppDatabase ---

@Database(
    entities = [
        ReceiptEntity::class,
        CustomerEntity::class,
        ProductEntity::class,
        SettingsEntity::class,
        TemplateEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun receiptDao(): ReceiptDao
    abstract fun customerDao(): CustomerDao
    abstract fun productDao(): ProductDao
    abstract fun settingsDao(): SettingsDao
    abstract fun templateDao(): TemplateDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "infinity_receipt_db"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
