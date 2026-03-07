package com.nexus.intelligence.data.embeddings

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.sqrt

// ── API Request/Response Models ──────────────────────────────────

data class EmbeddingRequest(
    val model: String = "default",
    val input: List<String>
)

data class EmbeddingResponse(
    val data: List<EmbeddingData>?,
    val model: String?,
    val usage: UsageInfo?
)

data class EmbeddingData(
    val embedding: List<Float>,
    val index: Int
)

data class UsageInfo(
    @SerializedName("prompt_tokens") val promptTokens: Int?,
    @SerializedName("total_tokens") val totalTokens: Int?
)

data class ChatRequest(
    val model: String = "default",
    val messages: List<ChatMessage>,
    val temperature: Float = 0.1f,
    @SerializedName("max_tokens") val maxTokens: Int = 500
)

data class ChatMessage(
    val role: String,
    val content: String
)

data class ChatResponse(
    val choices: List<ChatChoice>?
)

data class ChatChoice(
    val message: ChatMessage?
)

// ── Embedding Service ────────────────────────────────────────────

@Singleton
class EmbeddingService @Inject constructor() {

    private val gson = Gson()
    private var baseUrl = "http://127.0.0.1:8080"

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    fun setBaseUrl(url: String) {
        baseUrl = url.trimEnd('/')
    }

    fun getBaseUrl(): String = baseUrl

    /**
     * Check if the local API is reachable
     */
    suspend fun isApiAvailable(): Boolean = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$baseUrl/v1/models")
                .get()
                .build()
            val response = client.newCall(request).execute()
            response.isSuccessful
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Generate embeddings for a list of texts using the local API
     * Compatible with OpenAI /v1/embeddings endpoint
     */
    suspend fun getEmbeddings(texts: List<String>): List<FloatArray>? = withContext(Dispatchers.IO) {
        try {
            val requestBody = EmbeddingRequest(input = texts)
            val json = gson.toJson(requestBody)

            val request = Request.Builder()
                .url("$baseUrl/v1/embeddings")
                .post(json.toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return@withContext null

            val body = response.body?.string() ?: return@withContext null
            val embeddingResponse = gson.fromJson(body, EmbeddingResponse::class.java)

            embeddingResponse.data?.sortedBy { it.index }?.map { it.embedding.toFloatArray() }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Generate embedding for a single text
     */
    suspend fun getEmbedding(text: String): FloatArray? {
        val results = getEmbeddings(listOf(text))
        return results?.firstOrNull()
    }

    /**
     * Perform semantic search using chat completion
     * Sends context documents and a query to the local LLM
     */
    suspend fun semanticSearch(
        query: String,
        documentContexts: List<Pair<String, String>> // (filename, content_preview)
    ): String? = withContext(Dispatchers.IO) {
        try {
            val contextText = documentContexts.joinToString("\n\n") { (name, content) ->
                "=== Document: $name ===\n$content"
            }

            val messages = listOf(
                ChatMessage(
                    role = "system",
                    content = "You are NEXUS, a document intelligence assistant. Given the following document excerpts, answer the user's query. Reference specific documents by name. Be concise and precise."
                ),
                ChatMessage(
                    role = "user",
                    content = "Documents:\n$contextText\n\nQuery: $query"
                )
            )

            val requestBody = ChatRequest(messages = messages)
            val json = gson.toJson(requestBody)

            val request = Request.Builder()
                .url("$baseUrl/v1/chat/completions")
                .post(json.toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return@withContext null

            val body = response.body?.string() ?: return@withContext null
            val chatResponse = gson.fromJson(body, ChatResponse::class.java)

            chatResponse.choices?.firstOrNull()?.message?.content
        } catch (e: Exception) {
            null
        }
    }

    companion object {
        /**
         * Calculate cosine similarity between two vectors
         */
        fun cosineSimilarity(a: FloatArray, b: FloatArray): Float {
            if (a.size != b.size) return 0f
            var dotProduct = 0f
            var normA = 0f
            var normB = 0f
            for (i in a.indices) {
                dotProduct += a[i] * b[i]
                normA += a[i] * a[i]
                normB += b[i] * b[i]
            }
            val denominator = sqrt(normA) * sqrt(normB)
            return if (denominator == 0f) 0f else dotProduct / denominator
        }

        /**
         * Find top-K most similar documents by cosine similarity
         */
        fun findTopK(
            queryEmbedding: FloatArray,
            documentEmbeddings: List<Pair<Long, FloatArray>>, // (docId, embedding)
            topK: Int = 10
        ): List<Pair<Long, Float>> { // (docId, similarity)
            return documentEmbeddings
                .map { (id, emb) -> id to cosineSimilarity(queryEmbedding, emb) }
                .sortedByDescending { it.second }
                .take(topK)
        }
    }
}
