package io.heckel.ntfy.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.bottomnavigation.BottomNavigationView
import io.heckel.ntfy.R
import io.heckel.ntfy.msg.ApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

class QuakeHistoryActivity : AppCompatActivity() {
    private lateinit var bottomNavigation: BottomNavigationView
    private lateinit var recyclerView: RecyclerView
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var statusText: TextView
    private lateinit var adapter: QuakeHistoryAdapter

    private val httpClient = OkHttpClient.Builder()
        .callTimeout(15, TimeUnit.SECONDS)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_quake_history)

        title = getString(R.string.quake_history_title)

        bottomNavigation = findViewById(R.id.bottom_navigation)
        bottomNavigation.selectedItemId = R.id.menu_history
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.menu_alerts -> {
                    val intent = Intent(this, MainActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    startActivity(intent)
                    overridePendingTransition(0, 0)
                    finish()
                    true
                }
                R.id.menu_history -> true
                else -> false
            }
        }
        bottomNavigation.setOnItemReselectedListener { }

        recyclerView = findViewById(R.id.quake_history_list)
        swipeRefreshLayout = findViewById(R.id.quake_history_refresh)
        progressBar = findViewById(R.id.quake_history_progress)
        statusText = findViewById(R.id.quake_history_status_text)

        adapter = QuakeHistoryAdapter()
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        swipeRefreshLayout.setOnRefreshListener { loadReports(showProgress = false) }
        swipeRefreshLayout.setColorSchemeResources(Colors.refreshProgressIndicator)

        loadReports(showProgress = true)
    }

    private fun loadReports(showProgress: Boolean) {
        if (showProgress) {
            progressBar.isVisible = true
            statusText.isVisible = false
            recyclerView.isVisible = false
        } else {
            statusText.isVisible = false
        }
        lifecycleScope.launch {
            try {
                val reports = withContext(Dispatchers.IO) { fetchReports() }
                adapter.submitReports(reports)
                recyclerView.isVisible = reports.isNotEmpty()
                statusText.isVisible = reports.isEmpty()
                if (reports.isEmpty()) {
                    statusText.text = getString(R.string.quake_history_empty)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load quake history", e)
                adapter.submitReports(emptyList())
                recyclerView.isVisible = false
                statusText.isVisible = true
                statusText.text = getString(R.string.quake_history_error_generic)
            } finally {
                progressBar.isVisible = false
                swipeRefreshLayout.isRefreshing = false
            }
        }
    }

    private fun fetchReports(): List<QuakeReport> {
        val request = Request.Builder()
            .url(REPORTS_URL)
            .addHeader("User-Agent", ApiService.USER_AGENT)
            .addHeader("Accept", "application/json")
            .build()
        httpClient.newCall(request).execute().use { response ->
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
        val parsed = com.google.gson.JsonParser.parseString(json)
        val reports = mutableListOf<QuakeReport>()

        fun parseArray(array: com.google.gson.JsonArray) {
            array.forEach { element ->
                if (element.isJsonObject) {
                    parseItem(element.asJsonObject)?.let { reports.add(it) }
                }
            }
        }

        fun findArray(obj: com.google.gson.JsonObject): com.google.gson.JsonArray? {
            val preferredKeys = listOf("data", "laporan", "reports", "gempa", "items", "result")
            for (key in preferredKeys) {
                val candidate = obj.get(key) ?: continue
                when {
                    candidate.isJsonArray -> return candidate.asJsonArray
                    candidate.isJsonObject -> {
                        val nested = findArray(candidate.asJsonObject)
                        if (nested != null) {
                            return nested
                        }
                    }
                }
            }
            for (entry in obj.entrySet()) {
                val value = entry.value
                when {
                    value.isJsonArray -> return value.asJsonArray
                    value.isJsonObject -> {
                        val nested = findArray(value.asJsonObject)
                        if (nested != null) {
                            return nested
                        }
                    }
                }
            }
            return null
        }

        when {
            parsed.isJsonArray -> parseArray(parsed.asJsonArray)
            parsed.isJsonObject -> {
                val obj = parsed.asJsonObject
                val array = findArray(obj)
                if (array != null) {
                    parseArray(array)
                } else {
                    parseItem(obj)?.let { reports.add(it) }
                }
            }
        }

        return reports
    }

    private fun parseItem(obj: com.google.gson.JsonObject): QuakeReport? {
        data class FieldValue(val key: String, val value: String)

        val consumedKeys = mutableSetOf<String>()

        fun com.google.gson.JsonObject.valueFor(vararg keys: String): FieldValue? {
            for (candidate in keys) {
                val entry = entrySet()
                    .firstOrNull { it.key.equals(candidate, ignoreCase = true) && !consumedKeys.contains(it.key.lowercase()) }
                    ?: continue
                val element = entry.value
                if (element != null && element.isJsonPrimitive) {
                    val value = element.asString.trim()
                    if (value.isNotEmpty() && !value.equals("null", ignoreCase = true)) {
                        return FieldValue(entry.key, value)
                    }
                }
            }
            return null
        }

        fun FieldValue.consume(): String {
            consumedKeys.add(key.lowercase())
            return value
        }

        val date = obj.valueFor("tanggal", "date", "day", "tanggal_indo")?.consume()
        val time = obj.valueFor("jam", "time", "waktu")?.consume()
        val magnitude = obj.valueFor("magnitudo", "magnitude", "mag", "m", "sr")?.consume()
        var intensity = obj.valueFor(
            "intensitas",
            "intensitas_indo",
            "intensity",
            "intensitas_mmi",
            "intensity_mmi",
            "intensity_m",
            "skala_mmi"
        )?.consume()
        val depth = obj.valueFor("kedalaman", "depth")?.consume()
        val potential = obj.valueFor("potensi", "potential", "warning", "peringatan", "tsunami")?.consume()
        val coordinatesRaw = obj.valueFor("coordinates", "koordinat", "coordinate")?.consume()
        val latitude = obj.valueFor("lintang", "latitude", "lat")?.consume()
        val longitude = obj.valueFor("bujur", "longitude", "lon", "lng")?.consume()
        val coordinates = when {
            !coordinatesRaw.isNullOrBlank() -> coordinatesRaw
            !latitude.isNullOrBlank() || !longitude.isNullOrBlank() -> listOfNotNull(latitude, longitude).joinToString(", ")
            else -> null
        }
        val notes = obj.valueFor(
            "keterangan",
            "note",
            "ket",
            "information",
            "info",
            "deskripsi",
            "description",
            "dampak",
            "catatan",
            "impact"
        )?.consume()
        val felt = obj.valueFor("dirasakan", "dirasakan_indo", "felt", "mmi", "skala")?.consume()
        val shakemapUrl = obj.valueFor("shakemap", "shake_map", "shakemap_url", "shakeMap")?.consume()
        val location = obj.valueFor(
            "wilayah",
            "lokasi",
            "location",
            "area",
            "place",
            "region",
            "title",
            "judul",
            "event"
        )?.consume() ?: notes

        val extras = mutableListOf<QuakeReportField>()
        obj.entrySet().forEach { entry ->
            val element = entry.value
            if (!element.isJsonPrimitive) {
                return@forEach
            }
            val value = element.asString.trim()
            if (value.isEmpty() || value.equals("null", ignoreCase = true)) {
                return@forEach
            }
            val normalizedKey = entry.key.lowercase()
            if (normalizedKey in consumedKeys) {
                return@forEach
            }
            val isIntensityField = normalizedKey.contains("intens") ||
                (normalizedKey.contains("skala") && normalizedKey.contains("mmi"))
            if (intensity.isNullOrBlank() && isIntensityField) {
                intensity = value
                consumedKeys.add(normalizedKey)
                return@forEach
            }
            extras.add(QuakeReportField(key = entry.key, value = value))
        }

        val hasContent = listOf(
            date,
            time,
            magnitude,
            intensity,
            depth,
            location,
            potential,
            coordinates,
            notes,
            felt,
            shakemapUrl
        )
            .any { !it.isNullOrBlank() } || extras.isNotEmpty()
        if (!hasContent) {
            return null
        }

        return QuakeReport(
            date = date,
            time = time,
            magnitude = magnitude,
            intensity = intensity,
            depth = depth,
            location = location,
            potential = potential,
            coordinates = coordinates,
            notes = notes,
            felt = felt,
            shakemapUrl = shakemapUrl,
            extraFields = extras
        )
    }

    companion object {
        private const val REPORTS_URL = "https://quakealert.bananapixel.my.id/laporan"
        private const val TAG = "QuakeHistory"
    }
}
