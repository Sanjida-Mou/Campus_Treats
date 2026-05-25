package com.baust.cafe.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.baust.cafe.R
import com.baust.cafe.models.Order
import com.bumptech.glide.Glide
import com.google.firebase.database.FirebaseDatabase
import java.util.Locale

class AdminOrderAdapter(
    private var orders: List<Order>,
    private val onUpdateStatus: (Order) -> Unit,
    private val onHandleCancellation: (Order) -> Unit,
    private val onUserClick: (String) -> Unit
) : RecyclerView.Adapter<AdminOrderAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.orderCustomerName)
        val profileImage: ImageView = view.findViewById(R.id.orderProfileImage)
        val items: TextView = view.findViewById(R.id.orderItems)
        val address: TextView = view.findViewById(R.id.orderAddress)
        val payment: TextView = view.findViewById(R.id.orderPayment)
        val total: TextView = view.findViewById(R.id.orderTotal)
        val status: TextView = view.findViewById(R.id.orderStatusAdmin)
        val btnUpdateStatus: Button = view.findViewById(R.id.btnUpdateStatus)
        val btnHandleCancellation: Button = view.findViewById(R.id.btnHandleCancellation)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_order_admin, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val order = orders[position]
        holder.name.text = "${order.studentName} (${order.studentPhone})"
        
        // Load User Profile Image from Database
        FirebaseDatabase.getInstance().getReference("Users").child(order.studentId)
            .child("profileImage").get().addOnSuccessListener {
                val imageUrl = it.value.toString()
                if (imageUrl.isNotEmpty() && imageUrl != "null") {
                    if (imageUrl.startsWith("http")) {
                        Glide.with(holder.itemView.context).load(imageUrl)
                            .placeholder(R.drawable.ic_person).into(holder.profileImage)
                    } else {
                        try {
                            val imageBytes = android.util.Base64.decode(imageUrl, android.util.Base64.DEFAULT)
                            val bitmap = android.graphics.BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                            holder.profileImage.setImageBitmap(bitmap)
                        } catch (e: Exception) {
                            holder.profileImage.setImageResource(R.drawable.ic_person)
                        }
                    }
                }
            }

        holder.name.setOnClickListener { onUserClick(order.studentId) }
        holder.profileImage.setOnClickListener { onUserClick(order.studentId) }

        val itemsText = order.items.joinToString { "${it.itemName} x ${it.quantity}" }
        holder.items.text = "Items: $itemsText"
        
        holder.address.text = "Location: ${order.deliveryAddress}"
        holder.payment.text = order.specialInstructions // We stored payment method here
        holder.total.text = String.format(Locale.getDefault(), "Total: Tk %.2f", order.totalAmount)
        
        holder.status.text = order.status.replace("_", " ").uppercase()

        // Status Colors
        when (order.status.lowercase()) {
            "pending" -> holder.status.setTextColor(Color.parseColor("#FF9800"))
            "preparing" -> holder.status.setTextColor(Color.parseColor("#2196F3"))
            "ready" -> holder.status.setTextColor(Color.parseColor("#4CAF50"))
            "delivered" -> holder.status.setTextColor(Color.parseColor("#2E7D32"))
            "cancelled" -> holder.status.setTextColor(Color.parseColor("#F44336"))
            else -> holder.status.setTextColor(Color.GRAY)
        }

        // Hide buttons for finalized orders
        if (order.status.lowercase() == "delivered" || order.status.lowercase() == "cancelled") {
            holder.btnUpdateStatus.visibility = View.GONE
            holder.btnHandleCancellation.visibility = View.GONE
        } else {
            holder.btnUpdateStatus.visibility = View.VISIBLE
            holder.btnUpdateStatus.text = "Update Status"
            holder.btnUpdateStatus.setBackgroundColor(Color.parseColor("#4CAF50"))
            holder.btnHandleCancellation.visibility = View.GONE
        }

        holder.btnUpdateStatus.setOnClickListener {
            onUpdateStatus(order)
        }

        holder.btnHandleCancellation.setOnClickListener {
            onHandleCancellation(order)
        }
    }

    override fun getItemCount() = orders.size

    fun updateOrders(newOrders: List<Order>) {
        orders = newOrders
        notifyDataSetChanged()
    }
}
