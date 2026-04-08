package com.example.toycity.data

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.snapshots
import com.google.firebase.firestore.toObjects
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class FinancialRepository {
    private val db = FirebaseFirestore.getInstance()
    private val collection = db.collection("financial_records")

    fun getRecordFlow(userId: String, monthId: String): Flow<FinancialRecord?> {
        return collection.document("${userId}_${monthId}")
            .snapshots()
            .map { it.toObject(FinancialRecord::class.java) }
    }

    fun getAllRecords(userId: String): Flow<List<FinancialRecord>> {
        return collection.whereEqualTo("userId", userId)
            .snapshots()
            .map { it.toObjects(FinancialRecord::class.java) }
    }

    fun getAbsoluteAllRecords(): Flow<List<FinancialRecord>> {
        return collection
            .snapshots()
            .map { it.toObjects(FinancialRecord::class.java) }
    }

    suspend fun getRecord(userId: String, monthId: String): FinancialRecord? {
        return try {
            val document = collection.document("${userId}_${monthId}").get().await()
            document.toObject(FinancialRecord::class.java)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun saveRecord(record: FinancialRecord) {
        try {
            collection.document("${record.userId}_${record.id}")
                .set(record, SetOptions.merge())
                .await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
