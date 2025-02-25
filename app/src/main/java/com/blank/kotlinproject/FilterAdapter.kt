package com.blank.kotlinproject

import android.content.Context
import android.content.res.AssetManager
import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView


class FilterAdapter(
    private val context: Context,
    private val lutFiles: List<String>,
    private val onFilterSelected: (String) -> Unit
) : RecyclerView.Adapter<FilterAdapter.FilterViewHolder>() {

    class FilterViewHolder(view: android.view.View) : RecyclerView.ViewHolder(view) {
        val filterPreview: ImageView = view.findViewById(R.id.filterPreview)
        val filterName: android.widget.TextView = view.findViewById(R.id.filterName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FilterViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.filter_item, parent, false)
        return FilterViewHolder(view)
    }

    override fun onBindViewHolder(holder: FilterViewHolder, position: Int) {
        val lutFile = lutFiles[position]
        holder.filterName.text = lutFile


        val lutBitmap = context.assets.open("luts/$lutFile")
        val bitmap = BitmapFactory.decodeStream(lutBitmap)

        holder.filterPreview.setImageBitmap(bitmap)

        holder.itemView.setOnClickListener { onFilterSelected(lutFile) }
    }

    override fun getItemCount() = lutFiles.size
}