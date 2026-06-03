package com.baust.cafe.activities

import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.baust.cafe.R
import com.baust.cafe.models.MenuItem
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.bumptech.glide.Glide
import com.google.firebase.database.FirebaseDatabase
import java.util.UUID
import java.io.ByteArrayOutputStream

class AddMenuItemActivity : AppCompatActivity() {

    private lateinit var itemName: EditText
    private lateinit var itemDescription: EditText
    private lateinit var itemPrice: EditText
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
            Glide.with(this).load(selectedImageUri).into(addItemImage)
        }
    }

    private fun uploadFoodItem() {
        val name = itemName.text.toString().trim()
        val desc = itemDescription.text.toString().trim()
        val priceStr = itemPrice.text.toString().trim()

        if (name.isEmpty() || priceStr.isEmpty()) {
            Toast.makeText(this, "Please fill name and price", Toast.LENGTH_SHORT).show()
            return
        }

        if (selectedImageUri == null) {
            Toast.makeText(this, "Please select an image from gallery", Toast.LENGTH_SHORT).show()
            return
        }

        progressDialog.show()
        val price = try { priceStr.toDouble() } catch (e: Exception) { 0.0 }
        val itemId = UUID.randomUUID().toString()

        // CONVERT IMAGE TO BASE64
        try {
            val inputStream = contentResolver.openInputStream(selectedImageUri!!)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            
            if (bitmap == null) {
                progressDialog.dismiss()
                Toast.makeText(this, "Could not load image", Toast.LENGTH_SHORT).show()
                return
            }

            val scaledBitmap = scaleBitmap(bitmap, 600)
            
            val outputStream = ByteArrayOutputStream()
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 40, outputStream)
            val byteArray = outputStream.toByteArray()
            val base64Image = Base64.encodeToString(byteArray, Base64.NO_WRAP)
            
            saveToDatabase(itemId, name, desc, price, base64Image)
        } catch (e: Exception) {
            progressDialog.dismiss()
            Toast.makeText(this, "Error processing image: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun scaleBitmap(bitmap: Bitmap, maxSize: Int): Bitmap {
        var width = bitmap.width
        var height = bitmap.height
        val bitmapRatio = width.toFloat() / height.toFloat()
        if (bitmapRatio > 1) {
            width = maxSize
            height = (width / bitmapRatio).toInt()
        } else {
            height = maxSize
            width = (height * bitmapRatio).toInt()
        }
        return Bitmap.createScaledBitmap(bitmap, width, height, true)
    }

    private fun saveToDatabase(itemId: String, name: String, desc: String, price: Double, imageUrl: String) {
        val newItem = MenuItem(
            itemId = itemId,
            name = name,
            description = desc,
            price = price,
            category = "General",
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
                Toast.makeText(this, "Database error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
