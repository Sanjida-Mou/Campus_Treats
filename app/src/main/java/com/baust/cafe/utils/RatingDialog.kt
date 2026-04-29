package com.baust.cafe.utils

import android.app.Dialog
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.RatingBar
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.baust.cafe.R
import com.baust.cafe.models.Review
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class RatingDialog(private val itemId: String, private val itemName: String) : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.dialog_rating)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val ratingBar = dialog.findViewById<RatingBar>(R.id.dialogRatingBar)
        val commentEditText = dialog.findViewById<EditText>(R.id.dialogCommentEditText)
        val cancelButton = dialog.findViewById<Button>(R.id.dialogCancelButton)
        val submitButton = dialog.findViewById<Button>(R.id.dialogSubmitButton)

        cancelButton.setOnClickListener {
            dismiss()
        }

        submitButton.setOnClickListener {
            val rating = ratingBar.rating
            val comment = commentEditText.text.toString()

            if (rating == 0f) {
                Toast.makeText(context, "Please select a rating", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            saveReviewToFirebase(rating, comment)
        }

        return dialog
    }

    private fun saveReviewToFirebase(rating: Float, comment: String) {
        val auth = FirebaseAuth.getInstance()
        val userId = auth.currentUser?.uid ?: return
        val userName = auth.currentUser?.displayName ?: "Student"
        
        val database = FirebaseDatabase.getInstance().getReference("Reviews")
        val reviewId = database.push().key ?: return

        val review = Review(
            reviewId = reviewId,
            studentId = userId,
            studentName = userName,
            itemId = itemId,
            itemName = itemName,
            rating = rating,
            comment = comment
        )

        database.child(reviewId).setValue(review)
            .addOnSuccessListener {
                Toast.makeText(context, "Thank you for your review!", Toast.LENGTH_SHORT).show()
                dismiss()
            }
            .addOnFailureListener {
                Toast.makeText(context, "Failed to save review: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }
}