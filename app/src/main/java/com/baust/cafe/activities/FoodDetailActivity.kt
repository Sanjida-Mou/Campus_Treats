package com.baust.cafe.activities

import android.os.Bundle
import android.view.LayoutInflater
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.baust.cafe.R
import com.baust.cafe.adapters.UserReviewAdapter
import com.baust.cafe.models.Review
import com.baust.cafe.models.User
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.util.Locale

import android.content.Intent
import android.graphics.BitmapFactory
import android.util.Base64
import com.baust.cafe.models.CartItem

class FoodDetailActivity : AppCompatActivity() {

    private lateinit var foodImage: ImageView
    private lateinit var foodName: TextView
    private lateinit var foodPrice: TextView
    private lateinit var foodDesc: TextView
    private lateinit var quantityText: TextView
    private lateinit var plusButton: ImageView
    private lateinit var minusButton: ImageView
    private lateinit var addToCartButton: Button
    private lateinit var orderNowButton: Button
    private lateinit var preorderButton: Button
    private lateinit var backButton: ImageView

    private lateinit var detailRatingBar: RatingBar
    private lateinit var detailReviewComment: EditText
    private lateinit var submitReviewBtn: Button
    private lateinit var reviewsRecyclerView: RecyclerView
    
    private lateinit var reviewAdapter: UserReviewAdapter
    private val reviewsList = mutableListOf<Review>()
    private lateinit var database: DatabaseReference
    private lateinit var auth: FirebaseAuth

