package io.heckel.ntfy.ui

import io.heckel.ntfy.BuildConfig
import io.heckel.ntfy.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class QuakeHistoryService {
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    fun fetchReports(): List<QuakeHistoryReport> {
        val request = Request.Builder()
            .url(HISTORY_URL)
            .header("User-Agent", "quakealert-android/${BuildConfig.VERSION_NAME}")
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Unexpected HTTP ${response.code}")
            }
            val body = response.body?.string()?.trim() ?: return emptyList()
            if (body.isEmpty()) {
                return emptyList()
            }
            return parseBody(body)
        }
    }

    private fun parseBody(body: String): List<QuakeHistoryReport> {
        return try {
            when {
                body.startsWith("[") -> parseArray(JSONArray(body))
                body.startsWith("{") -> parseObject(JSONObject(body))
                else -> emptyList()
            }
        } catch (exception: Exception) {
            Log.e(TAG, "Unable to parse quake history", exception)
            emptyList()
        }
    }

    private fun parseObject(root: JSONObject): List<QuakeHistoryReport> {
        val arrayKeys = listOf("data", "laporan", "reports", "result")
        val nestedArray = arrayKeys.firstNotNullOfOrNull { key ->
            root.optJSONArray(key)
        }
        return if (nestedArray != null) {
            parseArray(nestedArray)
        } else {
            // Some APIs wrap entries in objects keyed by IDs
            val list = mutableListOf<QuakeHistoryReport>()
            val iterator = root.keys()
            while (iterator.hasNext()) {
                val key = iterator.next()
                val value = root.optJSONObject(key) ?: continue
                list += parseReport(value, key)
            }
            list
        }
    }

    private fun parseArray(array: JSONArray): List<QuakeHistoryReport> {
        val list = mutableListOf<QuakeHistoryReport>()
        for (i in 0 until array.length()) {
            val obj = array.optJSONObject(i) ?: continue
            list += parseReport(obj)
        }
        return list
    }

    private fun parseReport(obj: JSONObject, fallbackId: String? = null): QuakeHistoryReport {
        val id = obj.optString("id").takeUnless { it.isBlank() }
            ?: obj.optString("ID").takeUnless { it.isBlank() }
            ?: fallbackId
            ?: listOf(
                obj.optString("EventID"),
                obj.optString("event_id"),
                obj.optString("shakemap"),
                obj.optString("date")
            ).firstOrNull { it.isNotBlank() } ?: ""

        val tanggal = obj.optString("Tanggal", obj.optString("tanggal"))
        val jam = obj.optString("Jam", obj.optString("jam"))
        val datetime = obj.optString("DateTime", obj.optString("datetime"))
        val reportedAt = when {
            datetime.isNotBlank() -> datetime
            tanggal.isNotBlank() && jam.isNotBlank() -> "$tanggal $jam"
            tanggal.isNotBlank() -> tanggal
            else -> jam
        }

        val magnitude = obj.optString("Magnitude", obj.optString("magnitudo", obj.optString("magnitude")))
        val depth = obj.optString("Kedalaman", obj.optString("kedalaman", obj.optString("depth")))
        val lintang = obj.optString("Lintang", obj.optString("lintang"))
        val bujur = obj.optString("Bujur", obj.optString("bujur"))
        val coords = obj.optString("coordinates")
        val location = obj.optString("Wilayah", obj.optString("wilayah", obj.optString("area", obj.optString("lokasi"))))
        val potential = obj.optString("Potensi", obj.optString("potensi", obj.optString("potential")))
        val felt = obj.optString("Dirasakan", obj.optString("dirasakan", obj.optString("felt")))

        val formattedCoordinates = when {
            coords.isNotBlank() -> coords
            lintang.isNotBlank() || bujur.isNotBlank() -> listOf(lintang, bujur).filter { it.isNotBlank() }.joinToString(" ")
            else -> ""
        }

        return QuakeHistoryReport(
            id = id,
            location = location,
            dateTime = reportedAt,
            magnitude = if (magnitude.isNotBlank()) "M $magnitude" else "",
            depth = depth,
            coordinates = formattedCoordinates,
            potential = potential,
            felt = felt
        )
    }

    companion object {
        private const val TAG = "QuakeHistoryService"
        private const val HISTORY_URL = "https://quakealert.bananapixel.my.id/laporan"
    }
}
