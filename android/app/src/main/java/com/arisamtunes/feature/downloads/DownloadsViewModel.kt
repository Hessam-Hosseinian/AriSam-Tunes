package com.arisamtunes.feature.downloads

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import androidx.paging.PagingData
import com.arisamtunes.data.local.LocalLibraryRepository
import com.arisamtunes.data.preferences.PreferencesRepository
import com.arisamtunes.data.auth.AuthRepository
import com.arisamtunes.data.catalog.SongDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DownloadsUiState(
    val isPremium: Boolean = false,
    val isUpdatingPremium: Boolean = false,
    val errorCode: String? = null,
    val sortOrder: DownloadSortOrder = DownloadSortOrder.Newest,
)

enum class DownloadSortOrder(val databaseValue: String) { Newest("NEWEST"), Title("TITLE"), Artist("ARTIST") }

sealed interface DownloadsEvent {
    data object DownloadTapped : DownloadsEvent
    data object UpgradeTapped : DownloadsEvent
    data object DismissError : DownloadsEvent
    data class RetryDownload(val song: SongDto) : DownloadsEvent
    data class SortChanged(val sortOrder: DownloadSortOrder) : DownloadsEvent
    data class DeleteDownload(val songId: String) : DownloadsEvent
}

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class DownloadsViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
    private val authRepository: AuthRepository,
    private val downloadWorkScheduler: DownloadWorkScheduler,
    private val localLibraryRepository: LocalLibraryRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(DownloadsUiState())
    val state = _state.asStateFlow()

    private val sortOrder = MutableStateFlow(DownloadSortOrder.Newest)

    val downloads = kotlinx.coroutines.flow.combine(preferencesRepository.preferences, sortOrder) { preferences, order ->
        preferences to order
    }.flatMapLatest { (preferences, order) ->
            val ownerUserId = preferences.currentUserId
                ?: return@flatMapLatest flowOf(PagingData.empty())
            localLibraryRepository.claimLegacyDownloads(ownerUserId)
            Pager(
                config = PagingConfig(pageSize = 20, initialLoadSize = 20),
                pagingSourceFactory = { localLibraryRepository.downloads(ownerUserId, order.databaseValue) },
            ).flow
        }
        .cachedIn(viewModelScope)

    val completedDownloads = localLibraryRepository.completedDownloads().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    init {
        viewModelScope.launch {
            preferencesRepository.preferences.collectLatest { preferences ->
                _state.update { it.copy(isPremium = preferences.isPremium) }
            }
        }
    }

    fun onEvent(event: DownloadsEvent) {
        when (event) {
            DownloadsEvent.DownloadTapped -> {
                if (!_state.value.isPremium) {
                    _state.update { it.copy(errorCode = PremiumRequired) }
                }
            }
            DownloadsEvent.UpgradeTapped -> upgradeToPremium()
            DownloadsEvent.DismissError -> _state.update { it.copy(errorCode = null) }
            is DownloadsEvent.RetryDownload -> retry(event.song)
            is DownloadsEvent.SortChanged -> {
                sortOrder.value = event.sortOrder
                _state.update { it.copy(sortOrder = event.sortOrder) }
            }
            is DownloadsEvent.DeleteDownload -> delete(event.songId)
        }
    }

    private fun upgradeToPremium() = viewModelScope.launch {
        if (_state.value.isUpdatingPremium) return@launch
        _state.update { it.copy(isUpdatingPremium = true) }
        runCatching { authRepository.updatePremium(true) }
            .onSuccess {
                _state.update { it.copy(isUpdatingPremium = false, errorCode = null) }
            }
            .onFailure {
                _state.update { it.copy(isUpdatingPremium = false, errorCode = PremiumUpdateFailed) }
            }
    }

    private fun retry(song: SongDto) = viewModelScope.launch {
        runCatching { downloadWorkScheduler.enqueue(song) }
            .onSuccess { result ->
                _state.update {
                    it.copy(errorCode = if (result == DownloadEnqueueResult.PremiumRequired) PremiumRequired else null)
                }
            }
            .onFailure { _state.update { it.copy(errorCode = DownloadRetryFailed) } }
    }

    private fun delete(songId: String) = viewModelScope.launch {
        runCatching {
            downloadWorkScheduler.cancel(songId)
            check(localLibraryRepository.deleteDownloadAndFile(songId))
        }.onSuccess {
            _state.update { it.copy(errorCode = null) }
        }.onFailure {
            _state.update { it.copy(errorCode = DownloadDeleteFailed) }
        }
    }

    companion object {
        const val PremiumRequired = "PREMIUM_REQUIRED"
        const val PremiumUpdateFailed = "PREMIUM_UPDATE_FAILED"
        const val DownloadRetryFailed = "DOWNLOAD_RETRY_FAILED"
        const val DownloadDeleteFailed = "DOWNLOAD_DELETE_FAILED"
    }
}
