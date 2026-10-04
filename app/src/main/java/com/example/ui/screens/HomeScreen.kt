package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.MediaItem
import com.example.model.WatchProgress
import com.example.ui.components.ContinueWatchingRow
import com.example.ui.components.HeroBanner
import com.example.ui.components.MediaPosterCard
import com.example.ui.components.MediaRow
import com.example.ui.theme.ApexBlue
import com.example.ui.theme.ApexCyan
import com.example.ui.theme.CinemaDark
import com.example.ui.theme.CinemaGold
import com.example.ui.theme.CinemaSurface
import com.example.ui.theme.CinemaSurfaceVariant
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.CatalogUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    catalogState: CatalogUiState,
    continueWatching: List<WatchProgress>,
    isInWatchlist: (String) -> Boolean,
    onMediaClick: (MediaItem) -> Unit,
    onPlayClick: (MediaItem) -> Unit,
    onResumeProgress: (WatchProgress) -> Unit,
    onWatchlistToggle: (MediaItem) -> Unit,
    onSearchClick: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedFilter by remember { mutableStateOf("All") }
    val filters = listOf("All", "Trending", "Movies", "Web Series", "4K Ultra HD", "Dolby Atmos")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // MovieBox Style Cyan-Blue Badge
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    Brush.linearGradient(listOf(ApexCyan, ApexBlue))
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Videocam,
                                contentDescription = null,
                                tint = Color(0xFF070B13),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "APEX",
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 18.sp,
                                    letterSpacing = 1.sp
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "STREAM 4K",
                                    color = ApexCyan,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 12.sp
                                )
                            }
                            Text(
                                text = "Ultra 4K Stream • Dolby Atmos",
                                color = TextMuted,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        // Live Online Pill
                        Box(
                            modifier = Modifier
                                .background(Color(0x2610B981), RoundedCornerShape(12.dp))
                                .border(1.dp, Color(0xFF10B981), RoundedCornerShape(12.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "ATMOS 4K",
                                color = Color(0xFF34D399),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = onSearchClick,
                        modifier = Modifier.testTag("home_search_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = TextPrimary
                        )
                    }
                    IconButton(
                        onClick = onRefresh,
                        modifier = Modifier.testTag("home_refresh_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = TextSecondary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CinemaDark)
            )
        },
        containerColor = CinemaDark,
        modifier = modifier
    ) { innerPadding ->
        if (catalogState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(
                        color = ApexCyan,
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(52.dp)
                    )
                    Text(
                        text = "Loading Apex DRM & TMDB Data...",
                        color = TextSecondary,
                        fontSize = 14.sp
                    )
                }
            }
            return@Scaffold
        }

        if (catalogState.error != null && catalogState.allItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(24.dp)
                ) {
                    Text(
                        text = "Connection Error",
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = catalogState.error,
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                    IconButton(onClick = onRefresh) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Retry",
                            tint = ApexCyan
                        )
                    }
                }
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Hero Banner
            item {
                HeroBanner(
                    heroItems = catalogState.heroItems,
                    onPlayClick = onPlayClick,
                    onDetailsClick = onMediaClick,
                    onWatchlistToggle = onWatchlistToggle,
                    isInWatchlist = isInWatchlist
                )
            }

            // Filter Chips Bar
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filters) { filter ->
                        val selected = selectedFilter == filter
                        FilterChip(
                            selected = selected,
                            onClick = { selectedFilter = filter },
                            label = {
                                Text(
                                    text = filter,
                                    fontSize = 12.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = CinemaSurface,
                                labelColor = TextSecondary,
                                selectedContainerColor = ApexCyan,
                                selectedLabelColor = Color(0xFF070B13)
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = selected,
                                borderColor = Color(0x33FFFFFF),
                                selectedBorderColor = ApexCyan
                            )
                        )
                    }
                }
            }

            // Continue Watching section
            if (continueWatching.isNotEmpty()) {
                item {
                    ContinueWatchingRow(
                        progressList = continueWatching,
                        onResumeClick = onResumeProgress
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }

            // Categorized Rows based on selected filter
            when (selectedFilter) {
                "All" -> {
                    // Apex Top 10 Ranked Row
                    item {
                        ApexTop10Row(
                            items = catalogState.allItems.take(10),
                            onItemClick = onMediaClick
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                    }

                    // Trending
                    item {
                        MediaRow(
                            title = "Trending Movies & Series",
                            items = catalogState.allItems.take(12),
                            onItemClick = onMediaClick,
                            icon = Icons.Default.Whatshot
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                    }

                    // Web Series
                    if (catalogState.series.isNotEmpty()) {
                        item {
                            MediaRow(
                                title = "Popular Web Series",
                                items = catalogState.series,
                                onItemClick = onMediaClick,
                                icon = Icons.Default.Tv
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                        }
                    }

                    // 1080p Atmos
                    if (catalogState.fullHd.isNotEmpty()) {
                        item {
                            MediaRow(
                                title = "1080p Dolby Atmos Collection",
                                items = catalogState.fullHd,
                                onItemClick = onMediaClick,
                                icon = Icons.Default.HighQuality
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                        }
                    }

                    // Bollywood Classics
                    if (catalogState.classics.isNotEmpty()) {
                        item {
                            MediaRow(
                                title = "Bollywood Blockbusters & Classics",
                                items = catalogState.classics,
                                onItemClick = onMediaClick,
                                icon = Icons.Default.Movie
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                        }
                    }

                    // GoFile Vault
                    if (catalogState.goFileItems.isNotEmpty()) {
                        item {
                            MediaRow(
                                title = "GoFile Cloud Storage Vault",
                                items = catalogState.goFileItems,
                                onItemClick = onMediaClick,
                                icon = Icons.Default.CloudQueue
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                        }
                    }
                }

                "Top 10" -> {
                    item {
                        ApexTop10Row(
                            items = catalogState.allItems.take(10),
                            onItemClick = onMediaClick
                        )
                    }
                }

                "Movies" -> {
                    item {
                        MediaGridSection(
                            title = "All Movies",
                            items = catalogState.movies,
                            onItemClick = onMediaClick
                        )
                    }
                }

                "Web Series" -> {
                    item {
                        MediaGridSection(
                            title = "Web Series & Shows",
                            items = catalogState.series,
                            onItemClick = onMediaClick
                        )
                    }
                }

                "1080p Atmos" -> {
                    item {
                        MediaGridSection(
                            title = "1080p Full HD with Atmos Audio",
                            items = catalogState.fullHd,
                            onItemClick = onMediaClick
                        )
                    }
                }

                "GoFile Vault" -> {
                    item {
                        MediaGridSection(
                            title = "GoFile Cloud Vault",
                            items = catalogState.goFileItems,
                            onItemClick = onMediaClick
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

// Apex Signature Top 10 with giant numbered outlines
@Composable
fun ApexTop10Row(
    items: List<MediaItem>,
    onItemClick: (MediaItem) -> Unit,
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) return

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.TrendingUp,
                contentDescription = null,
                tint = ApexCyan,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = "Top 10 on Apex Today",
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            itemsIndexed(items) { index, media ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { onItemClick(media) }
                ) {
                    // Giant Outlined Rank Number
                    Text(
                        text = "${index + 1}",
                        fontSize = 80.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0x664FACFE),
                        lineHeight = 80.sp,
                        modifier = Modifier.padding(end = 4.dp)
                    )

                    // Poster Card
                    MediaPosterCard(
                        media = media,
                        onClick = { onItemClick(media) },
                        cardWidth = 125
                    )
                }
            }
        }
    }
}

@Composable
fun MediaGridSection(
    title: String,
    items: List<MediaItem>,
    onItemClick: (MediaItem) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = title,
            color = TextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        val chunked = items.chunked(3)
        chunked.forEach { rowItems ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                rowItems.forEach { media ->
                    MediaPosterCard(
                        media = media,
                        onClick = { onItemClick(media) },
                        modifier = Modifier.weight(1f)
                    )
                }
                if (rowItems.size < 3) {
                    repeat(3 - rowItems.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}