    private var quantity = 1
    private var basePrice = 0.0
    private var foodId: String = ""
    private var imageUrl: String = ""
    private var available: Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_food_detail)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().getReference("Reviews")

        // Initialize UI
        foodImage = findViewById(R.id.detailFoodImage)
        foodName = findViewById(R.id.detailFoodName)
        foodPrice = findViewById(R.id.detailFoodPrice)
        foodDesc = findViewById(R.id.detailFoodDesc)
        quantityText = findViewById(R.id.quantityText)
        plusButton = findViewById(R.id.plusButtonDetail)
        minusButton = findViewById(R.id.minusButton)
        addToCartButton = findViewById(R.id.addToCartButtonDetail)
        orderNowButton = findViewById(R.id.orderNowButton)
        preorderButton = findViewById(R.id.preorderButton)
        backButton = findViewById(R.id.backButton)

        detailRatingBar = findViewById(R.id.detailRatingBar)
        detailReviewComment = findViewById(R.id.detailReviewComment)
        submitReviewBtn = findViewById(R.id.submitDetailReviewBtn)
        reviewsRecyclerView = findViewById(R.id.detailReviewsRecyclerView)

        // Get Data from Intent
        foodId = intent.getStringExtra("FOOD_ID") ?: ""
        val name = intent.getStringExtra("FOOD_NAME") ?: "Food"
        val desc = intent.getStringExtra("FOOD_DESC") ?: "No description available"
        basePrice = intent.getDoubleExtra("FOOD_PRICE", 0.0)
        imageUrl = intent.getStringExtra("FOOD_IMAGE") ?: ""
        available = intent.getBooleanExtra("FOOD_AVAILABLE", true)

        // Set Data
        foodName.text = name
        foodDesc.text = desc
        updatePriceDisplay()

        setupReviews()
        loadFoodReviews()

        // Handle Availability
        if (!available) {
            addToCartButton.isEnabled = false
            addToCartButton.text = "Out of Stock"
            addToCartButton.alpha = 0.5f
            
            orderNowButton.isEnabled = false
            orderNowButton.alpha = 0.5f
            
            preorderButton.isEnabled = false
            preorderButton.alpha = 0.5f
            
            Toast.makeText(this, "This item is currently out of stock", Toast.LENGTH_LONG).show()
        }

        // Load image (Supports both URL and Base64)
        if (imageUrl.isNotEmpty()) {
            if (imageUrl.startsWith("http")) {
                Glide.with(this).load(imageUrl).placeholder(R.drawable.cafe_logo).into(foodImage)
            } else {
                try {
                    val imageBytes = Base64.decode(imageUrl, Base64.DEFAULT)
                    val decodedImage = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                    foodImage.setImageBitmap(decodedImage)
                } catch (e: Exception) {
                    foodImage.setImageResource(R.drawable.cafe_logo)
                }
            }
        }

        // Click Listeners
        backButton.setOnClickListener { finish() }

        plusButton.setOnClickListener {
            quantity++
            quantityText.text = quantity.toString()
            updatePriceDisplay()
        }

        minusButton.setOnClickListener {
            if (quantity > 1) {
                quantity--
                quantityText.text = quantity.toString()
                updatePriceDisplay()
            }
        }

        addToCartButton.setOnClickListener {
            val cartItem = CartItem(
                itemId = foodId,
                itemName = foodName.text.toString(),
                price = basePrice,
                quantity = quantity,
                imageUrl = imageUrl
            )
            com.baust.cafe.utils.CartManager.addItem(cartItem)
            Toast.makeText(this, "$quantity $name added to cart", Toast.LENGTH_SHORT).show()
        }

        orderNowButton.setOnClickListener {
            goToCheckout(false)
        }

        preorderButton.setOnClickListener {
            goToCheckout(true)
        }

        submitReviewBtn.setOnClickListener {
            submitReview(name)
        }
        
        // Ensure RatingBar is interactive
        detailRatingBar.setIsIndicator(false)
        detailRatingBar.rating = 5f
    }

    private fun setupReviews() {
        reviewAdapter = UserReviewAdapter(
            reviewsList,
            onUserClick = { userId -> showUserDetailsDialog(userId) },
            onEditClick = { review -> showEditReviewDialog(review) },
            onDeleteClick = { review -> showDeleteReviewDialog(review) }
        )
        reviewsRecyclerView.layoutManager = LinearLayoutManager(this)
        reviewsRecyclerView.adapter = reviewAdapter
    }

    private fun loadFoodReviews() {
        database.orderByChild("itemId").equalTo(foodId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    reviewsList.clear()
                    for (reviewSnapshot in snapshot.children) {
                        val review = reviewSnapshot.getValue(Review::class.java)
                        review?.let { reviewsList.add(it) }
                    }
                    reviewsList.reverse()
                    reviewAdapter.updateReviews(reviewsList)
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun submitReview(itemName: String) {
        val comment = detailReviewComment.text.toString().trim()
        val rating = detailRatingBar.rating

        if (comment.isEmpty()) {
            Toast.makeText(this, "Please write a comment", Toast.LENGTH_SHORT).show()
            return
        }

        val user = auth.currentUser
        if (user == null) {
            Toast.makeText(this, "Please login to review", Toast.LENGTH_SHORT).show()
            return
        }

        val reviewId = database.push().key ?: return
        val review = Review(
            reviewId = reviewId,
            studentId = user.uid,
            studentName = user.displayName ?: "Student",
            itemId = foodId,
            itemName = itemName,
            rating = rating,
            comment = comment,
            timestamp = System.currentTimeMillis()
        )

        database.child(reviewId).setValue(review).addOnSuccessListener {
            // Save Notification for Admin
            saveAdminNotification("New Review Received!", "@${review.studentName} sent a review for $itemName", "review")
            
            Toast.makeText(this, "Review submitted!", Toast.LENGTH_SHORT).show()
            detailReviewComment.setText("")
            detailRatingBar.rating = 5f
        }
    }

    private fun saveAdminNotification(title: String, message: String, type: String) {
        val notifyDb = FirebaseDatabase.getInstance().getReference("AdminNotifications")
        val id = notifyDb.push().key ?: return
        val notification = com.baust.cafe.models.AdminNotification(
            id = id,
            title = title,
            message = message,
            type = type,
            timestamp = System.currentTimeMillis(),
            read = false
        )
        notifyDb.child(id).setValue(notification)
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
        commentEdit.setSelection(review.comment.length) // Cursor at end

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
                                val imageBytes = Base64.decode(imageUrl, Base64.DEFAULT)
                                val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
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

    private fun goToCheckout(isPreorder: Boolean) {
        val cartItem = CartItem(
            itemId = foodId,
            itemName = foodName.text.toString(),
            price = basePrice,
            quantity = quantity,
            imageUrl = imageUrl
        )
        
        val itemsList = arrayListOf(cartItem)
        val intent = Intent(this, CheckoutActivity::class.java).apply {
            putExtra("CART_ITEMS", itemsList)
            putExtra("IS_PREORDER", isPreorder)
        }
        startActivity(intent)
    }

    private fun updatePriceDisplay() {
        val totalPrice = basePrice * quantity
        foodPrice.text = String.format(Locale.getDefault(), "Tk. %.2f", totalPrice)
    }
}
