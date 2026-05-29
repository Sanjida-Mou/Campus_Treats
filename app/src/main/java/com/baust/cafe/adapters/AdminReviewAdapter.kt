package com.baust.cafe.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.baust.cafe.R
import com.baust.cafe.models.Review
import com.bumptech.glide.Glide
import com.google.firebase.database.FirebaseDatabase

class AdminReviewAdapter(
    private var reviews: List<Review>,
    private val onUserClick: (String) -> Unit
) : RecyclerView.Adapter<AdminReviewAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.reviewerName)
        val profileImage: ImageView = view.findViewById(R.id.reviewProfileImage)
        val rating: RatingBar = view.findViewById(R.id.reviewRating)
        val item: TextView = view.findViewById(R.id.reviewedItem)
        val comment: TextView = view.findViewById(R.id.reviewCommentText)
        val replyText: TextView = view.findViewById(R.id.adminReplyText)
        val replyEdit: EditText = view.findViewById(R.id.replyEditText)
        val replyBtn: Button = view.findViewById(R.id.sendReplyButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_review_admin, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val review = reviews[position]
        holder.name.text = review.studentName
        
        if (review.itemId == "REPORT") {
            holder.rating.visibility = View.GONE
            holder.item.text = "Type: SYSTEM PROBLEM"
            holder.item.setTextColor(android.graphics.Color.RED)
        } else {
            holder.rating.visibility = View.VISIBLE
            holder.rating.rating = review.rating
            holder.item.text = "Item: ${review.itemName}"
            holder.item.setTextColor(holder.itemView.context.getColor(R.color.text_hint))
        }
        
        holder.comment.text = review.comment

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
            holder.replyText.text = review.reply
            holder.replyText.visibility = View.VISIBLE
            holder.replyText.setTextColor(holder.itemView.context.getColor(R.color.text_black))
        } else {
            holder.replyText.text = "Not replied yet"
        }

        holder.replyBtn.setOnClickListener {
            val replyStr = holder.replyEdit.text.toString().trim()
            if (replyStr.isNotEmpty()) {
                FirebaseDatabase.getInstance().getReference("Reviews")
                    .child(review.reviewId).child("reply").setValue(replyStr)
                holder.replyEdit.setText("")
                
                // NOTIFY USER OF REPLY
                val userNotifyDb = FirebaseDatabase.getInstance().getReference("UserNotifications").child(review.studentId)
                val id = userNotifyDb.push().key
                if (id != null) {
                    val notification = com.baust.cafe.models.UserNotification(
                        id = id,
                        title = "Admin Replied",
                        message = "The admin has replied to your ${if(review.itemId == "REPORT") "report" else "review"}.",
                        type = "review_reply",
                        timestamp = System.currentTimeMillis(),
                        read = false
                    )
                    userNotifyDb.child(id).setValue(notification)
                }
            }
        }
    }

    override fun getItemCount() = reviews.size

    fun updateReviews(newReviews: List<Review>) {
        reviews = newReviews
        notifyDataSetChanged()
    }
}
