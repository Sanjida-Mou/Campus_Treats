package com.baust.cafe.activities

import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.baust.cafe.R
import com.baust.cafe.adapters.AdminReviewAdapter
import com.baust.cafe.models.Review
import com.baust.cafe.models.User
import com.bumptech.glide.Glide
import com.google.firebase.database.*
import java.util.Locale

class ViewReviewsActivity : BaseAdminActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: AdminReviewAdapter
    private val reviewsList = mutableListOf<Review>()
    private lateinit var database: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_reviews)

        recyclerView = findViewById(R.id.reviewsRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        
        adapter = AdminReviewAdapter(reviewsList) { userId ->
            showUserDetailsDialog(userId)
        }
        recyclerView.adapter = adapter

        database = FirebaseDatabase.getInstance().getReference("Reviews")
        fetchReviews()
        setupBottomNavigation(0)
    }

    private fun showUserDetailsDialog(userId: String) {
        FirebaseDatabase.getInstance().getReference("Users").child(userId).get()
            .addOnSuccessListener { snapshot ->
                val user = snapshot.getValue(User::class.java)
                if (user != null) {
                    val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_user_details, null)
                    val dialog = AlertDialog.Builder(this)
                        .setView(dialogView)
                        .create()

                    dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

                    val userImage = dialogView.findViewById<ImageView>(R.id.dialogUserImage)
                    val userName = dialogView.findViewById<TextView>(R.id.dialogUserName)
                    val userDetails = dialogView.findViewById<TextView>(R.id.dialogUserDetails)
                    val okBtn = dialogView.findViewById<Button>(R.id.dialogOkButton)

                    userName.text = user.name
                    val totalSpentFormatted = String.format(Locale.getDefault(), "%.2f", user.totalSpent)
                    userDetails.text = "Email:  ${user.email}\n" +
                            "Phone:  ${user.phoneNumber}\n" +
                            "Student ID:  ${user.studentId}\n" +
                            "Location:  ${user.location}\n" +
                            "Total Spent:  Tk. $totalSpentFormatted"

                    // Load Image
                    val imageUrl = user.profileImage
                    if (imageUrl.isNotEmpty() && imageUrl != "null") {
                        if (imageUrl.startsWith("http")) {
                            Glide.with(this).load(imageUrl).placeholder(R.drawable.ic_person).into(userImage)
                        } else {
                            try {
                                val imageBytes = android.util.Base64.decode(imageUrl, android.util.Base64.DEFAULT)
                                val bitmap = android.graphics.BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                                userImage.setImageBitmap(bitmap)
                            } catch (e: Exception) {
                                userImage.setImageResource(R.drawable.ic_person)
                            }
                        }
                    }

                    okBtn.setOnClickListener { dialog.dismiss() }
                    dialog.show()
                }
            }
    }

    private fun fetchReviews() {
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                reviewsList.clear()
                for (reviewSnapshot in snapshot.children) {
                    val review = reviewSnapshot.getValue(Review::class.java)
                    review?.let { reviewsList.add(it) }
                }
                reviewsList.reverse() // Newest first
                adapter.updateReviews(reviewsList)
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@ViewReviewsActivity, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
