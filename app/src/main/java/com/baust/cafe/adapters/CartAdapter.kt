package com.baust.cafe.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.baust.cafe.R
import com.baust.cafe.models.CartItem
import java.util.Locale

class CartAdapter(
    private var cartItems: MutableList<CartItem>,
    private val onQuantityChanged: () -> Unit
) : RecyclerView.Adapter<CartAdapter.CartViewHolder>() {

    class CartViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nameText: TextView = itemView.findViewById(R.id.cartItemName)
        val priceText: TextView = itemView.findViewById(R.id.cartItemPrice)
        val quantityText: TextView = itemView.findViewById(R.id.quantityText)
        val plusButton: ImageButton = itemView.findViewById(R.id.plusButton)
        val minusButton: ImageButton = itemView.findViewById(R.id.minusButton)
        val itemImage: ImageView = itemView.findViewById(R.id.cartItemImage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CartViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_cart, parent, false)
        return CartViewHolder(view)
    }

    override fun onBindViewHolder(holder: CartViewHolder, position: Int) {
        val item = cartItems[position]
        holder.nameText.text = item.itemName
        holder.priceText.text = String.format(Locale.getDefault(), "$%.2f", item.price)
        holder.quantityText.text = item.quantity.toString()

        holder.plusButton.setOnClickListener {
            val newItem = item.copy(quantity = item.quantity + 1)
            cartItems[position] = newItem
            notifyItemChanged(position)
            onQuantityChanged()
        }

        holder.minusButton.setOnClickListener {
            if (item.quantity > 1) {
                val newItem = item.copy(quantity = item.quantity - 1)
                cartItems[position] = newItem
                notifyItemChanged(position)
                onQuantityChanged()
            } else {
                cartItems.removeAt(position)
                notifyItemRemoved(position)
                notifyItemRangeChanged(position, cartItems.size)
                onQuantityChanged()
            }
        }
    }

    override fun getItemCount(): Int = cartItems.size

    fun getItems(): List<CartItem> = cartItems
}