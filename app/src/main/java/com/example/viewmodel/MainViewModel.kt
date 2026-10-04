package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.MediaRepository
import com.example.data.WatchlistAndHistoryStore
import com.example.model.EpisodeItem
import com.example.model.MediaItem
import com.example.model.MediaType
import com.example.model.StreamSource
import com.example.model.WatchProgress
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AppScreen {
    HOME,
    SEARCH,
    DETAIL,
    PLAYER,
    LIBRARY,
    SETTINGS
}

data class PlayerLaunchConfig(
    val media: MediaItem,
    val streamUrl: String,
    val title: String,
    val subtitle: String,
    val initialPositionMs: Long = 0L,
    val availableQualities: List<StreamSource> = emptyList(),
    val currentEpisode: EpisodeItem? = null,
    val allEpisodes: List<EpisodeItem> = emptyList()
)

data class CatalogUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val heroItems: List<MediaItem> = emptyList(),
    val allItems: List<MediaItem> = emptyList(),
    val movies: List<MediaItem> = emptyList(),
    val series: List<MediaItem> = emptyList(),
    val classics: List<MediaItem> = emptyList(),
    val fullHd: List<MediaItem> = emptyList(),
    val goFileItems: List<MediaItem> = emptyList()
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MediaRepository(application)
    private val store = WatchlistAndHistoryStore(application)

    private val _currentScreen = MutableStateFlow(AppScreen.HOME)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    private val screenStack = mutableListOf(AppScreen.HOME)

    private val _catalogState = MutableStateFlow(CatalogUiState())
    val catalogState: StateFlow<CatalogUiState> = _catalogState.asStateFlow()

    private val _selectedMedia = MutableStateFlow<MediaItem?>(null)
    val selectedMedia: StateFlow<MediaItem?> = _selectedMedia.asStateFlow()

    private val _isDetailLoading = MutableStateFlow(false)
    val isDetailLoading: StateFlow<Boolean> = _isDetailLoading.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<List<MediaItem>>(emptyList())
    val searchResults: StateFlow<List<MediaItem>> = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _continueWatching = MutableStateFlow<List<WatchProgress>>(emptyList())
    val continueWatching: StateFlow<List<WatchProgress>> = _continueWatching.asStateFlow()

    private val _watchlist = MutableStateFlow<List<MediaItem>>(emptyList())
    val watchlist: StateFlow<List<MediaItem>> = _watchlist.asStateFlow()

    private val _playerConfig = MutableStateFlow<PlayerLaunchConfig?>(null)
    val playerConfig: StateFlow<PlayerLaunchConfig?> = _playerConfig.asStateFlow()

    private var searchJob: Job? = null

    init {
        loadCatalog()
        refreshWatchHistoryAndWatchlist()
    }

    fun loadCatalog() {
        viewModelScope.launch {
            _catalogState.update { it.copy(isLoading = true, error = null) }
            try {
                val items = repository.getFullCatalog()
                if (items.isEmpty()) {
                    _catalogState.update {
                        it.copy(
                            isLoading = false,
                            error = "Could not fetch media catalog. Check internet connection."
                        )
                    }
                    return@launch
                }

                // Curate hero banner items with top movies and series
                val heroCandidates = items.filter {
                    val lower = it.title.lowercase()
                    lower.contains("baaghi") ||
                            lower.contains("neagley") ||
                            lower.contains("marco") ||
                            lower.contains("hera pheri") ||
                            lower.contains("mahabharat") ||
                            lower.contains("dhurandhar")
                }
                val heroList = if (heroCandidates.isNotEmpty()) heroCandidates else items.take(5)

                val moviesList = items.filter { it.type == MediaType.MOVIE }
                val seriesList = items.filter { it.type == MediaType.SERIES }
                val classicsList = items.filter {
                    val y = it.year.toIntOrNull() ?: 2025
                    y < 2000 || it.title.contains("Deewaar") || it.title.contains("Aandhi") || it.title.contains("Batwara")
                }
                val fullHdList = items.filter { it.qualityTag.contains("1080p", ignoreCase = true) }
                val goFileList = items.filter { it.driveIndex == 1 }

                _catalogState.value = CatalogUiState(
                    isLoading = false,
                    heroItems = heroList,
                    allItems = items,
                    movies = moviesList,
                    series = seriesList,
                    classics = classicsList,
                    fullHd = fullHdList,
                    goFileItems = goFileList
                )

                refreshWatchHistoryAndWatchlist()
            } catch (e: Exception) {
                e.printStackTrace()
                _catalogState.update {
                    it.copy(isLoading = false, error = "Failed to connect to Apex DRM server: ${e.localizedMessage}")
                }
            }
        }
    }

    fun navigateTo(screen: AppScreen) {
        if (_currentScreen.value != screen) {
            screenStack.add(screen)
            _currentScreen.value = screen
        }
    }

    fun navigateBack(): Boolean {
        if (screenStack.size > 1) {
            screenStack.removeAt(screenStack.lastIndex)
            _currentScreen.value = screenStack.last()
            return true
        }
        return false
    }

    fun selectMedia(media: MediaItem) {
        _selectedMedia.value = media
        navigateTo(AppScreen.DETAIL)

        // Asynchronously load folder details (episodes or multi-quality streams)
        viewModelScope.launch {
            _isDetailLoading.value = true
            try {
                val detailed = repository.loadFolderDetails(media)
                _selectedMedia.value = detailed
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isDetailLoading.value = false
            }
        }
    }

    fun playMedia(media: MediaItem, streamSource: StreamSource? = null, episode: EpisodeItem? = null) {
        val selectedSource = streamSource 
            ?: episode?.streamSources?.firstOrNull() 
            ?: media.streamSources.firstOrNull()

        val streamUrl = selectedSource?.streamUrl
        if (streamUrl.isNullOrBlank()) {
            // Try loading details first
            viewModelScope.launch {
                _isDetailLoading.value = true
                val detailed = repository.loadFolderDetails(media)
                _selectedMedia.value = detailed
                _isDetailLoading.value = false

                val directSource = detailed.streamSources.firstOrNull()
                val directUrl = directSource?.streamUrl
                if (!directUrl.isNullOrBlank()) {
                    launchPlayer(detailed, directUrl, detailed.title, directSource.label, 0L, detailed.streamSources, null, detailed.episodes)
                }
            }
            return
        }

        val title = media.title
        val subtitle = episode?.title ?: selectedSource.label
        val initialSeek = store.getProgressForMedia(media.id, episode?.title)

        launchPlayer(
            media = media,
            streamUrl = streamUrl,
            title = title,
            subtitle = subtitle,
            initialPositionMs = initialSeek,
            availableQualities = if (episode != null) episode.streamSources else media.streamSources,
            currentEpisode = episode,
            allEpisodes = media.episodes
        )
    }

    private fun launchPlayer(
        media: MediaItem,
        streamUrl: String,
        title: String,
        subtitle: String,
        initialPositionMs: Long,
        availableQualities: List<StreamSource>,
        currentEpisode: EpisodeItem?,
        allEpisodes: List<EpisodeItem>
    ) {
        _playerConfig.value = PlayerLaunchConfig(
            media = media,
            streamUrl = streamUrl,
            title = title,
            subtitle = subtitle,
            initialPositionMs = initialPositionMs,
            availableQualities = availableQualities,
            currentEpisode = currentEpisode,
            allEpisodes = allEpisodes
        )
        navigateTo(AppScreen.PLAYER)
    }

    fun onPlaybackProgress(positionMs: Long, durationMs: Long) {
        val config = _playerConfig.value ?: return
        if (positionMs <= 0 || durationMs <= 0) return

        val progress = WatchProgress(
            mediaId = config.media.id,
            title = config.title,
            posterUrl = config.media.posterUrl,
            lastPositionMs = positionMs,
            durationMs = durationMs,
            episodeTitle = config.currentEpisode?.title,
            streamUrl = config.streamUrl,
            lastWatchedTimestamp = System.currentTimeMillis()
        )
        store.saveProgress(progress)
        _continueWatching.value = store.getWatchProgressList()
    }

    fun toggleWatchlist(media: MediaItem) {
        store.toggleWatchlist(media.id)
        refreshWatchHistoryAndWatchlist()
    }

    fun isMediaInWatchlist(mediaId: String): Boolean {
        return store.isInWatchlist(mediaId)
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        searchJob?.cancel()
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            _isSearching.value = false
            return
        }

        searchJob = viewModelScope.launch {
            delay(300) // debounce typing
            _isSearching.value = true
            val results = repository.searchCatalog(query, _catalogState.value.allItems)
            _searchResults.value = results
            _isSearching.value = false
        }
    }

    fun refreshWatchHistoryAndWatchlist() {
        _continueWatching.value = store.getWatchProgressList()
        val ids = store.getWatchlistIds()
        _watchlist.value = _catalogState.value.allItems.filter { ids.contains(it.id) }
    }

    fun clearHistory() {
        store.clearHistory()
        _continueWatching.value = emptyList()
    }
}
