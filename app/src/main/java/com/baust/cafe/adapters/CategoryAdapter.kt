package com.baust.cafe.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.baust.cafe.R

data class Category(val name: String, val iconRes: Int)

class CategoryAdapter(
    private val categories: List<Category>,
    private val onCategoryClicked: (String) -> Unit
) : RecyclerView.Adapter<CategoryAdapter.ViewHolder>() {

    private var selectedCategory: String = "All"

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val icon: ImageView = view.findViewById(R.id.categoryIcon)
        val name: TextView = view.findViewById(R.id.categoryName)
        val card: androidx.cardview.widget.CardView = view as androidx.cardview.widget.CardView
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_category, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val cat = categories[position]
        holder.name.text = cat.name
        holder.icon.setImageResource(cat.iconRes)

        if (selectedCategory == cat.name) {
            holder.card.setCardBackgroundColor(holder.itemView.context.getColor(R.color.primary_green))
            holder.name.setTextColor(holder.itemView.context.getColor(R.color.white))
            holder.icon.setColorFilter(holder.itemView.context.getColor(R.color.white))
        } else {
            holder.card.setCardBackgroundColor(holder.itemView.context.getColor(R.color.white))
            holder.name.setTextColor(holder.itemView.context.getColor(R.color.text_black))
            holder.icon.setColorFilter(holder.itemView.context.getColor(R.color.primary_green))
        }

        holder.itemView.setOnClickListener {
            selectedCategory = cat.name
            onCategoryClicked(cat.name)
            notifyDataSetChanged()
        }
    }

    override fun getItemCount() = categories.size
}
