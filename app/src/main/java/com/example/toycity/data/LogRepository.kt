package com.example.toycity.data

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import com.google.firebase.firestore.toObjects
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class LogRepository {
    private val db = FirebaseFirestore.getInstance()
    private val collection = db.collection("user_logs")

    fun getAllLogs(): Flow<List<UserLog>> {
        return collection
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .snapshots()
            .map { it.toObjects(UserLog::class.java) }
    }

    suspend fun addLog(log: UserLog) {
        try {
            val docRef = collection.document()
            collection.document(docRef.id).set(log.copy(id = docRef.id)).await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun clearAllLogs() {
        try {
            val snapshot = collection.get().await()
            val batch = db.batch()
            for (document in snapshot.documents) {
                batch.delete(document.reference)
            }
            batch.commit().await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
