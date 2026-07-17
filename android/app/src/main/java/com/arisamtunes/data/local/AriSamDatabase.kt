package com.arisamtunes.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.arisamtunes.data.local.dao.CachedSongDao
import com.arisamtunes.data.local.dao.CachedUserProfileDao
import com.arisamtunes.data.local.dao.ChatMessageDao
import com.arisamtunes.data.local.dao.DownloadedSongDao
import com.arisamtunes.data.local.dao.LikedSongDao
import com.arisamtunes.data.local.dao.RecentlyPlayedDao
import com.arisamtunes.data.local.dao.RemoteKeyDao
import com.arisamtunes.data.local.dao.SearchHistoryDao
import com.arisamtunes.data.local.entity.CachedSongEntity
import com.arisamtunes.data.local.entity.CachedUserProfileEntity
import com.arisamtunes.data.local.entity.ChatMessageEntity
import com.arisamtunes.data.local.entity.ChatRemoteKeyEntity
import com.arisamtunes.data.local.entity.DownloadedSongEntity
import com.arisamtunes.data.local.entity.LikedSongEntity
import com.arisamtunes.data.local.entity.RecentlyPlayedEntity
import com.arisamtunes.data.local.entity.RemoteKeyEntity
import com.arisamtunes.data.local.entity.SearchHistoryEntity

@Database(
    entities = [
        SearchHistoryEntity::class,
        LikedSongEntity::class,
        DownloadedSongEntity::class,
        RecentlyPlayedEntity::class,
        ChatMessageEntity::class,
        ChatRemoteKeyEntity::class,
        CachedUserProfileEntity::class,
        CachedSongEntity::class,
        RemoteKeyEntity::class,
    ],
    version = 3,
    exportSchema = false,
)
@TypeConverters(AriSamTypeConverters::class)
abstract class AriSamDatabase : RoomDatabase() {
    abstract fun searchHistoryDao(): SearchHistoryDao
    abstract fun likedSongDao(): LikedSongDao
    abstract fun downloadedSongDao(): DownloadedSongDao
    abstract fun recentlyPlayedDao(): RecentlyPlayedDao
    abstract fun chatMessageDao(): ChatMessageDao
    abstract fun cachedUserProfileDao(): CachedUserProfileDao
    abstract fun cachedSongDao(): CachedSongDao
    abstract fun remoteKeyDao(): RemoteKeyDao
}
