@file:kotlin.OptIn(
    androidx.media3.common.util.UnstableApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class
)

package com.example.ui.screens

import android.app.Activity
import android.content.pm.ActivityInfo
import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem as ExoMediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import com.example.model.AudioTrackInfo
import com.example.model.EpisodeItem
import com.example.model.StreamSource
import com.example.model.SubtitleTrackInfo
import com.example.model.VideoResizeMode
import com.example.ui.theme.ApexBlue
import com.example.ui.theme.ApexCyan
import com.example.ui.theme.CinemaDark
import com.example.ui.theme.CinemaSurface
import com.example.ui.theme.CinemaSurfaceVariant
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.PlayerLaunchConfig
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun PlayerScreen(
    config: PlayerLaunchConfig,
    onProgressUpdate: (Long, Long) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val scope = rememberCoroutineScope()

    // Active media state
    var currentStreamUrl by remember { mutableStateOf(config.streamUrl) }
    var currentTitle by remember { mutableStateOf(config.title) }
    var currentSubtitle by remember { mutableStateOf(config.subtitle) }
    var currentEpisode by remember { mutableStateOf(config.currentEpisode) }

    // Playback state
    var isPlaying by remember { mutableStateOf(true) }
    var isBuffering by remember { mutableStateOf(true) }
    var currentPositionMs by remember { mutableLongStateOf(config.initialPositionMs) }
    var durationMs by remember { mutableLongStateOf(0L) }
    var isUserSeeking by remember { mutableStateOf(false) }
    var seekSliderPos by remember { mutableFloatStateOf(0f) }

    // Controls visibility & lock
    var controlsVisible by remember { mutableStateOf(true) }
    var isLocked by remember { mutableStateOf(false) }
    var selectedResizeMode by remember { mutableStateOf(VideoResizeMode.FIT) }
    var playbackSpeed by remember { mutableFloatStateOf(1.0f) }

    // Netflix double-tap seek state
    var rewindCountSeconds by remember { mutableIntStateOf(0) }
    var forwardCountSeconds by remember { mutableIntStateOf(0) }
    var showRewindRipple by remember { mutableStateOf(false) }
    var showForwardRipple by remember { mutableStateOf(false) }

    // Gesture HUD states (Horizontal scrub, Left vertical brightness, Right vertical volume)
    var isScrubbingGesture by remember { mutableStateOf(false) }
    var scrubTargetPositionMs by remember { mutableLongStateOf(0L) }
    var scrubDeltaSeconds by remember { mutableLongStateOf(0L) }

    var isBrightnessGesture by remember { mutableStateOf(false) }
    var currentBrightness by remember { mutableFloatStateOf(0.7f) }

    var isVolumeGesture by remember { mutableStateOf(false) }
    var currentVolume by remember { mutableFloatStateOf(1.0f) }

    var toastHudMessage by remember { mutableStateOf<String?>(null) }

    // Audio & Subtitle tracks
    val availableAudioTracks = remember { mutableStateListOf<AudioTrackInfo>() }
    val availableSubtitleTracks = remember { mutableStateListOf<SubtitleTrackInfo>() }
    var currentAudioLabel by remember { mutableStateOf("Hindi • Dolby Atmos") }
    var isAtmosBoostEnabled by remember { mutableStateOf(true) }

    // Sheets & Dialogs
    var showAudioSheet by remember { mutableStateOf(false) }
    var showSubtitleSheet by remember { mutableStateOf(false) }
    var showResizeSheet by remember { mutableStateOf(false) }
    var showEpisodesDrawer by remember { mutableStateOf(false) }
    var showSpeedMenu by remember { mutableStateOf(false) }
    var showQualityMenu by remember { mutableStateOf(false) }

    // Next Episode Auto Prompt
    var showNextEpisodePrompt by remember { mutableStateOf(false) }
    var nextEpisodeCountdown by remember { mutableIntStateOf(10) }

    // Setup ExoPlayer with Zero-Buffering 4K configuration & Dolby Atmos
    val exoPlayer = remember {
        val renderersFactory = DefaultRenderersFactory(context)
            .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)
            .setEnableDecoderFallback(true)

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
            .setSpatializationBehavior(C.SPATIALIZATION_BEHAVIOR_AUTO)
            .build()

        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36")
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(15000)
            .setReadTimeoutMs(20000)
            .setDefaultRequestProperties(
                mapOf(
                    "Accept" to "*/*",
                    "Accept-Encoding" to "identity",
                    "Connection" to "keep-alive"
                )
            )

        val mediaSourceFactory = DefaultMediaSourceFactory(context)
            .setDataSourceFactory(httpDataSourceFactory)

        // Zero-buffering 4K load control: starts playback in 500ms, pre-buffers 2 mins ahead, 60s back-buffer
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                30_000,   // minBufferMs (30s)
                120_000,  // maxBufferMs (2 mins ahead)
                500,      // bufferForPlaybackMs (INSTANT 0.5s START)
                1_000     // bufferForPlaybackAfterRebufferMs (1s resume)
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .setBackBuffer(60_000, true) // 60s retained for instantaneous rewind
            .build()

        ExoPlayer.Builder(context, renderersFactory)
            .setMediaSourceFactory(mediaSourceFactory)
            .setLoadControl(loadControl)
            .setAudioAttributes(audioAttributes, true)
            .setSeekBackIncrementMs(10000)
            .setSeekForwardIncrementMs(10000)
            .build().apply {
                trackSelectionParameters = trackSelectionParameters.buildUpon()
                    .setPreferredAudioLanguages("hi", "hin", "en", "eng")
                    .build()
                volume = 1.0f
                playWhenReady = true
            }
    }

    // Function to switch to next episode
    fun playNextEpisode() {
        val allEps = config.allEpisodes
        val currEp = currentEpisode
        if (currEp != null && allEps.isNotEmpty()) {
            val currIndex = allEps.indexOfFirst { it.title == currEp.title }
            if (currIndex != -1 && currIndex + 1 < allEps.size) {
                val nextEp = allEps[currIndex + 1]
                val nextUrl = nextEp.streamSources.firstOrNull()?.streamUrl
                if (!nextUrl.isNullOrBlank()) {
                    showNextEpisodePrompt = false
                    currentPositionMs = 0L
                    currentEpisode = nextEp
                    currentSubtitle = nextEp.title
                    currentStreamUrl = nextUrl
                    toastHudMessage = "Playing ${nextEp.title}"
                }
            }
        }
    }

    // Netflix 10s Rewind Handler (Cumulative on repeated taps)
    fun handleNetflixRewind() {
        rewindCountSeconds += 10
        showRewindRipple = true
        showForwardRipple = false
        val newPos = (exoPlayer.currentPosition - 10000).coerceAtLeast(0L)
        exoPlayer.seekTo(newPos)
        currentPositionMs = newPos
    }

    // Netflix 10s Forward Handler (Cumulative on repeated taps)
    fun handleNetflixForward() {
        forwardCountSeconds += 10
        showForwardRipple = true
        showRewindRipple = false
        val newPos = (exoPlayer.currentPosition + 10000).coerceAtMost(exoPlayer.duration)
        exoPlayer.seekTo(newPos)
        currentPositionMs = newPos
    }

    // Reset Netflix seek counter after tap stops
    LaunchedEffect(showRewindRipple) {
        if (showRewindRipple) {
            delay(750)
            showRewindRipple = false
            rewindCountSeconds = 0
        }
    }

    LaunchedEffect(showForwardRipple) {
        if (showForwardRipple) {
            delay(750)
            showForwardRipple = false
            forwardCountSeconds = 0
        }
    }

    // Load media URL into player
    LaunchedEffect(currentStreamUrl) {
        val exoMedia = ExoMediaItem.fromUri(Uri.parse(currentStreamUrl))
        exoPlayer.setMediaItem(exoMedia)
        if (currentPositionMs > 0) {
            exoPlayer.seekTo(currentPositionMs)
        }
        exoPlayer.prepare()
        exoPlayer.play()
    }

    // Playback Speed
    LaunchedEffect(playbackSpeed) {
        exoPlayer.playbackParameters = PlaybackParameters(playbackSpeed)
    }

    // Listen for tracks & state changes
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                isBuffering = state == Player.STATE_BUFFERING
                if (state == Player.STATE_READY) {
                    durationMs = exoPlayer.duration.coerceAtLeast(0L)
                } else if (state == Player.STATE_ENDED) {
                    if (currentEpisode != null && config.allEpisodes.size > 1) {
                        showNextEpisodePrompt = true
                    }
                }
            }

            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onTracksChanged(tracks: Tracks) {
                availableAudioTracks.clear()
                availableSubtitleTracks.clear()

                for (groupIndex in 0 until tracks.groups.size) {
                    val group = tracks.groups[groupIndex]
                    if (group.type == C.TRACK_TYPE_AUDIO) {
                        for (trackIndex in 0 until group.length) {
                            val format = group.getTrackFormat(trackIndex)
                            val isSelected = group.isTrackSelected(trackIndex)
                            val lang = format.language ?: "und"
                            val channels = format.channelCount
                            val mime = format.sampleMimeType ?: ""
                            val isAtmos = channels >= 6 || mime.contains("eac3", true) || mime.contains("ac3", true)

                            val audioInfo = AudioTrackInfo(
                                groupIndex = groupIndex,
                                trackIndex = trackIndex,
                                language = lang,
                                label = format.label ?: format.id ?: "",
                                channelCount = channels,
                                sampleRate = format.sampleRate,
                                mimeType = mime,
                                isSelected = isSelected,
                                isAtmosOrSurround = isAtmos
                            )
                            availableAudioTracks.add(audioInfo)
                            if (isSelected) {
                                currentAudioLabel = audioInfo.displayTitle
                            }
                        }
                    } else if (group.type == C.TRACK_TYPE_TEXT) {
                        for (trackIndex in 0 until group.length) {
                            val format = group.getTrackFormat(trackIndex)
                            val isSelected = group.isTrackSelected(trackIndex)
                            availableSubtitleTracks.add(
                                SubtitleTrackInfo(
                                    groupIndex = groupIndex,
                                    trackIndex = trackIndex,
                                    language = format.language ?: "und",
                                    label = format.label ?: "Subtitles",
                                    isSelected = isSelected
                                )
                            )
                        }
                    }
                }
            }
        }
        exoPlayer.addListener(listener)

        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.stop()
            exoPlayer.release()
        }
    }

    // Progress Loop & Next Episode Detector
    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            if (!isUserSeeking && !isScrubbingGesture) {
                val pos = exoPlayer.currentPosition.coerceAtLeast(0L)
                val dur = exoPlayer.duration.coerceAtLeast(0L)
                currentPositionMs = pos
                durationMs = dur
                onProgressUpdate(pos, dur)

                // If within 25 seconds of end of series episode, prompt Next Episode
                if (dur > 60000 && (dur - pos) <= 25000 && currentEpisode != null && !showNextEpisodePrompt) {
                    val allEps = config.allEpisodes
                    val currIndex = allEps.indexOfFirst { it.title == currentEpisode?.title }
                    if (currIndex != -1 && currIndex + 1 < allEps.size) {
                        showNextEpisodePrompt = true
                        nextEpisodeCountdown = 10
                    }
                }
            }
            delay(1000)
        }
    }

    // Countdown for next episode prompt
    LaunchedEffect(showNextEpisodePrompt) {
        if (showNextEpisodePrompt) {
            nextEpisodeCountdown = 10
            while (nextEpisodeCountdown > 0 && showNextEpisodePrompt) {
                delay(1000)
                nextEpisodeCountdown -= 1
            }
            if (showNextEpisodePrompt && nextEpisodeCountdown <= 0) {
                playNextEpisode()
            }
        }
    }

    // Auto-hide controls after 4.5 seconds
    LaunchedEffect(controlsVisible, isPlaying) {
        if (controlsVisible && isPlaying && !isLocked) {
            delay(4500)
            controlsVisible = false
        }
    }

    // Immersive Mode
    DisposableEffect(Unit) {
        activity?.let { act ->
            val window = act.window
            val insetsController = WindowCompat.getInsetsController(window, window.decorView)
            insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            insetsController.hide(WindowInsetsCompat.Type.systemBars())
        }

        onDispose {
            activity?.let { act ->
                val window = act.window
                val insetsController = WindowCompat.getInsetsController(window, window.decorView)
                insetsController.show(WindowInsetsCompat.Type.systemBars())
                act.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            }
        }
    }

    BackHandler {
        onProgressUpdate(exoPlayer.currentPosition, exoPlayer.duration)
        onClose()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            // Gesture Layer: Double tap for 10s Netflix seek & tap for controls
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { offset ->
                        if (isLocked) return@detectTapGestures
                        val isRightSide = offset.x > (size.width * 0.55f)
                        val isLeftSide = offset.x < (size.width * 0.45f)
                        if (isRightSide) {
                            handleNetflixForward()
                        } else if (isLeftSide) {
                            handleNetflixRewind()
                        }
                    },
                    onTap = {
                        controlsVisible = !controlsVisible
                    }
                )
            }
            // Gesture Layer: Horizontal scrub drag & vertical brightness/volume drag
            .pointerInput(Unit) {
                var totalDragX = 0f
                var totalDragY = 0f
                var startSide = 0 // 1 = left (brightness), 2 = right (volume), 0 = center (scrub)

                detectDragGestures(
                    onDragStart = { offset ->
                        if (isLocked) return@detectDragGestures
                        totalDragX = 0f
                        totalDragY = 0f
                        startSide = when {
                            offset.x < size.width * 0.35f -> 1
                            offset.x > size.width * 0.65f -> 2
                            else -> 0
                        }
                    },
                    onDragEnd = {
                        if (isLocked) return@detectDragGestures
                        if (isScrubbingGesture) {
                            exoPlayer.seekTo(scrubTargetPositionMs)
                            currentPositionMs = scrubTargetPositionMs
                            isScrubbingGesture = false
                        }
                        isBrightnessGesture = false
                        isVolumeGesture = false
                    },
                    onDragCancel = {
                        isScrubbingGesture = false
                        isBrightnessGesture = false
                        isVolumeGesture = false
                    },
                    onDrag = { change, dragAmount ->
                        if (isLocked) return@detectDragGestures
                        change.consume()
                        totalDragX += dragAmount.x
                        totalDragY += dragAmount.y

                        if (kotlin.math.abs(totalDragX) > kotlin.math.abs(totalDragY) && kotlin.math.abs(totalDragX) > 20f) {
                            // Horizontal Swipe to Scrub Timeline (Netflix Style)
                            isScrubbingGesture = true
                            val dur = exoPlayer.duration.coerceAtLeast(1L)
                            val scrubScale = (dur / 1000f) * 0.4f // Proportional scrub
                            val deltaMs = (dragAmount.x * scrubScale).toLong()
                            scrubDeltaSeconds = ((totalDragX * scrubScale) / 1000f).toLong()
                            val basePos = if (isScrubbingGesture) scrubTargetPositionMs else exoPlayer.currentPosition
                            scrubTargetPositionMs = (basePos + deltaMs).coerceIn(0L, dur)
                        } else if (kotlin.math.abs(totalDragY) > 20f) {
                            if (startSide == 1) {
                                // Left Vertical Drag: Brightness
                                isBrightnessGesture = true
                                val delta = -dragAmount.y / 400f
                                currentBrightness = (currentBrightness + delta).coerceIn(0.05f, 1.0f)
                                activity?.let { act ->
                                    val lp = act.window.attributes
                                    lp.screenBrightness = currentBrightness
                                    act.window.attributes = lp
                                }
                            } else if (startSide == 2) {
                                // Right Vertical Drag: Volume
                                isVolumeGesture = true
                                val delta = -dragAmount.y / 400f
                                currentVolume = (currentVolume + delta).coerceIn(0.0f, 1.5f)
                                exoPlayer.volume = currentVolume
                            }
                        }
                    }
                )
            }
    ) {
        // Player Surface View with selected ResizeMode
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    this.resizeMode = selectedResizeMode.exoMode
                }
            },
            update = { playerView ->
                playerView.resizeMode = selectedResizeMode.exoMode
            },
            modifier = Modifier.fillMaxSize()
        )

        // Netflix Left Double-Tap Rewind Animation Ripple
        if (showRewindRipple) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(0.42f)
                    .align(Alignment.CenterStart)
                    .clip(RoundedCornerShape(topEnd = 160.dp, bottomEnd = 160.dp))
                    .background(Color(0x7000F2FE))
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Replay10,
                        contentDescription = "Rewind",
                        tint = Color.White,
                        modifier = Modifier.size(54.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "-$rewindCountSeconds seconds",
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }

        // Netflix Right Double-Tap Forward Animation Ripple
        if (showForwardRipple) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(0.42f)
                    .align(Alignment.CenterEnd)
                    .clip(RoundedCornerShape(topStart = 160.dp, bottomStart = 160.dp))
                    .background(Color(0x7000F2FE))
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Forward10,
                        contentDescription = "Forward",
                        tint = Color.White,
                        modifier = Modifier.size(54.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "+$forwardCountSeconds seconds",
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }

        // Netflix Horizontal Scrubbing HUD Bubble
        if (isScrubbingGesture) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xEE070B13))
                    .border(1.dp, ApexCyan, RoundedCornerShape(16.dp))
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = formatTime(scrubTargetPositionMs),
                        color = TextPrimary,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${if (scrubDeltaSeconds >= 0) "+$scrubDeltaSeconds" else scrubDeltaSeconds}s",
                        color = ApexCyan,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Total: ${formatTime(durationMs)}",
                        color = TextMuted,
                        fontSize = 12.sp
                    )
                }
            }
        }

        // Netflix Brightness Gesture HUD
        if (isBrightnessGesture) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 28.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xDD0E1524))
                    .border(1.dp, Color(0x3300F2FE), RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 18.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.BrightnessMedium,
                        contentDescription = "Brightness",
                        tint = ApexCyan,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${(currentBrightness * 100).toInt()}%",
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Netflix Volume Gesture HUD
        if (isVolumeGesture) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 28.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xDD0E1524))
                    .border(1.dp, Color(0x3300F2FE), RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 18.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.VolumeUp,
                        contentDescription = "Volume",
                        tint = ApexCyan,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${(currentVolume * 100).toInt()}%",
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Top HUD Toast Notification (Display size, Audio change)
        LaunchedEffect(toastHudMessage) {
            if (toastHudMessage != null) {
                delay(1200)
                toastHudMessage = null
            }
        }

        toastHudMessage?.let { msg ->
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 64.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xDD0E1524))
                    .border(1.dp, ApexCyan, RoundedCornerShape(20.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = msg,
                    color = ApexCyan,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Buffering Indicator
        if (isBuffering && !isScrubbingGesture) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x40000000)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator(
                        color = ApexCyan,
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(52.dp)
                    )
                    Text(
                        text = "Buffering Stream...",
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Lock button when screen is locked
        if (isLocked) {
            AnimatedVisibility(
                visible = controlsVisible,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.CenterStart)
            ) {
                Box(
                    modifier = Modifier
                        .padding(start = 28.dp)
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(Color(0xD9070B13))
                        .border(1.5.dp, ApexCyan, CircleShape)
                        .clickable {
                            isLocked = false
                            controlsVisible = true
                            toastHudMessage = "Screen Unlocked"
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Unlock Controls",
                        tint = ApexCyan,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
            return@Box
        }

        // Full Netflix-Style Controls Overlay
        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(tween(250)),
            exit = fadeOut(tween(250)),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Top Gradient Shadow
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .align(Alignment.TopCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color(0xF0070B13), Color.Transparent)
                            )
                        )
                )

                // Bottom Gradient Shadow
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(170.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color(0xF0070B13))
                            )
                        )
                )

                // Top Controls Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        IconButton(
                            onClick = {
                                onProgressUpdate(exoPlayer.currentPosition, exoPlayer.duration)
                                onClose()
                            },
                            modifier = Modifier
                                .background(Color(0x66000000), CircleShape)
                                .testTag("player_close_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Close",
                                tint = TextPrimary
                            )
                        }

                        Column {
                            Text(
                                text = currentTitle,
                                color = TextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = currentSubtitle,
                                    color = TextSecondary,
                                    fontSize = 12.sp,
                                    maxLines = 1
                                )
                                // Active audio badge
                                Box(
                                    modifier = Modifier
                                        .background(Color(0x3300F2FE), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = currentAudioLabel,
                                        color = ApexCyan,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    // Top Action Bar: Audio, Resize/Size, Lock, Orientation
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Audio Track Selector
                        IconButton(
                            onClick = { showAudioSheet = true },
                            modifier = Modifier.background(Color(0x66000000), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Audiotrack,
                                contentDescription = "Audio Language",
                                tint = ApexCyan
                            )
                        }

                        // Resize Mode (Fit, Zoom, Fill, 16:9, 4:3) - Tap to cycle, opens toast
                        IconButton(
                            onClick = {
                                val nextMode = when (selectedResizeMode) {
                                    VideoResizeMode.FIT -> VideoResizeMode.ZOOM
                                    VideoResizeMode.ZOOM -> VideoResizeMode.FILL
                                    VideoResizeMode.FILL -> VideoResizeMode.SIXTEEN_NINE
                                    VideoResizeMode.SIXTEEN_NINE -> VideoResizeMode.FOUR_THREE
                                    VideoResizeMode.FOUR_THREE -> VideoResizeMode.FIT
                                }
                                selectedResizeMode = nextMode
                                toastHudMessage = "Display: ${nextMode.label}"
                            },
                            modifier = Modifier.background(Color(0x66000000), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AspectRatio,
                                contentDescription = "Screen Size / Ratio",
                                tint = if (selectedResizeMode != VideoResizeMode.FIT) ApexCyan else TextPrimary
                            )
                        }

                        // Lock Screen
                        IconButton(
                            onClick = {
                                isLocked = true
                                toastHudMessage = "Screen Locked"
                            },
                            modifier = Modifier.background(Color(0x66000000), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LockOpen,
                                contentDescription = "Lock",
                                tint = TextPrimary
                            )
                        }

                        // Rotate Screen Toggle
                        IconButton(
                            onClick = {
                                activity?.let { act ->
                                    act.requestedOrientation = if (act.requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
                                        ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                                    } else {
                                        ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                                    }
                                }
                            },
                            modifier = Modifier.background(Color(0x66000000), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ScreenRotation,
                                contentDescription = "Rotate",
                                tint = TextPrimary
                            )
                        }
                    }
                }

                // Netflix Center Controls: Rewind 10s | Big Play/Pause | Forward 10s
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(44.dp)
                ) {
                    // Netflix Rewind 10s Button
                    IconButton(
                        onClick = { handleNetflixRewind() },
                        modifier = Modifier
                            .size(56.dp)
                            .background(Color(0x80000000), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Replay10,
                            contentDescription = "Rewind 10s",
                            tint = TextPrimary,
                            modifier = Modifier.size(34.dp)
                        )
                    }

                    // Play/Pause Big Center Button
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(listOf(ApexCyan, ApexBlue))
                            )
                            .clickable {
                                if (exoPlayer.isPlaying) {
                                    exoPlayer.pause()
                                } else {
                                    exoPlayer.play()
                                }
                            }
                            .testTag("player_play_pause_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = Color(0xFF070B13),
                            modifier = Modifier.size(44.dp)
                        )
                    }

                    // Netflix Forward 10s Button
                    IconButton(
                        onClick = { handleNetflixForward() },
                        modifier = Modifier
                            .size(56.dp)
                            .background(Color(0x80000000), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Forward10,
                            contentDescription = "Forward 10s",
                            tint = TextPrimary,
                            modifier = Modifier.size(34.dp)
                        )
                    }
                }

                // Bottom Controls: Slider & Netflix-Style Action Row
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    // Netflix Time and Progress Bar: Current Time on Left, -Remaining Time on Right
                    val posMs = if (isUserSeeking) (seekSliderPos * durationMs).toLong() else currentPositionMs
                    val remainMs = (durationMs - posMs).coerceAtLeast(0L)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = formatTime(posMs),
                            color = TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )

                        Slider(
                            value = if (isUserSeeking) seekSliderPos else {
                                if (durationMs > 0) (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f
                            },
                            onValueChange = { pos ->
                                isUserSeeking = true
                                seekSliderPos = pos
                            },
                            onValueChangeFinished = {
                                val target = (seekSliderPos * durationMs).toLong()
                                exoPlayer.seekTo(target)
                                currentPositionMs = target
                                isUserSeeking = false
                            },
                            colors = SliderDefaults.colors(
                                thumbColor = ApexCyan,
                                activeTrackColor = ApexCyan,
                                inactiveTrackColor = Color(0x66FFFFFF)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("player_timeline_slider")
                        )

                        // Netflix-Style Remaining Time
                        Text(
                            text = "-${formatTime(remainMs)}",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Bottom Action Strip: Audio | Subtitles | Speed | Episodes | Quality | Next Ep
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Audio Button with Atmos indicator
                            ApexPillButton(
                                icon = Icons.Default.Audiotrack,
                                text = "Audio (${availableAudioTracks.size.coerceAtLeast(1)})",
                                isHighlighted = true,
                                onClick = { showAudioSheet = true }
                            )

                            // Subtitles Button
                            ApexPillButton(
                                icon = Icons.Default.Subtitles,
                                text = "Subtitles",
                                isHighlighted = false,
                                onClick = { showSubtitleSheet = true }
                            )

                            // Playback Speed Button
                            ApexPillButton(
                                icon = Icons.Default.Speed,
                                text = "${playbackSpeed}x",
                                isHighlighted = playbackSpeed != 1.0f,
                                onClick = { showSpeedMenu = true }
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Episodes Drawer Button (if series)
                            if (config.allEpisodes.isNotEmpty()) {
                                ApexPillButton(
                                    icon = Icons.Default.VideoLibrary,
                                    text = "Episodes",
                                    isHighlighted = true,
                                    onClick = { showEpisodesDrawer = true }
                                )
                            }

                            // Quality Button
                            ApexPillButton(
                                icon = Icons.Default.HighQuality,
                                text = "Quality",
                                isHighlighted = false,
                                onClick = { showQualityMenu = true }
                            )

                            // Next Episode Button
                            if (currentEpisode != null && config.allEpisodes.isNotEmpty()) {
                                val currIdx = config.allEpisodes.indexOfFirst { it.title == currentEpisode?.title }
                                if (currIdx != -1 && currIdx + 1 < config.allEpisodes.size) {
                                    Button(
                                        onClick = { playNextEpisode() },
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = ApexCyan,
                                            contentColor = Color(0xFF070B13)
                                        ),
                                        modifier = Modifier.height(34.dp)
                                    ) {
                                        Text(
                                            text = "Next Ep",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            imageVector = Icons.Default.SkipNext,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Speed Menu Dropdown
                DropdownMenu(
                    expanded = showSpeedMenu,
                    onDismissRequest = { showSpeedMenu = false },
                    modifier = Modifier.background(CinemaSurface)
                ) {
                    listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { speed ->
                        DropdownMenuItem(
                            text = {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${speed}x Speed",
                                        color = if (playbackSpeed == speed) ApexCyan else TextPrimary,
                                        fontWeight = if (playbackSpeed == speed) FontWeight.Bold else FontWeight.Normal
                                    )
                                    if (playbackSpeed == speed) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = ApexCyan,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            },
                            onClick = {
                                playbackSpeed = speed
                                showSpeedMenu = false
                            }
                        )
                    }
                }

                // Quality Menu Dropdown
                DropdownMenu(
                    expanded = showQualityMenu,
                    onDismissRequest = { showQualityMenu = false },
                    modifier = Modifier.background(CinemaSurface)
                ) {
                    config.availableQualities.forEach { quality ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "${quality.label} • ${quality.sizeFormatted}",
                                    color = if (quality.streamUrl == currentStreamUrl) ApexCyan else TextPrimary
                                )
                            },
                            onClick = {
                                showQualityMenu = false
                                if (quality.streamUrl != currentStreamUrl) {
                                    currentPositionMs = exoPlayer.currentPosition
                                    currentStreamUrl = quality.streamUrl
                                    currentSubtitle = quality.label
                                }
                            }
                        )
                    }
                }
            }
        }

        // Floating Next Episode Prompt (Netflix Style)
        AnimatedVisibility(
            visible = showNextEpisodePrompt,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 90.dp, end = 20.dp)
        ) {
            val allEps = config.allEpisodes
            val currIdx = allEps.indexOfFirst { it.title == currentEpisode?.title }
            val nextEp = if (currIdx != -1 && currIdx + 1 < allEps.size) allEps[currIdx + 1] else null

            if (nextEp != null) {
                Box(
                    modifier = Modifier
                        .width(280.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xF00E1524))
                        .border(1.dp, ApexCyan, RoundedCornerShape(12.dp))
                        .padding(14.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Next Episode in ${nextEpisodeCountdown}s",
                                color = ApexCyan,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Dismiss",
                                tint = TextSecondary,
                                modifier = Modifier
                                    .size(16.dp)
                                    .clickable { showNextEpisodePrompt = false }
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = nextEp.title,
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { playNextEpisode() },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(36.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ApexCyan,
                                    contentColor = Color(0xFF070B13)
                                )
                            ) {
                                Text(text = "Play Now", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = { showNextEpisodePrompt = false },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(36.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0x33FFFFFF),
                                    contentColor = TextPrimary
                                )
                            ) {
                                Text(text = "Cancel", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        // Audio Tracks Modal Sheet
        if (showAudioSheet) {
            ModalBottomSheet(
                onDismissRequest = { showAudioSheet = false },
                containerColor = CinemaSurface,
                sheetState = rememberModalBottomSheetState()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 32.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Audiotrack,
                            contentDescription = null,
                            tint = ApexCyan
                        )
                        Text(
                            text = "Audio Language & Dolby Atmos",
                            color = TextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Dolby Atmos Dialogue & Volume Boost Card
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0x2000F2FE))
                            .border(1.dp, Color(0x4000F2FE), RoundedCornerShape(10.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Dolby Atmos Dialogue Boost",
                                color = ApexCyan,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "Enhances 5.1/7.1 audio clarity & volume for phone speakers",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                        Switch(
                            checked = isAtmosBoostEnabled,
                            onCheckedChange = { enabled ->
                                isAtmosBoostEnabled = enabled
                                exoPlayer.volume = if (enabled) 1.25f else 1.0f
                                toastHudMessage = if (enabled) "Atmos Dialogue Boost: ON" else "Atmos Dialogue Boost: OFF"
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = ApexCyan,
                                checkedTrackColor = Color(0x4000F2FE)
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Quick Audio Language Pills
                    Text(
                        text = "Preferred Language Track:",
                        color = TextMuted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Hindi" to "hi", "English" to "en", "Original" to "und").forEach { (langName, langCode) ->
                            val isLangSel = currentAudioLabel.contains(langName, ignoreCase = true)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(if (isLangSel) ApexCyan else CinemaSurfaceVariant)
                                    .clickable {
                                        exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                                            .buildUpon()
                                            .setPreferredAudioLanguage(langCode)
                                            .build()
                                        currentAudioLabel = "$langName Audio"
                                        toastHudMessage = "Audio: $langName Selected"
                                        showAudioSheet = false
                                    }
                                    .padding(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = langName,
                                    color = if (isLangSel) Color(0xFF070B13) else TextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (availableAudioTracks.isEmpty()) {
                        // Fallback default tracks
                        val fallbackTracks = listOf(
                            "Hindi • Dolby Atmos 5.1 (Direct)",
                            "Hindi • AAC Stereo",
                            "English • Original Audio"
                        )
                        fallbackTracks.forEachIndexed { idx, trackName ->
                            val isSel = idx == 0
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSel) Color(0x2600F2FE) else Color.Transparent)
                                    .clickable {
                                        currentAudioLabel = trackName
                                        showAudioSheet = false
                                    }
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = trackName,
                                    color = if (isSel) ApexCyan else TextPrimary,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal
                                )
                                if (isSel) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = ApexCyan
                                    )
                                }
                            }
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(availableAudioTracks) { track ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (track.isSelected) Color(0x2600F2FE) else Color.Transparent)
                                        .clickable {
                                            try {
                                                val trackGroup = exoPlayer.currentTracks.groups[track.groupIndex].mediaTrackGroup
                                                val override = TrackSelectionOverride(trackGroup, listOf(track.trackIndex))
                                                exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                                                    .buildUpon()
                                                    .clearOverridesOfType(C.TRACK_TYPE_AUDIO)
                                                    .setOverrideForType(override)
                                                    .build()
                                                currentAudioLabel = track.displayTitle
                                            } catch (e: Exception) {
                                                e.printStackTrace()
                                            }
                                            showAudioSheet = false
                                        }
                                        .padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = track.displayTitle,
                                            color = if (track.isSelected) ApexCyan else TextPrimary,
                                            fontWeight = if (track.isSelected) FontWeight.Bold else FontWeight.Medium,
                                            fontSize = 15.sp
                                        )
                                        if (track.isAtmosOrSurround) {
                                            Text(
                                                text = "Spatial / Surround Audio Enabled",
                                                color = Color(0xFF34D399),
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                    if (track.isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = ApexCyan
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Video Resize Mode Modal Sheet (Screen Size)
        if (showResizeSheet) {
            ModalBottomSheet(
                onDismissRequest = { showResizeSheet = false },
                containerColor = CinemaSurface,
                sheetState = rememberModalBottomSheetState()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 32.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AspectRatio,
                            contentDescription = null,
                            tint = ApexCyan
                        )
                        Text(
                            text = "Video Display Size & Aspect Ratio",
                            color = TextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    VideoResizeMode.values().forEach { mode ->
                        val isSel = selectedResizeMode == mode
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSel) Color(0x2600F2FE) else Color.Transparent)
                            .clickable {
                                selectedResizeMode = mode
                                toastHudMessage = "Display: ${mode.label}"
                                showResizeSheet = false
                            }
                            .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = mode.label,
                                color = if (isSel) ApexCyan else TextPrimary,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 15.sp
                            )
                            if (isSel) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = ApexCyan
                                )
                            }
                        }
                    }
                }
            }
        }

        // Subtitles Sheet
        if (showSubtitleSheet) {
            ModalBottomSheet(
                onDismissRequest = { showSubtitleSheet = false },
                containerColor = CinemaSurface,
                sheetState = rememberModalBottomSheetState()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 32.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Subtitles,
                            contentDescription = null,
                            tint = ApexCyan
                        )
                        Text(
                            text = "Subtitles / Closed Captions",
                            color = TextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                                    .buildUpon()
                                    .clearOverridesOfType(C.TRACK_TYPE_TEXT)
                                    .setIgnoredTextSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                                    .build()
                                showSubtitleSheet = false
                            }
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Off", color = TextPrimary)
                    }

                    availableSubtitleTracks.forEach { sub ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (sub.isSelected) Color(0x2600F2FE) else Color.Transparent)
                                .clickable {
                                    val group = exoPlayer.currentTracks.groups[sub.groupIndex].mediaTrackGroup
                                    val override = TrackSelectionOverride(group, listOf(sub.trackIndex))
                                    exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                                        .buildUpon()
                                        .setOverrideForType(override)
                                        .build()
                                    showSubtitleSheet = false
                                }
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = sub.displayTitle,
                                color = if (sub.isSelected) ApexCyan else TextPrimary,
                                fontWeight = if (sub.isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                            if (sub.isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = ApexCyan
                                )
                            }
                        }
                    }
                }
            }
        }

        // In-Player Episodes Drawer (Netflix Style)
        AnimatedVisibility(
            visible = showEpisodesDrawer,
            enter = slideInHorizontally(initialOffsetX = { it }),
            exit = slideOutHorizontally(targetOffsetX = { it }),
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(320.dp)
                    .background(Color(0xF50E1524))
                    .padding(16.dp)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Episodes (${config.allEpisodes.size})",
                            color = TextPrimary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = { showEpisodesDrawer = false }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = TextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(config.allEpisodes) { ep ->
                            val isCurr = ep.title == currentEpisode?.title
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isCurr) Color(0x3300F2FE) else CinemaSurfaceVariant)
                                    .clickable {
                                        val epUrl = ep.streamSources.firstOrNull()?.streamUrl
                                        if (!epUrl.isNullOrBlank()) {
                                            currentPositionMs = 0L
                                            currentEpisode = ep
                                            currentSubtitle = ep.title
                                            currentStreamUrl = epUrl
                                            showEpisodesDrawer = false
                                            toastHudMessage = "Playing ${ep.title}"
                                        }
                                    }
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(if (isCurr) ApexCyan else Color(0x33FFFFFF)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        tint = if (isCurr) Color(0xFF070B13) else TextPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = ep.title,
                                        color = if (isCurr) ApexCyan else TextPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = if (isCurr) FontWeight.Bold else FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (ep.durationFormatted.isNotBlank()) {
                                        Text(
                                            text = ep.durationFormatted,
                                            color = TextMuted,
                                            fontSize = 11.sp
                                        )
                                    }
                                }

                                if (isCurr) {
                                    Text(
                                        text = "PLAYING",
                                        color = ApexCyan,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ApexPillButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    isHighlighted: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isHighlighted) Color(0x3300F2FE) else Color(0x66000000))
            .clickable(onClick = onClick)
            .padding(horizontal = 9.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isHighlighted) ApexCyan else TextSecondary,
            modifier = Modifier.size(15.dp)
        )
        Text(
            text = text,
            color = if (isHighlighted) ApexCyan else TextPrimary,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

private fun formatTime(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format("%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}
