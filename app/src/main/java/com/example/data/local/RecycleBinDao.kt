package com.example.data.local

import androidx.room.*
import com.example.data.model.RecycleBinEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecycleBinDao {
    @Query("SELECT * FROM recycle_bin WHERE farmName = :farmName ORDER BY deletedAt DESC")
    fun getRecycleBinItemsByFarm(farmName: String): Flow<List<RecycleBinEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecycleBinItem(item: RecycleBinEntity)

    @Delete
    suspend fun deleteRecycleBinItem(item: RecycleBinEntity)

    @Query("DELETE FROM recycle_bin WHERE deletedAt < :cutoffTime")
    suspend fun deleteItemsOlderThan(cutoffTime: Long)

    @Query("DELETE FROM recycle_bin WHERE farmName = :farmName")
    suspend fun deleteAllRecycleBinOfFarm(farmName: String)
}
