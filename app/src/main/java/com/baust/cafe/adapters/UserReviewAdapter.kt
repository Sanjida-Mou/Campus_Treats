package com.baust.cafe.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.baust.cafe.R
import com.baust.cafe.models.Review
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class UserReviewAdapter(
    private var reviews: List<Review>,
    private val onUserClick: (String) -> Unit,
    private val onEditClick: ((Review) -> Unit)? = null,
    private val onDeleteClick: ((Review) -> Unit)? = null
) : RecyclerView.Adapter<UserReviewAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.reviewerNameUser)
        val profileImage: ImageView = view.findViewById(R.id.reviewerProfileImageUser)
        val rating: RatingBar = view.findViewById(R.id.reviewRatingUser)
        val comment: TextView = view.findViewById(R.id.reviewCommentUser)
        val replyLayout: View = view.findViewById(R.id.replyLayoutUser)
        val replyText: TextView = view.findViewById(R.id.adminReplyUser)
        val actionsLayout: View = view.findViewById(R.id.reviewActionsLayout)
        val editBtn: ImageView = view.findViewById(R.id.editReviewBtn)
        val deleteBtn: ImageView = view.findViewById(R.id.deleteReviewBtn)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_review_user, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val review = reviews[position]
        holder.name.text = review.studentName
        
        if (review.itemId == "REPORT") {
            holder.rating.visibility = View.GONE
        } else {
            holder.rating.visibility = View.VISIBLE
            holder.rating.rating = review.rating
        }

        holder.comment.text = review.comment

        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        if (review.studentId == currentUserId) {
            holder.actionsLayout.visibility = View.VISIBLE
            holder.editBtn.setOnClickListener { onEditClick?.invoke(review) }
            holder.deleteBtn.setOnClickListener { onDeleteClick?.invoke(review) }
        } else {
            holder.actionsLayout.visibility = View.GONE
        }

        // Load User Profile Image
        FirebaseDatabase.getInstance().getReference("Users").child(review.studentId)
            .child("profileImage").get().addOnSuccessListener {
                val imageUrl = it.value.toString()
                if (imageUrl.isNotEmpty() && imageUrl != "null") {
                    if (imageUrl.startsWith("http")) {
                        Glide.with(holder.itemView.context).load(imageUrl)
                            .placeholder(R.drawable.ic_person).into(holder.profileImage)
                    } else {
                        try {
                            val imageBytes = android.util.Base64.decode(imageUrl, android.util.Base64.DEFAULT)
                            val bitmap = android.graphics.BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                            holder.profileImage.setImageBitmap(bitmap)
                        } catch (e: Exception) {
                            holder.profileImage.setImageResource(R.drawable.ic_person)
                        }
                    }
                }
            }

        holder.name.setOnClickListener { onUserClick(review.studentId) }
        holder.profileImage.setOnClickListener { onUserClick(review.studentId) }

        if (review.reply.isNotEmpty()) {
            holder.replyLayout.visibility = View.VISIBLE
            holder.replyText.text = review.reply
        } else {
            holder.replyLayout.visibility = View.GONE
        }
    }

    override fun getItemCount() = reviews.size

    fun updateReviews(newReviews: List<Review>) {
        reviews = newReviews
        notifyDataSetChanged()
    }
}
