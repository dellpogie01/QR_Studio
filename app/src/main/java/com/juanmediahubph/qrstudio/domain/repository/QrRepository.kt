package com.juanmediahubph.qrstudio.domain.repository

import com.juanmediahubph.qrstudio.domain.model.HistoryFilter
import com.juanmediahubph.qrstudio.domain.model.SavedQr
import kotlinx.coroutines.flow.Flow

interface QrRepository {
    fun observeHistory(query: String, filter: HistoryFilter): Flow<List<SavedQr>>

    suspend fun getById(id: Long): SavedQr?

    suspend fun save(qr: SavedQr): Long

    /** Re-inserts a deleted record with its original id, for undo. */
    suspend fun restore(qr: SavedQr)

    suspend fun delete(id: Long)

    suspend fun deleteMany(ids: Collection<Long>)

    suspend fun setFavorite(id: Long, favorite: Boolean)

    suspend fun clearAll()
}
