package com.baust.cafe.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.baust.cafe.R
import com.baust.cafe.models.MenuItem
import com.bumptech.glide.Glide
import java.util.Locale

class PopularFoodAdapter(
    private var foodList: List<MenuItem>,
    private val onAddClicked: (MenuItem) -> Unit
) : RecyclerView.Adapter<PopularFoodAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val foodImage: ImageView = view.findViewById(R.id.foodImage)
        val foodName: TextView = view.findViewById(R.id.foodName)
        val foodSubtitle: TextView = view.findViewById(R.id.foodSubtitle)
        val foodPrice: TextView = view.findViewById(R.id.foodPrice)
        val plusButton: ImageView = view.findViewById(R.id.plusButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_popular_food, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val food = foodList[position]
        
        holder.foodName.text = food.name
        holder.foodSubtitle.text = food.description
        holder.foodPrice.text = String.format(Locale.getDefault(), "Tk. %.2f", food.price)

        if (food.imageUrl.isNotEmpty()) {
            Glide.with(holder.itemView.context)
                .load(food.imageUrl)
                .placeholder(R.drawable.cafe_logo)
                .into(holder.foodImage)
        } else {
            holder.foodImage.setImageResource(R.drawable.cafe_logo)
        }

        // Handle Availability
        if (food.available) {
            holder.plusButton.isEnabled = true
            holder.plusButton.alpha = 1.0f
        } else {
            holder.plusButton.isEnabled = false
            holder.plusButton.alpha = 0.5f
        }

        holder.plusButton.setOnClickListener { onAddClicked(food) }
        
        // Add click listener to the entire item to open details
        holder.itemView.setOnClickListener {
            val intent = android.content.Intent(holder.itemView.context, com.baust.cafe.activities.FoodDetailActivity::class.java).apply {
                putExtra("FOOD_ID", food.itemId)
                putExtra("FOOD_NAME", food.name)
                putExtra("FOOD_PRICE", food.price)
                putExtra("FOOD_DESC", food.description)
                putExtra("FOOD_IMAGE", food.imageUrl)
                putExtra("FOOD_AVAILABLE", food.available)
            }
            holder.itemView.context.startActivity(intent)
        }
    }

    override fun getItemCount() = foodList.size

    fun updateList(newList: List<MenuItem>) {
        foodList = newList
        notifyDataSetChanged()
    }
}
