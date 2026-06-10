package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.model.RecycleBinEntity

class RecycleBinRepository(private val db: AppDatabase) {
    val recycleBinDao = db.recycleBinDao()

    fun getItemsByFarm(farmName: String) = recycleBinDao.getRecycleBinItemsByFarm(farmName)
    suspend fun insertItem(item: RecycleBinEntity) = recycleBinDao.insertRecycleBinItem(item)
    suspend fun deleteItem(item: RecycleBinEntity) = recycleBinDao.deleteRecycleBinItem(item)
    suspend fun deleteItemsOlderThan(cutoffTime: Long) = recycleBinDao.deleteItemsOlderThan(cutoffTime)
    suspend fun deleteAllRecycleBinOfFarm(farmName: String) = recycleBinDao.deleteAllRecycleBinOfFarm(farmName)
}
