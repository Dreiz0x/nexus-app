package com.nexus.intelligence.data.local.dao

import androidx.room.*
import com.nexus.intelligence.data.local.entity.DocumentEntity
import com.nexus.intelligence.data.local.entity.MonitoredFolderEntity
import com.nexus.intelligence.data.local.entity.SearchHistoryEntity
import com.nexus.intelligence.data.local.entity.IndexingStatsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DocumentDao {

    // ── Document Operations ──────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocument(document: DocumentEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocuments(documents: List<DocumentEntity>)

    @Update
    suspend fun updateDocument(document: DocumentEntity)

    @Delete
    suspend fun deleteDocument(document: DocumentEntity)

    @Query("DELETE FROM documents WHERE filePath = :path")
    suspend fun deleteByPath(path: String)

    @Query("DELETE FROM documents")
    suspend fun deleteAllDocuments()

    @Query("SELECT * FROM documents ORDER BY indexedAt DESC")
    fun getAllDocuments(): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents ORDER BY indexedAt DESC LIMIT :limit")
    fun getRecentDocuments(limit: Int = 50): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents WHERE id = :id")
    suspend fun getDocumentById(id: Long): DocumentEntity?

    @Query("SELECT * FROM documents WHERE filePath = :path")
    suspend fun getDocumentByPath(path: String): DocumentEntity?

    @Query("SELECT * FROM documents WHERE fileType = :type ORDER BY lastModified DESC")
    fun getDocumentsByType(type: String): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents WHERE fileName LIKE '%' || :query || '%' OR fullTextContent LIKE '%' || :query || '%' ORDER BY lastModified DESC")
    suspend fun searchDocuments(query: String): List<DocumentEntity>

    @Query("SELECT * FROM documents WHERE parentDirectory = :directory ORDER BY fileName ASC")
    fun getDocumentsByDirectory(directory: String): Flow<List<DocumentEntity>>

    @Query("SELECT COUNT(*) FROM documents")
    fun getDocumentCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM documents WHERE fileType = :type")
    suspend fun getCountByType(type: String): Int

    @Query("SELECT DISTINCT parentDirectory FROM documents ORDER BY parentDirectory ASC")
    fun getAllDirectories(): Flow<List<String>>

    @Query("SELECT filePath FROM documents")
    suspend fun getAllFilePaths(): List<String>

    @Query("SELECT * FROM documents WHERE lastModified > :since ORDER BY lastModified DESC")
    suspend fun getDocumentsModifiedSince(since: Long): List<DocumentEntity>

    @Query("SELECT * FROM documents WHERE embeddingVector IS NOT NULL")
    suspend fun getDocumentsWithEmbeddings(): List<DocumentEntity>

    // ── Monitored Folders ────────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMonitoredFolder(folder: MonitoredFolderEntity): Long

    @Delete
    suspend fun deleteMonitoredFolder(folder: MonitoredFolderEntity)

    @Query("DELETE FROM monitored_folders WHERE path = :path")
    suspend fun deleteMonitoredFolderByPath(path: String)

    @Query("SELECT * FROM monitored_folders WHERE isEnabled = 1 ORDER BY addedAt DESC")
    fun getActiveMonitoredFolders(): Flow<List<MonitoredFolderEntity>>

    @Query("SELECT * FROM monitored_folders ORDER BY addedAt DESC")
    fun getAllMonitoredFolders(): Flow<List<MonitoredFolderEntity>>

    // ── Search History ───────────────────────────────────────────────

    @Insert
    suspend fun insertSearchHistory(entry: SearchHistoryEntity)

    @Query("SELECT * FROM search_history ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentSearches(limit: Int = 20): Flow<List<SearchHistoryEntity>>

    @Query("DELETE FROM search_history")
    suspend fun clearSearchHistory()

    // ── Indexing Stats ───────────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateIndexingStats(stats: IndexingStatsEntity)

    @Query("SELECT * FROM indexing_stats WHERE id = 1")
    fun getIndexingStats(): Flow<IndexingStatsEntity?>

    @Query("SELECT * FROM indexing_stats WHERE id = 1")
    suspend fun getIndexingStatsOnce(): IndexingStatsEntity?
}
