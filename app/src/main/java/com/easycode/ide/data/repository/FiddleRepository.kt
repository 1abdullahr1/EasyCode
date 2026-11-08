package com.easycode.ide.data.repository

import com.easycode.ide.data.db.FiddleDao
import com.easycode.ide.data.db.DraftDao
import com.easycode.ide.data.model.DraftEntity
import com.easycode.ide.data.model.FiddleEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

class FiddleRepository(private val fiddleDao: FiddleDao, private val draftDao: DraftDao) {

    val allFiddles: Flow<List<FiddleEntity>> = fiddleDao.getAllFiddles().flowOn(Dispatchers.IO)

    suspend fun getFiddleById(id: Long): FiddleEntity? = withContext(Dispatchers.IO) {
        fiddleDao.getFiddleById(id)
    }

    suspend fun getDraft(): DraftEntity? = withContext(Dispatchers.IO) { draftDao.getDraft() }

    suspend fun saveDraft(draft: DraftEntity) = withContext(Dispatchers.IO) { draftDao.saveDraft(draft) }

    suspend fun clearDraft() = withContext(Dispatchers.IO) { draftDao.clearDraft() }

    suspend fun saveFiddle(fiddle: FiddleEntity): Long = withContext(Dispatchers.IO) {
        if (fiddle.id > 0 && fiddleDao.getFiddleById(fiddle.id) != null) {
            fiddleDao.updateFiddle(fiddle)
            fiddle.id
        } else {
            fiddleDao.insertFiddle(fiddle.copy(id = 0))
        }
    }

    suspend fun updateFiddle(fiddle: FiddleEntity) = withContext(Dispatchers.IO) {
        fiddleDao.updateFiddle(fiddle)
    }

    suspend fun deleteFiddle(fiddle: FiddleEntity) = withContext(Dispatchers.IO) {
        fiddleDao.deleteFiddle(fiddle)
    }

    suspend fun deleteFiddleById(id: Long) = withContext(Dispatchers.IO) {
        fiddleDao.deleteFiddleById(id)
    }

    fun searchFiddles(query: String): Flow<List<FiddleEntity>> =
        fiddleDao.searchFiddles(query).flowOn(Dispatchers.IO)
}
