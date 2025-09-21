package io.heckel.ntfy.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

class MainPagerAdapter(
    private val layoutInflater: LayoutInflater,
    private val layouts: List<Int>,
    private val onPageBound: (position: Int, view: View) -> Unit
) : RecyclerView.Adapter<MainPagerAdapter.PageViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageViewHolder {
        val view = layoutInflater.inflate(viewType, parent, false)
        return PageViewHolder(view)
    }

    override fun onBindViewHolder(holder: PageViewHolder, position: Int) {
        onPageBound(position, holder.itemView)
    }

    override fun getItemCount(): Int = layouts.size

    override fun getItemViewType(position: Int): Int = layouts[position]

    class PageViewHolder(view: View) : RecyclerView.ViewHolder(view)
}
