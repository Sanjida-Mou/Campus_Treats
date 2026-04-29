package com.baust.cafe.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.baust.cafe.R
import com.baust.cafe.models.Order
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class OrderAdapter(private var orders: List<Order>) : RecyclerView.Adapter<OrderAdapter.OrderViewHolder>() {

    class OrderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val orderIdText: TextView = itemView.findViewById(R.id.orderIdText)
        val orderStatusText: TextView = itemView.findViewById(R.id.orderStatusText)
        val orderItemsText: TextView = itemView.findViewById(R.id.orderItemsText)
        val orderDateText: TextView = itemView.findViewById(R.id.orderDateText)
        val orderTotalText: TextView = itemView.findViewById(R.id.orderTotalText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_order, parent, false)
        return OrderViewHolder(view)
    }

    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        val order = orders[position]
        
        holder.orderIdText.text = "Order #${order.orderId.takeLast(6).uppercase()}"
        holder.orderStatusText.text = order.status.uppercase()
        
        // Format items string
        val itemsSummary = order.items.joinToString { "${it.itemName} x${it.quantity}" }
        holder.orderItemsText.text = itemsSummary
        
        // Format Date
        val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
        holder.orderDateText.text = sdf.format(Date(order.orderTime))
        
        holder.orderTotalText.text = "Total: $${String.format("%.2f", order.totalAmount)}"

        // Status Colors
        when (order.status.lowercase()) {
            "pending" -> holder.orderStatusText.setTextColor(Color.parseColor("#FF9800"))
            "preparing" -> holder.orderStatusText.setTextColor(Color.parseColor("#2196F3"))
            "ready" -> holder.orderStatusText.setTextColor(Color.parseColor("#4CAF50"))
            "delivered" -> holder.orderStatusText.setTextColor(Color.parseColor("#2E7D32"))
            "cancelled" -> holder.orderStatusText.setTextColor(Color.parseColor("#F44336"))
            else -> holder.orderStatusText.setTextColor(Color.GRAY)
        }
    }

    override fun getItemCount(): Int = orders.size

    fun updateOrders(newOrders: List<Order>) {
        orders = newOrders
        notifyDataSetChanged()
    }
}