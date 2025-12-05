package com.example.fastable.data.local

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.fastable.data.models.DefaultBatch

@Dao
interface DefaultBatchDao {

    @Query("SELECT * FROM default_batch WHERE id = 1")
    fun getDefaultBatch(): LiveData<DefaultBatch?>

    @Query("SELECT * FROM default_batch WHERE id = 1")
    suspend fun getDefaultBatchDirect(): DefaultBatch?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setDefaultBatch(batch: DefaultBatch)

    @Query("DELETE FROM default_batch")
    suspend fun clearDefaultBatch()
}

