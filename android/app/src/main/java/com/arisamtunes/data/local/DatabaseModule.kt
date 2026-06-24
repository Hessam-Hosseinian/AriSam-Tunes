package com.arisamtunes.data.local

import android.content.Context
import androidx.room.Room
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
    ).fallbackToDestructiveMigration(false).build()

    @Provides fun provideSearchHistoryDao(database: AriSamDatabase): SearchHistoryDao = database.searchHistoryDao()
    @Provides fun provideLikedSongDao(database: AriSamDatabase): LikedSongDao = database.likedSongDao()
    @Provides fun provideDownloadedSongDao(database: AriSamDatabase): DownloadedSongDao = database.downloadedSongDao()
    @Provides fun provideRecentlyPlayedDao(database: AriSamDatabase): RecentlyPlayedDao = database.recentlyPlayedDao()
    @Provides fun provideChatMessageDao(database: AriSamDatabase): ChatMessageDao = database.chatMessageDao()
    @Provides fun provideCachedUserProfileDao(database: AriSamDatabase): CachedUserProfileDao = database.cachedUserProfileDao()
    @Provides fun provideCachedSongDao(database: AriSamDatabase): CachedSongDao = database.cachedSongDao()
    @Provides fun provideRemoteKeyDao(database: AriSamDatabase): RemoteKeyDao = database.remoteKeyDao()
}
