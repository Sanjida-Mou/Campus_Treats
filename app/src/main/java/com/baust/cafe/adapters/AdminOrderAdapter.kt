package com.baust.cafe.adapters

import android.graphics.BitmapFactory
import android.graphics.Color
import android.util.Base64
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
    private val onReturnPayment: (Order) -> Unit,
    private val onDeleteOrder: (Order) -> Unit,
    private val onUserClick: (String) -> Unit
) : RecyclerView.Adapter<AdminOrderAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.orderCustomerName)
        val phone: TextView = view.findViewById(R.id.orderCustomerPhone)
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
        val btnReturnPayment: Button = view.findViewById(R.id.btnReturnPayment)
        val btnDeleteOrder: Button = view.findViewById(R.id.btnDeleteOrder)
        val adminActionLabel: TextView = view.findViewById(R.id.adminActionLabel)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_order_admin, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val order = orders[position]
        holder.name.text = order.studentName
        holder.phone.text = if (order.studentPhone.isNotEmpty()) "(${order.studentPhone})" else ""
        
        // Load User Profile Image
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
                            val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
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
        holder.items.text = itemsText
        
        holder.address.text = "Location: ${order.deliveryAddress}"
        
        // Payment Info & Status
        val isOnlinePayment = order.specialInstructions.contains("bKash", ignoreCase = true) || 
                             order.specialInstructions.contains("Nagad", ignoreCase = true)
        
        holder.payment.text = "Payment: ${order.specialInstructions.replace("Payment: ", "")}"
        
        if (order.transactionId.isNotEmpty()) {
            holder.trxId.text = "TrxID: ${order.transactionId} [${order.paymentStatus.replace("_", " ").uppercase()}]"
            holder.trxId.visibility = View.VISIBLE
            
            when (order.paymentStatus) {
                "pending_verification" -> holder.trxId.setTextColor(Color.parseColor("#E91E63"))
                "rejected" -> holder.trxId.setTextColor(Color.RED)
                else -> holder.trxId.setTextColor(Color.parseColor("#2E7D32"))
            }
        } else {
            holder.trxId.visibility = View.GONE
        }

        holder.total.text = String.format(Locale.getDefault(), "Total: Tk %.2f", order.totalAmount)
        holder.status.text = order.status.replace("_", " ").uppercase()

        // Status Colors
        when (order.status.lowercase()) {
            "pending" -> {
                holder.status.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#FFF3E0"))
                holder.status.setTextColor(Color.parseColor("#FF9800"))
            }
            "pending_verification" -> {
                holder.status.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#FCE4EC"))
                holder.status.setTextColor(Color.parseColor("#E91E63"))
            }
            "preparing" -> {
                holder.status.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#E3F2FD"))
                holder.status.setTextColor(Color.parseColor("#2196F3"))
            }
            "ready" -> {
                holder.status.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#E8F5E9"))
                holder.status.setTextColor(Color.parseColor("#4CAF50"))
            }
            "delivered" -> {
                holder.status.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#C8E6C9"))
                holder.status.setTextColor(Color.parseColor("#2E7D32"))
            }
            "cancelled" -> {
                holder.status.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#FFEBEE"))
                holder.status.setTextColor(Color.parseColor("#F44336"))
            }
        }

        // --- BUTTON VISIBILITY LOGIC ---
        
        // Path A: Verification Needed
        if (order.paymentStatus == "pending_verification") {
            holder.adminActionLabel.text = "Payment Verification:"
            holder.adminActionLabel.visibility = View.VISIBLE
            holder.btnVerifyPayment.visibility = View.VISIBLE
            holder.btnRejectPayment.visibility = View.VISIBLE
            holder.btnUpdateStatus.visibility = View.GONE
            holder.btnReturnPayment.visibility = View.GONE
            holder.btnDeleteOrder.visibility = View.GONE
        } 
        // Path B: Cancelled but money was taken -> REFUND REQUIRED
        else if (order.status == "cancelled" && isOnlinePayment && order.paymentStatus == "verified") {
            holder.adminActionLabel.text = "Refund Action Required:"
            holder.adminActionLabel.visibility = View.VISIBLE
            holder.btnReturnPayment.visibility = View.VISIBLE
            holder.btnDeleteOrder.visibility = View.GONE // FORBIDDEN TO DELETE
            
            holder.btnVerifyPayment.visibility = View.GONE
            holder.btnRejectPayment.visibility = View.GONE
            holder.btnUpdateStatus.visibility = View.GONE
            
            if (order.refundStatus == "sent_to_user") {
                holder.btnReturnPayment.text = "Sent (Waiting User)"
                holder.btnReturnPayment.isEnabled = false
                holder.btnReturnPayment.alpha = 0.5f
            } else {
                holder.btnReturnPayment.text = "Refund Money"
                holder.btnReturnPayment.isEnabled = true
                holder.btnReturnPayment.alpha = 1.0f
            }
        }
        // Path C: Standard Management
        else {
            holder.btnVerifyPayment.visibility = View.GONE
            holder.btnRejectPayment.visibility = View.GONE
            holder.btnReturnPayment.visibility = View.GONE
            
            // Can update status only if active
            if (order.status == "delivered" || order.status == "cancelled") {
                holder.btnUpdateStatus.visibility = View.GONE
                holder.btnDeleteOrder.visibility = View.VISIBLE
                holder.adminActionLabel.visibility = View.GONE
            } else {
                holder.btnUpdateStatus.visibility = View.VISIBLE
                holder.btnDeleteOrder.visibility = View.GONE
                holder.adminActionLabel.visibility = View.GONE
            }
        }

        holder.btnUpdateStatus.setOnClickListener { onUpdateStatus(order) }
        holder.btnVerifyPayment.setOnClickListener { onVerifyPayment(order) }
        holder.btnRejectPayment.setOnClickListener { onRejectPayment(order) }
        holder.btnReturnPayment.setOnClickListener { onReturnPayment(order) }
        holder.btnDeleteOrder.setOnClickListener { onDeleteOrder(order) }
    }

    override fun getItemCount() = orders.size

    fun updateOrders(newOrders: List<Order>) {
        orders = newOrders
        notifyDataSetChanged()
    }
}
