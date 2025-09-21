package io.heckel.ntfy.ui

data class QuakeReport(
    val date: String?,
    val time: String?,
    val magnitude: String?,
    val depth: String?,
    val location: String?,
    val potential: String?,
    val coordinates: String?,
    val notes: String?
)
