package com.baust.cafe.activities

import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.baust.cafe.R
import com.baust.cafe.models.MenuItem
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageException
import java.util.UUID

class AddMenuItemActivity : AppCompatActivity() {

    private lateinit var itemName: EditText
    private lateinit var itemDescription: EditText
    private lateinit var itemPrice: EditText
    private lateinit var itemCategory: EditText
    private lateinit var itemImageUrlField: EditText
    private lateinit var addFoodButton: Button
    private lateinit var addItemImage: ImageView
    private lateinit var selectImageFab: FloatingActionButton
    private lateinit var progressDialog: ProgressDialog

    private var selectedImageUri: Uri? = null
    private val PICK_IMAGE_REQUEST = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_menu_item)

        itemName = findViewById(R.id.itemName)
        itemDescription = findViewById(R.id.itemDescription)
        itemPrice = findViewById(R.id.itemPrice)
        itemCategory = findViewById(R.id.itemCategory)
        itemImageUrlField = findViewById(R.id.itemImageUrl)
        addFoodButton = findViewById(R.id.addFoodButton)
        addItemImage = findViewById(R.id.addItemImage)
        selectImageFab = findViewById(R.id.selectImageFab)

        progressDialog = ProgressDialog(this)
        progressDialog.setMessage("Saving Food Item...")
        progressDialog.setCancelable(false)

        selectImageFab.setOnClickListener {
            openGallery()
        }

        addFoodButton.setOnClickListener {
            uploadFoodItem()
        }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null && data.data != null) {
            selectedImageUri = data.data
            // Clear URL field if image is selected
            itemImageUrlField.setText("")
            Glide.with(this).load(selectedImageUri).into(addItemImage)
        }
    }

    private fun uploadFoodItem() {
        val name = itemName.text.toString().trim()
        val desc = itemDescription.text.toString().trim()
        val priceStr = itemPrice.text.toString().trim()
        val category = itemCategory.text.toString().trim()
        val directUrl = itemImageUrlField.text.toString().trim()

        if (name.isEmpty() || priceStr.isEmpty()) {
            Toast.makeText(this, "Please fill name and price", Toast.LENGTH_SHORT).show()
            return
        }

        val price = try { priceStr.toDouble() } catch (e: Exception) { 0.0 }
        val itemId = UUID.randomUUID().toString()

        // IMPORTANT: If a URL is provided, we skip Storage entirely!
        if (directUrl.isNotEmpty()) {
            progressDialog.show()
            addFoodButton.isEnabled = false
            saveToDatabase(itemId, name, desc, price, category, directUrl)
            return
        }

        // If no URL, but an image was selected from gallery
        if (selectedImageUri != null) {
            progressDialog.show()
            addFoodButton.isEnabled = false
            performUpload(itemId, name, desc, price, category)
        } else {
            Toast.makeText(this, "Please paste a URL or select an image", Toast.LENGTH_SHORT).show()
        }
    }

    private fun performUpload(itemId: String, name: String, desc: String, price: Double, category: String) {
        val storageRef = FirebaseStorage.getInstance().getReference("MenuImages").child("$itemId.jpg")
        
        storageRef.putFile(selectedImageUri!!)
            .addOnSuccessListener { taskSnapshot ->
                taskSnapshot.storage.downloadUrl.addOnSuccessListener { uri ->
                    saveToDatabase(itemId, name, desc, price, category, uri.toString())
                }
            }
            .addOnFailureListener { e ->
                progressDialog.dismiss()
                addFoodButton.isEnabled = true
                val msg = if (e is StorageException && e.errorCode == StorageException.ERROR_NOT_AUTHORIZED) {
                    "Permission Denied. Use the 'Paste URL' method instead."
                } else {
                    e.localizedMessage
                }
                Toast.makeText(this, "Upload failed: $msg", Toast.LENGTH_LONG).show()
            }
    }

    private fun saveToDatabase(itemId: String, name: String, desc: String, price: Double, category: String, imageUrl: String) {
        val newItem = MenuItem(
            itemId = itemId,
            name = name,
            description = desc,
            price = price,
            category = category,
            available = true,
            imageUrl = imageUrl,
            preparationTime = 15
        )
        
        FirebaseDatabase.getInstance().getReference("Menu").child(itemId).setValue(newItem)
            .addOnSuccessListener {
                progressDialog.dismiss()
                Toast.makeText(this, "Food added successfully!", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                progressDialog.dismiss()
                addFoodButton.isEnabled = true
                Toast.makeText(this, "Database error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
