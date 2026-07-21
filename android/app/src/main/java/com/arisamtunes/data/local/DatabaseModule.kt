package com.arisamtunes.data.local

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.arisamtunes.data.local.dao.CachedSongDao
import com.arisamtunes.data.local.dao.CachedUserProfileDao
import com.arisamtunes.data.local.dao.ChatMessageDao
import com.arisamtunes.data.local.dao.DownloadedSongDao
import com.arisamtunes.data.local.dao.FollowedArtistDao
import com.arisamtunes.data.local.dao.LikedSongDao
import com.arisamtunes.data.local.dao.RecentlyPlayedDao
import com.arisamtunes.data.local.dao.RemoteKeyDao
import com.arisamtunes.data.local.dao.SearchHistoryDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AriSamDatabase =
        AriSamDatabaseProvider.get(context)

    @Provides fun provideSearchHistoryDao(database: AriSamDatabase): SearchHistoryDao = database.searchHistoryDao()
    @Provides fun provideLikedSongDao(database: AriSamDatabase): LikedSongDao = database.likedSongDao()
    @Provides fun provideDownloadedSongDao(database: AriSamDatabase): DownloadedSongDao = database.downloadedSongDao()
    @Provides fun provideFollowedArtistDao(database: AriSamDatabase): FollowedArtistDao = database.followedArtistDao()
    @Provides fun provideRecentlyPlayedDao(database: AriSamDatabase): RecentlyPlayedDao = database.recentlyPlayedDao()
    @Provides fun provideChatMessageDao(database: AriSamDatabase): ChatMessageDao = database.chatMessageDao()
    @Provides fun provideCachedUserProfileDao(database: AriSamDatabase): CachedUserProfileDao = database.cachedUserProfileDao()
    @Provides fun provideCachedSongDao(database: AriSamDatabase): CachedSongDao = database.cachedSongDao()
    @Provides fun provideRemoteKeyDao(database: AriSamDatabase): RemoteKeyDao = database.remoteKeyDao()
}

internal object AriSamDatabaseProvider {
    @Volatile private var instance: AriSamDatabase? = null

    fun get(context: Context): AriSamDatabase = instance ?: synchronized(this) {
        instance ?: Room.databaseBuilder(
            context.applicationContext,
            AriSamDatabase::class.java,
            "arisam_tunes.db",
        ).addMigrations(
            ChatCacheMigration1To2,
            ChatInteractionsMigration2To3,
            DownloadProgressMigration3To4,
            DownloadMetadataMigration4To5,
            DownloadOwnershipMigration5To6,
            LibraryUxMigration6To7,
        ).fallbackToDestructiveMigration(false).build().also { instance = it }
    }
}

internal val LibraryUxMigration6To7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE liked_songs ADD COLUMN audioUrl TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE recently_played ADD COLUMN audioUrl TEXT NOT NULL DEFAULT ''")
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS followed_artists (
                artistId TEXT NOT NULL,
                name TEXT NOT NULL,
                imageUri TEXT NOT NULL,
                followedAt INTEGER NOT NULL,
                PRIMARY KEY(artistId)
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_followed_artists_followedAt ON followed_artists(followedAt)")
    }
}

internal const val LegacyDownloadOwnerId = "__legacy_download_owner__"

