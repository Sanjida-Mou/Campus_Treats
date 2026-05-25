package com.baust.cafe.activities

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.baust.cafe.R
import com.baust.cafe.adapters.MenuAdapter
import com.baust.cafe.models.MenuItem
import com.google.firebase.database.*

class MenuActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: MenuAdapter
    private val menuItemsList = mutableListOf<MenuItem>()
    private lateinit var database: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_menu)

        recyclerView = findViewById(R.id.menuRecyclerView)
        // Set up GridLayoutManager for 2 columns as shown in the image
        recyclerView.layoutManager = androidx.recyclerview.widget.GridLayoutManager(this, 2)
        
        adapter = MenuAdapter(
            menuItems = menuItemsList,
            isAdmin = false,
            onAddToCartClicked = { menuItem ->
                addToCart(menuItem)
            }
        )
        recyclerView.adapter = adapter

        fetchMenuData()
        setupNavigation()
    }

    private fun setupNavigation() {
        findViewById<android.widget.ImageView>(R.id.navHome).setOnClickListener {
            startActivity(android.content.Intent(this, HomeActivity::class.java))
            finish()
        }

        findViewById<android.widget.ImageView>(R.id.navBookmark).setOnClickListener {
            // Already on Menu
        }

        findViewById<android.widget.ImageView>(R.id.navHistory).setOnClickListener {
            startActivity(android.content.Intent(this, OrderHistoryActivity::class.java))
            finish()
        }

        findViewById<android.widget.ImageView>(R.id.navNotifications).setOnClickListener {
            Toast.makeText(this, "No new notifications", Toast.LENGTH_SHORT).show()
        }

        findViewById<android.widget.ImageView>(R.id.navProfile).setOnClickListener {
            startActivity(android.content.Intent(this, UserDashboardActivity::class.java))
            finish()
        }

        findViewById<android.widget.ImageView>(R.id.backBtn).setOnClickListener {
            finish()
        }
    }

    private fun fetchMenuData() {
        database = FirebaseDatabase.getInstance().getReference("Menu")
        
        // --- REMOVED SAMPLE DATA AUTO-GENERATION ---
        /*
        database.get().addOnSuccessListener { snapshot ->
            if (!snapshot.exists()) {
                val sampleItems = listOf(...)
                sampleItems.forEach { item ->
                    database.child(item.itemId).setValue(item)
                }
            }
        }
        */
        // -------------------------------------------

        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                menuItemsList.clear()
                for (menuSnapshot in snapshot.children) {
                    val item = menuSnapshot.getValue(MenuItem::class.java)
                    item?.let { menuItemsList.add(it) }
                }
                adapter.updateItems(menuItemsList)
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@MenuActivity, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun addToCart(item: MenuItem) {
        val cartItem = com.baust.cafe.models.CartItem(
            itemId = item.itemId,
            itemName = item.name,
            price = item.price,
            quantity = 1,
            imageUrl = item.imageUrl
        )
        com.baust.cafe.utils.CartManager.addItem(cartItem)
        Toast.makeText(this, "${item.name} added to cart", Toast.LENGTH_SHORT).show()
    }
}