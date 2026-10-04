package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.screens.DetailScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.PlayerScreen
import com.example.ui.screens.SearchScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.ApexCyan
import com.example.ui.theme.CinemaDark
import com.example.ui.theme.CinemaSurface
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.viewmodel.AppScreen
import com.example.viewmodel.MainViewModel

@Composable
fun MainApp(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val catalogState by viewModel.catalogState.collectAsStateWithLifecycle()
    val continueWatching by viewModel.continueWatching.collectAsStateWithLifecycle()
    val watchlist by viewModel.watchlist.collectAsStateWithLifecycle()
    val selectedMedia by viewModel.selectedMedia.collectAsStateWithLifecycle()
    val isDetailLoading by viewModel.isDetailLoading.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
    val isSearching by viewModel.isSearching.collectAsStateWithLifecycle()
    val playerConfig by viewModel.playerConfig.collectAsStateWithLifecycle()

    val showBottomBar = currentScreen != AppScreen.PLAYER

    Scaffold(
        bottomBar = {
            AnimatedVisibility(
                visible = showBottomBar,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it })
            ) {
                NavigationBar(
                    containerColor = CinemaSurface,
                    contentColor = TextPrimary,
                    modifier = Modifier
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .testTag("main_bottom_nav_bar")
                ) {
                    val navItems = listOf(
                        NavigationItem(
                            screen = AppScreen.HOME,
                            label = "Home",
                            selectedIcon = Icons.Filled.Home,
                            unselectedIcon = Icons.Outlined.Home
                        ),
                        NavigationItem(
                            screen = AppScreen.SEARCH,
                            label = "Search",
                            selectedIcon = Icons.Filled.Search,
                            unselectedIcon = Icons.Outlined.Search
                        ),
                        NavigationItem(
                            screen = AppScreen.LIBRARY,
                            label = "Library",
                            selectedIcon = Icons.Filled.VideoLibrary,
                            unselectedIcon = Icons.Outlined.VideoLibrary
                        ),
                        NavigationItem(
                            screen = AppScreen.SETTINGS,
                            label = "Server",
                            selectedIcon = Icons.Filled.Settings,
                            unselectedIcon = Icons.Outlined.Settings
                        )
                    )

                    navItems.forEach { item ->
                        val selected = currentScreen == item.screen
                        NavigationBarItem(
                            selected = selected,
                            onClick = { viewModel.navigateTo(item.screen) },
                            icon = {
                                Icon(
                                    imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = item.label
                                )
                            },
                            label = {
                                Text(
                                    text = item.label,
                                    fontSize = 11.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color(0xFF070B13),
                                unselectedIconColor = TextMuted,
                                selectedTextColor = ApexCyan,
                                unselectedTextColor = TextMuted,
                                indicatorColor = ApexCyan
                            ),
                            modifier = Modifier.testTag("nav_item_${item.label.lowercase()}")
                        )
                    }
                }
            }
        },
        containerColor = CinemaDark,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        when (currentScreen) {
            AppScreen.HOME -> {
                HomeScreen(
                    catalogState = catalogState,
                    continueWatching = continueWatching,
                    isInWatchlist = { id -> viewModel.isMediaInWatchlist(id) },
                    onMediaClick = { media -> viewModel.selectMedia(media) },
                    onPlayClick = { media -> viewModel.playMedia(media) },
                    onResumeProgress = { progress ->
                        val media = catalogState.allItems.firstOrNull { it.id == progress.mediaId }
                        if (media != null) {
                            viewModel.playMedia(media)
                        }
                    },
                    onWatchlistToggle = { media -> viewModel.toggleWatchlist(media) },
                    onSearchClick = { viewModel.navigateTo(AppScreen.SEARCH) },
                    onRefresh = { viewModel.loadCatalog() }
                )
            }

            AppScreen.SEARCH -> {
                SearchScreen(
                    searchQuery = searchQuery,
                    searchResults = searchResults,
                    isSearching = isSearching,
                    onQueryChange = { q -> viewModel.onSearchQueryChanged(q) },
                    onMediaClick = { media -> viewModel.selectMedia(media) },
                    onBack = { viewModel.navigateBack() }
                )
            }

            AppScreen.DETAIL -> {
                selectedMedia?.let { media ->
                    DetailScreen(
                        media = media,
                        isLoadingDetails = isDetailLoading,
                        isInWatchlist = viewModel.isMediaInWatchlist(media.id),
                        onBack = { viewModel.navigateBack() },
                        onPlay = { targetMedia, source, episode ->
                            viewModel.playMedia(targetMedia, source, episode)
                        },
                        onWatchlistToggle = { targetMedia -> viewModel.toggleWatchlist(targetMedia) }
                    )
                } ?: run {
                    viewModel.navigateBack()
                }
            }

            AppScreen.PLAYER -> {
                playerConfig?.let { config ->
                    PlayerScreen(
                        config = config,
                        onProgressUpdate = { pos, dur ->
                            viewModel.onPlaybackProgress(pos, dur)
                        },
                        onClose = { viewModel.navigateBack() }
                    )
                } ?: run {
                    viewModel.navigateBack()
                }
            }

            AppScreen.LIBRARY -> {
                LibraryScreen(
                    watchlist = watchlist,
                    watchHistory = continueWatching,
                    onMediaClick = { media -> viewModel.selectMedia(media) },
                    onResumeProgress = { progress ->
                        val media = catalogState.allItems.firstOrNull { it.id == progress.mediaId }
                        if (media != null) {
                            viewModel.playMedia(media)
                        }
                    },
                    onClearHistory = { viewModel.clearHistory() }
                )
            }

            AppScreen.SETTINGS -> {
                SettingsScreen(
                    onClearHistory = { viewModel.clearHistory() }
                )
            }
        }
    }
}

private data class NavigationItem(
    val screen: AppScreen,
    val label: String,
    val selectedIcon: androidx.compose.ui.graphics.vector.ImageVector,
    val unselectedIcon: androidx.compose.ui.graphics.vector.ImageVector
)
