package com.baust.cafe.activities

import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.baust.cafe.R
import com.baust.cafe.models.MenuItem
import com.bumptech.glide.Glide
import com.google.firebase.database.FirebaseDatabase
import java.util.UUID

class EditMenuItemActivity : AppCompatActivity() {

    private lateinit var itemName: EditText
    private lateinit var itemDescription: EditText
    private lateinit var itemPrice: EditText
    private lateinit var itemCategory: EditText
    private lateinit var itemImageUrlField: EditText
    private lateinit var updateFoodButton: Button
    private lateinit var editItemImage: ImageView
    private lateinit var progressDialog: ProgressDialog

    private var selectedImageUri: Uri? = null
    private val PICK_IMAGE_REQUEST = 1
    private var itemId: String = ""
    private var currentImageUrl: String = ""
    private var isAvailable: Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_menu_item) // Reuse layout

        itemName = findViewById(R.id.itemName)
        itemDescription = findViewById(R.id.itemDescription)
        itemPrice = findViewById(R.id.itemPrice)
        itemCategory = findViewById(R.id.itemCategory)
        itemImageUrlField = findViewById(R.id.itemImageUrl)
        updateFoodButton = findViewById(R.id.addFoodButton)
        editItemImage = findViewById(R.id.addItemImage)

        updateFoodButton.text = "UPDATE FOOD ITEM"

        progressDialog = ProgressDialog(this)
        progressDialog.setMessage("Updating Food Item...")
        progressDialog.setCancelable(false)

        // Get data from Intent
        itemId = intent.getStringExtra("ITEM_ID") ?: ""
        itemName.setText(intent.getStringExtra("ITEM_NAME"))
        itemDescription.setText(intent.getStringExtra("ITEM_DESC"))
        itemPrice.setText(intent.getDoubleExtra("ITEM_PRICE", 0.0).toString())
        itemCategory.setText(intent.getStringExtra("ITEM_CAT"))
        currentImageUrl = intent.getStringExtra("ITEM_IMAGE") ?: ""
        isAvailable = intent.getBooleanExtra("ITEM_AVAILABLE", true)
        itemImageUrlField.setText(currentImageUrl)

        if (currentImageUrl.isNotEmpty()) {
            Glide.with(this).load(currentImageUrl).placeholder(R.drawable.cafe_logo).into(editItemImage)
        }

        findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(R.id.selectImageFab).setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent, PICK_IMAGE_REQUEST)
        }

        updateFoodButton.setOnClickListener {
            updateFoodItem()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null && data.data != null) {
            selectedImageUri = data.data
            itemImageUrlField.setText("") // Clear URL if local image picked
            Glide.with(this).load(selectedImageUri).into(editItemImage)
        }
    }

    private fun updateFoodItem() {
        val name = itemName.text.toString().trim()
        val desc = itemDescription.text.toString().trim()
        val priceStr = itemPrice.text.toString().trim()
        val category = itemCategory.text.toString().trim()
        val directUrl = itemImageUrlField.text.toString().trim()

        if (name.isEmpty() || priceStr.isEmpty()) {
            Toast.makeText(this, "Please fill name and price", Toast.LENGTH_SHORT).show()
            return
        }

        val price = priceStr.toDouble()
        val imageUrl = if (directUrl.isNotEmpty()) directUrl else currentImageUrl

        progressDialog.show()

        val updatedItem = MenuItem(
            itemId = itemId,
            name = name,
            description = desc,
            price = price,
            category = category,
            available = isAvailable,
            imageUrl = imageUrl,
            preparationTime = 15
        )

        FirebaseDatabase.getInstance().getReference("Menu").child(itemId).setValue(updatedItem)
            .addOnSuccessListener {
                progressDialog.dismiss()
                Toast.makeText(this, "Food updated successfully!", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                progressDialog.dismiss()
                Toast.makeText(this, "Update failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
