package com.baust.cafe.activities

import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.baust.cafe.R
import com.bumptech.glide.Glide
import java.util.Locale

import android.content.Intent
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

    private var quantity = 1
    private var basePrice = 0.0
    private var foodId: String = ""
    private var imageUrl: String = ""
    private var available: Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_food_detail)

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

        if (imageUrl.isNotEmpty()) {
            Glide.with(this).load(imageUrl).placeholder(R.drawable.cafe_logo).into(foodImage)
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
