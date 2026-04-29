package com.baust.cafe.activities

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
        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = MenuAdapter(
            menuItems = menuItemsList,
            isAdmin = true,
            onAvailabilityChanged = { item, isAvailable ->
                updateItemAvailability(item, isAvailable)
            }
        )
        recyclerView.adapter = adapter

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
        database.child(item.itemId).child("isAvailable").setValue(isAvailable)
            .addOnSuccessListener {
                val status = if (isAvailable) "Available" else "Out of Stock"
                Toast.makeText(this, "${item.name} is now $status", Toast.LENGTH_SHORT).show()
            }
    }
}