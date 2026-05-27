package com.baust.cafe.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.baust.cafe.R
import com.baust.cafe.models.UserNotification
import java.text.SimpleDateFormat
import java.util.*

class UserNotificationAdapter(
    private var notifications: List<UserNotification>,
    private val onItemClick: (UserNotification) -> Unit
) : RecyclerView.Adapter<UserNotificationAdapter.ViewHolder>() {

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
            "stock" -> {
                holder.icon.setImageResource(R.drawable.ic_menu)
                holder.iconBg.backgroundTintList = android.content.res.ColorStateList.valueOf(holder.itemView.context.getColor(R.color.card_orange))
            }
            "order_status" -> {
                holder.icon.setImageResource(R.drawable.ic_history)
                holder.iconBg.backgroundTintList = android.content.res.ColorStateList.valueOf(holder.itemView.context.getColor(R.color.primary_green))
            }
        }

        holder.itemView.setOnClickListener { onItemClick(notify) }
        
        if (!notify.read) {
            holder.title.setTypeface(null, android.graphics.Typeface.BOLD)
            holder.itemView.alpha = 1.0f
        } else {
            holder.title.setTypeface(null, android.graphics.Typeface.NORMAL)
            holder.itemView.alpha = 0.7f
        }
    }

    override fun getItemCount() = notifications.size

    fun updateList(newList: List<UserNotification>) {
        notifications = newList
        notifyDataSetChanged()
    }
}
