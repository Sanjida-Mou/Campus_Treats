package com.baust.cafe.adapters

import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.baust.cafe.R
import com.baust.cafe.models.CartItem
import com.bumptech.glide.Glide
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
        holder.priceText.text = String.format(Locale.getDefault(), "Tk. %.2f", item.price)
        holder.quantityText.text = item.quantity.toString()

        // Support for both URL and Base64
        if (item.imageUrl.isNotEmpty()) {
            if (item.imageUrl.startsWith("http")) {
                Glide.with(holder.itemView.context)
                    .load(item.imageUrl)
                    .placeholder(R.drawable.cafe_logo)
                    .into(holder.itemImage)
            } else {
                try {
                    val imageBytes = Base64.decode(item.imageUrl, Base64.DEFAULT)
                    val decodedImage = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                    holder.itemImage.setImageBitmap(decodedImage)
                } catch (e: Exception) {
                    holder.itemImage.setImageResource(R.drawable.cafe_logo)
                }
            }
        } else {
            holder.itemImage.setImageResource(R.drawable.cafe_logo)
        }

        holder.plusButton.setOnClickListener {
            val currentPos = holder.adapterPosition
            if (currentPos != RecyclerView.NO_POSITION) {
                val item = cartItems[currentPos]
                val newItem = item.copy(quantity = item.quantity + 1)
                cartItems[currentPos] = newItem
                notifyItemChanged(currentPos)
                onQuantityChanged()
            }
        }

        holder.minusButton.setOnClickListener {
            val currentPos = holder.adapterPosition
            if (currentPos != RecyclerView.NO_POSITION) {
                val item = cartItems[currentPos]
                if (item.quantity > 1) {
                    val newItem = item.copy(quantity = item.quantity - 1)
                    cartItems[currentPos] = newItem
                    notifyItemChanged(currentPos)
                    onQuantityChanged()
                } else {
                    cartItems.removeAt(currentPos)
                    notifyItemRemoved(currentPos)
                    notifyItemRangeChanged(currentPos, cartItems.size)
                    onQuantityChanged()
                }
            }
        }
    }

    override fun getItemCount(): Int = cartItems.size

    fun getItems(): List<CartItem> = cartItems
}
