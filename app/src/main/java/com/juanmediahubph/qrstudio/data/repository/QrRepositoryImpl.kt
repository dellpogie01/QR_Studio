package com.juanmediahubph.qrstudio.data.repository

import com.juanmediahubph.qrstudio.data.AppJson
import com.juanmediahubph.qrstudio.data.db.QrDao
import com.juanmediahubph.qrstudio.data.db.QrEntity
import com.juanmediahubph.qrstudio.di.IoDispatcher
import com.juanmediahubph.qrstudio.domain.model.HistoryFilter
import com.juanmediahubph.qrstudio.domain.model.QrStyle
import com.juanmediahubph.qrstudio.domain.model.QrType
import com.juanmediahubph.qrstudio.domain.model.SavedQr
import com.juanmediahubph.qrstudio.domain.repository.QrRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QrRepositoryImpl @Inject constructor(
    private val dao: QrDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : QrRepository {

    private val json: Json = AppJson
    private val fieldsSerializer = MapSerializer(String.serializer(), String.serializer())

    override fun observeHistory(query: String, filter: HistoryFilter): Flow<List<SavedQr>> =
        dao.observe(
            query = query.trim(),
            favoritesOnly = filter.favoritesOnly,
            type = filter.type?.name,
        )
            .map { entities -> entities.map { it.toDomain() } }
            .flowOn(ioDispatcher)

    override suspend fun getById(id: Long): SavedQr? = withContext(ioDispatcher) {
        dao.getById(id)?.toDomain()
    }

    override suspend fun save(qr: SavedQr): Long = withContext(ioDispatcher) {
        val entity = qr.toEntity()
        if (entity.id == 0L) {
            dao.insert(entity)
        } else {
            dao.update(entity)
            entity.id
        }
    }

    override suspend fun restore(qr: SavedQr): Unit = withContext(ioDispatcher) {
        dao.insert(qr.toEntity())
        Unit
    }

    override suspend fun delete(id: Long) = withContext(ioDispatcher) {
        dao.deleteById(id)
    }

    override suspend fun deleteMany(ids: Collection<Long>) = withContext(ioDispatcher) {
        if (ids.isNotEmpty()) dao.deleteByIds(ids.toList())
    }

    override suspend fun setFavorite(id: Long, favorite: Boolean) = withContext(ioDispatcher) {
        dao.setFavorite(id, favorite, System.currentTimeMillis())
    }

    override suspend fun clearAll() = withContext(ioDispatcher) {
        dao.clear()
    }

    private fun QrEntity.toDomain(): SavedQr {
        val style = runCatching { json.decodeFromString<QrStyle>(styleJson) }
            .getOrDefault(QrStyle())
            .sanitized()
        val fields = runCatching {
            json.decodeFromString(fieldsSerializer, formFieldsJson)
        }.getOrDefault(emptyMap())
        return SavedQr(
            id = id,
            type = QrType.fromNameOrText(type),
            title = title.ifBlank { type },
            payloadText = payloadText,
            fields = fields,
            style = style,
            createdAt = createdAt,
            updatedAt = updatedAt,
            isFavorite = isFavorite,
        )
    }

    private fun SavedQr.toEntity(): QrEntity = QrEntity(
        id = id,
        type = type.name,
        title = title,
        payloadText = payloadText,
        formFieldsJson = json.encodeToString(fieldsSerializer, fields),
        styleJson = json.encodeToString(QrStyle.serializer(), style.sanitized()),
        createdAt = createdAt,
        updatedAt = updatedAt,
        isFavorite = isFavorite,
    )
}
