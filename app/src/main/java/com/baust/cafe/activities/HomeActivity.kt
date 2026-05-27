package com.baust.cafe.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.baust.cafe.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

import androidx.recyclerview.widget.RecyclerView
import com.baust.cafe.adapters.PopularFoodAdapter
import com.baust.cafe.models.MenuItem
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import java.util.Locale

class HomeActivity : BaseUserActivity() {

    private lateinit var userNameText: TextView
    private lateinit var totalSpentText: TextView
    private lateinit var profileImage: ImageView
    private lateinit var auth: FirebaseAuth
    private lateinit var navHome: ImageView
    private lateinit var navCart: ImageView
    private lateinit var navBookmark: ImageView
    private lateinit var navNotifications: ImageView
    private lateinit var navProfile: ImageView
    
    private lateinit var menuRecyclerView: RecyclerView
    private lateinit var popularFoodAdapter: PopularFoodAdapter
    private val foodList = mutableListOf<MenuItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        auth = FirebaseAuth.getInstance()
        
        // Initialize UI Elements
        userNameText = findViewById(R.id.userNameText)
        totalSpentText = findViewById(R.id.totalSpentText)
        profileImage = findViewById(R.id.profileImage)
        navHome = findViewById(R.id.navHome)
        navCart = findViewById(R.id.navCart)
        navBookmark = findViewById(R.id.navBookmark)
        navNotifications = findViewById(R.id.navNotifications)
        navProfile = findViewById(R.id.navProfile)
        menuRecyclerView = findViewById(R.id.menuRecyclerView)

        setupMenuRecyclerView()
        setupNavigation()
        loadUserData()
        loadMenuData()
        
        setupBottomNavigation(R.id.navHome)
    }

    private fun setupMenuRecyclerView() {
        menuRecyclerView.layoutManager = androidx.recyclerview.widget.GridLayoutManager(this, 2)

        popularFoodAdapter = PopularFoodAdapter(foodList) { menuItem ->
            val cartItem = com.baust.cafe.models.CartItem(
                itemId = menuItem.itemId,
                itemName = menuItem.name,
                price = menuItem.price,
                quantity = 1,
                imageUrl = menuItem.imageUrl
            )
            val intent = Intent(this, CheckoutActivity::class.java).apply {
                putExtra("CART_ITEMS", arrayListOf(cartItem))
            }
            startActivity(intent)
        }
        menuRecyclerView.adapter = popularFoodAdapter
    }

    private fun loadMenuData() {
        val database = FirebaseDatabase.getInstance().getReference("Menu")
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                foodList.clear()
                for (foodSnapshot in snapshot.children) {
                    val item = foodSnapshot.getValue(MenuItem::class.java)
                    if (item != null && item.available) {
                        foodList.add(item)
                    }
                }
                popularFoodAdapter.updateList(foodList)
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@HomeActivity, "Failed to load menu", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun setupNavigation() {
        navHome.setOnClickListener {
            // Already on Home
        }

        navCart.setOnClickListener {
            startActivity(Intent(this, CartActivity::class.java))
            finish()
        }

        navBookmark.setOnClickListener {
            startActivity(Intent(this, MenuActivity::class.java))
            finish()
        }

        navNotifications.setOnClickListener {
            startActivity(Intent(this, UserNotificationsActivity::class.java))
            finish()
        }

        navProfile.setOnClickListener {
            startActivity(Intent(this, UserDashboardActivity::class.java))
            finish()
        }

        findViewById<TextView>(R.id.viewAllText).setOnClickListener {
            startActivity(Intent(this, MenuActivity::class.java))
        }
    }

    private fun loadUserData() {
        val user = auth.currentUser ?: return
        val userId = user.uid
        val database = FirebaseDatabase.getInstance().getReference("Users").child(userId)

        user.displayName?.let {
            userNameText.text = "$it - Campus Treats"
        }

        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val name = snapshot.child("name").value.toString()
                    val imageUrl = snapshot.child("profileImage").value.toString()
                    val totalSpent = snapshot.child("totalSpent").getValue(Double::class.java) ?: 0.0

                    if (name.isNotEmpty() && name != "null") {
                        userNameText.text = "$name - Campus Treats"
                    }

                    totalSpentText.text = String.format(Locale.getDefault(), "%.2f", totalSpent)

                    if (imageUrl.isNotEmpty() && imageUrl != "null") {
                        if (imageUrl.startsWith("http")) {
                            com.bumptech.glide.Glide.with(this@HomeActivity)
                                .load(imageUrl)
                                .placeholder(R.drawable.cafe_logo)
                                .into(profileImage)
                        } else {
                            // Load personal photo (Base64)
                            try {
                                val imageBytes = android.util.Base64.decode(imageUrl, android.util.Base64.DEFAULT)
                                val decodedImage = android.graphics.BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                                profileImage.setImageBitmap(decodedImage)
                            } catch (e: Exception) {
                                profileImage.setImageResource(R.drawable.cafe_logo)
                            }
                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
            }
        })
    }
}
