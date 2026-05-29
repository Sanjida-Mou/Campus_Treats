package com.baust.cafe.activities

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.cardview.widget.CardView
import com.baust.cafe.R
import com.baust.cafe.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class UserDashboardActivity : BaseUserActivity() {

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
        val reportProblemCard = findViewById<CardView>(R.id.giveReviewCard)
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

        reportProblemCard.setOnClickListener {
            startActivity(Intent(this, ReportProblemActivity::class.java))
        }

        profileDetailsCard.setOnClickListener {
            startActivity(Intent(this, EditProfileActivity::class.java))
        }

        logoutCard.setOnClickListener {
            showLogoutDialog()
        }
        
        setupNavigation()
        loadUserData()
        
        setupBottomNavigation(R.id.navProfile)
    }

    private fun setupNavigation() {
        findViewById<android.widget.ImageView>(R.id.navHome)?.setOnClickListener {
            startActivity(Intent(this, HomeActivity::class.java))
            finish()
        }

        findViewById<android.widget.ImageView>(R.id.navCart)?.setOnClickListener {
            startActivity(Intent(this, CartActivity::class.java))
            finish()
        }

        findViewById<android.widget.ImageView>(R.id.navBookmark)?.setOnClickListener {
            startActivity(Intent(this, MenuActivity::class.java))
            finish()
        }

        findViewById<android.widget.ImageView>(R.id.navNotifications)?.setOnClickListener {
            startActivity(Intent(this, UserNotificationsActivity::class.java))
            finish()
        }
        
        // CLICK ON LARGE NAV PROFILE ICON -> Show Big Picture
        findViewById<android.widget.ImageView>(R.id.navProfile)?.setOnClickListener {
            showBigPicture()
        }
    }

    private fun showBigPicture() {
        val user = currentUser ?: return
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_user_details, null)
        val dialog = AlertDialog.Builder(this).setView(dialogView).create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val bigImage = dialogView.findViewById<ImageView>(R.id.dialogUserImage)
        val nameText = dialogView.findViewById<TextView>(R.id.dialogUserName)
        val detailsText = dialogView.findViewById<TextView>(R.id.dialogUserDetails)
        val okBtn = dialogView.findViewById<android.widget.Button>(R.id.dialogOkButton)

        nameText.text = user.name
        detailsText.text = "Student ID: ${user.studentId}"
        
        val imageUrl = user.profileImage
        if (imageUrl.isNotEmpty() && imageUrl != "null") {
            if (imageUrl.startsWith("http")) {
                com.bumptech.glide.Glide.with(this).load(imageUrl).into(bigImage)
            } else {
                try {
                    val imageBytes = android.util.Base64.decode(imageUrl, android.util.Base64.DEFAULT)
                    val bitmap = android.graphics.BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                    bigImage.setImageBitmap(bitmap)
                } catch (e: Exception) {
                    bigImage.setImageResource(R.drawable.ic_person)
                }
            }
        }

        okBtn.setOnClickListener { dialog.dismiss() }
        dialog.show()
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
        builder.setNegativeButton("No", null)
        builder.show()
    }

    private fun loadUserData() {
        val user = auth.currentUser ?: return
        val database = FirebaseDatabase.getInstance().getReference("Users").child(user.uid)

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
