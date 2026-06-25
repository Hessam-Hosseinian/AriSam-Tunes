package com.arisamtunes.feature.downloads

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import com.arisamtunes.data.local.LocalLibraryRepository
import com.arisamtunes.data.preferences.PreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DownloadsUiState(
    val isPremium: Boolean = false,
    val errorCode: String? = null,
)

sealed interface DownloadsEvent {
    data object DownloadTapped : DownloadsEvent
    data object UpgradeTapped : DownloadsEvent
    data object DismissError : DownloadsEvent
}

@HiltViewModel
class DownloadsViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
    localLibraryRepository: LocalLibraryRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(DownloadsUiState())
    val state = _state.asStateFlow()

    val downloads = Pager(
        config = PagingConfig(pageSize = 20, initialLoadSize = 20),
        pagingSourceFactory = localLibraryRepository::downloads,
    ).flow.cachedIn(viewModelScope)

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
            DownloadsEvent.UpgradeTapped -> viewModelScope.launch {
                preferencesRepository.setPremium(true)
                _state.update { it.copy(errorCode = null) }
            }
            DownloadsEvent.DismissError -> _state.update { it.copy(errorCode = null) }
        }
    }

    companion object {
        const val PremiumRequired = "PREMIUM_REQUIRED"
    }
}
