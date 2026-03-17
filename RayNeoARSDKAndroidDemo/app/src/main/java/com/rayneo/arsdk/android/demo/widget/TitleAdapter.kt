package com.rayneo.arsdk.android.demo.widget

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.rayneo.arsdk.android.demo.R

class TitleAdapter : RecyclerView.Adapter<TitleAdapter.ViewHolder>() {
    private var titles: List<String> = emptyList()

    fun setData(data: List<String>) {
        titles = data
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_title, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.titleText.text = titles[position]
    }

    override fun getItemCount() = titles.size

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val titleText: TextView = view.findViewById(R.id.titleText)
    }
} 