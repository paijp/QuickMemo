package com.example.quickmemo

import android.content.Context
import org.json.JSONArray

object KeywordRepository {
    private const val PREFS_NAME = "quick_memo_prefs"
    private const val KEY_KEYWORDS = "keywords"
    private val DEFAULTS = listOf("電話", "mail", "SMS", "連絡", "確認", "買う", "事務", "書類")

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getAll(context: Context): MutableList<String> {
        val json = prefs(context).getString(KEY_KEYWORDS, null)
        if (json == null) {
            save(context, DEFAULTS)
            return DEFAULTS.toMutableList()
        }
        val arr = JSONArray(json)
        val list = mutableListOf<String>()
        for (i in 0 until arr.length()) list.add(arr.getString(i))
        return list
    }

    fun save(context: Context, list: List<String>) {
        val arr = JSONArray()
        list.forEach { arr.put(it) }
        prefs(context).edit().putString(KEY_KEYWORDS, arr.toString()).apply()
    }
}
