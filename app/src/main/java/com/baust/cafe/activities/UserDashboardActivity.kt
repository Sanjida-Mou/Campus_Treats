package com.baust.cafe.activities

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.baust.cafe.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class UserDashboardActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var welcomeUserText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_dashboard)

        auth = FirebaseAuth.getInstance()
        welcomeUserText = findViewById(R.id.welcomeUserText)

        // Dashboard Cards
        val browseMenuCard = findViewById<CardView>(R.id.browseMenuCard)
        val myOrdersCard = findViewById<CardView>(R.id.myOrdersCard)
        val myCartCard = findViewById<CardView>(R.id.myCartCard)
        val giveReviewCard = findViewById<CardView>(R.id.giveReviewCard)

        // Bottom Navigation
        val navHome = findViewById<ImageView>(R.id.navHome)
        val navHistory = findViewById<ImageView>(R.id.navHistory)

        browseMenuCard.setOnClickListener {
            startActivity(Intent(this, MenuActivity::class.java))
        }

        myOrdersCard.setOnClickListener {
            startActivity(Intent(this, OrderHistoryActivity::class.java))
        }

        myCartCard.setOnClickListener {
            startActivity(Intent(this, CartActivity::class.java))
        }

        giveReviewCard.setOnClickListener {
            Toast.makeText(this, "Review feature coming soon", Toast.LENGTH_SHORT).show()
        }

        navHome.setOnClickListener {
            startActivity(Intent(this, HomeActivity::class.java))
            finish()
        }

        navHistory.setOnClickListener {
            startActivity(Intent(this, OrderHistoryActivity::class.java))
        }

        loadUserData()
    }

    private fun loadUserData() {
        val userId = auth.currentUser?.uid ?: return
        val database = FirebaseDatabase.getInstance().getReference("Users").child(userId)

        database.get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                val name = snapshot.child("name").value.toString()
                welcomeUserText.text = "Welcome back, $name"
            }
        }
    }
}