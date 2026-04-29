package com.baust.cafe.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.baust.cafe.R

class AdminDashboardActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_dashboard)

        val manageMenuCard = findViewById<CardView>(R.id.manageMenuCard)
        val viewOrdersCard = findViewById<CardView>(R.id.viewOrdersCard)
        val viewReviewsCard = findViewById<CardView>(R.id.viewReviewsCard)
        val adminLogoutCard = findViewById<CardView>(R.id.adminLogoutCard)

        manageMenuCard.setOnClickListener {
            startActivity(Intent(this, ManageMenuActivity::class.java))
        }

        viewOrdersCard.setOnClickListener {
            // startActivity(Intent(this, ViewOrdersActivity::class.java))
            Toast.makeText(this, "Orders coming soon", Toast.LENGTH_SHORT).show()
        }

        viewReviewsCard.setOnClickListener {
            Toast.makeText(this, "Reviews coming soon", Toast.LENGTH_SHORT).show()
        }

        adminLogoutCard.setOnClickListener {
            startActivity(Intent(this, AdminLoginActivity::class.java))
            finish()
        }
    }
}