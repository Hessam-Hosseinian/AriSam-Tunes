package com.arisamtunes.feature.chat

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arisamtunes.data.chat.ChatConnectionStatus
import com.arisamtunes.data.chat.ChatConversationDto
import com.arisamtunes.data.chat.ChatMessageDto
import com.arisamtunes.data.chat.ChatRepository
import com.arisamtunes.data.social.PublicUserDto
import com.arisamtunes.data.social.SocialRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

data class ChatListUiState(
    val isLoading: Boolean = true,
    val conversations: List<ChatConversationDto> = emptyList(),
    val starters: List<PublicUserDto> = emptyList(),
    val hasError: Boolean = false,
)

@HiltViewModel
class ChatListViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val socialRepository: SocialRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(ChatListUiState())
    val state = _state.asStateFlow()

    init { refresh() }

    fun refresh() = viewModelScope.launch {
        _state.value = _state.value.copy(isLoading = true, hasError = false)
        runCatching {
            val conversations = chatRepository.conversations()
            val starters = if (conversations.isEmpty()) socialRepository.following() else emptyList()
            conversations to starters
        }.onSuccess { (conversations, starters) ->
            _state.value = ChatListUiState(isLoading = false, conversations = conversations, starters = starters)
        }.onFailure {
            _state.value = _state.value.copy(isLoading = false, hasError = true)
        }
    }
}

data class ChatDetailUiState(
    val isLoading: Boolean = true,
    val meId: String = "",
    val peer: PublicUserDto? = null,
    val messages: List<ChatMessageDto> = emptyList(),
    val draft: String = "",
    val status: ChatConnectionStatus = ChatConnectionStatus.Disconnected,
    val hasError: Boolean = false,
)

@HiltViewModel
class ChatDetailViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val socialRepository: SocialRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val userId: String = checkNotNull(savedStateHandle["userId"])
    private val _state = MutableStateFlow(ChatDetailUiState())
    val state = _state.asStateFlow()

    init {
        refresh()
        chatRepository.connect(userId)
        viewModelScope.launch {
            chatRepository.status.collectLatest { status -> _state.value = _state.value.copy(status = status) }
        }
        viewModelScope.launch {
            chatRepository.observeMessages(userId).distinctUntilChanged().collect { messages ->
                _state.value = _state.value.copy(messages = messages, isLoading = false)
            }
        }
    }

    fun refresh() = viewModelScope.launch {
        _state.value = _state.value.copy(isLoading = true, hasError = false)
        runCatching {
            Triple(chatRepository.currentUserId(), socialRepository.user(userId), chatRepository.messages(userId))
        }.onSuccess { (me, peer, messages) ->
            _state.value = _state.value.copy(isLoading = false, meId = me, peer = peer, messages = messages)
        }.onFailure {
            _state.value = _state.value.copy(isLoading = false, hasError = true)
        }
    }

    fun updateDraft(value: String) {
        _state.value = _state.value.copy(draft = value)
    }

    fun send() = viewModelScope.launch {
        val text = _state.value.draft.trim()
        if (text.isBlank()) return@launch
        _state.value = _state.value.copy(draft = "")
        runCatching { chatRepository.sendText(userId, text) }
            .onFailure { _state.value = _state.value.copy(hasError = true) }
    }

    override fun onCleared() {
        chatRepository.disconnect(userId)
        super.onCleared()
    }
}
