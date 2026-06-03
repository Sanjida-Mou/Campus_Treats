package com.baust.cafe.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.baust.cafe.R
import com.baust.cafe.models.UserNotification
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

abstract class BaseAdminActivity : AppCompatActivity() {

    protected lateinit var notificationBadge: TextView
    private lateinit var badgeDb: DatabaseReference
    private var badgeListener: ValueEventListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    protected fun setupBottomNavigation(currentId: Int) {
        val navHome = findViewById<ImageView>(R.id.navHome)
        val navNotifications = findViewById<ImageView>(R.id.navNotifications)
        notificationBadge = findViewById(R.id.notificationBadge)

        navHome?.setOnClickListener {
            if (currentId != R.id.navHome) {
                startActivity(Intent(this, AdminDashboardActivity::class.java))
                finish()
            }
        }

        navNotifications?.setOnClickListener {
            if (currentId != R.id.navNotifications) {
                startActivity(Intent(this, AdminNotificationsActivity::class.java))
                finish()
            }
        }

        setupBadgeListener()
    }

    private fun setupBadgeListener() {
        badgeDb = FirebaseDatabase.getInstance().getReference("AdminNotifications")
        badgeListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var unreadCount = 0
                for (notifySnapshot in snapshot.children) {
                    val read = notifySnapshot.child("read").getValue(Boolean::class.java) ?: false
                    if (!read) unreadCount++
                }
                updateBadgeUI(unreadCount)
            }
            override fun onCancelled(error: DatabaseError) {
                // Ignore permission errors during logout
                if (FirebaseAuth.getInstance().currentUser != null && error.code != DatabaseError.PERMISSION_DENIED) {
                    android.util.Log.e("BaseAdmin", "Database Error: ${error.message}")
                }
            }
        }
        badgeDb.addValueEventListener(badgeListener!!)
    }

    private fun updateBadgeUI(count: Int) {
        if (::notificationBadge.isInitialized) {
            if (count > 0) {
                notificationBadge.text = count.toString()
                notificationBadge.visibility = View.VISIBLE
            } else {
                notificationBadge.visibility = View.GONE
            }
        }
    }

    protected fun sendUserNotification(userId: String, title: String, message: String, type: String) {
        val userNotifyDb = FirebaseDatabase.getInstance().getReference("UserNotifications").child(userId)
        val id = userNotifyDb.push().key ?: return
        val notification = UserNotification(id, title, message, type, System.currentTimeMillis(), false)
        userNotifyDb.child(id).setValue(notification)
    }

    protected fun broadcastStockNotification(itemName: String, isAvailable: Boolean) {
        val usersDb = FirebaseDatabase.getInstance().getReference("Users")
        val status = if (isAvailable) "Available now!" else "Out of Stock"
        val message = "$itemName is $status"
        
        usersDb.get().addOnSuccessListener { snapshot ->
            for (userSnapshot in snapshot.children) {
                val userId = userSnapshot.key ?: continue
                sendUserNotification(userId, "Food Availability Update", message, "stock")
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::badgeDb.isInitialized && badgeListener != null) {
            badgeDb.removeEventListener(badgeListener!!)
        }
    }
}
