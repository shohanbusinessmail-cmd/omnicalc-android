package com.shohan.omnicalc.core.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {

    @Insert
    suspend fun insert(entry: HistoryEntity): Long

    @Update
    suspend fun update(entry: HistoryEntity)

    @Delete
    suspend fun delete(entry: HistoryEntity)

    @Query("DELETE FROM history WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM history")
    suspend fun clearAll()

    @Query("SELECT * FROM history ORDER BY createdAt DESC LIMIT :limit")
    fun recent(limit: Int): Flow<List<HistoryEntity>>

    @Query("SELECT * FROM history ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<HistoryEntity>>

    /**
     * Search expression/result/label — [query] is already `%`-wrapped and
     * Bengali-normalized by the repository (SQL LIKE is ASCII-case-insensitive
     * only; queries here are matched literally, which fits digit-oriented
     * searching).
     */
    @Query(
        """
        SELECT * FROM history
        WHERE expression LIKE '%' || :query || '%'
           OR resultPlain LIKE '%' || :query || '%'
           OR label LIKE '%' || :query || '%'
        ORDER BY createdAt DESC
        """,
    )
    fun search(query: String): Flow<List<HistoryEntity>>

    @Query("SELECT * FROM history WHERE toolType = :toolType ORDER BY createdAt DESC")
    fun byTool(toolType: String): Flow<List<HistoryEntity>>

    @Query("SELECT COUNT(*) FROM history")
    suspend fun count(): Int

    @Query("UPDATE history SET label = :label WHERE id = :id")
    suspend fun updateLabel(id: Long, label: String?)
}

@Dao
interface FavoritesDao {

    @Insert
    suspend fun insert(favorite: FavoriteEntity): Long

    @Delete
    suspend fun delete(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM favorites ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<FavoriteEntity>>

    @Query("SELECT * FROM favorites WHERE type = :type ORDER BY createdAt DESC")
    fun observeByType(type: String): Flow<List<FavoriteEntity>>

    @Query("SELECT * FROM favorites WHERE type = :type AND payload = :payload LIMIT 1")
    suspend fun findByPayload(type: String, payload: String): FavoriteEntity?

    @Query("DELETE FROM favorites WHERE type = :type AND payload = :payload")
    suspend fun deleteByPayload(type: String, payload: String)
}
