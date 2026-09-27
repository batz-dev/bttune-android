package com.bt.bttune.ui.sharedmusic

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bt.bttune.extensions.toMediaItem
import com.bt.bttune.innertube.models.WatchEndpoint
import com.bt.bttune.playback.PlayerConnection
import com.bt.bttune.playback.queues.ListQueue
import com.bt.bttune.playback.queues.YouTubeQueue
import com.bt.bttune.providers.ProviderSong
import com.bt.bttune.share.SharedContent
import com.bt.bttune.usecases.IdentificationOutcome
import com.bt.bttune.usecases.IdentifySharedMusicUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SharedMusicViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val identifyUseCase = IdentifySharedMusicUseCase(context)

    private val _uiState = MutableStateFlow<SharedMusicUiState>(SharedMusicUiState.Idle)
    val uiState: StateFlow<SharedMusicUiState> = _uiState.asStateFlow()

    private var currentJob: Job? = null

    fun processSharedContent(content: SharedContent) {
        currentJob?.cancel()
        currentJob = viewModelScope.launch {
            _uiState.value = SharedMusicUiState.Idle

            identifyUseCase.execute(content).collect { (step, outcome) ->
                if (outcome == null) {
                    _uiState.value = SharedMusicUiState.Processing(step)
                } else {
                    _uiState.value = when (outcome) {
                        is IdentificationOutcome.Success -> SharedMusicUiState.Success(
                            identifiedSong = outcome.song,
                            airBeatsMatch = outcome.airBeatsMatch,
                            candidates = outcome.candidates
                        )
                        is IdentificationOutcome.NoMusicFound -> SharedMusicUiState.NoMusicFound
                        is IdentificationOutcome.NoAudio -> SharedMusicUiState.NoAudio
                        is IdentificationOutcome.UnsupportedMedia -> SharedMusicUiState.UnsupportedMedia(outcome.reason)
                        is IdentificationOutcome.IsUrlOnly -> SharedMusicUiState.UrlShared(outcome.url)
                        is IdentificationOutcome.NetworkError -> SharedMusicUiState.NetworkError
                        is IdentificationOutcome.Error -> SharedMusicUiState.Error(outcome.message)
                    }
                }
            }
        }
    }

    fun playSong(providerSong: ProviderSong, playerConnection: PlayerConnection) {
        if (providerSong.provider == "JIOSAAVN") {
            playerConnection.playQueue(
                ListQueue(
                    title = providerSong.title,
                    items = listOf(providerSong.mediaMetadata.toMediaItem())
                )
            )
        } else {
            playerConnection.playQueue(
                YouTubeQueue(
                    WatchEndpoint(videoId = providerSong.id),
                    providerSong.mediaMetadata
                )
            )
        }
    }

    fun cancel() {
        currentJob?.cancel()
        _uiState.value = SharedMusicUiState.Idle
    }
}
