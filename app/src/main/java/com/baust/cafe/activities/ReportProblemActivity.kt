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

class ReportProblemActivity : BaseUserActivity() {

    private lateinit var commentEdit: EditText
    private lateinit var submitBtn: Button
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: UserReviewAdapter
    private val reportsList = mutableListOf<Review>()
    private lateinit var database: DatabaseReference
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_report_problem)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().getReference("Reviews")

        commentEdit = findViewById(R.id.reportComment)
        submitBtn = findViewById(R.id.submitReportBtn)
        recyclerView = findViewById(R.id.reportsRecyclerView)

        setupRecyclerView()
        fetchMyReports()

        submitBtn.setOnClickListener {
            submitReport()
        }

        setupBottomNavigation(R.id.navProfile)
    }

    private fun setupRecyclerView() {
        adapter = UserReviewAdapter(
            reportsList,
            onUserClick = { userId -> showUserDetailsDialog(userId) },
            onEditClick = { report -> showEditReportDialog(report) },
            onDeleteClick = { report -> showDeleteReportDialog(report) }
        )
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
    }

    private fun showEditReportDialog(report: Review) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_rating, null)
        val dialog = AlertDialog.Builder(this).setView(dialogView).create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val ratingBar = dialogView.findViewById<RatingBar>(R.id.dialogRatingBar)
        val commentEdit = dialogView.findViewById<EditText>(R.id.dialogCommentEditText)
        val submitBtn = dialogView.findViewById<Button>(R.id.dialogSubmitButton)
        val cancelBtn = dialogView.findViewById<Button>(R.id.dialogCancelButton)

        ratingBar.visibility = View.GONE // Hide rating for problem reports
        commentEdit.setText(report.comment)
        commentEdit.setTextColor(getColor(R.color.text_black))
        commentEdit.setSelection(report.comment.length)

        submitBtn.setOnClickListener {
            val newComment = commentEdit.text.toString().trim()
            if (newComment.isNotEmpty()) {
                val updates = mapOf("comment" to newComment)
                database.child(report.reviewId).updateChildren(updates)
                dialog.dismiss()
            }
        }
        cancelBtn.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun showDeleteReportDialog(report: Review) {
        AlertDialog.Builder(this)
            .setTitle("Delete Report")
            .setMessage("Are you sure you want to delete this report?")
            .setPositiveButton("Delete") { _, _ ->
                database.child(report.reviewId).removeValue()
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

    private fun fetchMyReports() {
        val userId = auth.currentUser?.uid ?: return
        // Fetch only reports (itemId = "REPORT") belonging to this user
        database.orderByChild("itemId").equalTo("REPORT")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    reportsList.clear()
                    for (reportSnapshot in snapshot.children) {
                        val report = reportSnapshot.getValue(Review::class.java)
                        if (report?.studentId == userId) {
                            reportsList.add(report)
                        }
                    }
                    reportsList.reverse()
                    adapter.updateReviews(reportsList)
                }
                override fun onCancelled(error: DatabaseError) {
                    if (FirebaseAuth.getInstance().currentUser != null && error.code != DatabaseError.PERMISSION_DENIED) {
                        Toast.makeText(this@ReportProblemActivity, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            })
    }

    private fun submitReport() {
        val comment = commentEdit.text.toString().trim()

        if (comment.isEmpty()) {
            Toast.makeText(this, "Please describe the problem", Toast.LENGTH_SHORT).show()
            return
        }

        val user = auth.currentUser ?: return
        val reviewId = database.push().key ?: return
        val report = Review(
            reviewId = reviewId,
            studentId = user.uid,
            studentName = user.displayName ?: "Student",
            itemId = "REPORT",
            itemName = "System Report",
            rating = 5f,
            comment = comment,
            timestamp = System.currentTimeMillis()
        )

        submitBtn.isEnabled = false
        database.child(reviewId).setValue(report).addOnSuccessListener {
            saveAdminNotification("Problem Reported!", "@${report.studentName} reported a system issue", "report")
            Toast.makeText(this, "Reported successfully!", Toast.LENGTH_SHORT).show()
            commentEdit.setText("")
            submitBtn.isEnabled = true
        }.addOnFailureListener {
            submitBtn.isEnabled = true
            Toast.makeText(this, "Failed: ${it.message}", Toast.LENGTH_SHORT).show()
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
