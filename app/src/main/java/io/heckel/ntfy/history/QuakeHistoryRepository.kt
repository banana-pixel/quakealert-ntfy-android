package io.heckel.ntfy.history

import io.heckel.ntfy.msg.ApiService
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class QuakeHistoryRepository {
    private val client = OkHttpClient.Builder()
        .callTimeout(15, TimeUnit.SECONDS)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    @Throws(IOException::class)
    fun fetchReports(): List<QuakeReport> {
        val request = Request.Builder()
            .url(HISTORY_URL)
            .addHeader("User-Agent", ApiService.USER_AGENT)
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Unexpected response ${response.code}")
            }
            val body = response.body?.string()?.trim().orEmpty()
            if (body.isEmpty()) {
                return emptyList()
            }
            return parseReports(body)
        }
    }

    private fun parseReports(json: String): List<QuakeReport> {
        val array = when {
            json.startsWith("[") -> JSONArray(json)
            json.startsWith("{") -> {
                val root = JSONObject(json)
                extractArray(root) ?: JSONArray().apply { put(root) }
            }
            else -> JSONArray()
        }
        val reports = mutableListOf<QuakeReport>()
        for (i in 0 until array.length()) {
            val item = array.optJSONObject(i) ?: continue
            reports.add(parseObject(item))
        }
        return reports
    }

    private fun extractArray(root: JSONObject): JSONArray? {
        val candidates = listOf("laporan", "data", "results", "items", "records")
        candidates.forEach { key ->
            if (root.has(key)) {
                val value = root.get(key)
                when (value) {
                    is JSONArray -> return value
                    is JSONObject -> {
                        extractArray(value)?.let { return it }
                        value.optJSONArray("records")?.let { return it }
                    }
                }
            }
        }
        return null
    }

    private fun parseObject(obj: JSONObject): QuakeReport {
        val coordinateObject = obj.optJSONObject("coordinates")
        val latitude = coordinateObject?.optStringOrNull("lintang", "latitude")
            ?: obj.optStringOrNull("lintang", "latitude")
        val longitude = coordinateObject?.optStringOrNull("bujur", "longitude")
            ?: obj.optStringOrNull("bujur", "longitude")
        return QuakeReport(
            date = obj.optStringOrNull("tanggal", "date", "day"),
            time = obj.optStringOrNull("jam", "time", "clock"),
            magnitude = obj.optStringOrNull("magnitudo", "magnitude", "mag"),
            depth = obj.optStringOrNull("kedalaman", "depth"),
            location = obj.optStringOrNull("wilayah", "location", "region", "place"),
            potential = obj.optStringOrNull("potensi", "potential", "warning"),
            felt = obj.optStringOrNull("dirasakan", "felt", "impact"),
            latitude = latitude,
            longitude = longitude,
            source = obj.optStringOrNull("source", "sumber", "provider"),
            eventId = obj.optStringOrNull("eventid", "event_id", "id")
        )
    }

    private fun JSONObject.optStringOrNull(vararg names: String): String? {
        names.forEach { name ->
            if (has(name)) {
                val value = optString(name, "").trim()
                if (value.isNotEmpty()) {
                    return value
                }
            }
        }
        return null
    }

    companion object {
        private const val HISTORY_URL = "https://quakealert.bananapixel.my.id/laporan"
    }
}
