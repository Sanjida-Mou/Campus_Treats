package com.baust.cafe.activities

import android.content.Intent
import android.os.Bundle
import androidx.recyclerview.widget.RecyclerView
import com.baust.cafe.R
import com.baust.cafe.adapters.MenuAdapter
import com.baust.cafe.models.MenuItem
import com.baust.cafe.models.UserNotification
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import android.widget.Toast

class ManageMenuActivity : BaseAdminActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: MenuAdapter
    private val menuItemsList = mutableListOf<MenuItem>()
    private lateinit var database: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manage_menu)

        recyclerView = findViewById(R.id.manageMenuRecyclerView)
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
        setupBottomNavigation(0)
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
                if (FirebaseAuth.getInstance().currentUser != null && error.code != DatabaseError.PERMISSION_DENIED) {
                    Toast.makeText(this@ManageMenuActivity, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            }
        })
    }

    private fun updateItemAvailability(item: MenuItem, isAvailable: Boolean) {
        database.child(item.itemId).child("available").setValue(isAvailable)
            .addOnSuccessListener {
                val status = if (isAvailable) "Available" else "Out of Stock"
                Toast.makeText(this, "${item.name} is now $status", Toast.LENGTH_SHORT).show()
                
                // BROADCAST NOTIFICATION TO ALL USERS WITH EXACT REQUESTED WORDING
                broadcastStockUpdate(item.name, isAvailable)
            }
    }

    private fun broadcastStockUpdate(itemName: String, isAvailable: Boolean) {
        val usersDb = FirebaseDatabase.getInstance().getReference("Users")
        // Exact wording: "That item is out of stock right now..." or "the item is available now.."
        val message = if (isAvailable) "$itemName is available now.." else "$itemName is out of stock right now..."
        
        usersDb.get().addOnSuccessListener { snapshot ->
            for (userSnapshot in snapshot.children) {
                val userId = userSnapshot.key ?: continue
                
                val notifyDb = FirebaseDatabase.getInstance().getReference("UserNotifications").child(userId)
                val id = notifyDb.push().key ?: continue
                val notification = UserNotification(
                    id = id,
                    title = "Menu Update",
                    message = message,
                    type = "stock",
                    timestamp = System.currentTimeMillis(),
                    read = false
                )
                notifyDb.child(id).setValue(notification)
            }
        }.addOnFailureListener {
            android.util.Log.e("ManageMenu", "Failed to broadcast: ${it.message}")
        }
    }
}
