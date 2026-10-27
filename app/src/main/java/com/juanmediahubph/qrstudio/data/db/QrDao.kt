package com.juanmediahubph.qrstudio.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface QrDao {
    @Query(
        """
        SELECT * FROM qr_codes
        WHERE (:query = '' OR title LIKE '%' || :query || '%' OR payloadText LIKE '%' || :query || '%')
          AND (:favoritesOnly = 0 OR isFavorite = 1)
          AND (:type IS NULL OR type = :type)
        ORDER BY updatedAt DESC, id DESC
        """,
    )
    fun observe(
        query: String,
        favoritesOnly: Boolean,
        type: String?,
    ): Flow<List<QrEntity>>

    @Query("SELECT * FROM qr_codes WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): QrEntity?

    @Query("SELECT * FROM qr_codes ORDER BY updatedAt DESC")
    suspend fun getAll(): List<QrEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: QrEntity): Long

    @Update
    suspend fun update(entity: QrEntity)

    @Delete
    suspend fun delete(entity: QrEntity)

    @Query("DELETE FROM qr_codes WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM qr_codes WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    @Query("UPDATE qr_codes SET isFavorite = :favorite, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setFavorite(id: Long, favorite: Boolean, updatedAt: Long)

    @Query("DELETE FROM qr_codes")
    suspend fun clear()

    @Query("SELECT COUNT(*) FROM qr_codes")
    suspend fun count(): Int
}
