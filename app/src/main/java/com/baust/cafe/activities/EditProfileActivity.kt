package com.baust.cafe.activities

import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.baust.cafe.R
import com.baust.cafe.models.User
import com.bumptech.glide.Glide
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageException
import de.hdodenhof.circleimageview.CircleImageView

class EditProfileActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var storage: FirebaseStorage
    
    private lateinit var profileImageView: CircleImageView
    private lateinit var editName: EditText
    private lateinit var editPhone: EditText
    private lateinit var editLocation: EditText
    private lateinit var editStudentId: EditText
    private lateinit var saveButton: Button
    private lateinit var backButton: ImageView
    private lateinit var changeImageFab: FloatingActionButton
    private lateinit var progressDialog: ProgressDialog

    private var selectedImageUri: Uri? = null
    private var currentUser: User? = null

    companion object {
        private const val PICK_IMAGE_REQUEST = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()
        storage = FirebaseStorage.getInstance()

        profileImageView = findViewById(R.id.editProfileImage)
        editName = findViewById(R.id.editName)
        editPhone = findViewById(R.id.editPhone)
        editLocation = findViewById(R.id.editLocation)
        editStudentId = findViewById(R.id.editStudentId)
        saveButton = findViewById(R.id.saveProfileButton)
        backButton = findViewById(R.id.backButton)
        changeImageFab = findViewById(R.id.changeImageFab)

        progressDialog = ProgressDialog(this)
        progressDialog.setMessage("Updating profile...")
        progressDialog.setCancelable(false)

        backButton.setOnClickListener { finish() }

        changeImageFab.setOnClickListener {
            openGallery()
        }

        saveButton.setOnClickListener {
            updateProfile()
        }

        loadCurrentData()
    }

    private fun openGallery() {
        val intent = Intent()
        intent.type = "image/*"
        intent.action = Intent.ACTION_GET_CONTENT
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null && data.data != null) {
            selectedImageUri = data.data
            profileImageView.setImageURI(selectedImageUri)
        }
    }

    private fun loadCurrentData() {
        val userId = auth.currentUser?.uid ?: return
        database.getReference("Users").child(userId).get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                currentUser = snapshot.getValue(User::class.java)
                currentUser?.let {
                    editName.setText(it.name)
                    editPhone.setText(it.phoneNumber)
                    editLocation.setText(it.location)
                    editStudentId.setText(it.studentId)
                    
                    if (it.profileImage.isNotEmpty()) {
                        if (it.profileImage.startsWith("http")) {
                            // It's a normal URL
                            Glide.with(this).load(it.profileImage).placeholder(R.drawable.cafe_logo).into(profileImageView)
                        } else {
                            // It's your personal photo (Base64)
                            try {
                                val imageBytes = android.util.Base64.decode(it.profileImage, android.util.Base64.DEFAULT)
                                val decodedImage = android.graphics.BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                                profileImageView.setImageBitmap(decodedImage)
                            } catch (e: Exception) {
                                profileImageView.setImageResource(R.drawable.cafe_logo)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun updateProfile() {
        val name = editName.text.toString().trim()
        val phone = editPhone.text.toString().trim()
        val location = editLocation.text.toString().trim()
        val studentId = editStudentId.text.toString().trim()

        if (name.isEmpty()) {
            Toast.makeText(this, "Name cannot be empty", Toast.LENGTH_SHORT).show()
            return
        }

        val userId = auth.currentUser?.uid ?: return
        progressDialog.show()

        if (selectedImageUri != null) {
            // CONVERT IMAGE TO STRING (Base64)
            try {
                val inputStream = contentResolver.openInputStream(selectedImageUri!!)
                val bitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
                
                // Compress image so it fits in Database
                val outputStream = java.io.ByteArrayOutputStream()
                bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 25, outputStream)
                val byteArray = outputStream.toByteArray()
                val base64Image = android.util.Base64.encodeToString(byteArray, android.util.Base64.DEFAULT)
                
                saveToDatabase(userId, name, phone, location, studentId, base64Image)
            } catch (e: Exception) {
                progressDialog.dismiss()
                Toast.makeText(this, "Error processing image: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
            saveToDatabase(userId, name, phone, location, studentId, currentUser?.profileImage ?: "")
        }
    }

    private fun saveToDatabase(userId: String, name: String, phone: String, location: String, studentId: String, imageUrl: String) {
        val updates = HashMap<String, Any>()
        updates["name"] = name
        updates["phoneNumber"] = phone
        updates["location"] = location
        updates["studentId"] = studentId
        updates["profileImage"] = imageUrl

        database.getReference("Users").child(userId).updateChildren(updates)
            .addOnSuccessListener {
                progressDialog.dismiss()
                Toast.makeText(this, "Profile updated successfully!", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener {
                progressDialog.dismiss()
                Toast.makeText(this, "Failed to update profile", Toast.LENGTH_SHORT).show()
            }
    }
}
