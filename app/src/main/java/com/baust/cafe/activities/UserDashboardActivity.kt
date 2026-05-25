package com.baust.cafe.activities

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.baust.cafe.R
import com.baust.cafe.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class UserDashboardActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var welcomeUserText: TextView
    private var currentUser: User? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_dashboard)

        auth = FirebaseAuth.getInstance()
        welcomeUserText = findViewById(R.id.welcomeUserText)

        // Initialize Cards
        val browseMenuCard = findViewById<CardView>(R.id.browseMenuCard)
        val myOrdersCard = findViewById<CardView>(R.id.myOrdersCard)
        val myCartCard = findViewById<CardView>(R.id.myCartCard)
        val giveReviewCard = findViewById<CardView>(R.id.giveReviewCard)
        val profileDetailsCard = findViewById<CardView>(R.id.profileDetailsCard)
        val logoutCard = findViewById<CardView>(R.id.logoutCard)

        // Set Listeners
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
            startActivity(Intent(this, RateUsActivity::class.java))
        }

        profileDetailsCard.setOnClickListener {
            startActivity(Intent(this, EditProfileActivity::class.java))
        }

        logoutCard.setOnClickListener {
            showLogoutDialog()
        }

        setupNavigation()
        loadUserData()
    }

    private fun setupNavigation() {
        findViewById<android.widget.ImageView>(R.id.navHome).setOnClickListener {
            startActivity(Intent(this, HomeActivity::class.java))
            finish()
        }

        findViewById<android.widget.ImageView>(R.id.navBookmark).setOnClickListener {
            startActivity(Intent(this, MenuActivity::class.java))
            finish()
        }

        findViewById<android.widget.ImageView>(R.id.navHistory).setOnClickListener {
            startActivity(Intent(this, OrderHistoryActivity::class.java))
            finish()
        }

        findViewById<android.widget.ImageView>(R.id.navNotifications).setOnClickListener {
            Toast.makeText(this, "No new notifications", Toast.LENGTH_SHORT).show()
        }

        findViewById<android.widget.ImageView>(R.id.navProfile).setOnClickListener {
            // Already on Profile
        }
    }

    private fun showProfileDialog() {
        currentUser?.let { user ->
            val builder = AlertDialog.Builder(this)
            builder.setTitle("My Profile")
            builder.setMessage("Name: ${user.name}\nEmail: ${user.email}\nStudent ID: ${user.studentId}\nLocation: ${if(user.location.isEmpty()) "Not Set" else user.location}\nTotal Spent: $${String.format("%.2f", user.totalSpent)}")
            builder.setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
            builder.show()
        } ?: Toast.makeText(this, "Loading user data...", Toast.LENGTH_SHORT).show()
    }

    private fun showLogoutDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Logout")
        builder.setMessage("Are you sure you want to logout?")
        builder.setPositiveButton("Yes") { _, _ ->
            auth.signOut()
            val intent = Intent(this, WelcomeActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
        builder.setNegativeButton("No") { dialog, _ -> dialog.dismiss() }
        builder.show()
    }

    private fun loadUserData() {
        val user = auth.currentUser ?: return
        val database = FirebaseDatabase.getInstance().getReference("Users").child(user.uid)

        // Default from Auth
        welcomeUserText.text = "Welcome back, ${user.displayName ?: "User"}"

        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    currentUser = snapshot.getValue(User::class.java)
                    currentUser?.let {
                        welcomeUserText.text = "Welcome back, ${it.name}"
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
            }
        })
    }
}
