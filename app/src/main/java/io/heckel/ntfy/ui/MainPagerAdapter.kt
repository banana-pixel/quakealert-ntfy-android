package io.heckel.ntfy.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.heckel.ntfy.R

class MainPagerAdapter(private val activity: MainActivity) :
    RecyclerView.Adapter<MainPagerAdapter.PageViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageViewHolder {
        val layout = when (viewType) {
            0 -> R.layout.page_quake_alerts
            else -> R.layout.page_quake_history
        }
        val view = LayoutInflater.from(parent.context).inflate(layout, parent, false)
        return PageViewHolder(view)
    }

    override fun onBindViewHolder(holder: PageViewHolder, position: Int) {
        if (position == 0) {
            activity.bindAlertsPage(holder.itemView)
        } else {
            activity.bindHistoryPage(holder.itemView)
        }
    }

    override fun getItemCount(): Int = 2

    override fun getItemViewType(position: Int): Int = position

    class PageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
}
