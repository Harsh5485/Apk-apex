package com.example.data

import android.content.Context
import com.example.model.ApexFileItem
import com.example.model.EpisodeItem
import com.example.model.MediaItem
import com.example.model.MediaType
import com.example.model.StreamSource
import com.example.network.ApexApiClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class MediaRepository(private val context: Context? = null) {

    companion object {
        // Fast in-memory cache
        private var memoryCache: List<MediaItem>? = null
        private const val PREFS_CACHE = "apex_catalog_cache"
        private const val KEY_CACHE_JSON = "key_cached_catalog"
    }

    suspend fun getFullCatalog(forceRefresh: Boolean = false): List<MediaItem> = withContext(Dispatchers.IO) {
        if (!forceRefresh) {
            // 1. Immediate memory cache (0ms)
            memoryCache?.let {
                if (it.isNotEmpty()) return@withContext it
            }

            // 2. Immediate disk cache (10ms)
            if (context != null) {
                val diskCached = readDiskCache(context)
                if (diskCached.isNotEmpty()) {
                    memoryCache = diskCached
                    // Background silent refresh
                    CoroutineScope(Dispatchers.IO).launch { fetchNetworkCatalog() }
                    return@withContext diskCached
                }
            }

            // 3. Immediate pre-seeded baseline on cold start
            val baseline = getPreseededCatalog()
            memoryCache = baseline
            if (context != null) {
                saveDiskCache(context, baseline)
            }
            CoroutineScope(Dispatchers.IO).launch { fetchNetworkCatalog() }
            return@withContext baseline
        }

        fetchNetworkCatalog()
    }

    private suspend fun fetchNetworkCatalog(): List<MediaItem> = withContext(Dispatchers.IO) {
        val drive0Deferred = async { ApexApiClient.listDrive(0) }
        val drive1Deferred = async { ApexApiClient.listDrive(1) }

        val drive0Items = drive0Deferred.await()
        val drive1Items = drive1Deferred.await()

        if (drive0Items.isEmpty() && drive1Items.isEmpty()) {
            return@withContext memoryCache ?: getPreseededCatalog()
        }

        val combinedRaw = mutableListOf<Pair<Int, ApexFileItem>>()
        drive0Items.forEach { item ->
            if (!item.name.contains("GoFile Storage", ignoreCase = true)) {
                combinedRaw.add(0 to item)
            }
        }
        drive1Items.forEach { item ->
            combinedRaw.add(1 to item)
        }

        val grouped = combinedRaw.groupBy { pair ->
            normalizeTitle(pair.second.name)
        }

        val mediaList = mutableListOf<MediaItem>()

        for ((_, items) in grouped) {
            val primary = items.first().second
            val driveIdx = items.first().first
            val rawName = primary.name
            val meta = MediaCatalogMetadata.enrich(rawName)

            val isSeries = meta.isSeries ||
                    rawName.contains("season", ignoreCase = true) ||
                    rawName.contains("s0", ignoreCase = true) ||
                    rawName.contains("s1", ignoreCase = true) ||
                    items.any { it.second.name.contains("season", ignoreCase = true) }

            val mediaType = if (isSeries) MediaType.SERIES else MediaType.MOVIE

            val folderPath = if (primary.mimeType.contains("folder") || primary.mimeType.contains("directory")) {
                primary.name
            } else ""

            val streamUrl = ApexApiClient.resolveStreamUrl(primary)
            val streamSources = if (streamUrl != null) {
                listOf(
                    StreamSource(
                        label = MediaCatalogMetadata.extractQualityTag(primary.name),
                        resolution = if (primary.name.contains("1080p")) "1080p" else if (primary.name.contains("720p")) "720p" else "Standard",
                        streamUrl = streamUrl,
                        sizeFormatted = MediaCatalogMetadata.formatFileSize(primary.size),
                        codec = MediaCatalogMetadata.extractAudioTag(primary.name),
                        rawName = primary.name
                    )
                )
            } else emptyList()

            mediaList.add(
                MediaItem(
                    id = primary.id ?: "${driveIdx}_${primary.name}",
                    driveIndex = driveIdx,
                    title = meta.title,
                    rawName = rawName,
                    type = mediaType,
                    year = meta.year,
                    rating = meta.rating,
                    voteCount = meta.voteCount,
                    qualityTag = MediaCatalogMetadata.extractQualityTag(rawName),
                    audioTag = MediaCatalogMetadata.extractAudioTag(rawName),
                    posterUrl = meta.posterUrl,
                    backdropUrl = meta.backdropUrl,
                    synopsis = meta.synopsis,
                    folderPath = folderPath,
                    isSeries = isSeries,
                    streamSources = streamSources,
                    sourceDriveName = "Apex CDN",
                    genres = meta.genres,
                    cast = meta.cast,
                    runtime = meta.runtime,
                    hasAtmos = meta.hasAtmos
                )
            )
        }

        memoryCache = mediaList
        if (context != null) {
            saveDiskCache(context, mediaList)
        }

        mediaList
    }

    suspend fun loadFolderDetails(media: MediaItem): MediaItem = withContext(Dispatchers.IO) {
        if (media.folderPath.isBlank()) return@withContext media

        val files = ApexApiClient.listDrive(media.driveIndex, media.folderPath)
        if (files.isEmpty()) return@withContext media

        val videoFiles = files.filter {
            it.mimeType.startsWith("video/") ||
                    it.name.endsWith(".mkv", ignoreCase = true) ||
                    it.name.endsWith(".mp4", ignoreCase = true)
        }

        if (videoFiles.isEmpty()) return@withContext media

        val hasEpisodes = videoFiles.any {
            Regex("s\\d+e\\d+|episode|ep\\d+", RegexOption.IGNORE_CASE).containsMatchIn(it.name)
        }

        if (hasEpisodes || media.isSeries) {
            val episodes = videoFiles.mapIndexed { idx, file ->
                val streamUrl = ApexApiClient.resolveStreamUrl(file) ?: ""
                val epMatch = Regex("s(\\d+)e(\\d+)", RegexOption.IGNORE_CASE).find(file.name)
                val seasonNum = epMatch?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 1
                val epNum = epMatch?.groupValues?.getOrNull(2)?.toIntOrNull() ?: (idx + 1)

                val cleanEpTitle = cleanEpisodeTitle(file.name, epNum)

                EpisodeItem(
                    title = cleanEpTitle,
                    seasonNumber = seasonNum,
                    episodeNumber = epNum,
                    streamSources = if (streamUrl.isNotBlank()) {
                        listOf(
                            StreamSource(
                                label = MediaCatalogMetadata.extractQualityTag(file.name),
                                resolution = if (file.name.contains("1080p")) "1080p" else "720p",
                                streamUrl = streamUrl,
                                sizeFormatted = MediaCatalogMetadata.formatFileSize(file.size),
                                codec = MediaCatalogMetadata.extractAudioTag(file.name),
                                rawName = file.name
                            )
                        )
                    } else emptyList(),
                    rawName = file.name,
                    durationFormatted = MediaCatalogMetadata.formatFileSize(file.size),
                    thumbnail = media.backdropUrl
                )
            }.sortedBy { it.episodeNumber }

            val allStreamSources = episodes.flatMap { it.streamSources }
            media.copy(
                isSeries = true,
                episodes = episodes,
                streamSources = allStreamSources
            )
        } else {
            val sources = videoFiles.map { file ->
                val streamUrl = ApexApiClient.resolveStreamUrl(file) ?: ""
                StreamSource(
                    label = MediaCatalogMetadata.extractQualityTag(file.name),
                    resolution = when {
                        file.name.contains("1080p", ignoreCase = true) -> "1080p"
                        file.name.contains("720p", ignoreCase = true) -> "720p"
                        file.name.contains("480p", ignoreCase = true) -> "480p"
                        file.name.contains("288p", ignoreCase = true) -> "288p"
                        file.name.contains("270p", ignoreCase = true) -> "270p"
                        else -> "4K / HD Direct Stream"
                    },
                    streamUrl = streamUrl,
                    sizeFormatted = MediaCatalogMetadata.formatFileSize(file.size),
                    codec = MediaCatalogMetadata.extractAudioTag(file.name),
                    rawName = file.name
                )
            }.filter { it.streamUrl.isNotBlank() }

            media.copy(streamSources = sources)
        }
    }

    suspend fun searchCatalog(query: String, existingCatalog: List<MediaItem>): List<MediaItem> = withContext(Dispatchers.IO) {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return@withContext emptyList()

        val localMatches = existingCatalog.filter {
            it.title.lowercase().contains(q) ||
                    it.rawName.lowercase().contains(q) ||
                    it.synopsis.lowercase().contains(q) ||
                    it.year.contains(q)
        }.toMutableList()

        try {
            val remoteResults = ApexApiClient.search(0, query)
            for (file in remoteResults) {
                val meta = MediaCatalogMetadata.enrich(file.name)
                val exists = localMatches.any { it.title.equals(meta.title, ignoreCase = true) }
                if (!exists) {
                    val streamUrl = ApexApiClient.resolveStreamUrl(file)
                    val isFolder = file.mimeType.contains("folder")
                    localMatches.add(
                        MediaItem(
                            id = file.id ?: "search_${file.name}",
                            driveIndex = 0,
                            title = meta.title,
                            rawName = file.name,
                            type = if (meta.isSeries) MediaType.SERIES else MediaType.MOVIE,
                            year = meta.year,
                            rating = meta.rating,
                            voteCount = meta.voteCount,
                            qualityTag = MediaCatalogMetadata.extractQualityTag(file.name),
                            audioTag = MediaCatalogMetadata.extractAudioTag(file.name),
                            posterUrl = meta.posterUrl,
                            backdropUrl = meta.backdropUrl,
                            synopsis = meta.synopsis,
                            folderPath = if (isFolder) file.name else "",
                            isSeries = meta.isSeries,
                            streamSources = if (streamUrl != null) listOf(
                                StreamSource(
                                    label = MediaCatalogMetadata.extractQualityTag(file.name),
                                    resolution = "HD",
                                    streamUrl = streamUrl,
                                    sizeFormatted = MediaCatalogMetadata.formatFileSize(file.size),
                                    codec = MediaCatalogMetadata.extractAudioTag(file.name),
                                    rawName = file.name
                                )
                            ) else emptyList(),
                            sourceDriveName = "Apex CDN",
                            genres = meta.genres,
                            cast = meta.cast,
                            runtime = meta.runtime,
                            hasAtmos = meta.hasAtmos
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        localMatches
    }

    private fun normalizeTitle(raw: String): String {
        return raw.lowercase()
            .replace(Regex("\\b(19\\d\\d|20\\d\\d)\\b.*"), "")
            .replace(Regex("[^a-z0-9]"), "")
            .trim()
    }

    private fun cleanEpisodeTitle(filename: String, fallbackNum: Int): String {
        val match = Regex("S\\d+E(\\d+)\\.([^.]+)", RegexOption.IGNORE_CASE).find(filename)
        return if (match != null) {
            val epNum = match.groupValues[1].toIntOrNull() ?: fallbackNum
            val name = match.groupValues[2].replace(".", " ").trim()
            "Episode $epNum: $name"
        } else {
            "Episode $fallbackNum"
        }
    }

    private fun readDiskCache(ctx: Context): List<MediaItem> {
        return try {
            val prefs = ctx.getSharedPreferences(PREFS_CACHE, Context.MODE_PRIVATE)
            val json = prefs.getString(KEY_CACHE_JSON, null) ?: return emptyList()
            val arr = JSONArray(json)
            val list = mutableListOf<MediaItem>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val rawName = obj.optString("rawName")
                val meta = MediaCatalogMetadata.enrich(rawName)
                list.add(
                    MediaItem(
                        id = obj.optString("id"),
                        driveIndex = obj.optInt("driveIndex", 0),
                        title = meta.title,
                        rawName = rawName,
                        type = if (obj.optBoolean("isSeries")) MediaType.SERIES else MediaType.MOVIE,
                        year = meta.year,
                        rating = meta.rating,
                        voteCount = meta.voteCount,
                        qualityTag = obj.optString("qualityTag", "HD"),
                        audioTag = obj.optString("audioTag", "Dolby Atmos"),
                        posterUrl = meta.posterUrl,
                        backdropUrl = meta.backdropUrl,
                        synopsis = meta.synopsis,
                        folderPath = obj.optString("folderPath"),
                        isSeries = obj.optBoolean("isSeries"),
                        sourceDriveName = "Apex CDN",
                        genres = meta.genres,
                        cast = meta.cast,
                        runtime = meta.runtime,
                        hasAtmos = meta.hasAtmos
                    )
                )
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun saveDiskCache(ctx: Context, items: List<MediaItem>) {
        try {
            val arr = JSONArray()
            items.forEach { m ->
                val obj = JSONObject().apply {
                    put("id", m.id)
                    put("driveIndex", m.driveIndex)
                    put("title", m.title)
                    put("rawName", m.rawName)
                    put("isSeries", m.isSeries)
                    put("qualityTag", m.qualityTag)
                    put("audioTag", m.audioTag)
                    put("folderPath", m.folderPath)
                }
                arr.put(obj)
            }
            ctx.getSharedPreferences(PREFS_CACHE, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_CACHE_JSON, arr.toString())
                .apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Pre-seeded verified catalog so cold-start launch takes literally 0ms!
    private fun getPreseededCatalog(): List<MediaItem> {
        val titles = listOf(
            "Baaghi 4 (2025)" to false,
            "Mahabharat.2013" to true,
            "Neagley Season 01" to true,
            "MARCO (2024)" to false,
            "Phir Hera Pheri (2006)" to false,
            "Deewaar (1975)" to false,
            "Aandhi (1975)" to false,
            "Batwara.1989" to false,
            "Dhurandhar.Raw.&.Undekha.2026" to false,
            "Sardar 2 (2026)" to false,
            "Dhamaal.4.2026" to false,
            "Aukaat.Ke.Bahar.S01" to true,
            "Dupahiya" to false,
            "Kattalan" to false,
            "Khilona (1970)" to false,
            "Khuddar.1994" to false,
            "Reply (1988)" to false,
            "SULTANA.2026" to false,
            "Habeebi (2026)" to false
        )

        return titles.mapIndexed { idx, (rawName, isSeries) ->
            val meta = MediaCatalogMetadata.enrich(rawName)
            val driveIdx = if (rawName.contains("Neagley") || rawName.contains("Phir Hera Pheri") || rawName.contains("Habeebi")) 1 else 0
            MediaItem(
                id = "pre_${idx}_${meta.title}",
                driveIndex = driveIdx,
                title = meta.title,
                rawName = rawName,
                type = if (isSeries) MediaType.SERIES else MediaType.MOVIE,
                year = meta.year,
                rating = meta.rating,
                voteCount = meta.voteCount,
                qualityTag = "1080p Ultra HD",
                audioTag = if (meta.hasAtmos) "Dolby Atmos 5.1" else "Hindi AAC",
                posterUrl = meta.posterUrl,
                backdropUrl = meta.backdropUrl,
                synopsis = meta.synopsis,
                folderPath = rawName,
                isSeries = isSeries,
                streamSources = emptyList(),
                sourceDriveName = "Apex CDN",
                genres = meta.genres,
                cast = meta.cast,
                runtime = meta.runtime,
                hasAtmos = meta.hasAtmos
            )
        }
    }
}
