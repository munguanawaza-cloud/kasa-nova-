package com.example.data.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.example.data.OperationType
import com.example.data.handleFirestoreError
import com.example.data.local.KasaDatabase
import com.example.data.local.ProductCategory
import com.google.firebase.FirebaseApp
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlin.random.Random

private const val TAG = "FirebaseSyncManager"

data class SyncReport(
    val syncedCustomers: Int,
    val syncedProducts: Int,
    val syncedSales: Int,
    val syncedPayments: Int,
    val syncedExpenses: Int
) {
    val totalSynced: Int
        get() = syncedCustomers + syncedProducts + syncedSales + syncedPayments + syncedExpenses
}

data class CashDropRecord(
    val id: String,
    val amount: Double,
    val currency: String,
    val otp: String,
    val status: String, // PENDING or COMPLETED
    val agentName: String,
    val note: String,
    val createdAtMillis: Long,
    val confirmedAtMillis: Long? = null
)

data class ChatMessageRecord(
    val id: String,
    val senderRole: String, // OWNER or AGENT
    val senderName: String,
    val text: String,
    val createdAtMillis: Long
)

class FirebaseSyncManager(
    private val context: Context,
    private val localDb: KasaDatabase
) {
    private fun getAuthOrNull(): FirebaseAuth? = runCatching {
        if (FirebaseApp.getApps(context).isEmpty()) {
            FirebaseApp.initializeApp(context)
        }
        FirebaseAuth.getInstance()
    }.onFailure { err ->
        Log.w(TAG, "FirebaseAuth unavailable: ${err.message}")
    }.getOrNull()

    private fun getFirestoreOrNull(): FirebaseFirestore? = runCatching {
        if (FirebaseApp.getApps(context).isEmpty()) {
            FirebaseApp.initializeApp(context)
        }
        FirebaseFirestore.getInstance()
    }.onFailure { err ->
        Log.w(TAG, "FirebaseFirestore unavailable: ${err.message}")
    }.getOrNull()

    fun isOnline(): Boolean = runCatching {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return@runCatching false
        val network = cm.activeNetwork ?: return@runCatching false
        val caps = cm.getNetworkCapabilities(network) ?: return@runCatching false
        caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }.getOrDefault(false)

    fun observeCashDrops(): Flow<List<CashDropRecord>> = callbackFlow {
        var registration: ListenerRegistration? = null
        try {
            val uid = getAuthOrNull()?.currentUser?.uid
            val db = if (uid != null) getFirestoreOrNull() else null
            if (uid == null || db == null) {
                trySend(emptyList())
                close()
                return@callbackFlow
            }
            val path = "users/$uid/cash_drops"
            registration = db.collection("users").document(uid)
                .collection("cash_drops")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        handleFirestoreError(error, OperationType.LIST, path)
                        trySend(emptyList())
                        return@addSnapshotListener
                    }
                    val list = snapshot?.documents?.mapNotNull { doc ->
                        val amount = doc.getDouble("amount") ?: return@mapNotNull null
                        val currency = doc.getString("currency") ?: "USD"
                        val otp = doc.getString("otp") ?: "0000"
                        val status = doc.getString("status") ?: "PENDING"
                        val agentName = doc.getString("agentName") ?: "Agent"
                        val note = doc.getString("note") ?: ""
                        val createdAt = (doc.get("createdAt") as? Timestamp)?.toDate()?.time
                            ?: System.currentTimeMillis()
                        val confirmedAt = (doc.get("confirmedAt") as? Timestamp)?.toDate()?.time
                        CashDropRecord(
                            id = doc.id,
                            amount = amount,
                            currency = currency,
                            otp = otp,
                            status = status,
                            agentName = agentName,
                            note = note,
                            createdAtMillis = createdAt,
                            confirmedAtMillis = confirmedAt
                        )
                    } ?: emptyList()
                    trySend(list)
                }
        } catch (t: Throwable) {
            Log.w(TAG, "observeCashDrops running in local-only mode: ${t.message}")
            trySend(emptyList())
            close()
        }
        awaitClose {
            runCatching { registration?.remove() }
        }
    }

    suspend fun createCashDrop(
        amount: Double,
        currency: String,
        agentName: String,
        note: String
    ): Result<CashDropRecord> = withContext(Dispatchers.IO) {
        if (!isOnline()) return@withContext Result.failure(IllegalStateException("Offline"))
        val uid = getAuthOrNull()?.currentUser?.uid
            ?: return@withContext Result.failure(IllegalStateException("Google Sign-In required"))
        val db = getFirestoreOrNull()
            ?: return@withContext Result.failure(IllegalStateException("Cloud database unavailable"))
        val dropId = "drop_${System.currentTimeMillis()}_${Random.nextInt(100, 999)}"
        val otp = Random.nextInt(1000, 10000).toString()
        val path = "users/$uid/cash_drops/$dropId"
        try {
            require(amount > 0.0) { "Amount must be positive" }
            val safeAgent = agentName.trim().ifBlank { "Agent Kolwezi" }.take(80)
            val safeNote = note.trim().take(200)
            db.collection("users").document(uid)
                .collection("cash_drops").document(dropId)
                .set(
                    mapOf(
                        "userId" to uid,
                        "amount" to amount,
                        "currency" to currency,
                        "otp" to otp,
                        "status" to "PENDING",
                        "agentName" to safeAgent,
                        "note" to safeNote,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(
                CashDropRecord(
                    id = dropId,
                    amount = amount,
                    currency = currency,
                    otp = otp,
                    status = "PENDING",
                    agentName = safeAgent,
                    note = safeNote,
                    createdAtMillis = System.currentTimeMillis()
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun confirmCashDropWithOtp(drop: CashDropRecord, enteredOtp: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            if (!isOnline()) return@withContext Result.failure(IllegalStateException("Offline"))
            val uid = getAuthOrNull()?.currentUser?.uid
                ?: return@withContext Result.failure(IllegalStateException("Google Sign-In required"))
            val db = getFirestoreOrNull()
                ?: return@withContext Result.failure(IllegalStateException("Cloud database unavailable"))
            if (enteredOtp.trim() != drop.otp) {
                return@withContext Result.failure(IllegalArgumentException("INVALID_OTP"))
            }
            val path = "users/$uid/cash_drops/${drop.id}"
            try {
                db.collection("users").document(uid)
                    .collection("cash_drops").document(drop.id)
                    .update(
                        mapOf(
                            "status" to "COMPLETED",
                            "confirmedAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                Result.success(Unit)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.WRITE, path)
                Result.failure(e)
            }
        }

    fun observeChatMessages(): Flow<List<ChatMessageRecord>> = callbackFlow {
        var registration: ListenerRegistration? = null
        try {
            val uid = getAuthOrNull()?.currentUser?.uid
            val db = if (uid != null) getFirestoreOrNull() else null
            if (uid == null || db == null) {
                trySend(emptyList())
                close()
                return@callbackFlow
            }
            val path = "users/$uid/chat_messages"
            registration = db.collection("users").document(uid)
                .collection("chat_messages")
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        handleFirestoreError(error, OperationType.LIST, path)
                        trySend(emptyList())
                        return@addSnapshotListener
                    }
                    val list = snapshot?.documents?.mapNotNull { doc ->
                        val role = doc.getString("senderRole") ?: "AGENT"
                        val name = doc.getString("senderName") ?: role
                        val text = doc.getString("text") ?: return@mapNotNull null
                        val createdAt = (doc.get("createdAt") as? Timestamp)?.toDate()?.time
                            ?: System.currentTimeMillis()
                        ChatMessageRecord(
                            id = doc.id,
                            senderRole = role,
                            senderName = name,
                            text = text,
                            createdAtMillis = createdAt
                        )
                    } ?: emptyList()
                    trySend(list)
                }
        } catch (t: Throwable) {
            Log.w(TAG, "observeChatMessages running in local-only mode: ${t.message}")
            trySend(emptyList())
            close()
        }
        awaitClose {
            runCatching { registration?.remove() }
        }
    }

    suspend fun sendChatMessage(
        senderRole: String,
        senderName: String,
        text: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        if (!isOnline()) return@withContext Result.failure(IllegalStateException("Offline · saved locally"))
        val uid = getAuthOrNull()?.currentUser?.uid
            ?: return@withContext Result.failure(IllegalStateException("Local-only mode · saved locally"))
        val db = getFirestoreOrNull()
            ?: return@withContext Result.failure(IllegalStateException("Cloud unavailable · saved locally"))
        val msgId = "msg_${System.currentTimeMillis()}_${Random.nextInt(100, 999)}"
        val path = "users/$uid/chat_messages/$msgId"
        try {
            val cleanText = text.trim()
            require(cleanText.isNotEmpty()) { "Empty message" }
            db.collection("users").document(uid)
                .collection("chat_messages").document(msgId)
                .set(
                    mapOf(
                        "userId" to uid,
                        "senderRole" to if (senderRole == "OWNER") "OWNER" else "AGENT",
                        "senderName" to senderName.trim().ifBlank { senderRole }.take(80),
                        "text" to cleanText.take(1000),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun syncPendingData(): Result<SyncReport> = withContext(Dispatchers.IO) {
        if (!isOnline()) {
            return@withContext Result.failure(IllegalStateException("Device is offline (local Room mode)"))
        }
        val uid = getAuthOrNull()?.currentUser?.uid
            ?: return@withContext Result.failure(IllegalStateException("Local-only mode (not signed in)"))
        val db = getFirestoreOrNull()
            ?: return@withContext Result.failure(IllegalStateException("Cloud sync unavailable"))

        var currentPath = "users/$uid"
        try {
            val userDoc = db.collection("users").document(uid)

            val unsyncedCustomers = localDb.customerDao().getUnsynced()
            for (c in unsyncedCustomers) {
                val docRef = userDoc.collection("customers").document("cust_${c.id}")
                currentPath = docRef.path
                val exists = docRef.get().await().exists()
                if (exists) {
                    docRef.update(
                        mapOf(
                            "name" to c.name.take(120),
                            "phone" to c.phone.take(40),
                            "address" to c.address.take(200),
                            "gpsLat" to c.gpsLat,
                            "gpsLng" to c.gpsLng,
                            "source" to c.source,
                            "loyaltyPoints" to c.loyaltyPoints,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                } else {
                    docRef.set(
                        mapOf(
                            "userId" to uid,
                            "localId" to c.id,
                            "name" to c.name.take(120),
                            "phone" to c.phone.take(40),
                            "address" to c.address.take(200),
                            "gpsLat" to c.gpsLat,
                            "gpsLng" to c.gpsLng,
                            "source" to c.source,
                            "loyaltyPoints" to c.loyaltyPoints,
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                }
            }
            if (unsyncedCustomers.isNotEmpty()) {
                localDb.customerDao().markSynced(unsyncedCustomers.map { it.id })
            }

            val unsyncedProducts = localDb.productDao().getUnsynced()
            for (p in unsyncedProducts) {
                val safeCategory = if (p.category in ProductCategory.ALL) p.category else ProductCategory.CUISINE
                val docRef = userDoc.collection("products").document("prod_${p.id}")
                currentPath = docRef.path
                val exists = docRef.get().await().exists()
                if (exists) {
                    docRef.update(
                        mapOf(
                            "name" to p.name.take(120),
                            "category" to safeCategory,
                            "costUsd" to p.costUsd,
                            "costCdf" to p.costCdf,
                            "priceUsd" to p.priceUsd,
                            "priceCdf" to p.priceCdf,
                            "stockQuantity" to p.stockQuantity,
                            "videoUrl" to p.videoUrl.take(500),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                } else {
                    docRef.set(
                        mapOf(
                            "userId" to uid,
                            "localId" to p.id,
                            "name" to p.name.take(120),
                            "category" to safeCategory,
                            "costUsd" to p.costUsd,
                            "costCdf" to p.costCdf,
                            "priceUsd" to p.priceUsd,
                            "priceCdf" to p.priceCdf,
                            "stockQuantity" to p.stockQuantity,
                            "videoUrl" to p.videoUrl.take(500),
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                }
            }
            if (unsyncedProducts.isNotEmpty()) {
                localDb.productDao().markSynced(unsyncedProducts.map { it.id })
            }

            val unsyncedSales = localDb.saleDao().getUnsynced()
            for (s in unsyncedSales) {
                val docRef = userDoc.collection("sales").document("sale_${s.id}")
                currentPath = docRef.path
                val exists = docRef.get().await().exists()
                if (exists) {
                    docRef.update(
                        mapOf(
                            "paidAmount" to s.paidAmount,
                            "remainingAmount" to s.remainingAmount,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                } else {
                    docRef.set(
                        mapOf(
                            "userId" to uid,
                            "localId" to s.id,
                            "customerId" to s.customerId,
                            "productId" to s.productId,
                            "customerName" to s.customerName.take(120),
                            "productName" to s.productName.take(120),
                            "customerSource" to s.customerSource,
                            "type" to s.type,
                            "quantity" to s.quantity,
                            "totalPrice" to s.totalPrice,
                            "profitAmount" to s.profitAmount,
                            "currency" to s.currency,
                            "paidAmount" to s.paidAmount,
                            "remainingAmount" to s.remainingAmount,
                            "dueDate" to s.dueDate,
                            "signaturePath" to s.signaturePath.take(500),
                            "isGiveawayEntry" to s.isGiveawayEntry,
                            "dayKey" to s.dayKey,
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                }
            }
            if (unsyncedSales.isNotEmpty()) {
                localDb.saleDao().markSynced(unsyncedSales.map { it.id })
            }

            val unsyncedPayments = localDb.paymentDao().getUnsynced()
            for (pay in unsyncedPayments) {
                val docRef = userDoc.collection("payments").document("pay_${pay.id}")
                currentPath = docRef.path
                if (!docRef.get().await().exists()) {
                    docRef.set(
                        mapOf(
                            "userId" to uid,
                            "localId" to pay.id,
                            "saleId" to pay.saleId,
                            "customerName" to pay.customerName.take(120),
                            "amount" to pay.amount,
                            "discountApplied" to pay.discountApplied,
                            "currency" to pay.currency,
                            "dayKey" to pay.dayKey,
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                }
            }
            if (unsyncedPayments.isNotEmpty()) {
                localDb.paymentDao().markSynced(unsyncedPayments.map { it.id })
            }

            val unsyncedExpenses = localDb.expenseDao().getUnsynced()
            for (exp in unsyncedExpenses) {
                val docRef = userDoc.collection("expenses").document("exp_${exp.id}")
                currentPath = docRef.path
                if (!docRef.get().await().exists()) {
                    docRef.set(
                        mapOf(
                            "userId" to uid,
                            "localId" to exp.id,
                            "title" to exp.title.take(150),
                            "amount" to exp.amount,
                            "currency" to exp.currency,
                            "dayKey" to exp.dayKey,
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                }
            }
            if (unsyncedExpenses.isNotEmpty()) {
                localDb.expenseDao().markSynced(unsyncedExpenses.map { it.id })
            }

            Result.success(
                SyncReport(
                    syncedCustomers = unsyncedCustomers.size,
                    syncedProducts = unsyncedProducts.size,
                    syncedSales = unsyncedSales.size,
                    syncedPayments = unsyncedPayments.size,
                    syncedExpenses = unsyncedExpenses.size
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, currentPath)
            Result.failure(e)
        }
    }
}
