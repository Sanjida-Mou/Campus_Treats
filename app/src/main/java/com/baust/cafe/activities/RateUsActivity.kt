package com.baust.cafe.activities

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.baust.cafe.R
import com.baust.cafe.adapters.UserReviewAdapter
import com.baust.cafe.models.Review
import com.baust.cafe.models.User
import com.baust.cafe.models.AdminNotification
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.util.Locale

class RateUsActivity : BaseUserActivity() {

    private lateinit var ratingBar: RatingBar
    private lateinit var commentEdit: EditText
    private lateinit var submitBtn: Button
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
        recyclerView = findViewById(R.id.rateUsRecyclerView)

        setupRecyclerView()
        fetchReviews()

        submitBtn.setOnClickListener {
            submitReview()
        }

        // Ensure RatingBar is interactive
        ratingBar.setIsIndicator(false)
        ratingBar.rating = 5f

        setupBottomNavigation(R.id.navProfile) // Highlight profile as it's a dashboard sub-feature
    }

    private fun setupRecyclerView() {
        adapter = UserReviewAdapter(
            reviewsList,
            onUserClick = { userId -> showUserDetailsDialog(userId) },
            onEditClick = { review -> showEditReviewDialog(review) },
            onDeleteClick = { review -> showDeleteReviewDialog(review) }
        )
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
    }

    private fun showEditReviewDialog(review: Review) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_rating, null)
        val dialog = AlertDialog.Builder(this).setView(dialogView).create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val ratingBar = dialogView.findViewById<RatingBar>(R.id.dialogRatingBar)
        val commentEdit = dialogView.findViewById<EditText>(R.id.dialogCommentEditText)
        val submitBtn = dialogView.findViewById<Button>(R.id.dialogSubmitButton)
        val cancelBtn = dialogView.findViewById<Button>(R.id.dialogCancelButton)

        ratingBar.rating = review.rating
        commentEdit.setText(review.comment)
        commentEdit.setTextColor(getColor(R.color.text_black))
        commentEdit.setSelection(review.comment.length)

        submitBtn.setOnClickListener {
            val newComment = commentEdit.text.toString().trim()
            val newRating = ratingBar.rating
            if (newComment.isNotEmpty()) {
                val updates = mapOf("comment" to newComment, "rating" to newRating)
                database.child(review.reviewId).updateChildren(updates)
                dialog.dismiss()
            }
        }
        cancelBtn.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun showDeleteReviewDialog(review: Review) {
        AlertDialog.Builder(this)
            .setTitle("Delete Review")
            .setMessage("Are you sure you want to delete this review?")
            .setPositiveButton("Delete") { _, _ ->
                database.child(review.reviewId).removeValue()
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun showUserDetailsDialog(userId: String) {
        FirebaseDatabase.getInstance().getReference("Users").child(userId).get()
            .addOnSuccessListener { snapshot ->
                val user = snapshot.getValue(User::class.java)
                if (user != null) {
                    val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_user_details, null)
                    val dialog = AlertDialog.Builder(this).setView(dialogView).create()
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
        // Only fetch cafe reviews (where itemId is empty)
        database.orderByChild("itemId").equalTo("")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    reviewsList.clear()
                    for (reviewSnapshot in snapshot.children) {
                        val review = reviewSnapshot.getValue(Review::class.java)
                        review?.let { reviewsList.add(it) }
                    }
                    reviewsList.reverse()
                    adapter.updateReviews(reviewsList)
                }
                override fun onCancelled(error: DatabaseError) {
                    if (FirebaseAuth.getInstance().currentUser != null && error.code != DatabaseError.PERMISSION_DENIED) {
                        Toast.makeText(this@RateUsActivity, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                    }
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
            saveAdminNotification("New Cafe Review!", "@${review.studentName} sent a review for Cafe Service", "review")
            Toast.makeText(this, "Thank you for your feedback!", Toast.LENGTH_SHORT).show()
            commentEdit.setText("")
            ratingBar.rating = 5f
            submitBtn.isEnabled = true
        }.addOnFailureListener {
            submitBtn.isEnabled = true
            Toast.makeText(this, "Failed to submit: ${it.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveAdminNotification(title: String, message: String, type: String) {
        val notifyDb = FirebaseDatabase.getInstance().getReference("AdminNotifications")
        val id = notifyDb.push().key ?: return
        val notification = AdminNotification(
            id = id,
            title = title,
            message = message,
            type = type,
            timestamp = System.currentTimeMillis(),
            read = false
        )
        notifyDb.child(id).setValue(notification)
    }
}
