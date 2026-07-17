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
import com.arisamtunes.data.chat.ChatMessageTypeDto
import com.arisamtunes.data.chat.ChatPresenceDto
import com.arisamtunes.data.chat.ChatSocketTypeDto
import com.arisamtunes.data.chat.ChatRepository
import com.arisamtunes.data.social.PublicUserDto
import com.arisamtunes.data.social.SocialRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onEach
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
    val isSearchPending: Boolean = false,
)

@HiltViewModel
@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class ChatListViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val socialRepository: SocialRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(ChatListUiState())
    val state = _state.asStateFlow()
    val conversations: Flow<PagingData<ChatConversationDto>> = chatRepository.conversationsPager().cachedIn(viewModelScope)
    val starters: Flow<PagingData<PublicUserDto>> = socialRepository.followingPager().cachedIn(viewModelScope)
    private val query = MutableStateFlow("")
    val searchResults: Flow<PagingData<PublicUserDto>> = query
        .debounce(350)
        .flatMapLatest { value ->
            if (value.isBlank()) {
                flowOf(PagingData.empty())
            } else {
                socialRepository.searchUsersPager(value)
            }
        }
        .onEach { _state.value = _state.value.copy(isSearchPending = false) }
        .cachedIn(viewModelScope)

    fun updateSearch(value: String) {
        val safe = value.take(100)
        _state.value = _state.value.copy(
            searchQuery = safe,
            isSearching = safe.isNotBlank(),
            isSearchPending = safe.isNotBlank(),
        )
        query.value = safe.trim()
    }

    fun clearSearch() = updateSearch("")
}

sealed interface ChatEffect {
    data object SendFailed : ChatEffect
    data object EditFailed : ChatEffect
    data object DeleteFailed : ChatEffect
    data object ReactionFailed : ChatEffect
    data object SearchFailed : ChatEffect
    data object RetryFailed : ChatEffect
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
    val replyPreviews: Map<String, ChatMessageDto> = emptyMap(),
    val replyTo: ChatMessageDto? = null,
    val editingMessage: ChatMessageDto? = null,
    val selectedMessage: ChatMessageDto? = null,
    val isSearchOpen: Boolean = false,
    val searchQuery: String = "",
    val searchResults: List<ChatMessageDto> = emptyList(),
    val isSearching: Boolean = false,
    val scrollToMessageId: String? = null,
    val scrollToMessageIndex: Int? = null,
    val highlightedMessageId: String? = null,
    val isSending: Boolean = false,
    val peerPresence: ChatPresenceDto? = null,
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
    private val loadingSongs = ConcurrentHashMap.newKeySet<String>()
    private val readReceiptsQueued = ConcurrentHashMap.newKeySet<String>()
    private val reactionsInFlight = ConcurrentHashMap.newKeySet<String>()
    private var typingStopJob: Job? = null
    private var typingStartJob: Job? = null
    private var peerTypingExpiryJob: Job? = null
    private var typingSent = false
    private var searchJob: Job? = null
    private var searchJumpJob: Job? = null
    private var highlightJob: Job? = null
    private var editAckJob: Job? = null
    private var pendingEditId: String? = null
    private var draftBeforeEdit: String? = null

