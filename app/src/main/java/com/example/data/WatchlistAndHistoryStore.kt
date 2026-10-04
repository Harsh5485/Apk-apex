package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.model.WatchProgress
import org.json.JSONArray
import org.json.JSONObject

class WatchlistAndHistoryStore(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("apex_stream_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_WATCH_PROGRESS = "key_watch_progress"
        private const val KEY_WATCHLIST_IDS = "key_watchlist_ids"
    }

    fun saveProgress(progress: WatchProgress) {
        val currentList = getWatchProgressList().toMutableList()
        // Remove if existing
        currentList.removeAll { it.mediaId == progress.mediaId && it.episodeTitle == progress.episodeTitle }
        // Add to front
        currentList.add(0, progress)
        // Keep max 20 entries
        val trimmed = currentList.take(20)

        val jsonArray = JSONArray()
        trimmed.forEach { item ->
            val obj = JSONObject().apply {
                put("mediaId", item.mediaId)
                put("title", item.title)
                put("posterUrl", item.posterUrl)
                put("lastPositionMs", item.lastPositionMs)
                put("durationMs", item.durationMs)
                put("episodeTitle", item.episodeTitle ?: "")
                put("streamUrl", item.streamUrl)
                put("lastWatchedTimestamp", item.lastWatchedTimestamp)
            }
            jsonArray.put(obj)
        }

        prefs.edit().putString(KEY_WATCH_PROGRESS, jsonArray.toString()).apply()
    }

    fun getWatchProgressList(): List<WatchProgress> {
        val jsonStr = prefs.getString(KEY_WATCH_PROGRESS, null) ?: return emptyList()
        val results = mutableListOf<WatchProgress>()
        try {
            val arr = JSONArray(jsonStr)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val lastPos = obj.optLong("lastPositionMs", 0L)
                val duration = obj.optLong("durationMs", 0L)
                // Filter out finished items (>95% watched)
                if (duration > 0 && lastPos > duration * 0.95) continue

                val ep = obj.optString("episodeTitle", "")
                results.add(
                    WatchProgress(
                        mediaId = obj.getString("mediaId"),
                        title = obj.getString("title"),
                        posterUrl = obj.optString("posterUrl", ""),
                        lastPositionMs = lastPos,
                        durationMs = duration,
                        episodeTitle = if (ep.isEmpty()) null else ep,
                        streamUrl = obj.getString("streamUrl"),
                        lastWatchedTimestamp = obj.optLong("lastWatchedTimestamp", System.currentTimeMillis())
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return results
    }

    fun getProgressForMedia(mediaId: String, episodeTitle: String? = null): Long {
        return getWatchProgressList().firstOrNull {
            it.mediaId == mediaId && (episodeTitle == null || it.episodeTitle == episodeTitle)
        }?.lastPositionMs ?: 0L
    }

    fun toggleWatchlist(mediaId: String): Boolean {
        val current = getWatchlistIds().toMutableSet()
        val added = if (current.contains(mediaId)) {
            current.remove(mediaId)
            false
        } else {
            current.add(mediaId)
            true
        }
        prefs.edit().putStringSet(KEY_WATCHLIST_IDS, current).apply()
        return added
    }

    fun isInWatchlist(mediaId: String): Boolean {
        return getWatchlistIds().contains(mediaId)
    }

    fun getWatchlistIds(): Set<String> {
        return prefs.getStringSet(KEY_WATCHLIST_IDS, emptySet()) ?: emptySet()
    }

    fun clearHistory() {
        prefs.edit().remove(KEY_WATCH_PROGRESS).apply()
    }
}
