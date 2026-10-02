package com.easycode.ide.data.repository

import com.easycode.ide.data.db.FiddleDao
import com.easycode.ide.data.model.FiddleEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

class FiddleRepository(private val fiddleDao: FiddleDao) {

    val allFiddles: Flow<List<FiddleEntity>> = fiddleDao.getAllFiddles().flowOn(Dispatchers.IO)

    suspend fun getFiddleById(id: Long): FiddleEntity? = withContext(Dispatchers.IO) {
        fiddleDao.getFiddleById(id)
    }

    suspend fun saveFiddle(fiddle: FiddleEntity): Long = withContext(Dispatchers.IO) {
        fiddleDao.insertFiddle(fiddle)
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
