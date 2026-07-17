package com.arisamtunes.data.local

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.arisamtunes.data.local.dao.CachedSongDao
import com.arisamtunes.data.local.dao.CachedUserProfileDao
import com.arisamtunes.data.local.dao.ChatMessageDao
import com.arisamtunes.data.local.dao.DownloadedSongDao
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
    fun provideDatabase(@ApplicationContext context: Context): AriSamDatabase = Room.databaseBuilder(
        context,
        AriSamDatabase::class.java,
        "arisam_tunes.db",
    ).addMigrations(ChatCacheMigration1To2, ChatInteractionsMigration2To3).fallbackToDestructiveMigration(false).build()

    @Provides fun provideSearchHistoryDao(database: AriSamDatabase): SearchHistoryDao = database.searchHistoryDao()
    @Provides fun provideLikedSongDao(database: AriSamDatabase): LikedSongDao = database.likedSongDao()
    @Provides fun provideDownloadedSongDao(database: AriSamDatabase): DownloadedSongDao = database.downloadedSongDao()
    @Provides fun provideRecentlyPlayedDao(database: AriSamDatabase): RecentlyPlayedDao = database.recentlyPlayedDao()
    @Provides fun provideChatMessageDao(database: AriSamDatabase): ChatMessageDao = database.chatMessageDao()
    @Provides fun provideCachedUserProfileDao(database: AriSamDatabase): CachedUserProfileDao = database.cachedUserProfileDao()
    @Provides fun provideCachedSongDao(database: AriSamDatabase): CachedSongDao = database.cachedSongDao()
    @Provides fun provideRemoteKeyDao(database: AriSamDatabase): RemoteKeyDao = database.remoteKeyDao()
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
