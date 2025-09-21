package io.heckel.ntfy.ui.history

data class EarthquakeReport(
    val id: String = "",
    val time: String = "",
    val magnitude: String = "",
    val depth: String = "",
    val region: String = "",
    val potential: String = "",
    val felt: String = "",
    val latitude: String = "",
    val longitude: String = "",
    val additionalDetails: List<Pair<String, String>> = emptyList()
) {
    val hasCoordinates: Boolean
        get() = latitude.isNotBlank() || longitude.isNotBlank()

    val coordinatesLabel: String
        get() = listOf(latitude, longitude)
            .filter { it.isNotBlank() }
            .joinToString(separator = " • ")
}
