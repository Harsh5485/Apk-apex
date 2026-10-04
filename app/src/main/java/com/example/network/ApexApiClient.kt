package com.example.network

import android.net.Uri
import com.example.model.ApexFileItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object ApexApiClient {
    const val BASE_URL = "https://apex-drm.harshkumar36335.workers.dev"
    private const val USER_AGENT = "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"

    val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(25, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(25, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    suspend fun listDrive(driveIndex: Int, subPath: String = ""): List<ApexFileItem> = withContext(Dispatchers.IO) {
        val cleanPath = subPath.trim().trim('/')
        val pathSegment = if (cleanPath.isEmpty()) {
            "$driveIndex:/"
        } else {
            // Encode subfolder path components
            val encodedSub = cleanPath.split("/").joinToString("/") { Uri.encode(it) }
            "$driveIndex:/$encodedSub/"
        }

        val url = "$BASE_URL/$pathSegment"
        val jsonBody = """{"page_token":null,"page_index":0}""".toRequestBody(JSON_MEDIA_TYPE)

        val request = Request.Builder()
            .url(url)
            .post(jsonBody)
            .header("User-Agent", USER_AGENT)
            .header("Accept", "application/json, text/plain, */*")
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyList()
                val bodyStr = response.body?.string() ?: return@withContext emptyList()
                parseFilesJson(bodyStr)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun search(driveIndex: Int, query: String): List<ApexFileItem> = withContext(Dispatchers.IO) {
        val url = "$BASE_URL/$driveIndex:search"
        val payload = JSONObject().apply {
            put("q", query)
            put("page_token", JSONObject.NULL)
            put("page_index", 0)
        }
        val requestBody = payload.toString().toRequestBody(JSON_MEDIA_TYPE)

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .header("User-Agent", USER_AGENT)
            .header("Accept", "application/json, text/plain, */*")
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyList()
                val bodyStr = response.body?.string() ?: return@withContext emptyList()
                parseFilesJson(bodyStr)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    private fun parseFilesJson(jsonStr: String): List<ApexFileItem> {
        val results = mutableListOf<ApexFileItem>()
        try {
            val root = JSONObject(jsonStr)
            val data = root.optJSONObject("data") ?: return results
            val filesArray = data.optJSONArray("files") ?: return results

            for (i in 0 until filesArray.length()) {
                val fileObj = filesArray.getJSONObject(i)
                val name = fileObj.optString("name", "")
                if (name.isBlank()) continue

                val mimeType = fileObj.optString("mimeType", "")
                
                // Parse size which can be number, string, or null
                val sizeVal = when {
                    fileObj.isNull("size") -> 0L
                    fileObj.optLong("size", -1L) != -1L -> fileObj.optLong("size", 0L)
                    else -> {
                        val s = fileObj.optString("size", "0")
                        s.toLongOrNull() ?: 0L
                    }
                }

                val id = if (fileObj.isNull("id")) null else fileObj.optString("id")
                val driveId = if (fileObj.isNull("driveId")) null else fileObj.optString("driveId")
                val link = if (fileObj.isNull("link")) null else fileObj.optString("link")
                val gofileLink = if (fileObj.isNull("gofileLink")) null else fileObj.optString("gofileLink")
                val isGoFile = fileObj.optBoolean("isGoFile", false)
                val modifiedTime = fileObj.optString("modifiedTime", null)
                val fileExtension = fileObj.optString("fileExtension", null)

                results.add(
                    ApexFileItem(
                        name = name,
                        mimeType = mimeType,
                        size = sizeVal,
                        id = id,
                        driveId = driveId,
                        link = link,
                        gofileLink = gofileLink,
                        isGoFile = isGoFile,
                        modifiedTime = modifiedTime,
                        fileExtension = fileExtension
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return results
    }

    fun resolveStreamUrl(item: ApexFileItem): String? {
        if (!item.link.isNullOrBlank()) {
            return if (item.link.startsWith("http://") || item.link.startsWith("https://")) {
                item.link
            } else {
                val path = if (item.link.startsWith("/")) item.link else "/${item.link}"
                "$BASE_URL$path"
            }
        }
        if (!item.gofileLink.isNullOrBlank()) {
            return item.gofileLink
        }
        return null
    }
}
