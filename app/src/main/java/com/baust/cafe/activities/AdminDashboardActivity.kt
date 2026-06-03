package com.baust.cafe.activities

import android.content.Intent
import android.os.Bundle
import androidx.cardview.widget.CardView
import com.baust.cafe.R
import com.baust.cafe.models.AdminNotification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.database.*

class AdminDashboardActivity : BaseAdminActivity() {

    private lateinit var database: DatabaseReference
    private var startTime: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_dashboard)

        startTime = System.currentTimeMillis()

        // Initialize Firebase
        database = FirebaseDatabase.getInstance().getReference("Orders")
        setupOrderListener()
        setupReviewListener()

        val manageMenuCard = findViewById<CardView>(R.id.manageMenuCard)
        val viewOrdersCard = findViewById<CardView>(R.id.viewOrdersCard)
        val viewFoodReviewsCard = findViewById<CardView>(R.id.viewFoodReviewsCard)
        val viewReportsCard = findViewById<CardView>(R.id.viewReportsCard)
        val adminLogoutCard = findViewById<CardView>(R.id.adminLogoutCard)

        manageMenuCard.setOnClickListener {
            startActivity(Intent(this, ManageMenuActivity::class.java))
        }

        viewOrdersCard.setOnClickListener {
            startActivity(Intent(this, ViewOrdersActivity::class.java))
        }

        viewFoodReviewsCard.setOnClickListener {
            val intent = Intent(this, ViewReviewsActivity::class.java)
            intent.putExtra("VIEW_MODE", "FOOD")
            startActivity(intent)
        }

        viewReportsCard.setOnClickListener {
            val intent = Intent(this, ViewReviewsActivity::class.java)
            intent.putExtra("VIEW_MODE", "REPORTS")
            startActivity(intent)
        }

        adminLogoutCard.setOnClickListener {
            startActivity(Intent(this, AdminLoginActivity::class.java))
            finish()
        }

        setupBottomNavigation(R.id.navHome)
    }

    private fun setupOrderListener() {
        database.orderByChild("orderTime").startAt(startTime.toDouble())
            .addChildEventListener(object : ChildEventListener {
                override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                    val orderTime = snapshot.child("orderTime").getValue(Long::class.java) ?: 0
                    if (orderTime >= startTime) {
                        val customerName = snapshot.child("studentName").value.toString()
                        val total = snapshot.child("totalAmount").value.toString()
                        
                        val intent = Intent(this@AdminDashboardActivity, ViewOrdersActivity::class.java)
                        showNotification("New Order Received!", "@$customerName ordered food - Tk $total", intent)
                    }
                }

                override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                    val status = snapshot.child("status").value.toString()
                    val customerName = snapshot.child("studentName").value.toString()
                    if (status == "cancelled") {
                        val title = "Order Cancelled"
                        val message = "@$customerName has cancelled their order"
                        
                        val intent = Intent(this@AdminDashboardActivity, ViewOrdersActivity::class.java)
                        showNotification(title, message, intent)

                        // Save cancellation notification
                        val notifyDb = FirebaseDatabase.getInstance().getReference("AdminNotifications")
                        val id = notifyDb.push().key
                        if (id != null) {
                            val notification = AdminNotification(id, title, message, "cancellation", System.currentTimeMillis(), read = false)
                            notifyDb.child(id).setValue(notification)
                        }
                    }
                }

                override fun onChildRemoved(snapshot: DataSnapshot) {}
                override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun setupReviewListener() {
        val reviewDb = FirebaseDatabase.getInstance().getReference("Reviews")
        reviewDb.orderByChild("timestamp").startAt(startTime.toDouble())
            .addChildEventListener(object : ChildEventListener {
                override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                    val timestamp = snapshot.child("timestamp").getValue(Long::class.java) ?: 0
                    if (timestamp >= startTime) {
                        val reviewerName = snapshot.child("studentName").value.toString()
                        val itemId = snapshot.child("itemId").value.toString()
                        
                        if (itemId == "REPORT") {
                            val intent = Intent(this@AdminDashboardActivity, ViewReviewsActivity::class.java)
                            intent.putExtra("VIEW_MODE", "REPORTS")
                            showNotification("New Report Received!", "@$reviewerName reported a problem", intent)
                        } else {
                            val itemName = snapshot.child("itemName").value.toString()
                            val foodName = if (itemName.isEmpty() || itemName == "null") "Food" else itemName
                            val intent = Intent(this@AdminDashboardActivity, ViewReviewsActivity::class.java)
                            intent.putExtra("VIEW_MODE", "FOOD")
                            showNotification("New Review Received!", "@$reviewerName sent a review for $foodName", intent)
                        }
                    }
                }
                override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
                override fun onChildRemoved(snapshot: DataSnapshot) {}
                override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun showNotification(title: String, message: String, intent: Intent) {
        val channelId = "admin_alerts"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Admin Notifications", NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }

        val pendingIntent = PendingIntent.getActivity(this, System.currentTimeMillis().toInt(), intent, PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notifications)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
}
