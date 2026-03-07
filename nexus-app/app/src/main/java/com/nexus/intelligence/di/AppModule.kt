package com.nexus.intelligence.di

import android.content.Context
import androidx.room.Room
import com.nexus.intelligence.data.embeddings.EmbeddingService
import com.nexus.intelligence.data.local.dao.DocumentDao
import com.nexus.intelligence.data.local.database.NexusDatabase
import com.nexus.intelligence.data.parser.DocumentParser
import com.nexus.intelligence.data.repository.DocumentRepositoryImpl
import com.nexus.intelligence.domain.repository.DocumentRepository
import com.nexus.intelligence.domain.usecase.*
import com.nexus.intelligence.service.network.NexusLocalServer
import com.nexus.intelligence.service.network.NexusNetworkManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    // ── Database ─────────────────────────────────────────────────

    @Provides
    @Singleton
    fun provideNexusDatabase(
        @ApplicationContext context: Context
    ): NexusDatabase {
        return Room.databaseBuilder(
            context,
            NexusDatabase::class.java,
            NexusDatabase.DATABASE_NAME
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    @Singleton
    fun provideDocumentDao(database: NexusDatabase): DocumentDao {
        return database.documentDao()
    }

    // ── Services ─────────────────────────────────────────────────

    @Provides
    @Singleton
    fun provideDocumentParser(
        @ApplicationContext context: Context
    ): DocumentParser {
        return DocumentParser(context)
    }

    @Provides
    @Singleton
    fun provideEmbeddingService(): EmbeddingService {
        return EmbeddingService()
    }

    @Provides
    @Singleton
    fun provideNexusNetworkManager(): NexusNetworkManager {
        return NexusNetworkManager()
    }

    // ── Repository ───────────────────────────────────────────────

    @Provides
    @Singleton
    fun provideDocumentRepository(
        documentDao: DocumentDao,
        documentParser: DocumentParser,
        embeddingService: EmbeddingService
    ): DocumentRepository {
        return DocumentRepositoryImpl(documentDao, documentParser, embeddingService)
    }

    // ── Network Server ───────────────────────────────────────────

    @Provides
    @Singleton
    fun provideNexusLocalServer(
        repository: DocumentRepository,
        networkManager: NexusNetworkManager
    ): NexusLocalServer {
        return NexusLocalServer(repository, networkManager)
    }

    // ── Use Cases ────────────────────────────────────────────────

    @Provides
    @Singleton
    fun provideSearchDocumentsUseCase(
        repository: DocumentRepository
    ): SearchDocumentsUseCase {
        return SearchDocumentsUseCase(repository)
    }

    @Provides
    @Singleton
    fun provideIndexDocumentsUseCase(
        repository: DocumentRepository
    ): IndexDocumentsUseCase {
        return IndexDocumentsUseCase(repository)
    }

    @Provides
    @Singleton
    fun provideGetDashboardStatsUseCase(
        repository: DocumentRepository
    ): GetDashboardStatsUseCase {
        return GetDashboardStatsUseCase(repository)
    }

    @Provides
    @Singleton
    fun provideGetFileMapUseCase(
        repository: DocumentRepository
    ): GetFileMapUseCase {
        return GetFileMapUseCase(repository)
    }

    @Provides
    @Singleton
    fun provideManageSettingsUseCase(
        repository: DocumentRepository
    ): ManageSettingsUseCase {
        return ManageSettingsUseCase(repository)
    }
}
