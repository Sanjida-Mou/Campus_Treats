package com.baust.cafe.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RatingBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.baust.cafe.R
import com.baust.cafe.models.Review

class UserReviewAdapter(private var reviews: List<Review>) : RecyclerView.Adapter<UserReviewAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.reviewerNameUser)
        val rating: RatingBar = view.findViewById(R.id.reviewRatingUser)
        val comment: TextView = view.findViewById(R.id.reviewCommentUser)
        val replyLayout: View = view.findViewById(R.id.replyLayoutUser)
        val replyText: TextView = view.findViewById(R.id.adminReplyUser)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_review_user, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val review = reviews[position]
        holder.name.text = review.studentName
        holder.rating.rating = review.rating
        holder.comment.text = review.comment

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
