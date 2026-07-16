package com.arisamtunes.feature.chat

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.arisamtunes.data.catalog.CatalogRepository
import com.arisamtunes.data.catalog.SongDto
import com.arisamtunes.data.chat.ChatConnectionStatus
import com.arisamtunes.data.chat.ChatConversationDto
import com.arisamtunes.data.chat.ChatMessageDto
import com.arisamtunes.data.chat.ChatMessageStatusDto
import com.arisamtunes.data.chat.ChatSocketTypeDto
import com.arisamtunes.data.chat.ChatRepository
import com.arisamtunes.data.social.PublicUserDto
import com.arisamtunes.data.social.SocialRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

@HiltViewModel
class ChatConnectionViewModel @Inject constructor(private val repository: ChatRepository) : ViewModel() {
    fun start() = repository.startRealtime()
    fun stop() = repository.stopRealtime()
    override fun onCleared() {
        stop()
        super.onCleared()
    }
}

data class ChatListUiState(
    val searchQuery: String = "",
    val isSearching: Boolean = false,
)

@HiltViewModel
@OptIn(FlowPreview::class)
class ChatListViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val socialRepository: SocialRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(ChatListUiState())
    val state = _state.asStateFlow()
    val conversations: Flow<PagingData<ChatConversationDto>> = chatRepository.conversationsPager().cachedIn(viewModelScope)
    val starters: Flow<PagingData<PublicUserDto>> = socialRepository.followingPager().cachedIn(viewModelScope)
    private val query = MutableStateFlow("")
    private val _searchResults = MutableStateFlow<PagingData<PublicUserDto>>(PagingData.empty())
    val searchResults = _searchResults.asStateFlow()

    init {
        viewModelScope.launch {
            query.debounce(350).collectLatest { value ->
                if (value.isBlank()) {
                    _searchResults.value = PagingData.empty()
                } else {
                    socialRepository.searchUsersPager(value).collectLatest { _searchResults.value = it }
                }
            }
        }
    }

    fun updateSearch(value: String) {
        val safe = value.take(100)
        _state.value = _state.value.copy(searchQuery = safe, isSearching = safe.isNotBlank())
        query.value = safe.trim()
    }

    fun clearSearch() = updateSearch("")
}

sealed interface ChatEffect {
    data object SendFailed : ChatEffect
    data object InvalidSong : ChatEffect
}

data class ChatDetailUiState(
    val isLoadingPeer: Boolean = true,
    val meId: String = "",
    val peer: PublicUserDto? = null,
    val draft: String = "",
    val status: ChatConnectionStatus = ChatConnectionStatus.Disconnected,
    val isPeerTyping: Boolean = false,
    val hasPeerError: Boolean = false,
    val songCards: Map<String, SongDto> = emptyMap(),
    val unavailableSongIds: Set<String> = emptySet(),
)

