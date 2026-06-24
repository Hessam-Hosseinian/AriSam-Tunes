package com.arisamtunes.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "search_history",
    indices = [Index(value = ["query", "filter"], unique = true), Index("lastSearchedAt")],
)
data class SearchHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val query: String,
    val filter: String,
    val resultCount: Long? = null,
    val lastSearchedAt: Long,
)

@Entity(tableName = "liked_songs", indices = [Index("likedAt")])
data class LikedSongEntity(
    @PrimaryKey val songId: String,
    val title: String,
    val artistName: String,
    val album: String? = null,
    val coverImageUrl: String? = null,
    val durationSeconds: Int = 0,
    val likedAt: Long,
)

@Entity(tableName = "downloaded_songs", indices = [Index("downloadedAt"), Index("localFilePath", unique = true)])
data class DownloadedSongEntity(
    @PrimaryKey val songId: String,
    val title: String,
    val artistName: String,
    val album: String? = null,
    val audioUrl: String,
    val coverImageUrl: String? = null,
    val localFilePath: String,
    val mimeType: String? = null,
    val fileSizeBytes: Long? = null,
    val downloadState: String,
    val downloadedAt: Long,
)

@Entity(tableName = "recently_played", indices = [Index("playedAt")])
data class RecentlyPlayedEntity(
    @PrimaryKey val songId: String,
    val title: String,
    val artistName: String,
    val album: String? = null,
    val coverImageUrl: String? = null,
    val durationSeconds: Int = 0,
    val lastPositionSeconds: Int = 0,
    val playCount: Long = 1,
    val playedAt: Long,
)

@Entity(
    tableName = "chat_messages",
    indices = [Index("conversationUserId"), Index("createdAt"), Index(value = ["conversationUserId", "createdAt"])],
)
data class ChatMessageEntity(
    @PrimaryKey val messageId: String,
    val conversationUserId: String,
    val senderUserId: String,
    val receiverUserId: String,
    val body: String,
    val messageType: String = "TEXT",
    val deliveryState: String = "PENDING",
    val isMine: Boolean,
    val createdAt: String,
    val sentAt: String? = null,
    val deliveredAt: String? = null,
    val readAt: String? = null,
    val cachedAt: Long,
)

@Entity(tableName = "cached_user_profiles", indices = [Index("displayName"), Index("cachedAt")])
data class CachedUserProfileEntity(
    @PrimaryKey val userId: String,
    val displayName: String,
    val username: String? = null,
    val email: String? = null,
    val avatarUrl: String? = null,
    val bio: String? = null,
    val followerCount: Long = 0,
    val followingCount: Long = 0,
    val isFollowing: Boolean = false,
    val cachedAt: Long,
)

@Entity(
    tableName = "cached_songs",
    indices = [Index("artistName"), Index("album"), Index("genre"), Index("cachedAt")],
)
data class CachedSongEntity(
    @PrimaryKey val id: String,
    val title: String,
    val artistId: String? = null,
    val artistName: String,
    val album: String? = null,
    val albumArtist: String? = null,
    val genre: String? = null,
    val durationSeconds: Int = 0,
    val audioUrl: String,
    val coverImageUrl: String,
    val trackNumber: Int? = null,
    val discNumber: Int? = null,
    val bitrateKbps: Int? = null,
    val sampleRateHz: Int? = null,
    val channels: String? = null,
    val codec: String? = null,
    val fileFormat: String? = null,
    val releaseYear: Int? = null,
    val releaseDate: String? = null,
    val language: String? = null,
    val composer: String? = null,
    val producer: String? = null,
    val mood: String? = null,
    val tags: List<String> = emptyList(),
    val popularity: Int = 0,
    val playCount: Long = 0,
    val isExplicit: Boolean = false,
    val isLocal: Boolean = false,
    val isDemo: Boolean = false,
    val audioFileSize: Long? = null,
    val extraMetadataJson: String = "{}",
    val cachedAt: Long,
)

@Entity(
    tableName = "remote_keys",
    primaryKeys = ["scope", "cacheKey", "itemId"],
    indices = [Index(value = ["scope", "cacheKey"]), Index("createdAt")],
)
data class RemoteKeyEntity(
    val scope: String,
    val cacheKey: String,
    val itemId: String,
    val prevKey: Int? = null,
    val currentPage: Int,
    val nextKey: Int? = null,
    val createdAt: Long,
)
