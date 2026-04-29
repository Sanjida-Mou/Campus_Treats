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
    }

    private fun fetchMenuData() {
        database = FirebaseDatabase.getInstance().getReference("Menu")
        
        // --- ADDING SAMPLE DATA IF MENU IS EMPTY ---
        database.get().addOnSuccessListener { snapshot ->
            if (!snapshot.exists()) {
                val sampleItems = listOf(
                    MenuItem("1", "Burger", "Juicy chicken burger with cheese", 120.0, "Snacks", true),
                    MenuItem("2", "Sandwich", "Healthy club sandwich with egg", 80.0, "Snacks", true),
                    MenuItem("3", "Shingara", "Classic potato shingara (2 pcs)", 20.0, "Snacks", true),
                    MenuItem("4", "Puri", "Crispy dal puri (2 pcs)", 20.0, "Snacks", false),
                    MenuItem("5", "Chicken Biriyani", "Fragrant basmati rice with chicken", 180.0, "Meals", true),
                    MenuItem("6", "Khichuri", "Delicious bhuna khichuri with egg", 100.0, "Meals", true),
                    MenuItem("7", "Chola", "Spicy chola masala with paratha", 40.0, "Breakfast", true)
                )
                sampleItems.forEach { item ->
                    database.child(item.itemId).setValue(item)
                }
            }
        }
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
        // Logic to add item to Cart (e.g., saving to Firebase or local storage)
        Toast.makeText(this, "${item.name} added to cart", Toast.LENGTH_SHORT).show()
    }
}