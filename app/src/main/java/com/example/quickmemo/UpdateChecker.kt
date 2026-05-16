package com.example.quickmemo

import android.content.Context
import android.content.Intent
import android.net.Uri
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

data class UpdateInfo(
    val versionName: String,
    val versionCode: Int,
    val releaseNotes: String,
    val downloadUrl: String,
    val htmlUrl: String
)

object UpdateChecker {
    private const val PREFS_NAME = "quick_memo_prefs"
    private const val KEY_UPDATE_JSON = "update_json"
    private const val KEY_UPDATE_CHECKED = "update_checked_at"
    private const val CACHE_DURATION_MS = 24 * 60 * 60 * 1000L
    private const val RELEASES_URL = "https://api.github.com/repos/paijp/QuickMemo/releases/latest"

    private val executor = Executors.newSingleThreadExecutor()
    private var cachedInfo: UpdateInfo? = null

    fun checkIfNeeded(context: Context, onResult: (UpdateInfo?) -> Unit) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastChecked = prefs.getLong(KEY_UPDATE_CHECKED, 0)
        val now = System.currentTimeMillis()

        // Return cached result if within 24h
        if (now - lastChecked < CACHE_DURATION_MS) {
            if (cachedInfo == null) {
                val json = prefs.getString(KEY_UPDATE_JSON, null)
                if (json != null) cachedInfo = parseJson(json)
            }
            val info = cachedInfo
            if (info != null && isNewer(context, info)) {
                onResult(info)
            } else {
                onResult(null)
            }
            return
        }

        executor.execute {
            var result: UpdateInfo? = null
            try {
                val url = URL(RELEASES_URL)
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 10000
                conn.readTimeout = 10000
                conn.setRequestProperty("Accept", "application/vnd.github.v3+json")

                if (conn.responseCode == 200) {
                    val reader = BufferedReader(InputStreamReader(conn.inputStream))
                    val sb = StringBuilder()
                    var line = reader.readLine()
                    while (line != null) { sb.append(line); line = reader.readLine() }
                    reader.close()

                    val json = sb.toString()
                    prefs.edit()
                        .putString(KEY_UPDATE_JSON, json)
                        .putLong(KEY_UPDATE_CHECKED, now)
                        .apply()

                    val info = parseJson(json)
                    cachedInfo = info
                    if (info != null && isNewer(context, info)) {
                        result = info
                    }
                }
                conn.disconnect()
            } catch (_: Exception) {}

            val finalResult = result
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                onResult(finalResult)
            }
        }
    }

    private fun parseJson(json: String): UpdateInfo? {
        return try {
            val obj = JSONObject(json)
            val tagName = obj.optString("tag_name", "")
            val body = obj.optString("body", "")
            val htmlUrl = obj.optString("html_url", "")

            // Extract version code from tag (e.g. "v2" -> 2, "v1.0.3" -> 103)
            val versionCode = extractVersionCode(tagName)

            // Find APK download URL from assets
            var apkUrl = ""
            val assets = obj.optJSONArray("assets")
            if (assets != null) {
                for (i in 0 until assets.length()) {
                    val asset = assets.getJSONObject(i)
                    val name = asset.optString("name", "")
                    if (name.endsWith(".apk")) {
                        apkUrl = asset.optString("browser_download_url", "")
                        break
                    }
                }
            }

            UpdateInfo(
                versionName = tagName,
                versionCode = versionCode,
                releaseNotes = body,
                downloadUrl = apkUrl,
                htmlUrl = htmlUrl
            )
        } catch (_: Exception) { null }
    }

    private fun extractVersionCode(tag: String): Int {
        // "v1" -> 1, "v2" -> 2, "v1.0.3" -> 10003, "v1.2" -> 10002
        val nums = tag.replace(Regex("[^0-9.]"), "")
        val parts = nums.split(".")
        return try {
            when (parts.size) {
                1 -> parts[0].toIntOrNull() ?: 0
                2 -> (parts[0].toIntOrNull() ?: 0) * 10000 + (parts[1].toIntOrNull() ?: 0)
                3 -> (parts[0].toIntOrNull() ?: 0) * 10000 + (parts[1].toIntOrNull() ?: 0) * 100 + (parts[2].toIntOrNull() ?: 0)
                else -> 0
            }
        } catch (_: Exception) { 0 }
    }

    private fun isNewer(context: Context, info: UpdateInfo): Boolean {
        return try {
            val pm = context.packageManager
            val pi = pm.getPackageInfo(context.packageName, 0)
            info.versionCode > pi.versionCode
        } catch (_: Exception) { false }
    }

    fun openDownload(context: Context, info: UpdateInfo) {
        val url = if (info.downloadUrl.isNotEmpty()) info.downloadUrl else info.htmlUrl
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    fun openReleaseNotes(context: Context, info: UpdateInfo) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(info.htmlUrl)).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }
}
