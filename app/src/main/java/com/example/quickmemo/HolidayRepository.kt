package com.example.quickmemo

import android.content.Context
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.Executors

object HolidayRepository {
    private const val PREFS_NAME = "quick_memo_prefs"
    private const val KEY_HOLIDAYS = "holidays_json"
    private const val KEY_HOLIDAYS_FETCHED = "holidays_fetched_at"
    private const val CACHE_DURATION_MS = 24 * 60 * 60 * 1000L // 24 hours
    private const val CSV_URL = "https://www8.cao.go.jp/chosei/shukujitsu/syukujitsu.csv"

    private val executor = Executors.newSingleThreadExecutor()

    // date format: "yyyy-MM-dd" -> holiday name
    private var holidays: Map<String, String>? = null

    fun getHolidayName(context: Context, year: Int, month: Int, day: Int): String? {
        val key = String.format("%04d-%02d-%02d", year, month + 1, day)
        return getAll(context)[key]
    }

    fun isHoliday(context: Context, year: Int, month: Int, day: Int): Boolean {
        return getHolidayName(context, year, month, day) != null
    }

    fun getAll(context: Context): Map<String, String> {
        if (holidays != null) return holidays!!
        // Load from cache
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_HOLIDAYS, null)
        if (json != null) {
            holidays = parseJson(json)
            return holidays!!
        }
        return emptyMap()
    }

    fun fetchIfNeeded(context: Context, onComplete: () -> Unit) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastFetched = prefs.getLong(KEY_HOLIDAYS_FETCHED, 0)
        val now = System.currentTimeMillis()

        if (now - lastFetched < CACHE_DURATION_MS && holidays != null) {
            onComplete()
            return
        }

        // Also check if we have cached data even if not in memory
        if (now - lastFetched < CACHE_DURATION_MS) {
            val json = prefs.getString(KEY_HOLIDAYS, null)
            if (json != null) {
                holidays = parseJson(json)
                onComplete()
                return
            }
        }

        executor.execute {
            try {
                val url = URL(CSV_URL)
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 10000
                conn.readTimeout = 10000
                conn.requestMethod = "GET"

                if (conn.responseCode == 200) {
                    val reader = BufferedReader(
                        InputStreamReader(conn.inputStream, "Shift_JIS")
                    )
                    val map = mutableMapOf<String, String>()
                    val dateFormat = SimpleDateFormat("yyyy/M/d", Locale.JAPAN)
                    val keyFormat = SimpleDateFormat("yyyy-MM-dd", Locale.JAPAN)

                    reader.readLine() // skip header
                    var line = reader.readLine()
                    while (line != null) {
                        val parts = line.split(",", limit = 2)
                        if (parts.size == 2) {
                            try {
                                val date = dateFormat.parse(parts[0].trim())
                                if (date != null) {
                                    map[keyFormat.format(date)] = parts[1].trim()
                                }
                            } catch (_: Exception) {}
                        }
                        line = reader.readLine()
                    }
                    reader.close()

                    // Save to prefs
                    val jsonObj = JSONObject()
                    map.forEach { (k, v) -> jsonObj.put(k, v) }
                    prefs.edit()
                        .putString(KEY_HOLIDAYS, jsonObj.toString())
                        .putLong(KEY_HOLIDAYS_FETCHED, now)
                        .apply()

                    holidays = map
                }
                conn.disconnect()
            } catch (_: Exception) {
                // Network error - use cached data if available
            }

            android.os.Handler(android.os.Looper.getMainLooper()).post { onComplete() }
        }
    }

    private fun parseJson(json: String): Map<String, String> {
        val map = mutableMapOf<String, String>()
        try {
            val obj = JSONObject(json)
            val keys = obj.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                map[k] = obj.getString(k)
            }
        } catch (_: Exception) {}
        return map
    }
}
