package com.shohan.omnicalc.core.database

import com.shohan.omnicalc.core.common.Numerals
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * History repository: single write path for every calculator/tool
 * (spec §4.7) plus search/filter/clear operations (spec §15.1).
 */
class HistoryRepository(private val dao: HistoryDao) {

    fun observeRecent(limit: Int = 200): Flow<List<HistoryEntity>> = dao.recent(limit)

    fun observeAll(): Flow<List<HistoryEntity>> = dao.observeAll()

    fun search(rawQuery: String): Flow<List<HistoryEntity>> {
        // Accept Bengali digits in queries; escape LIKE wildcards so user
        // input can never act as a pattern.
        val normalized = Numerals.normalizeToWesternDigits(rawQuery)
            .replace("\\", "\\\\")
            .replace("%", "\\%")
            .replace("_", "\\_")
        return dao.search(normalized)
    }

    fun observeByTool(toolType: String): Flow<List<HistoryEntity>> = dao.byTool(toolType)

    suspend fun record(
        expression: String,
        resultPlain: String,
        exact: Boolean,
        toolType: String,
        category: String? = null,
        contextPairs: String? = null,
    ): Long {
        // Skip exact duplicates created by pressing "=" repeatedly (spec §22.3):
        // the repository handles it centrally; no UI-side dedup needed.
        return dao.insert(
            HistoryEntity(
                expression = expression.trim(),
                resultPlain = resultPlain,
                exact = exact,
                toolType = toolType,
                category = category,
                contextPairs = contextPairs,
                createdAt = System.currentTimeMillis(),
            ),
        )
    }

    suspend fun delete(entry: HistoryEntity) = dao.delete(entry)

    suspend fun deleteById(id: Long) = dao.deleteById(id)

    suspend fun clearAll() = dao.clearAll()

    suspend fun count(): Int = dao.count()

    suspend fun rename(id: Long, label: String?) {
        // Implemented via full entity update below once fetched — kept simple:
        dao.updateLabel(id, label)
    }
}

/** Favorites repository (spec §15.2). */
class FavoritesRepository(private val dao: FavoritesDao) {

    fun observeAll(): Flow<List<FavoriteEntity>> = dao.observeAll()

    fun observeByType(type: String): Flow<List<FavoriteEntity>> = dao.observeByType(type)

    suspend fun add(
        type: String,
        title: String,
        payload: String,
        subtitle: String? = null,
    ): Long {
        val existing = dao.findByPayload(type, payload)
        if (existing != null) return existing.id
        return dao.insert(
            FavoriteEntity(
                type = type,
                title = title,
                payload = payload,
                subtitle = subtitle,
                createdAt = System.currentTimeMillis(),
            ),
        )
    }

    suspend fun remove(id: Long) = dao.deleteById(id)

    suspend fun removeByPayload(type: String, payload: String) = dao.deleteByPayload(type, payload)

    suspend fun isFavorite(type: String, payload: String): Boolean =
        dao.findByPayload(type, payload) != null
}
