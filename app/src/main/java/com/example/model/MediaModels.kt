package com.example.model

enum class MediaType {
    MOVIE,
    SERIES,
    FOLDER,
    VIDEO
}

data class ApexFileItem(
    val name: String,
    val mimeType: String = "",
    val size: Long = 0L,
    val id: String? = null,
    val driveId: String? = null,
    val link: String? = null,
    val gofileLink: String? = null,
    val isGoFile: Boolean = false,
    val modifiedTime: String? = null,
    val fileExtension: String? = null
)

data class StreamSource(
    val label: String,         // e.g. "1080p Full HD", "720p HD", "288p Fast"
    val resolution: String,    // "1080p", "720p", "288p", "Original"
    val streamUrl: String,     // Full streaming URL
    val sizeFormatted: String, // e.g. "1.4 GB"
    val codec: String,         // "H.265 HEVC", "H.264", "DDP 5.1"
    val rawName: String
)

data class EpisodeItem(
    val title: String,         // e.g. "Episode 1: L. Train"
    val seasonNumber: Int = 1,
    val episodeNumber: Int = 1,
    val streamSources: List<StreamSource> = emptyList(),
    val rawName: String = "",
    val durationFormatted: String = "",
    val thumbnail: String = ""
)

data class MediaItem(
    val id: String,
    val driveIndex: Int,       // 0 = HEX-RIPS, 1 = GoFile Storage
    val title: String,         // Clean display title
    val rawName: String,
    val type: MediaType,
    val year: String = "",
    val rating: String = "8.4",
    val voteCount: String = "15k+",
    val qualityTag: String = "HD",
    val audioTag: String = "Dolby Atmos • Hindi",
    val posterUrl: String = "",
    val backdropUrl: String = "",
    val synopsis: String = "",
    val folderPath: String = "", // e.g. "0:/Baaghi 4 (2025)/"
    val isSeries: Boolean = false,
    val streamSources: List<StreamSource> = emptyList(),
    val episodes: List<EpisodeItem> = emptyList(),
    val sourceDriveName: String = "HEX-RIPS",
    val genres: List<String> = emptyList(),
    val cast: List<String> = emptyList(),
    val runtime: String = "2h 15m",
    val hasAtmos: Boolean = true
)

data class WatchProgress(
    val mediaId: String,
    val title: String,
    val posterUrl: String,
    val lastPositionMs: Long,
    val durationMs: Long,
    val episodeTitle: String? = null,
    val streamUrl: String,
    val lastWatchedTimestamp: Long = System.currentTimeMillis()
) {
    val progressFraction: Float
        get() = if (durationMs > 0) (lastPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f
    
    val formattedRemaining: String
        get() {
            val remainMs = (durationMs - lastPositionMs).coerceAtLeast(0)
            val minutes = (remainMs / 1000) / 60
            return if (minutes > 60) "${minutes / 60}h ${minutes % 60}m left" else "${minutes}m left"
        }
}
