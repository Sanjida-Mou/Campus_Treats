package com.baust.cafe.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.baust.cafe.R
import com.baust.cafe.models.Order
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class OrderAdapter(
    private var orders: List<Order>,
    private val onActionClick: (Order) -> Unit,
    private val onVerifyRefundClick: (Order) -> Unit
) : RecyclerView.Adapter<OrderAdapter.OrderViewHolder>() {

    class OrderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val orderIdText: TextView = itemView.findViewById(R.id.orderIdText)
        val orderStatusText: TextView = itemView.findViewById(R.id.orderStatusText)
        val orderItemsText: TextView = itemView.findViewById(R.id.orderItemsText)
        val orderDateText: TextView = itemView.findViewById(R.id.orderDateText)
        val orderTotalText: TextView = itemView.findViewById(R.id.orderTotalText)
        val actionButton: Button = itemView.findViewById(R.id.orderActionButton)
        val verifyRefundButton: Button = itemView.findViewById(R.id.orderVerifyRefundButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_order, parent, false)
        return OrderViewHolder(view)
    }

    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        val order = orders[position]
        
        holder.orderIdText.text = "Order #${order.orderId.takeLast(6).uppercase()}"
        
        // Show status with refund info if applicable
        var statusStr = order.status.replace("_", " ").uppercase()
        if (order.status == "cancelled" && order.refundStatus == "sent_to_user") {
            statusStr += " (REFUND SENT)"
        }
        holder.orderStatusText.text = statusStr
        
        val itemsSummary = order.items.joinToString { "${it.itemName} x${it.quantity}" }
        holder.orderItemsText.text = itemsSummary
        
        val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
        holder.orderDateText.text = sdf.format(Date(order.orderTime))
        
        holder.orderTotalText.text = String.format(Locale.getDefault(), "Total: Tk. %.2f", order.totalAmount)

        // Action Button Logic
        when (order.status.lowercase()) {
            "pending", "preparing", "pending_verification" -> {
                holder.actionButton.text = "Cancel Order"
                holder.actionButton.visibility = View.VISIBLE
                holder.actionButton.setBackgroundColor(Color.parseColor("#F44336"))
            }
            "ready" -> {
                holder.actionButton.visibility = View.GONE 
            }
            "delivered", "handed_over", "cancelled" -> {
                holder.actionButton.text = "Delete History"
                holder.actionButton.visibility = View.VISIBLE
                holder.actionButton.setBackgroundColor(Color.GRAY)
            }
            else -> {
                holder.actionButton.visibility = View.GONE
            }
        }

        // Verify Refund Button Logic
        if (order.status == "cancelled" && order.refundStatus == "sent_to_user") {
            holder.verifyRefundButton.visibility = View.VISIBLE
            holder.actionButton.visibility = View.GONE // Hide Delete until verified
        } else {
            holder.verifyRefundButton.visibility = View.GONE
        }

        holder.actionButton.setOnClickListener { onActionClick(order) }
        holder.verifyRefundButton.setOnClickListener { onVerifyRefundClick(order) }

        // Status Colors
        when (order.status.lowercase()) {
            "pending" -> holder.orderStatusText.setTextColor(Color.parseColor("#FF9800"))
            "pending_verification" -> holder.orderStatusText.setTextColor(Color.parseColor("#E91E63"))
            "preparing" -> holder.orderStatusText.setTextColor(Color.parseColor("#2196F3"))
            "ready" -> holder.orderStatusText.setTextColor(Color.parseColor("#4CAF50"))
            "delivered" -> holder.orderStatusText.setTextColor(Color.parseColor("#2E7D32"))
            "handed_over" -> holder.orderStatusText.setTextColor(Color.parseColor("#0288D1"))
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
