package com.baust.cafe.activities

import android.os.Bundle
import com.baust.cafe.R
import com.baust.cafe.adapters.AdminNotificationAdapter
import com.baust.cafe.models.AdminNotification
import com.google.firebase.database.*
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.widget.ImageView
import android.content.Intent

class AdminNotificationsActivity : BaseAdminActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: AdminNotificationAdapter
    private val notificationList = mutableListOf<AdminNotification>()
    private lateinit var database: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_notifications)

        recyclerView = findViewById(R.id.adminNotificationsRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        
        adapter = AdminNotificationAdapter(notificationList) { notification ->
            handleNotificationClick(notification)
        }
        recyclerView.adapter = adapter

        database = FirebaseDatabase.getInstance().getReference("AdminNotifications")
        fetchNotifications()
        setupBottomNavigation(R.id.navNotifications)
        markAllAsRead()
    }

    private fun fetchNotifications() {
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                notificationList.clear()
                if (snapshot.exists()) {
                    for (notifySnapshot in snapshot.children) {
                        val notify = notifySnapshot.getValue(AdminNotification::class.java)
                        notify?.let { notificationList.add(it) }
                    }
                    notificationList.sortByDescending { it.timestamp }
                    adapter.updateList(notificationList)
                } else {
                    android.widget.Toast.makeText(this@AdminNotificationsActivity, "No notifications yet", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
            override fun onCancelled(error: DatabaseError) {
                android.widget.Toast.makeText(this@AdminNotificationsActivity, "Database Error: ${error.message}", android.widget.Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun handleNotificationClick(notification: AdminNotification) {
        when (notification.type) {
            "order", "cancellation" -> {
                startActivity(Intent(this, ViewOrdersActivity::class.java))
            }
            "review" -> {
                startActivity(Intent(this, ViewReviewsActivity::class.java))
            }
        }
    }

    private fun markAllAsRead() {
        database.get().addOnSuccessListener { snapshot ->
            for (notifySnapshot in snapshot.children) {
                val read = notifySnapshot.child("read").getValue(Boolean::class.java) ?: false
                if (!read) {
                    notifySnapshot.ref.child("read").setValue(true)
                }
            }
        }
    }
}
