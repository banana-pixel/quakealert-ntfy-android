package io.heckel.ntfy.ui.history

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import io.heckel.ntfy.R

class QuakeHistoryAdapter : ListAdapter<EarthquakeReport, QuakeHistoryAdapter.ViewHolder>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_quake_history, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val idView: TextView = itemView.findViewById(R.id.history_item_id)
        private val regionView: TextView = itemView.findViewById(R.id.history_item_region)
        private val timeView: TextView = itemView.findViewById(R.id.history_item_time)
        private val magnitudeView: TextView = itemView.findViewById(R.id.history_item_magnitude)
        private val depthView: TextView = itemView.findViewById(R.id.history_item_depth)
        private val potentialView: TextView = itemView.findViewById(R.id.history_item_potential)
        private val feltView: TextView = itemView.findViewById(R.id.history_item_felt)
        private val coordinatesView: TextView = itemView.findViewById(R.id.history_item_coordinates)
        private val additionalView: TextView = itemView.findViewById(R.id.history_item_additional)

        fun bind(report: EarthquakeReport) {
            idView.text = report.id
            idView.isVisible = report.id.isNotBlank()

            val fallbackTitle = itemView.context.getString(R.string.nav_history_title)
            regionView.text = report.region.ifBlank { fallbackTitle }

            timeView.text = report.time
            timeView.isVisible = report.time.isNotBlank()

            magnitudeView.text = report.magnitude
            magnitudeView.isVisible = report.magnitude.isNotBlank()

            if (report.depth.isNotBlank()) {
                depthView.text = report.depth
                depthView.visibility = View.VISIBLE
            } else {
                depthView.visibility = View.GONE
            }

            potentialView.text = report.potential
            potentialView.isVisible = report.potential.isNotBlank()

            feltView.text = report.felt
            feltView.isVisible = report.felt.isNotBlank()

            coordinatesView.text = report.coordinatesLabel
            coordinatesView.isVisible = report.hasCoordinates

            if (report.additionalDetails.isNotEmpty()) {
                val formatted = report.additionalDetails.joinToString(separator = "\n") { (key, value) ->
                    "$key: $value"
                }
                additionalView.text = formatted
                additionalView.visibility = View.VISIBLE
            } else {
                additionalView.visibility = View.GONE
            }
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<EarthquakeReport>() {
            override fun areItemsTheSame(oldItem: EarthquakeReport, newItem: EarthquakeReport): Boolean {
                return if (oldItem.id.isNotBlank() && newItem.id.isNotBlank()) {
                    oldItem.id == newItem.id
                } else {
                    oldItem === newItem
                }
            }

            override fun areContentsTheSame(oldItem: EarthquakeReport, newItem: EarthquakeReport): Boolean =
                oldItem == newItem
        }
    }
}
