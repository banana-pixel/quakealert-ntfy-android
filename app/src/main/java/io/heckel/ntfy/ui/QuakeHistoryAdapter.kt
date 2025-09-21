package io.heckel.ntfy.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import io.heckel.ntfy.R

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
        private val locationView: TextView = itemView.findViewById(R.id.quake_history_location)
        private val dateView: TextView = itemView.findViewById(R.id.quake_history_date)
        private val detailsView: TextView = itemView.findViewById(R.id.quake_history_details)
        private val additionalView: TextView = itemView.findViewById(R.id.quake_history_additional)

        fun bind(report: QuakeReport) {
            val context = itemView.context
            val location = report.location?.takeIf { it.isNotBlank() }
                ?: context.getString(R.string.quake_history_unknown_location)
            locationView.text = location

            val dateParts = listOfNotNull(
                report.date?.takeIf { it.isNotBlank() },
                report.time?.takeIf { it.isNotBlank() }
            )
            dateView.isVisible = dateParts.isNotEmpty()
            if (dateParts.isNotEmpty()) {
                dateView.text = dateParts.joinToString(" • ")
            }

            val detailParts = mutableListOf<String>()
            report.magnitude?.takeIf { it.isNotBlank() }?.let {
                detailParts.add(context.getString(R.string.quake_history_magnitude_format, it))
            }
            report.depth?.takeIf { it.isNotBlank() }?.let {
                detailParts.add(context.getString(R.string.quake_history_depth_format, it))
            }
            report.coordinates?.takeIf { it.isNotBlank() }?.let {
                detailParts.add(context.getString(R.string.quake_history_coordinates_format, it))
            }
            detailsView.isVisible = detailParts.isNotEmpty()
            if (detailParts.isNotEmpty()) {
                detailsView.text = detailParts.joinToString("   ")
            }

            val infoParts = mutableListOf<String>()
            report.potential?.takeIf { it.isNotBlank() }?.let {
                infoParts.add(context.getString(R.string.quake_history_potential_format, it))
            }
            report.notes?.takeIf { it.isNotBlank() }?.let { infoParts.add(it) }
            additionalView.isVisible = infoParts.isNotEmpty()
            if (infoParts.isNotEmpty()) {
                additionalView.text = infoParts.joinToString("\n")
            }
        }
    }
}
