package com.example.quickmemo

import android.content.Context
import org.json.JSONArray

object MemoRepository {
    private const val PREFS_NAME = "quick_memo_prefs"
    private const val KEY_MEMOS = "memos"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getAll(context: Context): MutableList<String> {
        val json = prefs(context).getString(KEY_MEMOS, "[]") ?: "[]"
        val arr = JSONArray(json)
        val list = mutableListOf<String>()
        for (i in 0 until arr.length()) list.add(arr.getString(i))
        return list
    }

    fun add(context: Context, memo: String) {
        val list = getAll(context)
        list.add(0, memo)
        save(context, list)
    }

    fun insertAt(context: Context, index: Int, memo: String) {
        val list = getAll(context)
        val safeIndex = index.coerceIn(0, list.size)
        list.add(safeIndex, memo)
        save(context, list)
    }

    fun removeAt(context: Context, index: Int) {
        val list = getAll(context)
        if (index in list.indices) {
            list.removeAt(index)
            save(context, list)
        }
    }

    fun count(context: Context): Int = getAll(context).size

    private fun save(context: Context, list: List<String>) {
        val arr = JSONArray()
        list.forEach { arr.put(it) }
        prefs(context).edit().putString(KEY_MEMOS, arr.toString()).apply()
    }
}
