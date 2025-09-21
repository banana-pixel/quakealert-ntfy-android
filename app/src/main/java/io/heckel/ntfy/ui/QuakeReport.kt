package io.heckel.ntfy.ui

data class QuakeReport(
    val date: String?,
    val time: String?,
    val magnitude: String?,
    val depth: String?,
    val location: String?,
    val potential: String?,
    val coordinates: String?,
    val notes: String?,
    val felt: String?,
    val shakemapUrl: String?,
    val extraFields: List<QuakeReportField>
)

data class QuakeReportField(
    val key: String,
    val value: String
)
