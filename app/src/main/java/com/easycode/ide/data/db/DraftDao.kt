package com.easycode.ide.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.easycode.ide.data.model.DraftEntity

@Dao
interface DraftDao {
    @Query("SELECT * FROM drafts WHERE id = 0 LIMIT 1")
    suspend fun getDraft(): DraftEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveDraft(draft: DraftEntity)

    @Query("DELETE FROM drafts WHERE id = 0")
    suspend fun clearDraft()
}
