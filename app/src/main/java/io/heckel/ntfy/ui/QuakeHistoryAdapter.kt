package io.heckel.ntfy.ui

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.text.SpannableString
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import android.text.util.Linkify
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import io.heckel.ntfy.R
import java.util.Locale
import kotlin.math.min

class QuakeHistoryAdapter : RecyclerView.Adapter<QuakeHistoryAdapter.QuakeHistoryViewHolder>() {
    private val reports = mutableListOf<QuakeReport>()

    fun submitReports(newReports: List<QuakeReport>) {
        reports.clear()
        reports.addAll(newReports)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QuakeHistoryViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_quake_history, parent, false)
        return QuakeHistoryViewHolder(view)
    }

    override fun getItemCount(): Int = reports.size

    override fun onBindViewHolder(holder: QuakeHistoryViewHolder, position: Int) {
        holder.bind(reports[position])
    }

    class QuakeHistoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val magnitudeView: TextView = itemView.findViewById(R.id.quake_history_magnitude_badge)
        private val locationView: TextView = itemView.findViewById(R.id.quake_history_location)
        private val dateTimeView: TextView = itemView.findViewById(R.id.quake_history_datetime)
        private val highlightsGroup: ChipGroup = itemView.findViewById(R.id.quake_history_highlights)
        private val feltView: TextView = itemView.findViewById(R.id.quake_history_felt)
        private val notesView: TextView = itemView.findViewById(R.id.quake_history_notes)
        private val extrasDivider: View = itemView.findViewById(R.id.quake_history_extra_divider)
        private val extrasTitle: TextView = itemView.findViewById(R.id.quake_history_extra_title)
        private val extrasContainer: LinearLayout = itemView.findViewById(R.id.quake_history_extra_container)

        fun bind(report: QuakeReport) {
            val context = itemView.context

            val magnitudeRaw = report.magnitude
            val hasMagnitude = !magnitudeRaw.isNullOrBlank()
            val magnitudeText = formatMagnitudeText(magnitudeRaw)
            val intensityText = formatIntensityText(findIntensityValue(report))
            magnitudeView.text = buildMagnitudeBadgeText(magnitudeText, intensityText, hasMagnitude)
            magnitudeView.contentDescription = when {
                intensityText != null && hasMagnitude -> {
                    context.getString(
                        R.string.quake_history_magnitude_intensity_badge_content_description,
                        magnitudeText,
                        intensityText
                    )
                }
                intensityText != null -> {
                    context.getString(
                        R.string.quake_history_intensity_badge_content_description,
                        intensityText
                    )
                }
                else -> {
                    context.getString(
                        R.string.quake_history_magnitude_badge_content_description,
                        magnitudeText
                    )
                }
            }
            val magnitudeColor = ContextCompat.getColor(context, magnitudeColorRes(parseMagnitude(report.magnitude)))
            magnitudeView.background?.let { drawable ->
                val tinted = DrawableCompat.wrap(drawable.mutate())
                DrawableCompat.setTint(tinted, magnitudeColor)
                magnitudeView.background = tinted
            }

            val location = report.location?.takeIf { it.isNotBlank() }
                ?: context.getString(R.string.quake_history_unknown_location)
            locationView.text = location

            val dateParts = listOfNotNull(
                report.date?.takeIf { it.isNotBlank() },
                report.time?.takeIf { it.isNotBlank() }
            )
            dateTimeView.isVisible = dateParts.isNotEmpty()
            if (dateParts.isNotEmpty()) {
                dateTimeView.text = dateParts.joinToString(" • ")
            }

            highlightsGroup.removeAllViews()
            report.potential?.takeIf { it.isNotBlank() }?.let { potential ->
                val chip = newChip()
                chip.text = potential
                stylePotentialChip(chip, potential)
                highlightsGroup.addView(chip)
            }
            report.depth?.takeIf { it.isNotBlank() }?.let {
                val chip = newChip()
                chip.text = context.getString(R.string.quake_history_chip_depth, it)
                highlightsGroup.addView(chip)
            }
            report.coordinates?.takeIf { it.isNotBlank() }?.let {
                val chip = newChip()
                chip.text = context.getString(R.string.quake_history_chip_coordinates, it)
                highlightsGroup.addView(chip)
            }
            highlightsGroup.isVisible = highlightsGroup.childCount > 0

            val feltText = report.felt?.takeIf { it.isNotBlank() }
            feltView.isVisible = feltText != null
            if (feltText != null) {
                feltView.text = context.getString(R.string.quake_history_felt_format, feltText)
            }

            val notesText = report.notes?.takeIf { note ->
                note.isNotBlank() && !note.equals(location, ignoreCase = true)
            }
            notesView.isVisible = notesText != null
            if (notesText != null) {
                notesView.text = context.getString(R.string.quake_history_notes_format, notesText)
            }

            val extras = mutableListOf<QuakeReportField>().apply {
                val shakemap = report.shakemapUrl?.takeIf { it.isNotBlank() }
                if (shakemap != null && report.extraFields.none { it.key.equals("shakemap", ignoreCase = true) }) {
                    add(QuakeReportField(key = "shakemap", value = shakemap))
                }
                addAll(report.extraFields)
            }.filter { it.value.isNotBlank() }

            extrasContainer.removeAllViews()
            if (extras.isNotEmpty()) {
                extrasDivider.isVisible = true
                extrasTitle.isVisible = true
                extrasContainer.isVisible = true
                val inflater = LayoutInflater.from(context)
                extras.forEach { field ->
                    val textView = inflater.inflate(
                        R.layout.view_quake_history_extra_field,
                        extrasContainer,
                        false
                    ) as TextView
                    textView.text = buildExtraText(context, field)
                    if (field.value.contains("http", ignoreCase = true)) {
                        textView.autoLinkMask = Linkify.WEB_URLS
                        textView.linksClickable = true
                        textView.movementMethod = LinkMovementMethod.getInstance()
                    }
                    extrasContainer.addView(textView)
                }
            } else {
                extrasDivider.isVisible = false
                extrasTitle.isVisible = false
                extrasContainer.isVisible = false
            }
        }

        private fun newChip(): Chip {
            return LayoutInflater.from(itemView.context)
                .inflate(R.layout.view_quake_history_chip, highlightsGroup, false) as Chip
        }

        private fun stylePotentialChip(chip: Chip, text: String) {
            val lower = text.lowercase(Locale.getDefault())
            val (background, foreground) = when {
                lower.contains("tidak") || lower.contains("aman") || lower.contains("no") ->
                    R.color.potential_safe_bg to R.color.potential_safe_text
                lower.contains("tsunami") || lower.contains("warning") || lower.contains("peringatan") ->
                    R.color.potential_warning_bg to R.color.potential_warning_text
                else -> R.color.potential_unknown_bg to R.color.potential_unknown_text
            }
            val context = chip.context
            chip.chipBackgroundColor = ColorStateList.valueOf(ContextCompat.getColor(context, background))
            chip.setTextColor(ContextCompat.getColor(context, foreground))
        }

        private fun formatMagnitudeText(raw: String?): String {
            if (raw.isNullOrBlank()) {
                return itemView.context.getString(R.string.quake_history_magnitude_placeholder)
            }
            val trimmed = raw.trim()
            val numeric = trimmed.takeWhile { it.isDigit() || it == '.' || it == ',' }
            val display = if (numeric.isNotEmpty()) numeric else trimmed
            return display.replace(',', '.')
        }

        private fun findIntensityValue(report: QuakeReport): String? {
            report.intensity?.takeIf { it.isNotBlank() }?.let { return it }
            report.extraFields.firstOrNull { field ->
                val key = field.key.lowercase(Locale.getDefault())
                key.contains("intens") || (key.contains("skala") && key.contains("mmi"))
            }?.value?.takeIf { it.isNotBlank() }?.let { return it }
            report.felt?.takeIf { looksLikeIntensity(it) }?.let { return it }
            return null
        }

        private fun formatIntensityText(raw: String?): String? {
            if (raw.isNullOrBlank()) {
                return null
            }
            val cleaned = raw.replace('=', ':')
                .replace("intensitas", "", ignoreCase = true)
                .replace("intensity", "", ignoreCase = true)
                .replace("dirasakan", "", ignoreCase = true)
                .replace("felt", "", ignoreCase = true)
                .trim()
            val afterSeparator = cleaned.substringAfter(':', cleaned)
            val trimmed = afterSeparator.trim().trimStart('-', '–', '—').trim()
            return trimmed.ifEmpty { null }
        }

        private fun looksLikeIntensity(text: String): Boolean {
            val lower = text.lowercase(Locale.getDefault())
            if (lower.contains("intens")) {
                return true
            }
            if (MMI_WORD_REGEX.containsMatchIn(text)) {
                return true
            }
            return ROMAN_INTENSITY_REGEX.containsMatchIn(text)
        }

        private fun buildMagnitudeBadgeText(
            magnitude: String,
            intensity: String?,
            hasMagnitude: Boolean
        ): CharSequence {
            if (!hasMagnitude && !intensity.isNullOrBlank()) {
                return intensity
            }
            if (intensity.isNullOrBlank()) {
                return magnitude
            }
            val combined = "$magnitude\n$intensity"
            val newlineIndex = combined.indexOf('\n')
            val spannable = SpannableString(combined)
            if (newlineIndex in 0 until combined.length) {
                val start = newlineIndex + 1
                spannable.setSpan(RelativeSizeSpan(0.6f), start, combined.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                spannable.setSpan(StyleSpan(Typeface.NORMAL), start, combined.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
            return spannable
        }

        private fun parseMagnitude(raw: String?): Double? {
            if (raw.isNullOrBlank()) {
                return null
            }
            val numeric = raw.trim()
                .replace(',', '.')
                .takeWhile { it.isDigit() || it == '.' }
            return numeric.toDoubleOrNull()
        }

        private fun magnitudeColorRes(magnitude: Double?): Int {
            return when {
                magnitude == null -> R.color.magnitude_low
                magnitude >= 6.0 -> R.color.magnitude_high
                magnitude >= 5.0 -> R.color.magnitude_moderate
                else -> R.color.magnitude_low
            }
        }

        private fun buildExtraText(context: Context, field: QuakeReportField): CharSequence {
            val label = labelForKey(context, field.key)
            val combined = "$label: ${field.value}"
            val spannable = SpannableString(combined)
            val end = min(label.length + 1, spannable.length)
            if (end > 0) {
                spannable.setSpan(StyleSpan(Typeface.BOLD), 0, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
            return spannable
        }

        private fun labelForKey(context: Context, key: String): String {
            val normalized = key.lowercase(Locale.getDefault())
            val labelRes = when (normalized) {
                "shakemap", "shake_map", "shakemap_url", "shakemaplink" -> R.string.quake_history_label_shakemap
                "eventid", "event_id", "id" -> R.string.quake_history_label_event_id
                "source", "sumber" -> R.string.quake_history_label_source
                "update", "updated", "lastupdate" -> R.string.quake_history_label_updated
                else -> null
            }
            if (labelRes != null) {
                return context.getString(labelRes)
            }
            val words = normalized.split('_', '-', ' ', '.').filter { it.isNotBlank() }
            if (words.isEmpty()) {
                return key
            }
            return words.joinToString(" ") { word ->
                word.replaceFirstChar { char ->
                    if (char.isLowerCase()) {
                        char.titlecase(Locale.getDefault())
                    } else {
                        char.toString()
                    }
                }
            }
        }
        companion object {
            private val ROMAN_INTENSITY_REGEX = Regex(
                "(?i)\\b(i|ii|iii|iv|v|vi|vii|viii|ix|x)(?:[-\\/ ](i|ii|iii|iv|v|vi|vii|viii|ix|x))?\\b"
            )
            private val MMI_WORD_REGEX = Regex("(?i)\\bmmi\\b")
        }
    }
}
