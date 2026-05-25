package com.baust.cafe.activities

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.baust.cafe.R

import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.baust.cafe.adapters.MenuAdapter
import com.baust.cafe.models.MenuItem
import com.google.firebase.database.*
import android.widget.Toast

class ManageMenuActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: MenuAdapter
    private val menuItemsList = mutableListOf<MenuItem>()
    private lateinit var database: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manage_menu)

        recyclerView = findViewById(R.id.manageMenuRecyclerView)
        // Set to 2-column Grid for Admin too
        recyclerView.layoutManager = androidx.recyclerview.widget.GridLayoutManager(this, 2)

        adapter = MenuAdapter(
            menuItems = menuItemsList,
            isAdmin = true,
            onAvailabilityChanged = { item, isAvailable ->
                updateItemAvailability(item, isAvailable)
            },
            onEditClicked = { item ->
                val intent = Intent(this, EditMenuItemActivity::class.java).apply {
                    putExtra("ITEM_ID", item.itemId)
                    putExtra("ITEM_NAME", item.name)
                    putExtra("ITEM_DESC", item.description)
                    putExtra("ITEM_PRICE", item.price)
                    putExtra("ITEM_CAT", item.category)
                    putExtra("ITEM_IMAGE", item.imageUrl)
                    putExtra("ITEM_AVAILABLE", item.available)
                }
                startActivity(intent)
            }
        )
        recyclerView.adapter = adapter

        findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(R.id.addMenuFab).setOnClickListener {
            startActivity(Intent(this, AddMenuItemActivity::class.java))
        }

        fetchMenuData()
    }

    private fun fetchMenuData() {
        database = FirebaseDatabase.getInstance().getReference("Menu")
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
                Toast.makeText(this@ManageMenuActivity, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun updateItemAvailability(item: MenuItem, isAvailable: Boolean) {
        database.child(item.itemId).child("available").setValue(isAvailable)
            .addOnSuccessListener {
                val status = if (isAvailable) "Available" else "Out of Stock"
                Toast.makeText(this, "${item.name} is now $status", Toast.LENGTH_SHORT).show()
            }
    }
}
