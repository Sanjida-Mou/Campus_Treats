package com.baust.cafe.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.baust.cafe.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class HomeActivity : AppCompatActivity() {

    private lateinit var userNameText: TextView
    private lateinit var auth: FirebaseAuth
    private lateinit var cartIcon: View
    private lateinit var navHome: ImageView
    private lateinit var navBookmark: ImageView
    private lateinit var navHistory: ImageView
    private lateinit var navNotifications: ImageView
    private lateinit var navProfile: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        auth = FirebaseAuth.getInstance()
        
        // Initialize UI Elements
        userNameText = findViewById(R.id.userNameText)
        cartIcon = findViewById(R.id.cartIcon)
        navHome = findViewById(R.id.navHome)
        navBookmark = findViewById(R.id.navBookmark)
        navHistory = findViewById(R.id.navHistory)
        navNotifications = findViewById(R.id.navNotifications)
        navProfile = findViewById(R.id.navProfile)

        setupNavigation()
        loadUserData()
    }

    private fun setupNavigation() {
        // Top Cart Icon
        cartIcon.setOnClickListener {
            startActivity(Intent(this, CartActivity::class.java))
        }

        // Bottom Navigation
        navHome.setOnClickListener {
            // Already on Home
        }

        navBookmark.setOnClickListener {
            startActivity(Intent(this, MenuActivity::class.java))
        }

        navHistory.setOnClickListener {
            startActivity(Intent(this, OrderHistoryActivity::class.java))
        }

        navNotifications.setOnClickListener {
            Toast.makeText(this, "No new notifications", Toast.LENGTH_SHORT).show()
        }

        navProfile.setOnClickListener {
            startActivity(Intent(this, UserDashboardActivity::class.java))
        }
    }

    private fun loadUserData() {
        val userId = auth.currentUser?.uid ?: return
        val database = FirebaseDatabase.getInstance().getReference("Users").child(userId)

        database.get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                val name = snapshot.child("name").value.toString()
                userNameText.text = "$name - Campus Treats"
            }
        }.addOnFailureListener {
            Toast.makeText(this, "Failed to load profile", Toast.LENGTH_SHORT).show()
        }
    }
}