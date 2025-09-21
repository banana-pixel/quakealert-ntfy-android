package io.heckel.ntfy.history

data class QuakeReport(
    val date: String?,
    val time: String?,
    val magnitude: String?,
    val depth: String?,
    val location: String?,
    val potential: String?,
    val felt: String?,
    val latitude: String?,
    val longitude: String?,
    val source: String?,
    val eventId: String?
) {
    val coordinates: String?
        get() {
            val lat = latitude?.trim().orEmpty()
            val lon = longitude?.trim().orEmpty()
            return when {
                lat.isNotEmpty() && lon.isNotEmpty() -> "$lat / $lon"
                lat.isNotEmpty() -> lat
                lon.isNotEmpty() -> lon
                else -> null
            }
        }

    val header: String
        get() {
            val parts = listOfNotNull(date?.trim()?.takeIf { it.isNotEmpty() }, time?.trim()?.takeIf { it.isNotEmpty() })
            return parts.joinToString(" • ")
        }
}
