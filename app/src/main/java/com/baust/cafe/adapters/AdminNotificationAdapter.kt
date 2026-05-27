package com.baust.cafe.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.baust.cafe.R
import com.baust.cafe.models.AdminNotification
import java.text.SimpleDateFormat
import java.util.*

class AdminNotificationAdapter(
    private var notifications: List<AdminNotification>,
    private val onItemClick: (AdminNotification) -> Unit
) : RecyclerView.Adapter<AdminNotificationAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.notifyTitle)
        val message: TextView = view.findViewById(R.id.notifyMessage)
        val time: TextView = view.findViewById(R.id.notifyTime)
        val icon: ImageView = view.findViewById(R.id.notificationTypeIcon)
        val iconBg: View = view.findViewById(R.id.notificationIcon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_admin_notification, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val notify = notifications[position]
        holder.title.text = notify.title
        holder.message.text = notify.message
        
        val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
        holder.time.text = sdf.format(Date(notify.timestamp))

        when (notify.type) {
            "order" -> {
                holder.icon.setImageResource(R.drawable.ic_history)
                holder.iconBg.backgroundTintList = android.content.res.ColorStateList.valueOf(holder.itemView.context.getColor(R.color.card_blue))
            }
            "cancellation" -> {
                holder.icon.setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
                holder.iconBg.backgroundTintList = android.content.res.ColorStateList.valueOf(holder.itemView.context.getColor(R.color.error_red))
            }
            "review" -> {
                holder.icon.setImageResource(R.drawable.ic_plus)
                holder.icon.rotation = 45f
                holder.iconBg.backgroundTintList = android.content.res.ColorStateList.valueOf(holder.itemView.context.getColor(R.color.card_pink))
            }
        }

        holder.itemView.setOnClickListener { onItemClick(notify) }
        
        // Highlight unread vs read
        if (!notify.read) {
            holder.title.setTextColor(holder.itemView.context.getColor(R.color.text_black))
            holder.title.setTypeface(null, android.graphics.Typeface.BOLD)
            holder.message.setTextColor(holder.itemView.context.getColor(R.color.text_black))
            holder.itemView.alpha = 1.0f
        } else {
            holder.title.setTextColor(holder.itemView.context.getColor(R.color.text_hint))
            holder.title.setTypeface(null, android.graphics.Typeface.NORMAL)
            holder.message.setTextColor(holder.itemView.context.getColor(R.color.text_hint))
            holder.itemView.alpha = 0.8f
        }
    }

    override fun getItemCount() = notifications.size

    fun updateList(newList: List<AdminNotification>) {
        notifications = newList
        notifyDataSetChanged()
    }
}
