package com.easycode.ide.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.easycode.ide.data.model.FiddleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FiddleDao {

    @Query("SELECT * FROM fiddles ORDER BY updatedAt DESC")
    fun getAllFiddles(): Flow<List<FiddleEntity>>

    @Query("SELECT * FROM fiddles WHERE id = :id")
    suspend fun getFiddleById(id: Long): FiddleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFiddle(fiddle: FiddleEntity): Long

    @Update
    suspend fun updateFiddle(fiddle: FiddleEntity)

    @Delete
    suspend fun deleteFiddle(fiddle: FiddleEntity)

    @Query("DELETE FROM fiddles WHERE id = :id")
    suspend fun deleteFiddleById(id: Long)

    @Query("SELECT * FROM fiddles WHERE title LIKE '%' || :query || '%' ORDER BY updatedAt DESC")
    fun searchFiddles(query: String): Flow<List<FiddleEntity>>
}