internal val DownloadOwnershipMigration5To6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE downloaded_songs RENAME TO downloaded_songs_legacy")
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS downloaded_songs (
                ownerUserId TEXT NOT NULL,
                songId TEXT NOT NULL,
                title TEXT NOT NULL,
                artistName TEXT NOT NULL,
                album TEXT,
                audioUrl TEXT NOT NULL,
                coverImageUrl TEXT,
                durationSeconds INTEGER NOT NULL,
                lyrics TEXT,
                extraMetadataJson TEXT NOT NULL,
                localFilePath TEXT NOT NULL,
                mimeType TEXT,
                fileSizeBytes INTEGER,
                downloadState TEXT NOT NULL,
                downloadProgress INTEGER NOT NULL,
                failureReason TEXT,
                downloadedAt INTEGER NOT NULL,
                PRIMARY KEY(ownerUserId, songId)
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            INSERT INTO downloaded_songs (
                ownerUserId, songId, title, artistName, album, audioUrl, coverImageUrl,
                durationSeconds, lyrics, extraMetadataJson, localFilePath, mimeType,
                fileSizeBytes, downloadState, downloadProgress, failureReason, downloadedAt
            )
            SELECT '$LegacyDownloadOwnerId', songId, title, artistName, album, audioUrl, coverImageUrl,
                durationSeconds, lyrics, extraMetadataJson, localFilePath, mimeType,
                fileSizeBytes, downloadState, downloadProgress, failureReason, downloadedAt
            FROM downloaded_songs_legacy
            """.trimIndent(),
        )
        db.execSQL("DROP TABLE downloaded_songs_legacy")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_downloaded_songs_downloadedAt ON downloaded_songs(downloadedAt)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_downloaded_songs_localFilePath ON downloaded_songs(localFilePath)")
    }
}

internal val DownloadProgressMigration3To4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE downloaded_songs ADD COLUMN downloadProgress INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE downloaded_songs ADD COLUMN failureReason TEXT")
    }
}

internal val DownloadMetadataMigration4To5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE downloaded_songs ADD COLUMN durationSeconds INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE downloaded_songs ADD COLUMN lyrics TEXT")
        db.execSQL("ALTER TABLE downloaded_songs ADD COLUMN extraMetadataJson TEXT NOT NULL DEFAULT '{}'")
        db.execSQL(
            """
            UPDATE downloaded_songs
            SET durationSeconds = COALESCE(
                    (SELECT durationSeconds FROM cached_songs WHERE cached_songs.id = downloaded_songs.songId),
                    durationSeconds
                ),
                extraMetadataJson = COALESCE(
                    (SELECT extraMetadataJson FROM cached_songs WHERE cached_songs.id = downloaded_songs.songId),
                    extraMetadataJson
                )
            """.trimIndent(),
        )
    }
}

private val ChatInteractionsMigration2To3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE chat_messages ADD COLUMN replyToId TEXT")
        db.execSQL("ALTER TABLE chat_messages ADD COLUMN reactionsJson TEXT NOT NULL DEFAULT '[]'")
        db.execSQL("ALTER TABLE chat_messages ADD COLUMN editedAt TEXT")
        db.execSQL("ALTER TABLE chat_messages ADD COLUMN deletedAt TEXT")
    }
}

private val ChatCacheMigration1To2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("DROP TABLE IF EXISTS chat_messages")
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS chat_messages (
                ownerUserId TEXT NOT NULL,
                messageId TEXT NOT NULL,
                clientMessageId TEXT NOT NULL,
                conversationUserId TEXT NOT NULL,
                senderUserId TEXT NOT NULL,
                receiverUserId TEXT NOT NULL,
                body TEXT NOT NULL,
                messageType TEXT NOT NULL,
                songId TEXT,
                deliveryState TEXT NOT NULL,
                isMine INTEGER NOT NULL,
                createdAt TEXT NOT NULL,
                sentAt TEXT,
                deliveredAt TEXT,
                readAt TEXT,
                readReceiptPending INTEGER NOT NULL,
                updatedAt TEXT NOT NULL,
                cachedAt INTEGER NOT NULL,
                PRIMARY KEY(ownerUserId, messageId)
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS index_chat_messages_ownerUserId_conversationUserId_createdAt ON chat_messages(ownerUserId, conversationUserId, createdAt)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_chat_messages_ownerUserId_clientMessageId ON chat_messages(ownerUserId, clientMessageId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_chat_messages_ownerUserId_deliveryState ON chat_messages(ownerUserId, deliveryState)")
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS chat_remote_keys (
                ownerUserId TEXT NOT NULL,
                conversationUserId TEXT NOT NULL,
                nextCursor TEXT,
                reachedEnd INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL,
                PRIMARY KEY(ownerUserId, conversationUserId)
            )
        """.trimIndent())
    }
}
