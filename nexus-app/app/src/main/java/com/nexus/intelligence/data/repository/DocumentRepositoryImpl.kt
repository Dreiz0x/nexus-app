package com.nexus.intelligence.data.repository

import com.nexus.intelligence.data.local.dao.DocumentDao
import com.nexus.intelligence.data.local.entity.DocumentEntity
import com.nexus.intelligence.data.local.entity.MonitoredFolderEntity
import com.nexus.intelligence.data.local.entity.SearchHistoryEntity
import com.nexus.intelligence.data.local.entity.IndexingStatsEntity
import com.nexus.intelligence.data.parser.DocumentParser
import com.nexus.intelligence.data.embeddings.EmbeddingService
import com.nexus.intelligence.domain.model.DocumentInfo
import com.nexus.intelligence.domain.model.SearchResult
import com.nexus.intelligence.domain.repository.DocumentRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DocumentRepositoryImpl @Inject constructor(
    private val documentDao: DocumentDao,
    private val documentParser: DocumentParser,
    private val embeddingService: EmbeddingService
) : DocumentRepository {

    // ── Document CRUD ────────────────────────────────────────────────

    override fun getAllDocuments(): Flow<List<DocumentInfo>> {
        return documentDao.getAllDocuments().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override fun getRecentDocuments(limit: Int): Flow<List<DocumentInfo>> {
        return documentDao.getRecentDocuments(limit).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override fun getDocumentsByType(type: String): Flow<List<DocumentInfo>> {
        return documentDao.getDocumentsByType(type).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override fun getDocumentsByDirectory(directory: String): Flow<List<DocumentInfo>> {
        return documentDao.getDocumentsByDirectory(directory).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override fun getDocumentCount(): Flow<Int> = documentDao.getDocumentCount()

    override fun getAllDirectories(): Flow<List<String>> = documentDao.getAllDirectories()

    override suspend fun getDocumentById(id: Long): DocumentInfo? {
        return documentDao.getDocumentById(id)?.toDomainModel()
    }

    // ── Indexing ─────────────────────────────────────────────────────

    override suspend fun indexFile(file: File): DocumentInfo? = withContext(Dispatchers.IO) {
        try {
            val existing = documentDao.getDocumentByPath(file.absolutePath)
            if (existing != null && existing.lastModified >= file.lastModified()) {
                return@withContext existing.toDomainModel()
            }

            val parseResult = documentParser.parseFile(file)
            if (!parseResult.success) return@withContext null

            val contentPreview = parseResult.text.take(500)
            val typeLabel = DocumentParser.getDocumentTypeLabel(file)

            val entity = DocumentEntity(
                id = existing?.id ?: 0,
                filePath = file.absolutePath,
                fileName = file.name,
                fileType = typeLabel,
                fileSize = file.length(),
                lastModified = file.lastModified(),
                indexedAt = System.currentTimeMillis(),
                contentPreview = contentPreview,
                fullTextContent = parseResult.text,
                parentDirectory = file.parent ?: "",
                mimeType = getMimeType(file),
                pageCount = parseResult.pageCount
            )

            val id = documentDao.insertDocument(entity)
            entity.copy(id = id).toDomainModel()
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun removeDeletedFiles() = withContext(Dispatchers.IO) {
        val allPaths = documentDao.getAllFilePaths()
        for (path in allPaths) {
            if (!File(path).exists()) {
                documentDao.deleteByPath(path)
            }
        }
    }

    override suspend fun clearIndex() {
        documentDao.deleteAllDocuments()
    }

    // ── Search ───────────────────────────────────────────────────────

    override suspend fun textSearch(query: String): List<SearchResult> = withContext(Dispatchers.IO) {
        val results = documentDao.searchDocuments(query)
        results.mapIndexed { index, entity ->
            val relevance = calculateTextRelevance(query, entity)
            SearchResult(
                document = entity.toDomainModel(),
                relevanceScore = relevance,
                matchedSnippet = extractSnippet(query, entity.fullTextContent),
                searchType = "TEXT"
            )
        }.sortedByDescending { it.relevanceScore }
    }

    override suspend fun semanticSearch(query: String): List<SearchResult> = withContext(Dispatchers.IO) {
        try {
            // Get query embedding
            val queryEmbedding = embeddingService.getEmbedding(query) ?: return@withContext textSearch(query)

            // Get all documents with embeddings
            val docsWithEmbeddings = documentDao.getDocumentsWithEmbeddings()
            if (docsWithEmbeddings.isEmpty()) return@withContext textSearch(query)

            val docEmbeddings = docsWithEmbeddings.mapNotNull { doc ->
                val embedding = parseEmbeddingVector(doc.embeddingVector) ?: return@mapNotNull null
                doc.id to embedding
            }

            // Find top matches
            val topMatches = EmbeddingService.findTopK(queryEmbedding, docEmbeddings, 20)

            topMatches.mapNotNull { (docId, similarity) ->
                val doc = documentDao.getDocumentById(docId) ?: return@mapNotNull null
                SearchResult(
                    document = doc.toDomainModel(),
                    relevanceScore = similarity,
                    matchedSnippet = doc.contentPreview,
                    searchType = "SEMANTIC"
                )
            }
        } catch (e: Exception) {
            textSearch(query)
        }
    }

    override suspend fun generateEmbeddingsForDocument(docId: Long): Boolean = withContext(Dispatchers.IO) {
        try {
            val doc = documentDao.getDocumentById(docId) ?: return@withContext false
            val textToEmbed = doc.contentPreview.ifBlank { doc.fullTextContent.take(1000) }
            if (textToEmbed.isBlank()) return@withContext false

            val embedding = embeddingService.getEmbedding(textToEmbed) ?: return@withContext false
            val embeddingJson = embedding.joinToString(",", "[", "]")

            documentDao.updateDocument(doc.copy(embeddingVector = embeddingJson))
            true
        } catch (e: Exception) {
            false
        }
    }

    // ── Monitored Folders ────────────────────────────────────────────

    override fun getMonitoredFolders(): Flow<List<MonitoredFolderEntity>> {
        return documentDao.getAllMonitoredFolders()
    }

    override suspend fun addMonitoredFolder(path: String, label: String) {
        documentDao.insertMonitoredFolder(
            MonitoredFolderEntity(path = path, label = label)
        )
    }

    override suspend fun removeMonitoredFolder(path: String) {
        documentDao.deleteMonitoredFolderByPath(path)
    }

    // ── Search History ───────────────────────────────────────────────

    override suspend fun addSearchHistory(query: String, resultCount: Int) {
        documentDao.insertSearchHistory(
            SearchHistoryEntity(query = query, resultCount = resultCount)
        )
    }

    override fun getSearchHistory(): Flow<List<SearchHistoryEntity>> {
        return documentDao.getRecentSearches()
    }

    // ── Stats ────────────────────────────────────────────────────────

    override fun getIndexingStats(): Flow<IndexingStatsEntity?> {
        return documentDao.getIndexingStats()
    }

    override suspend fun updateIndexingStats(stats: IndexingStatsEntity) {
        documentDao.updateIndexingStats(stats)
    }

    // ── API Status ───────────────────────────────────────────────────

    override suspend fun isApiAvailable(): Boolean = embeddingService.isApiAvailable()

    // ── Private Helpers ──────────────────────────────────────────────

    private fun calculateTextRelevance(query: String, entity: DocumentEntity): Float {
        val queryLower = query.lowercase()
        val nameLower = entity.fileName.lowercase()
        val contentLower = entity.fullTextContent.lowercase()

        var score = 0f

        // Filename match bonus
        if (nameLower.contains(queryLower)) score += 0.4f

        // Content match - count occurrences
        val words = queryLower.split(" ").filter { it.length > 2 }
        for (word in words) {
            val count = contentLower.windowed(word.length) { it }.count { it == word }
            score += (count.coerceAtMost(10) * 0.05f)
        }

        return score.coerceIn(0f, 1f)
    }

    private fun extractSnippet(query: String, content: String, snippetLength: Int = 200): String {
        val queryLower = query.lowercase()
        val contentLower = content.lowercase()
        val index = contentLower.indexOf(queryLower)

        return if (index >= 0) {
            val start = (index - snippetLength / 2).coerceAtLeast(0)
            val end = (start + snippetLength).coerceAtMost(content.length)
            "…${content.substring(start, end)}…"
        } else {
            content.take(snippetLength) + "…"
        }
    }

    private fun parseEmbeddingVector(json: String?): FloatArray? {
        if (json == null) return null
        return try {
            json.removeSurrounding("[", "]")
                .split(",")
                .filter { it.isNotBlank() }
                .map { it.trim().toFloat() }
                .toFloatArray()
        } catch (e: Exception) {
            null
        }
    }

    private fun getMimeType(file: File): String {
        return when (file.extension.lowercase()) {
            "pdf" -> "application/pdf"
            "doc" -> "application/msword"
            "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            "xls" -> "application/vnd.ms-excel"
            "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            "ppt" -> "application/vnd.ms-powerpoint"
            "pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation"
            "txt" -> "text/plain"
            "csv" -> "text/csv"
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "bmp" -> "image/bmp"
            "webp" -> "image/webp"
            else -> "application/octet-stream"
        }
    }
}

// ── Extension Functions ──────────────────────────────────────────

fun DocumentEntity.toDomainModel(): DocumentInfo {
    return DocumentInfo(
        id = id,
        filePath = filePath,
        fileName = fileName,
        fileType = fileType,
        fileSize = fileSize,
        lastModified = lastModified,
        indexedAt = indexedAt,
        contentPreview = contentPreview,
        parentDirectory = parentDirectory,
        mimeType = mimeType,
        pageCount = pageCount,
        isFromNetwork = isFromNetwork,
        networkSourceDevice = networkSourceDevice,
        hasEmbedding = embeddingVector != null
    )
}
