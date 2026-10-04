package com.example.data

object MediaCatalogMetadata {

    fun enrich(rawTitle: String): TmdbMediaMeta {
        return TmdbHelper.getMeta(rawTitle)
    }

    fun cleanTitle(raw: String): String {
        var clean = raw
            .replace(Regex("\\b(19\\d\\d|20\\d\\d)\\b.*"), "")
            .replace(Regex("\\b(1080p|720p|480p|270p|288p|WEB-DL|AMZN|AAC|H\\.264|H\\.265|HEVC|HrX|APeX|mkv|zip)\\b", RegexOption.IGNORE_CASE), "")
            .replace(".", " ")
            .replace("_", " ")
            .replace(Regex("\\s+"), " ")
            .trim()

        if (clean.isEmpty()) {
            clean = raw.replace(".mkv", "").replace(".zip", "")
        }
        return clean
    }

    fun extractYear(raw: String): String {
        val match = Regex("\\b(19\\d\\d|20\\d\\d)\\b").find(raw)
        return match?.value ?: ""
    }

    fun extractQualityTag(raw: String): String {
        return when {
            raw.contains("1080p", ignoreCase = true) -> "1080p Full HD"
            raw.contains("720p", ignoreCase = true) -> "720p HD"
            raw.contains("480p", ignoreCase = true) -> "480p SD"
            raw.contains("288p", ignoreCase = true) -> "288p Mobile"
            raw.contains("270p", ignoreCase = true) -> "270p Light"
            raw.contains("2160p", ignoreCase = true) || raw.contains("4k", ignoreCase = true) -> "4K Ultra HD"
            else -> "Full HD"
        }
    }

    fun extractAudioTag(raw: String): String {
        return when {
            raw.contains("DDP5.1", ignoreCase = true) || raw.contains("DDP.5.1", ignoreCase = true) -> "Dolby Atmos • DDP 5.1"
            raw.contains("DDP", ignoreCase = true) -> "Dolby Digital Plus"
            raw.contains("AAC2.0", ignoreCase = true) -> "Hindi AAC 2.0"
            raw.contains("AAC", ignoreCase = true) -> "Hindi AAC Stereo"
            else -> "Hindi Audio"
        }
    }

    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return ""
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0
        return when {
            gb >= 1.0 -> String.format("%.2f GB", gb)
            mb >= 1.0 -> String.format("%.1f MB", mb)
            else -> String.format("%.0f KB", kb)
        }
    }
}
