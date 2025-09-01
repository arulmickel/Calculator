package com.example.calculator.history

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class HistoryItem(
    val expression: String,
    val result: String,
    val ts: Long = System.currentTimeMillis()
)

/**
 * I tried to Store history in SharedPreferences + JSON.
 * It will Keeps the most recent [MAX] items.
 */

object HistoryStore {
    private const val FILE = "dfcalculator_history"
    private const val KEY = "items"
    private const val MAX = 50

    fun add(context: Context, item: HistoryItem) {
        val list = get(context).toMutableList()
        // de-dup consecutive identical entries (optional)
        if (list.firstOrNull()?.expression == item.expression &&
            list.firstOrNull()?.result == item.result) return
        list.add(0, item)
        while (list.size > MAX) list.removeLast()
        save(context, list)
    }

    fun get(context: Context): List<HistoryItem> {
        val sp = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        val raw = sp.getString(KEY, null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                HistoryItem(
                    expression = o.optString("expression"),
                    result = o.optString("result"),
                    ts = o.optLong("ts", System.currentTimeMillis())
                )
            }
        }.getOrElse { emptyList() }
    }

    fun clear(context: Context) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit().remove(KEY).apply()
    }

    private fun save(context: Context, list: List<HistoryItem>) {
        val arr = JSONArray()
        list.forEach {
            arr.put(JSONObject().apply {
                put("expression", it.expression)
                put("result", it.result)
                put("ts", it.ts)
            })
        }
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit().putString(KEY, arr.toString()).apply()
    }
}
