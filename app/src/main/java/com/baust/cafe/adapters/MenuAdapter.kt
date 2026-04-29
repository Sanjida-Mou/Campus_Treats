package com.baust.cafe.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.baust.cafe.R
import com.baust.cafe.models.MenuItem
import java.util.Locale

class MenuAdapter(
    private var menuItems: List<MenuItem>,
    private val isAdmin: Boolean = false,
    private val onAddToCartClicked: ((MenuItem) -> Unit)? = null,
    private val onAvailabilityChanged: ((MenuItem, Boolean) -> Unit)? = null
) : RecyclerView.Adapter<MenuAdapter.MenuViewHolder>() {

    class MenuViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nameText: TextView = itemView.findViewById(R.id.menuItemName)
        val descriptionText: TextView = itemView.findViewById(R.id.menuItemDescription)
        val priceText: TextView = itemView.findViewById(R.id.menuItemPrice)
        val availabilityText: TextView = itemView.findViewById(R.id.availabilityText)
        val itemImage: ImageView = itemView.findViewById(R.id.menuItemImage)
        val addToCartButton: Button = itemView.findViewById(R.id.addToCartButton)
        val availabilitySwitch: androidx.appcompat.widget.SwitchCompat = itemView.findViewById(R.id.availabilitySwitch)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MenuViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_menu, parent, false)
        return MenuViewHolder(view)
    }

    override fun onBindViewHolder(holder: MenuViewHolder, position: Int) {
        val item = menuItems[position]
        holder.nameText.text = item.name
        holder.descriptionText.text = item.description
        holder.priceText.text = String.format(Locale.getDefault(), "$%.2f", item.price)
        
        if (isAdmin) {
            holder.addToCartButton.visibility = View.GONE
            holder.availabilitySwitch.visibility = View.VISIBLE
            holder.availabilitySwitch.isChecked = item.isAvailable
            
            holder.availabilitySwitch.setOnCheckedChangeListener { _, isChecked ->
                onAvailabilityChanged?.invoke(item, isChecked)
            }
        } else {
            holder.addToCartButton.visibility = View.VISIBLE
            holder.availabilitySwitch.visibility = View.GONE
            
            if (item.isAvailable) {
                holder.availabilityText.text = "Available"
                holder.availabilityText.setTextColor(android.graphics.Color.parseColor("#4CAF50"))
                holder.addToCartButton.isEnabled = true
                holder.addToCartButton.alpha = 1.0f
            } else {
                holder.availabilityText.text = "Out of Stock"
                holder.availabilityText.setTextColor(android.graphics.Color.parseColor("#F44336"))
                holder.addToCartButton.isEnabled = false
                holder.addToCartButton.alpha = 0.5f
            }

            holder.addToCartButton.setOnClickListener {
                onAddToCartClicked?.invoke(item)
            }
        }
    }

    override fun getItemCount(): Int = menuItems.size

    fun updateItems(newItems: List<MenuItem>) {
        menuItems = newItems
        notifyDataSetChanged()
    }
}