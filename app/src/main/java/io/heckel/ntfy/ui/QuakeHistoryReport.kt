package io.heckel.ntfy.ui

data class QuakeHistoryReport(
    val id: String,
    val location: String,
    val dateTime: String,
    val magnitude: String,
    val depth: String,
    val coordinates: String,
    val potential: String,
    val felt: String
)
