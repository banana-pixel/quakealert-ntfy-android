package io.heckel.ntfy.ui.history

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import io.heckel.ntfy.R
import io.heckel.ntfy.history.QuakeReport

class QuakeHistoryAdapter : ListAdapter<QuakeReport, QuakeHistoryAdapter.HistoryViewHolder>(DiffCallback) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.item_quake_history, parent, false)
        return HistoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class HistoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val header: TextView = itemView.findViewById(R.id.history_item_header)
        private val location: TextView = itemView.findViewById(R.id.history_item_location)
        private val coordinates: TextView = itemView.findViewById(R.id.history_item_coordinates)
        private val magnitude: TextView = itemView.findViewById(R.id.history_item_magnitude)
        private val depth: TextView = itemView.findViewById(R.id.history_item_depth)
        private val potential: TextView = itemView.findViewById(R.id.history_item_potential)
        private val felt: TextView = itemView.findViewById(R.id.history_item_felt)
        private val source: TextView = itemView.findViewById(R.id.history_item_source)
        private val eventId: TextView = itemView.findViewById(R.id.history_item_event_id)

        fun bind(report: QuakeReport) {
            val context = itemView.context
            val headerText = report.header
            header.text = if (headerText.isNotBlank()) {
                headerText
            } else {
                context.getString(R.string.history_item_unknown_time)
            }

            bindWithLabel(location, R.string.history_item_location, report.location)
            bindWithLabel(coordinates, R.string.history_item_coordinates, report.coordinates)
            bindWithLabel(magnitude, R.string.history_item_magnitude, report.magnitude)
            bindWithLabel(depth, R.string.history_item_depth, report.depth)
            bindWithLabel(potential, R.string.history_item_potential, report.potential)
            bindWithLabel(felt, R.string.history_item_felt, report.felt)
            bindWithLabel(source, R.string.history_item_source, report.source)
            bindWithLabel(eventId, R.string.history_item_event_id, report.eventId)
        }

        private fun bindWithLabel(textView: TextView, labelRes: Int, value: String?) {
            if (!value.isNullOrBlank()) {
                textView.visibility = View.VISIBLE
                textView.text = textView.context.getString(labelRes, value)
            } else {
                textView.visibility = View.GONE
            }
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<QuakeReport>() {
        override fun areItemsTheSame(oldItem: QuakeReport, newItem: QuakeReport): Boolean {
            val oldId = oldItem.eventId?.takeIf { it.isNotBlank() }
            val newId = newItem.eventId?.takeIf { it.isNotBlank() }
            return when {
                oldId != null && newId != null -> oldId == newId
                else -> oldItem.date == newItem.date && oldItem.time == newItem.time && oldItem.location == newItem.location
            }
        }

        override fun areContentsTheSame(oldItem: QuakeReport, newItem: QuakeReport): Boolean = oldItem == newItem
    }
}
