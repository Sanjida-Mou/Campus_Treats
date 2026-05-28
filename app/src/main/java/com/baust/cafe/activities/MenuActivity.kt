package com.baust.cafe.activities

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.Toast
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.baust.cafe.R
import com.baust.cafe.adapters.MenuAdapter
import com.baust.cafe.models.MenuItem
import com.google.firebase.database.*
import java.util.Locale

class MenuActivity : BaseUserActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var searchEdit: EditText
    private lateinit var adapter: MenuAdapter
    private val allMenuItems = mutableListOf<MenuItem>()
    private val filteredMenuItems = mutableListOf<MenuItem>()
    private lateinit var database: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_menu)

        recyclerView = findViewById(R.id.menuRecyclerView)
        searchEdit = findViewById(R.id.menuSearchEdit)
        
        recyclerView.layoutManager = androidx.recyclerview.widget.GridLayoutManager(this, 2)
        
        adapter = MenuAdapter(
            menuItems = filteredMenuItems,
            isAdmin = false,
            onAddToCartClicked = { menuItem ->
                addToCart(menuItem)
            }
        )
        recyclerView.adapter = adapter

        setupSearch()
        fetchMenuData()
        setupNavigation()
        
        setupBottomNavigation(R.id.navBookmark)
    }

    private fun setupSearch() {
        searchEdit.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterMenu(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun filterMenu(query: String) {
        filteredMenuItems.clear()
        if (query.isEmpty()) {
            filteredMenuItems.addAll(allMenuItems)
        } else {
            val lowerQuery = query.lowercase(Locale.getDefault())
            for (item in allMenuItems) {
                if (item.name.lowercase(Locale.getDefault()).contains(lowerQuery) || 
                    item.category.lowercase(Locale.getDefault()).contains(lowerQuery)) {
                    filteredMenuItems.add(item)
                }
            }
        }
        adapter.notifyDataSetChanged()
    }

    private fun setupNavigation() {
        findViewById<android.widget.ImageView>(R.id.navHome).setOnClickListener {
            startActivity(Intent(this, HomeActivity::class.java))
            finish()
        }

        findViewById<android.widget.ImageView>(R.id.navCart).setOnClickListener {
            startActivity(Intent(this, CartActivity::class.java))
            finish()
        }

        findViewById<android.widget.ImageView>(R.id.navBookmark).setOnClickListener {
            // Already on Menu
        }

        findViewById<android.widget.ImageView>(R.id.navNotifications).setOnClickListener {
            Toast.makeText(this, "No new notifications", Toast.LENGTH_SHORT).show()
        }

        findViewById<android.widget.ImageView>(R.id.navProfile).setOnClickListener {
            startActivity(Intent(this, UserDashboardActivity::class.java))
            finish()
        }

    }

    private fun fetchMenuData() {
        database = FirebaseDatabase.getInstance().getReference("Menu")
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                allMenuItems.clear()
                for (menuSnapshot in snapshot.children) {
                    val item = menuSnapshot.getValue(MenuItem::class.java)
                    item?.let { allMenuItems.add(it) }
                }
                filterMenu(searchEdit.text.toString())
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
