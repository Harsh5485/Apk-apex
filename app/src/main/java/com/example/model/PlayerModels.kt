package com.example.model

data class AudioTrackInfo(
    val groupIndex: Int,
    val trackIndex: Int,
    val language: String,
    val label: String,
    val channelCount: Int,
    val sampleRate: Int = 48000,
    val mimeType: String,
    val isSelected: Boolean,
    val isAtmosOrSurround: Boolean
) {
    val displayTitle: String
        get() {
            val langName = when (language.lowercase()) {
                "hi", "hin" -> "Hindi"
                "en", "eng" -> "English"
                "ta", "tam" -> "Tamil"
                "te", "tel" -> "Telugu"
                "bn", "ben" -> "Bengali"
                "ml", "mal" -> "Malayalam"
                "mr", "mar" -> "Marathi"
                "pa", "pan" -> "Punjabi"
                "ur", "urd" -> "Urdu"
                "und", "" -> if (label.isNotBlank() && !label.contains("/")) label else "Main Audio"
                else -> language.uppercase()
            }

            val channelLabel = when {
                channelCount >= 8 -> "Dolby Atmos 7.1"
                channelCount == 6 -> "Dolby 5.1 Surround"
                channelCount == 2 -> "Stereo 2.0"
                channelCount == 1 -> "Mono"
                else -> ""
            }

            val codecLabel = when {
                mimeType.contains("eac3", ignoreCase = true) -> "DDP / Atmos"
                mimeType.contains("ac3", ignoreCase = true) -> "Dolby Digital"
                mimeType.contains("dts", ignoreCase = true) -> "DTS-HD"
                mimeType.contains("aac", ignoreCase = true) -> "AAC"
                mimeType.contains("opus", ignoreCase = true) -> "Opus"
                else -> ""
            }

            return buildString {
                append(langName)
                if (channelLabel.isNotBlank()) append(" • $channelLabel")
                if (codecLabel.isNotBlank()) append(" ($codecLabel)")
            }
        }
}

data class SubtitleTrackInfo(
    val groupIndex: Int,
    val trackIndex: Int,
    val language: String,
    val label: String,
    val isSelected: Boolean
) {
    val displayTitle: String
        get() = when (language.lowercase()) {
            "hi", "hin" -> "Hindi Subtitles"
            "en", "eng" -> "English Subtitles"
            "und", "" -> if (label.isNotBlank()) label else "Default Subtitles"
            else -> "${language.uppercase()} Subtitles"
        }
}

enum class VideoResizeMode(val label: String, val exoMode: Int) {
    FIT("Fit Screen (Original)", 0),     // RESIZE_MODE_FIT
    ZOOM("Zoom (Fill Display)", 4),     // RESIZE_MODE_ZOOM
    FILL("Stretch to Fill", 3),         // RESIZE_MODE_FILL
    SIXTEEN_NINE("16:9 Widescreen", 1), // RESIZE_MODE_FIXED_WIDTH
    FOUR_THREE("4:3 Classic", 2)        // RESIZE_MODE_FIXED_HEIGHT
}
