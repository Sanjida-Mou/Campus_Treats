package com.baust.cafe.activities

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.baust.cafe.R
import com.baust.cafe.adapters.AdminReviewAdapter
import com.baust.cafe.models.Review
import com.baust.cafe.models.User
import com.google.firebase.database.*
import java.util.Locale

class ViewReviewsActivity : AppCompatActivity() {

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
    }

    private fun showUserDetailsDialog(userId: String) {
        FirebaseDatabase.getInstance().getReference("Users").child(userId).get()
            .addOnSuccessListener { snapshot ->
                val user = snapshot.getValue(User::class.java)
                if (user != null) {
                    AlertDialog.Builder(this)
                        .setTitle("User Details")
                        .setMessage("Name: ${user.name}\n" +
                                "Email: ${user.email}\n" +
                                "Phone: ${user.phoneNumber}\n" +
                                "Student ID: ${user.studentId}\n" +
                                "Location: ${user.location}\n" +
                                "Total Spent: Tk. ${String.format(Locale.getDefault(), "%.2f", user.totalSpent)}")
                        .setPositiveButton("OK", null)
                        .show()
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
