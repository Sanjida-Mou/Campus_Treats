package com.baust.cafe.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.baust.cafe.R
import com.baust.cafe.models.MenuItem
import com.bumptech.glide.Glide
import com.google.firebase.database.FirebaseDatabase
import java.util.Locale

class MenuAdapter(
    private var menuItems: List<MenuItem>,
    private val isAdmin: Boolean = false,
    private val onAddToCartClicked: ((MenuItem) -> Unit)? = null,
    private val onAvailabilityChanged: ((MenuItem, Boolean) -> Unit)? = null,
    private val onEditClicked: ((MenuItem) -> Unit)? = null
) : RecyclerView.Adapter<MenuAdapter.MenuViewHolder>() {

    class MenuViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nameText: TextView = itemView.findViewById(R.id.menuItemName)
        val descriptionText: TextView = itemView.findViewById(R.id.menuItemDescription)
        val priceText: TextView = itemView.findViewById(R.id.menuItemPrice)
        val availabilityText: TextView = itemView.findViewById(R.id.availabilityText)
        val itemImage: ImageView = itemView.findViewById(R.id.menuItemImage)
        val addToCartButton: ImageButton = itemView.findViewById(R.id.addToCartButton)
        val availabilitySwitch: androidx.appcompat.widget.SwitchCompat = itemView.findViewById(R.id.availabilitySwitch)
        val editButton: ImageButton = itemView.findViewById(R.id.editButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MenuViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_menu, parent, false)
        return MenuViewHolder(view)
    }

    override fun onBindViewHolder(holder: MenuViewHolder, position: Int) {
        val item = menuItems[position]
        holder.nameText.text = item.name
        holder.descriptionText.text = item.description
        holder.priceText.text = String.format(Locale.getDefault(), "Tk. %.2f", item.price)

        // Load image from URL
        if (item.imageUrl.isNotEmpty()) {
            Glide.with(holder.itemView.context)
                .load(item.imageUrl)
                .placeholder(R.drawable.cafe_logo)
                .into(holder.itemImage)
        } else {
            holder.itemImage.setImageResource(R.drawable.cafe_logo)
        }
        
        // Availability Status Display
        if (item.available) {
            holder.availabilityText.text = "Available"
            holder.availabilityText.setTextColor(android.graphics.Color.parseColor("#4CAF50"))
            holder.availabilityText.visibility = View.VISIBLE
        } else {
            holder.availabilityText.text = "Out of Stock"
            holder.availabilityText.setTextColor(android.graphics.Color.parseColor("#F44336"))
            holder.availabilityText.visibility = View.VISIBLE
        }

        if (isAdmin) {
            // ADMIN PANEL LOGIC
            holder.itemView.setOnClickListener(null) // Disable opening details for Admin
            
            holder.editButton.visibility = View.VISIBLE
            holder.availabilitySwitch.visibility = View.VISIBLE
            
            // Set switch state without triggering listener
            holder.availabilitySwitch.setOnCheckedChangeListener(null)
            holder.availabilitySwitch.isChecked = item.available
            
            // Set up cart button as DELETE for admin
            holder.addToCartButton.visibility = View.VISIBLE
            holder.addToCartButton.setImageResource(android.R.drawable.ic_menu_delete)
            holder.addToCartButton.isEnabled = true
            holder.addToCartButton.alpha = 1.0f
            
            holder.addToCartButton.setOnClickListener {
                showDeleteDialog(holder.itemView.context, item)
            }

            holder.editButton.setOnClickListener {
                onEditClicked?.invoke(item)
            }

            holder.availabilitySwitch.setOnCheckedChangeListener { _, isChecked ->
                onAvailabilityChanged?.invoke(item, isChecked)
                // Immediate local visual update
                if (isChecked) {
                    holder.availabilityText.text = "Available"
                    holder.availabilityText.setTextColor(android.graphics.Color.parseColor("#4CAF50"))
                } else {
                    holder.availabilityText.text = "Out of Stock"
                    holder.availabilityText.setTextColor(android.graphics.Color.parseColor("#F44336"))
                }
            }
        } else {
            // USER VIEW LOGIC
            holder.editButton.visibility = View.GONE
            holder.availabilitySwitch.visibility = View.GONE
            holder.addToCartButton.visibility = View.VISIBLE
            holder.addToCartButton.setImageResource(android.R.drawable.ic_input_add)
            
            if (item.available) {
                holder.addToCartButton.isEnabled = true
                holder.addToCartButton.alpha = 1.0f
            } else {
                holder.addToCartButton.isEnabled = false
                holder.addToCartButton.alpha = 0.5f
            }

            // Clicking item opens User Detail page
            holder.itemView.setOnClickListener {
                val intent = android.content.Intent(holder.itemView.context, com.baust.cafe.activities.FoodDetailActivity::class.java).apply {
                    putExtra("FOOD_ID", item.itemId)
                    putExtra("FOOD_NAME", item.name)
                    putExtra("FOOD_PRICE", item.price)
                    putExtra("FOOD_DESC", item.description)
                    putExtra("FOOD_IMAGE", item.imageUrl)
                    putExtra("FOOD_AVAILABLE", item.available)
                }
                holder.itemView.context.startActivity(intent)
            }

            holder.addToCartButton.setOnClickListener {
                onAddToCartClicked?.invoke(item)
            }
        }
    }

    private fun showDeleteDialog(context: android.content.Context, item: MenuItem) {
        AlertDialog.Builder(context)
            .setTitle("Delete Item")
            .setMessage("Are you sure you want to delete ${item.name}?")
            .setPositiveButton("Delete") { _, _ ->
                FirebaseDatabase.getInstance().getReference("Menu").child(item.itemId).removeValue()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun getItemCount(): Int = menuItems.size

    fun updateItems(newItems: List<MenuItem>) {
        menuItems = newItems
        notifyDataSetChanged()
    }
}