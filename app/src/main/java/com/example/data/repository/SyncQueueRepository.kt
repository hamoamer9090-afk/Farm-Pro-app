package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.model.SyncQueueEntity
import kotlinx.coroutines.flow.Flow

class SyncQueueRepository(private val db: AppDatabase) {

    // جلب جميع العمليات المعلقة في طابور المزامنة
    fun getPendingSyncItems(): Flow<List<SyncQueueEntity>> = db.syncQueueDao().getPendingSyncItems()

    // إضافة عملية جديدة للطابور عند انقطاع الإنترنت أو وجود تعديل محلي
    suspend fun enqueueOperation(
        farmName: String,
        operationType: String,
        collectionName: String,
        documentId: String,
        payloadJson: String
    ) {
        val syncItem = SyncQueueEntity(
            farmName = farmName,
            operationType = operationType,
            collectionName = collectionName,
            documentId = documentId,
            payloadJson = payloadJson
        )
        db.syncQueueDao().insertSyncItem(syncItem)
    }

    // حذف العملية من الطابور بعد نجاح رفعها السحابي (Firestore)
    suspend fun dequeueOperation(syncItem: SyncQueueEntity) {
        db.syncQueueDao().deleteSyncItem(syncItem)
    }
}
