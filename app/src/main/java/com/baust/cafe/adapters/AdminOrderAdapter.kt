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
    private val onVerifyPayment: (Order) -> Unit,
    private val onRejectPayment: (Order) -> Unit,
    private val onUserClick: (String) -> Unit
) : RecyclerView.Adapter<AdminOrderAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.orderCustomerName)
        val profileImage: ImageView = view.findViewById(R.id.orderProfileImage)
        val items: TextView = view.findViewById(R.id.orderItems)
        val address: TextView = view.findViewById(R.id.orderAddress)
        val payment: TextView = view.findViewById(R.id.orderPayment)
        val trxId: TextView = view.findViewById(R.id.orderTrxId)
        val total: TextView = view.findViewById(R.id.orderTotal)
        val status: TextView = view.findViewById(R.id.orderStatusAdmin)
        val btnUpdateStatus: Button = view.findViewById(R.id.btnUpdateStatus)
        val btnVerifyPayment: Button = view.findViewById(R.id.btnVerifyPayment)
        val btnRejectPayment: Button = view.findViewById(R.id.btnRejectPayment)
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
        
        // Payment Info & Status
        val payMethod = order.specialInstructions
        holder.payment.text = payMethod
        
        if (order.transactionId.isNotEmpty()) {
            holder.trxId.text = "TrxID: ${order.transactionId} [${order.paymentStatus.replace("_", " ").uppercase()}]"
            holder.trxId.visibility = View.VISIBLE
            
            if (order.paymentStatus == "pending_verification") {
                holder.trxId.setTextColor(Color.parseColor("#E91E63")) // Pink for alert
            } else if (order.paymentStatus == "rejected") {
                holder.trxId.setTextColor(Color.RED)
            } else {
                holder.trxId.setTextColor(Color.parseColor("#2E7D32")) // Green for verified
            }
        } else {
            holder.trxId.visibility = View.GONE
        }

        holder.total.text = String.format(Locale.getDefault(), "Total: Tk %.2f", order.totalAmount)
        holder.status.text = order.status.replace("_", " ").uppercase()

        // Status Colors
        when (order.status.lowercase()) {
            "pending" -> holder.status.setTextColor(Color.parseColor("#FF9800"))
            "pending_verification" -> holder.status.setTextColor(Color.parseColor("#E91E63"))
            "preparing" -> holder.status.setTextColor(Color.parseColor("#2196F3"))
            "ready" -> holder.status.setTextColor(Color.parseColor("#4CAF50"))
            "delivered" -> holder.status.setTextColor(Color.parseColor("#2E7D32"))
            "cancelled" -> holder.status.setTextColor(Color.parseColor("#F44336"))
            else -> holder.status.setTextColor(Color.GRAY)
        }

        // Admin Action Buttons
        if (order.paymentStatus == "pending_verification") {
            holder.btnVerifyPayment.visibility = View.VISIBLE
            holder.btnRejectPayment.visibility = View.VISIBLE
            holder.btnUpdateStatus.visibility = View.GONE
        } else {
            holder.btnVerifyPayment.visibility = View.GONE
            holder.btnRejectPayment.visibility = View.GONE
            
            // Show update status only for active/verified orders
            if (order.status == "delivered" || order.status == "cancelled" || order.paymentStatus == "rejected") {
                holder.btnUpdateStatus.visibility = View.GONE
            } else {
                holder.btnUpdateStatus.visibility = View.VISIBLE
            }
        }

        holder.btnUpdateStatus.setOnClickListener { onUpdateStatus(order) }
        holder.btnVerifyPayment.setOnClickListener { onVerifyPayment(order) }
        holder.btnRejectPayment.setOnClickListener { onRejectPayment(order) }
    }

    override fun getItemCount() = orders.size

    fun updateOrders(newOrders: List<Order>) {
        orders = newOrders
        notifyDataSetChanged()
    }
}
