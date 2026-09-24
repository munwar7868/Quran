package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface QuranDao {
    @Query("SELECT * FROM bookmarks ORDER BY createdAt DESC")
    fun getAllBookmarks(): Flow<List<BookmarkEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: BookmarkEntity): Long

    @Query("DELETE FROM bookmarks WHERE id = :id")
    suspend fun deleteBookmarkById(id: Long)

    @Query("DELETE FROM bookmarks WHERE pageNumber = :pageNumber AND mushafMode = :mushafMode")
    suspend fun deleteBookmarkByPage(pageNumber: Int, mushafMode: String)

    @Query("SELECT EXISTS(SELECT 1 FROM bookmarks WHERE pageNumber = :pageNumber AND mushafMode = :mushafMode LIMIT 1)")
    fun isPageBookmarked(pageNumber: Int, mushafMode: String): Flow<Boolean>

    @Query("SELECT * FROM reading_history WHERE id = 1 LIMIT 1")
    fun getLastRead(): Flow<ReadingHistoryEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveLastRead(history: ReadingHistoryEntity)
}
