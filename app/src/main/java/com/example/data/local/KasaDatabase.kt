package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        CustomerEntity::class,
        ProductEntity::class,
        SaleEntity::class,
        PaymentEntity::class,
        ExpenseEntity::class,
        ChatEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class KasaDatabase : RoomDatabase() {
    abstract fun customerDao(): CustomerDao
    abstract fun productDao(): ProductDao
    abstract fun saleDao(): SaleDao
    abstract fun paymentDao(): PaymentDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun chatDao(): ChatDao

    companion object {
        @Volatile
        private var INSTANCE: KasaDatabase? = null

        fun getInstance(context: Context): KasaDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    KasaDatabase::class.java,
                    "kasa_nova_offline.db"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
