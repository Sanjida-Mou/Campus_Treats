package com.baust.cafe.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.baust.cafe.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

abstract class BaseUserActivity : AppCompatActivity() {

    protected lateinit var notificationBadge: TextView
    private lateinit var badgeDb: DatabaseReference
    private var badgeListener: ValueEventListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    protected fun setupBottomNavigation(currentId: Int) {
        val navHome = findViewById<ImageView>(R.id.navHome)
        val navCart = findViewById<ImageView>(R.id.navCart)
        val navBookmark = findViewById<ImageView>(R.id.navBookmark)
        val navNotifications = findViewById<ImageView>(R.id.navNotifications)
        val navProfile = findViewById<ImageView>(R.id.navProfile)
        notificationBadge = findViewById(R.id.notificationBadge)

        navHome?.setOnClickListener {
            if (currentId != R.id.navHome) {
                startActivity(Intent(this, HomeActivity::class.java))
                finish()
            }
        }

        navCart?.setOnClickListener {
            if (currentId != R.id.navCart) {
                startActivity(Intent(this, CartActivity::class.java))
                finish()
            }
        }

        navBookmark?.setOnClickListener {
            if (currentId != R.id.navBookmark) {
                startActivity(Intent(this, MenuActivity::class.java))
                finish()
            }
        }

        navNotifications?.setOnClickListener {
            if (currentId != R.id.navNotifications) {
                startActivity(Intent(this, UserNotificationsActivity::class.java))
                finish()
            }
        }

        navProfile?.setOnClickListener {
            if (currentId != R.id.navProfile) {
                startActivity(Intent(this, UserDashboardActivity::class.java))
                finish()
            }
        }

        setupBadgeListener()
    }

    private fun setupBadgeListener() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        badgeDb = FirebaseDatabase.getInstance().getReference("UserNotifications").child(userId)
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
                    android.util.Log.e("BaseUser", "Database Error: ${error.message}")
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

    override fun onDestroy() {
        super.onDestroy()
        if (::badgeDb.isInitialized) {
            badgeListener?.let { badgeDb.removeEventListener(it) }
        }
    }
}
