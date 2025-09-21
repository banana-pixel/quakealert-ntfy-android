package io.heckel.ntfy.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import io.heckel.ntfy.R

class QuakeHistoryAdapter :
    ListAdapter<QuakeHistoryReport, QuakeHistoryAdapter.HistoryViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_quake_history, parent, false)
        return HistoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class HistoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val idChip: Chip = itemView.findViewById(R.id.history_report_id)
        private val location: TextView = itemView.findViewById(R.id.history_report_location)
        private val dateTime: TextView = itemView.findViewById(R.id.history_report_datetime)
        private val magnitude: TextView = itemView.findViewById(R.id.history_report_magnitude)
        private val depth: TextView = itemView.findViewById(R.id.history_report_depth)
        private val coordinates: TextView = itemView.findViewById(R.id.history_report_coordinates)
        private val potential: TextView = itemView.findViewById(R.id.history_report_potential)
        private val felt: TextView = itemView.findViewById(R.id.history_report_felt)

        fun bind(report: QuakeHistoryReport) {
            val idText = if (report.id.isBlank()) {
                itemView.context.getString(R.string.quake_history_no_id)
            } else {
                itemView.context.getString(R.string.quake_history_id_prefix, report.id)
            }
            idChip.text = idText
            location.text = report.location.ifBlank { itemView.context.getString(R.string.quake_history_unknown_location) }
            dateTime.text = report.dateTime.ifBlank { itemView.context.getString(R.string.quake_history_unknown_time) }
            magnitude.text = report.magnitude.ifBlank { itemView.context.getString(R.string.quake_history_unknown_magnitude) }
            depth.text = report.depth.ifBlank { itemView.context.getString(R.string.quake_history_unknown_depth) }
            coordinates.text = report.coordinates.ifBlank { itemView.context.getString(R.string.quake_history_unknown_coordinates) }
            potential.text = report.potential.ifBlank { itemView.context.getString(R.string.quake_history_no_potential) }
            felt.visibility = if (report.felt.isBlank()) View.GONE else View.VISIBLE
            felt.text = report.felt
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<QuakeHistoryReport>() {
        override fun areItemsTheSame(oldItem: QuakeHistoryReport, newItem: QuakeHistoryReport): Boolean =
            oldItem.id == newItem.id && oldItem.dateTime == newItem.dateTime

        override fun areContentsTheSame(oldItem: QuakeHistoryReport, newItem: QuakeHistoryReport): Boolean =
            oldItem == newItem
    }
}
