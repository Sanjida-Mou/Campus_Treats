package com.baust.cafe.activities

import android.os.Bundle
import com.baust.cafe.R
import com.baust.cafe.adapters.UserNotificationAdapter
import com.baust.cafe.models.UserNotification
import com.google.firebase.database.*
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.widget.ImageView
import android.content.Intent
import com.google.firebase.auth.FirebaseAuth

class UserNotificationsActivity : BaseUserActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: UserNotificationAdapter
    private val notificationList = mutableListOf<UserNotification>()
    private lateinit var database: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_notifications)

        recyclerView = findViewById(R.id.userNotificationsRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        
        adapter = UserNotificationAdapter(notificationList) { notification ->
            handleNotificationClick(notification)
        }
        recyclerView.adapter = adapter

        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        database = FirebaseDatabase.getInstance().getReference("UserNotifications").child(userId)
        
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
                        val notify = notifySnapshot.getValue(UserNotification::class.java)
                        notify?.let { notificationList.add(it) }
                    }
                } else {
                    android.widget.Toast.makeText(this@UserNotificationsActivity, "No notifications found", android.widget.Toast.LENGTH_SHORT).show()
                }
                notificationList.sortByDescending { it.timestamp }
                adapter.updateList(notificationList)
            }
            override fun onCancelled(error: DatabaseError) {
                android.widget.Toast.makeText(this@UserNotificationsActivity, "Error: ${error.message}", android.widget.Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun handleNotificationClick(notification: UserNotification) {
        if (notification.type == "order_status") {
            startActivity(Intent(this, OrderHistoryActivity::class.java))
        } else {
            startActivity(Intent(this, MenuActivity::class.java))
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
