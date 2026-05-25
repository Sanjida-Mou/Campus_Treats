package com.baust.cafe.activities

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.baust.cafe.R
import com.baust.cafe.adapters.UserReviewAdapter
import com.baust.cafe.models.Review
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class RateUsActivity : AppCompatActivity() {

    private lateinit var ratingBar: RatingBar
    private lateinit var commentEdit: EditText
    private lateinit var submitBtn: Button
    private lateinit var backBtn: ImageView
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: UserReviewAdapter
    private val reviewsList = mutableListOf<Review>()
    private lateinit var database: DatabaseReference
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rate_us)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().getReference("Reviews")

        ratingBar = findViewById(R.id.rateUsRatingBar)
        commentEdit = findViewById(R.id.rateUsComment)
        submitBtn = findViewById(R.id.submitRateUsBtn)
        backBtn = findViewById(R.id.backButton)
        recyclerView = findViewById(R.id.rateUsRecyclerView)

        setupRecyclerView()
        fetchReviews()

        backBtn.setOnClickListener { finish() }

        submitBtn.setOnClickListener {
            submitReview()
        }
    }

    private fun setupRecyclerView() {
        adapter = UserReviewAdapter(reviewsList)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
    }

    private fun fetchReviews() {
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                reviewsList.clear()
                for (reviewSnapshot in snapshot.children) {
                    val review = reviewSnapshot.getValue(Review::class.java)
                    // We only show general reviews here (itemId is empty) or all? 
                    // User said "Rate Us", usually it's app/cafe level.
                    // But seeing previous code, it was item based.
                    // Let's show all reviews for now as "All Reviews".
                    review?.let { reviewsList.add(it) }
                }
                reviewsList.reverse()
                adapter.updateReviews(reviewsList)
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@RateUsActivity, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun submitReview() {
        val rating = ratingBar.rating
        val comment = commentEdit.text.toString().trim()

        if (comment.isEmpty()) {
            Toast.makeText(this, "Please enter a comment", Toast.LENGTH_SHORT).show()
            return
        }

        val user = auth.currentUser
        if (user == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show()
            return
        }

        val reviewId = database.push().key ?: return
        val review = Review(
            reviewId = reviewId,
            studentId = user.uid,
            studentName = user.displayName ?: "Student",
            itemId = "", // General Cafe Review
            itemName = "Cafe Service",
            rating = rating,
            comment = comment,
            timestamp = System.currentTimeMillis()
        )

        submitBtn.isEnabled = false
        database.child(reviewId).setValue(review).addOnSuccessListener {
            Toast.makeText(this, "Thank you for your feedback!", Toast.LENGTH_SHORT).show()
            commentEdit.setText("")
            ratingBar.rating = 5f
            submitBtn.isEnabled = true
        }.addOnFailureListener {
            submitBtn.isEnabled = true
            Toast.makeText(this, "Failed to submit: ${it.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