    init {
        chatRepository.subscribePresence(userId)
        refreshPeer()
        viewModelScope.launch {
            chatRepository.status.collectLatest { _state.value = _state.value.copy(status = it) }
        }
        viewModelScope.launch {
            chatRepository.events.collect { event ->
                if (event.type == ChatSocketTypeDto.ERROR) {
                    val effect = when {
                        event.error == "SONG_NOT_FOUND" -> ChatEffect.InvalidSong
                        event.requestType == ChatSocketTypeDto.EDIT_MESSAGE -> ChatEffect.EditFailed
                        event.requestType == ChatSocketTypeDto.DELETE_MESSAGE -> ChatEffect.DeleteFailed
                        event.requestType == ChatSocketTypeDto.ADD_REACTION || event.requestType == ChatSocketTypeDto.REMOVE_REACTION -> ChatEffect.ReactionFailed
                        else -> ChatEffect.SendFailed
                    }
                    if (event.requestType == ChatSocketTypeDto.EDIT_MESSAGE) {
                        editAckJob?.cancel()
                        pendingEditId = null
                        _state.value = _state.value.copy(isSending = false)
                    }
                    _effects.send(effect)
                }
                if (event.type == ChatSocketTypeDto.MESSAGE_UPDATED && event.message?.id == pendingEditId) {
                    editAckJob?.cancel()
                    pendingEditId = null
                    _state.value = _state.value.copy(
                        draft = draftBeforeEdit.orEmpty(),
                        editingMessage = null,
                        isSending = false,
                    )
                    draftBeforeEdit = null
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
        viewModelScope.launch {
            chatRepository.presence.collectLatest { presence ->
                _state.value = _state.value.copy(peerPresence = presence[userId])
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
        val safeValue = value.take(4_000)
        _state.value = _state.value.copy(draft = safeValue)
        typingStopJob?.cancel()
        if (safeValue.isBlank()) {
            stopTypingFromUi()
            return
        }
        startTypingIfNeeded()
        typingStopJob = viewModelScope.launch {
            delay(1_500)
            sendTypingStopped()
        }
    }

    fun send() = viewModelScope.launch {
        val snapshot = _state.value
        val text = snapshot.draft.trim()
        if (text.isBlank() || snapshot.isSending) return@launch
        _state.value = snapshot.copy(isSending = true)
        stopTypingFromUi()
        try {
            if (snapshot.editingMessage != null) {
                pendingEditId = snapshot.editingMessage.id
                chatRepository.editMessage(snapshot.editingMessage.id, text)
                if (pendingEditId == snapshot.editingMessage.id) {
                    editAckJob?.cancel()
                    editAckJob = viewModelScope.launch {
                        delay(8_000)
                        if (pendingEditId == snapshot.editingMessage.id) {
                            pendingEditId = null
                            _state.value = _state.value.copy(isSending = false)
                            _effects.send(ChatEffect.EditFailed)
                        }
                    }
                }
            } else {
                chatRepository.sendText(userId, text, snapshot.replyTo?.id)
                _state.value = _state.value.copy(
                    draft = if (_state.value.draft == snapshot.draft) "" else _state.value.draft,
                    replyTo = null,
                    isSending = false,
                )
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Throwable) {
            if (snapshot.editingMessage != null) pendingEditId = null
            _state.value = _state.value.copy(isSending = false)
            _effects.send(if (snapshot.editingMessage != null) ChatEffect.EditFailed else ChatEffect.SendFailed)
        }
    }

    fun sendSong(songId: String) = viewModelScope.launch {
        val snapshot = _state.value
        if (snapshot.isSending || snapshot.editingMessage != null) return@launch
        _state.value = snapshot.copy(isSending = true)
        stopTypingFromUi()
        try {
            chatRepository.sendSong(userId, songId, snapshot.replyTo?.id)
            _state.value = _state.value.copy(replyTo = null, isSending = false)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Throwable) {
            _state.value = _state.value.copy(isSending = false)
            _effects.send(ChatEffect.SendFailed)
        }
    }

    fun onMessagePresented(message: ChatMessageDto) {
        message.replyToId?.takeIf { it !in _state.value.replyPreviews }?.let { replyId ->
            viewModelScope.launch {
                try {
                    chatRepository.localMessage(replyId)?.let { reply ->
                        _state.value = _state.value.copy(replyPreviews = _state.value.replyPreviews + (replyId to reply))
                    }
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (_: Throwable) {
                    // A missing reply preview must not affect the message list.
                }
            }
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

    fun markVisibleMessagesRead(messages: List<ChatMessageDto>) {
        messages.asSequence()
            .filter { it.senderId == userId && it.status != ChatMessageStatusDto.READ }
            .filter { readReceiptsQueued.add(it.id) }
            .forEach { message ->
                viewModelScope.launch {
                    try {
                        // The repository persists a pending receipt before trying the socket,
                        // so an offline result is still durably retryable.
                        chatRepository.markMessageRead(message.id)
                    } catch (cancellation: CancellationException) {
                        readReceiptsQueued.remove(message.id)
                        throw cancellation
                    } catch (_: Throwable) {
                        readReceiptsQueued.remove(message.id)
                    }
                }
            }
    }

    fun retry(message: ChatMessageDto) = viewModelScope.launch {
        runCatching { chatRepository.retryMessage(message.clientMessageId) }
            .onFailure { _effects.send(ChatEffect.RetryFailed) }
    }

    fun retryConnection() = chatRepository.retryRealtime()

    fun selectMessage(message: ChatMessageDto?) { _state.value = _state.value.copy(selectedMessage = message) }

    fun replyTo(message: ChatMessageDto) {
        _state.value = _state.value.copy(replyTo = message, editingMessage = null, selectedMessage = null)
    }

    fun edit(message: ChatMessageDto) {
        if (message.senderId != _state.value.meId || message.deletedAt != null || message.messageType != ChatMessageTypeDto.TEXT) return
        if (_state.value.editingMessage == null) draftBeforeEdit = _state.value.draft
        _state.value = _state.value.copy(draft = message.content.orEmpty(), editingMessage = message, replyTo = null, selectedMessage = null)
    }

    fun cancelComposerContext() {
        val wasEditing = _state.value.editingMessage != null
        _state.value = _state.value.copy(
            replyTo = null,
            editingMessage = null,
            draft = if (wasEditing) draftBeforeEdit.orEmpty() else _state.value.draft,
        )
        if (wasEditing) draftBeforeEdit = null
    }

    fun delete(message: ChatMessageDto) = viewModelScope.launch {
        _state.value = _state.value.copy(selectedMessage = null)
        runCatching { chatRepository.deleteMessage(message.id) }.onFailure { _effects.send(ChatEffect.DeleteFailed) }
    }

    fun toggleReaction(message: ChatMessageDto, reaction: String) = viewModelScope.launch {
        _state.value = _state.value.copy(selectedMessage = null)
        val key = "${message.id}:$reaction"
        if (!reactionsInFlight.add(key)) return@launch
        val add = message.reactions.none { it.reaction == reaction && it.reactedByMe }
        try {
            chatRepository.setReaction(message.id, reaction, add)
            delay(600)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Throwable) {
            _effects.send(ChatEffect.ReactionFailed)
        } finally {
            reactionsInFlight.remove(key)
        }
    }

    fun toggleSearch() {
        searchJob?.cancel()
        searchJumpJob?.cancel()
        val open = !_state.value.isSearchOpen
        _state.value = _state.value.copy(isSearchOpen = open, searchQuery = "", searchResults = emptyList(), isSearching = false)
    }

    fun updateSearch(value: String) {
        val query = value.take(100)
        _state.value = _state.value.copy(searchQuery = query)
        searchJob?.cancel()
        searchJumpJob?.cancel()
        if (query.trim().length < 2) {
            _state.value = _state.value.copy(searchResults = emptyList(), isSearching = false)
            return
        }
        searchJob = viewModelScope.launch {
            delay(350)
            _state.value = _state.value.copy(isSearching = true)
            try {
                val results = chatRepository.searchMessages(userId, query.trim())
                if (_state.value.searchQuery == query) _state.value = _state.value.copy(searchResults = results, isSearching = false)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Throwable) {
                if (_state.value.searchQuery == query) _state.value = _state.value.copy(isSearching = false)
                _effects.send(ChatEffect.SearchFailed)
            }
        }
    }

    fun openSearchResult(message: ChatMessageDto) {
        searchJob?.cancel()
        searchJumpJob?.cancel()
        searchJumpJob = viewModelScope.launch {
            try {
                val position = chatRepository.messagePosition(userId, message)
                _state.value = _state.value.copy(
                    isSearchOpen = false,
                    searchResults = emptyList(),
                    isSearching = false,
                    scrollToMessageId = message.id,
                    scrollToMessageIndex = position,
                    highlightedMessageId = message.id,
                )
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Throwable) {
                _effects.send(ChatEffect.SearchFailed)
            }
        }
    }

    fun consumeScrollTarget() {
        _state.value = _state.value.copy(scrollToMessageId = null, scrollToMessageIndex = null)
        highlightJob?.cancel()
        highlightJob = viewModelScope.launch {
            delay(1_600)
            _state.value = _state.value.copy(highlightedMessageId = null)
        }
    }

    fun searchJumpFailed() {
        _state.value = _state.value.copy(
            isSearchOpen = true,
            scrollToMessageId = null,
            scrollToMessageIndex = null,
            highlightedMessageId = null,
        )
        viewModelScope.launch { _effects.send(ChatEffect.SearchFailed) }
    }

    private fun startTypingIfNeeded() {
        if (typingSent || typingStartJob?.isActive == true) return
        typingStartJob = viewModelScope.launch {
            try {
                typingSent = chatRepository.setTyping(userId, true)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Throwable) {
                typingSent = false
            }
        }
    }

    private fun stopTypingFromUi() {
        typingStopJob?.cancel()
        typingStopJob = null
        typingStartJob?.cancel()
        typingStartJob = null
        viewModelScope.launch { sendTypingStopped() }
    }

    private suspend fun sendTypingStopped() {
        if (!typingSent) return
        try {
            if (chatRepository.setTyping(userId, false)) typingSent = false
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Throwable) {
            // Typing is best-effort and must never cancel send/edit operations.
        }
    }

    override fun onCleared() {
        typingStopJob?.cancel()
        typingStartJob?.cancel()
        peerTypingExpiryJob?.cancel()
        searchJob?.cancel()
        searchJumpJob?.cancel()
        highlightJob?.cancel()
        editAckJob?.cancel()
        if (typingSent) chatRepository.clearTyping(userId)
        chatRepository.unsubscribePresence(userId)
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