@HiltViewModel
class ChatDetailViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val socialRepository: SocialRepository,
    private val catalogRepository: CatalogRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val userId: String = checkNotNull(savedStateHandle["userId"])
    private val _state = MutableStateFlow(ChatDetailUiState())
    val state = _state.asStateFlow()
    val messages = chatRepository.messagesPager(userId).cachedIn(viewModelScope)
    private val _effects = Channel<ChatEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()
    private val presentedMessages = ConcurrentHashMap.newKeySet<String>()
    private val loadingSongs = ConcurrentHashMap.newKeySet<String>()
    private var typingStopJob: Job? = null
    private var peerTypingExpiryJob: Job? = null
    private var typingSent = false

    init {
        refreshPeer()
        viewModelScope.launch {
            chatRepository.status.collectLatest { _state.value = _state.value.copy(status = it) }
        }
        viewModelScope.launch {
            chatRepository.events.collect { event ->
                if (event.type == ChatSocketTypeDto.ERROR) {
                    _effects.send(if (event.error == "SONG_NOT_FOUND") ChatEffect.InvalidSong else ChatEffect.SendFailed)
                }
                if (event.senderId == userId && event.type == ChatSocketTypeDto.TYPING_START) {
                    _state.value = _state.value.copy(isPeerTyping = true)
                    peerTypingExpiryJob?.cancel()
                    peerTypingExpiryJob = viewModelScope.launch {
                        delay(4_000)
                        _state.value = _state.value.copy(isPeerTyping = false)
                    }
                }
                if (event.senderId == userId && event.type == ChatSocketTypeDto.TYPING_STOP) {
                    peerTypingExpiryJob?.cancel()
                    _state.value = _state.value.copy(isPeerTyping = false)
                }
            }
        }
    }

    fun refreshPeer() = viewModelScope.launch {
        _state.value = _state.value.copy(isLoadingPeer = true, hasPeerError = false)
        runCatching { chatRepository.currentUserId() }
            .onSuccess { me -> _state.value = _state.value.copy(meId = me) }
        runCatching { socialRepository.user(userId) }
            .onSuccess { peer -> _state.value = _state.value.copy(isLoadingPeer = false, peer = peer) }
            .onFailure { _state.value = _state.value.copy(isLoadingPeer = false, hasPeerError = true) }
    }

    fun updateDraft(value: String) {
        _state.value = _state.value.copy(draft = value.take(4_000))
        typingStopJob?.cancel()
        peerTypingExpiryJob?.cancel()
        if (value.isNotBlank() && !typingSent) {
            typingSent = true
            viewModelScope.launch { chatRepository.setTyping(userId, true) }
        }
        typingStopJob = viewModelScope.launch {
            delay(1_500)
            stopTyping()
        }
    }

    fun send() = viewModelScope.launch {
        val text = _state.value.draft.trim()
        if (text.isBlank()) return@launch
        _state.value = _state.value.copy(draft = "")
        stopTyping()
        runCatching { chatRepository.sendText(userId, text) }.onFailure { _effects.send(ChatEffect.SendFailed) }
    }

    fun sendSong(songId: String) = viewModelScope.launch {
        stopTyping()
        runCatching { chatRepository.sendSong(userId, songId) }
            .onFailure { _effects.send(ChatEffect.SendFailed) }
    }

    fun onMessagePresented(message: ChatMessageDto) {
        if (!presentedMessages.add(message.id)) return
        if (message.senderId == userId && message.status != ChatMessageStatusDto.READ) {
            viewModelScope.launch { chatRepository.markMessageRead(message.id) }
        }
        val songId = message.songId ?: return
        if (songId in _state.value.songCards || !loadingSongs.add(songId)) return
        viewModelScope.launch {
            runCatching { catalogRepository.song(songId) }
                .onSuccess { song -> _state.value = _state.value.copy(songCards = _state.value.songCards + (songId to song)) }
                .onFailure {
                    _state.value = _state.value.copy(unavailableSongIds = _state.value.unavailableSongIds + songId)
                    _effects.send(ChatEffect.InvalidSong)
                }
            loadingSongs.remove(songId)
        }
    }

    fun retry(message: ChatMessageDto) = viewModelScope.launch {
        runCatching { chatRepository.retryMessage(message.clientMessageId) }
            .onFailure { _effects.send(ChatEffect.SendFailed) }
    }

    fun retryConnection() = chatRepository.retryRealtime()

    private suspend fun stopTyping() {
        typingStopJob?.cancel()
        if (typingSent) chatRepository.setTyping(userId, false)
        typingSent = false
    }

    override fun onCleared() {
        typingStopJob?.cancel()
        if (typingSent) chatRepository.clearTyping(userId)
        super.onCleared()
    }
}

data class ShareSongUiState(val song: SongDto? = null, val isLoading: Boolean = true, val isSending: Boolean = false, val hasError: Boolean = false)

sealed interface ShareSongEffect { data object Sent : ShareSongEffect }

@HiltViewModel
class ShareSongViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val socialRepository: SocialRepository,
    private val catalogRepository: CatalogRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val songId: String = checkNotNull(savedStateHandle["songId"])
    private val _state = MutableStateFlow(ShareSongUiState())
    val state = _state.asStateFlow()
    val friends = socialRepository.followingPager().cachedIn(viewModelScope)
    private val _effects = Channel<ShareSongEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    init { loadSong() }

    fun loadSong() = viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, hasError = false)
            runCatching { catalogRepository.song(songId) }
                .onSuccess { _state.value = ShareSongUiState(song = it, isLoading = false) }
                .onFailure { _state.value = ShareSongUiState(isLoading = false, hasError = true) }
    }

    fun sendTo(user: PublicUserDto) = viewModelScope.launch {
        if (_state.value.isSending) return@launch
        _state.value = _state.value.copy(isSending = true, hasError = false)
        runCatching { chatRepository.sendSong(user.id, songId) }
            .onSuccess { _effects.send(ShareSongEffect.Sent) }
            .onFailure { _state.value = _state.value.copy(isSending = false, hasError = true) }
    }
}
