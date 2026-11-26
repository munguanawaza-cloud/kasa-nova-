package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomerDao {
    @Query("SELECT * FROM customers ORDER BY name ASC")
    fun observeAll(): Flow<List<CustomerEntity>>

    @Query("SELECT * FROM customers WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): CustomerEntity?

    @Query("SELECT * FROM customers WHERE is_synced = 0")
    suspend fun getUnsynced(): List<CustomerEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(customer: CustomerEntity): Long

    @Update
    suspend fun update(customer: CustomerEntity)

    @Query("UPDATE customers SET loyalty_points = loyalty_points + :pointsDelta, is_synced = 0, updated_at = :updatedAt WHERE id = :customerId")
    suspend fun addLoyaltyPoints(customerId: Long, pointsDelta: Int, updatedAt: Long)

    @Query("UPDATE customers SET is_synced = 1 WHERE id IN (:ids)")
    suspend fun markSynced(ids: List<Long>)
}

@Dao
interface ProductDao {
    @Query("SELECT * FROM products ORDER BY name ASC")
    fun observeAll(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): ProductEntity?

    @Query("SELECT * FROM products WHERE is_synced = 0")
    suspend fun getUnsynced(): List<ProductEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(product: ProductEntity): Long

    @Update
    suspend fun update(product: ProductEntity)

    @Delete
    suspend fun delete(product: ProductEntity)

    @Query("UPDATE products SET stock_quantity = :newStock, is_synced = 0, updated_at = :updatedAt WHERE id = :productId")
    suspend fun updateStock(productId: Long, newStock: Int, updatedAt: Long)

    @Query("UPDATE products SET is_synced = 1 WHERE id IN (:ids)")
    suspend fun markSynced(ids: List<Long>)
}

@Dao
interface SaleDao {
    @Query("SELECT * FROM sales ORDER BY created_at DESC")
    fun observeAll(): Flow<List<SaleEntity>>

    @Query("SELECT * FROM sales WHERE type = 'INSTALLMENT' AND remaining_amount > 0.0 ORDER BY due_date ASC, created_at DESC")
    fun observeActiveInstallments(): Flow<List<SaleEntity>>

    @Query("SELECT * FROM sales WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): SaleEntity?

    @Query("SELECT * FROM sales WHERE day_key = :dayKey")
    suspend fun getByDay(dayKey: String): List<SaleEntity>

    @Query("SELECT * FROM sales WHERE is_synced = 0")
    suspend fun getUnsynced(): List<SaleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSale(sale: SaleEntity): Long

    @Update
    suspend fun updateSale(sale: SaleEntity)

    @Query("UPDATE sales SET is_synced = 1 WHERE id IN (:ids)")
    suspend fun markSynced(ids: List<Long>)

    @Transaction
    suspend fun recordSaleAndDecrementStock(
        sale: SaleEntity,
        productDao: ProductDao,
        paymentDao: PaymentDao,
        customerDao: CustomerDao,
        loyaltyPointsAwarded: Int,
        dayKey: String
    ): Long {
        val product = productDao.getById(sale.productId)
            ?: throw IllegalStateException("Product not found")
        require(product.stockQuantity >= sale.quantity) { "Insufficient stock" }
        val newStock = product.stockQuantity - sale.quantity
        productDao.updateStock(product.id, newStock, sale.createdAt)
        val saleId = insertSale(sale)
        if (sale.paidAmount > 0.0) {
            paymentDao.insert(
                PaymentEntity(
                    saleId = saleId,
                    customerName = sale.customerName,
                    amount = sale.paidAmount,
                    discountApplied = 0.0,
                    currency = sale.currency,
                    paymentDate = sale.createdAt,
                    dayKey = dayKey,
                    isSynced = false
                )
            )
        }
        if (loyaltyPointsAwarded > 0) {
            customerDao.addLoyaltyPoints(sale.customerId, loyaltyPointsAwarded, sale.createdAt)
        }
        return saleId
    }
}

@Dao
interface PaymentDao {
    @Query("SELECT * FROM payments ORDER BY payment_date DESC")
    fun observeAll(): Flow<List<PaymentEntity>>

    @Query("SELECT * FROM payments WHERE sale_id = :saleId ORDER BY payment_date DESC")
    fun observeForSale(saleId: Long): Flow<List<PaymentEntity>>

    @Query("SELECT * FROM payments WHERE day_key = :dayKey")
    suspend fun getByDay(dayKey: String): List<PaymentEntity>

    @Query("SELECT * FROM payments WHERE is_synced = 0")
    suspend fun getUnsynced(): List<PaymentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(payment: PaymentEntity): Long

    @Query("UPDATE payments SET is_synced = 1 WHERE id IN (:ids)")
    suspend fun markSynced(ids: List<Long>)
}

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses ORDER BY expense_date DESC")
    fun observeAll(): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses WHERE day_key = :dayKey")
    suspend fun getByDay(dayKey: String): List<ExpenseEntity>

    @Query("SELECT * FROM expenses WHERE is_synced = 0")
    suspend fun getUnsynced(): List<ExpenseEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(expense: ExpenseEntity): Long

    @Query("UPDATE expenses SET is_synced = 1 WHERE id IN (:ids)")
    suspend fun markSynced(ids: List<Long>)
}

@Dao
interface ChatDao {
    @Query("SELECT * FROM chat_messages ORDER BY created_at ASC")
    fun observeAll(): Flow<List<ChatEntity>>

    @Query("SELECT * FROM chat_messages WHERE remote_id = :remoteId LIMIT 1")
    suspend fun findByRemoteId(remoteId: String): ChatEntity?

    @Query("SELECT * FROM chat_messages WHERE is_synced = 0")
    suspend fun getUnsynced(): List<ChatEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: ChatEntity): Long

    @Query("UPDATE chat_messages SET is_synced = 1 WHERE id IN (:ids)")
    suspend fun markSynced(ids: List<Long>)
}
