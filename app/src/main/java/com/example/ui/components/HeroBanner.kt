package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.MediaItem
import com.example.ui.theme.ApexBlue
import com.example.ui.theme.ApexCyan
import com.example.ui.theme.CinemaDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.delay

@Composable
fun HeroBanner(
    heroItems: List<MediaItem>,
    onPlayClick: (MediaItem) -> Unit,
    onDetailsClick: (MediaItem) -> Unit,
    onWatchlistToggle: (MediaItem) -> Unit,
    isInWatchlist: (String) -> Boolean,
    modifier: Modifier = Modifier
) {
    if (heroItems.isEmpty()) return

    var currentIndex by remember { mutableIntStateOf(0) }

    // Auto rotate hero items every 6 seconds
    LaunchedEffect(heroItems.size) {
        if (heroItems.size > 1) {
            while (true) {
                delay(6000)
                currentIndex = (currentIndex + 1) % heroItems.size
            }
        }
    }

    val currentItem = heroItems[currentIndex.coerceIn(0, heroItems.lastIndex)]

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(420.dp)
            .background(CinemaDark)
    ) {
        // Crossfade image background
        AnimatedContent(
            targetState = currentItem,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "hero_banner"
        ) { targetMedia ->
            Box(modifier = Modifier.fillMaxSize()) {
                AsyncImage(
                    model = targetMedia.backdropUrl.ifBlank { targetMedia.posterUrl },
                    contentDescription = targetMedia.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Cinematic Gradients: Top status bar protection & Bottom fade
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xB3070B13),
                                    Color(0x33070B13),
                                    Color(0x99070B13),
                                    CinemaDark
                                )
                            )
                        )
                )
            }
        }

        // Overlay Content
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 20.dp)
        ) {
            // Badges row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .background(
                            Brush.horizontalGradient(listOf(ApexCyan, ApexBlue)),
                            RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = if (currentItem.isSeries) "FEATURED SERIES" else "FEATURED MOVIE",
                        color = Color(0xFF070B13),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.5.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .background(Color(0x66000000), RoundedCornerShape(4.dp))
                        .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = currentItem.qualityTag,
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                if (currentItem.year.isNotBlank()) {
                    Text(
                        text = currentItem.year,
                        color = TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Main Title
            Text(
                text = currentItem.title,
                color = TextPrimary,
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 32.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Synopsis preview
            Text(
                text = currentItem.synopsis,
                color = TextSecondary,
                fontSize = 13.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Stream Now Primary Action
                Button(
                    onClick = { onPlayClick(currentItem) },
                    modifier = Modifier
                        .weight(1.3f)
                        .height(46.dp)
                        .testTag("hero_play_button"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ApexCyan,
                        contentColor = Color(0xFF070B13)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Watch Now",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                // Watchlist toggle button
                val inList = isInWatchlist(currentItem.id)
                OutlinedButton(
                    onClick = { onWatchlistToggle(currentItem) },
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                        .testTag("hero_watchlist_button"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = TextPrimary
                    ),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = Brush.horizontalGradient(
                            if (inList) listOf(ApexCyan, ApexBlue) else listOf(Color(0x4DFFFFFF), Color(0x4DFFFFFF))
                        )
                    )
                ) {
                    Icon(
                        imageVector = if (inList) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        contentDescription = null,
                        tint = if (inList) ApexCyan else TextPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (inList) "Added" else "My List",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Info / Details icon button
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0x33FFFFFF))
                        .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(10.dp))
                        .clickable { onDetailsClick(currentItem) }
                        .testTag("hero_info_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Details",
                        tint = TextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Carousel dots indicator
            if (heroItems.size > 1) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    heroItems.forEachIndexed { idx, _ ->
                        val isSelected = idx == currentIndex
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 3.dp)
                                .size(width = if (isSelected) 18.dp else 6.dp, height = 6.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) ApexCyan else TextMuted.copy(alpha = 0.5f))
                                .clickable { currentIndex = idx }
                        )
                    }
                }
            }
        }
    }
}
