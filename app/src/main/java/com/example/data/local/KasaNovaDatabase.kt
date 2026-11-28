package com.example.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import kotlinx.coroutines.flow.Flow

// ==========================================
// 1. Entities & Definitions (HAMADA GROUP Cash-Flow)
// ==========================================

enum class TransactionType { SALE, COLLECTION, EXPENSE }
enum class CurrencyType { USD, CDF }
enum class PaymentChannel { CASH_USD, CASH_CDF, MOBILE_MONEY, BANK_TRANSFER }
enum class SyncStatus { PENDING, SYNCED }

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: TransactionType,
    val amount: Double,
    val currency: CurrencyType,
    val paymentChannel: PaymentChannel,
    val customerName: String? = null,
    val costOfGoods: Double? = null,
    val description: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val syncStatus: SyncStatus = SyncStatus.PENDING
)

class KasaTransactionConverters {
    @TypeConverter
    fun fromTransactionType(value: TransactionType): String = value.name

    @TypeConverter
    fun toTransactionType(value: String): TransactionType = TransactionType.valueOf(value)

    @TypeConverter
    fun fromCurrencyType(value: CurrencyType): String = value.name

    @TypeConverter
    fun toCurrencyType(value: String): CurrencyType = CurrencyType.valueOf(value)

    @TypeConverter
    fun fromPaymentChannel(value: PaymentChannel): String = value.name

    @TypeConverter
    fun toPaymentChannel(value: String): PaymentChannel = PaymentChannel.valueOf(value)

    @TypeConverter
    fun fromSyncStatus(value: SyncStatus): String = value.name

    @TypeConverter
    fun toSyncStatus(value: String): SyncStatus = SyncStatus.valueOf(value)
}

// ==========================================
// 2. Data Access Object (DAO)
// ==========================================

@Dao
interface TransactionDao {
    @Insert
    suspend fun insertTransaction(transaction: TransactionEntity)

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    // Actual USD liquidity = (Sales + Collections) - Expenses
    @Query(
        """
        SELECT COALESCE(SUM(CASE WHEN type = 'EXPENSE' THEN -amount ELSE amount END), 0.0)
        FROM transactions
        WHERE currency = 'USD'
        """
    )
    fun getCashBalanceUSD(): Flow<Double?>

    // Actual CDF liquidity = (Sales + Collections) - Expenses
    @Query(
        """
        SELECT COALESCE(SUM(CASE WHEN type = 'EXPENSE' THEN -amount ELSE amount END), 0.0)
        FROM transactions
        WHERE currency = 'CDF'
        """
    )
    fun getCashBalanceCDF(): Flow<Double?>

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM transactions WHERE type = 'SALE' AND currency = 'USD'")
    fun getTotalSalesUSD(): Flow<Double?>

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM transactions WHERE type = 'EXPENSE' AND currency = 'USD'")
    fun getTotalExpensesUSD(): Flow<Double?>

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM transactions WHERE type = 'SALE' AND currency = 'CDF'")
    fun getTotalSalesCDF(): Flow<Double?>

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM transactions WHERE type = 'EXPENSE' AND currency = 'CDF'")
    fun getTotalExpensesCDF(): Flow<Double?>

    @Query("SELECT * FROM transactions WHERE syncStatus = 'PENDING'")
    suspend fun getPendingTransactions(): List<TransactionEntity>
}

// ==========================================
// 3. Room Database
// ==========================================

@Database(entities = [TransactionEntity::class], version = 1, exportSchema = false)
@TypeConverters(KasaTransactionConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "kasa_nova_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
