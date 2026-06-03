package com.baust.cafe.activities

import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.baust.cafe.R
import com.baust.cafe.models.User
import com.bumptech.glide.Glide
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import de.hdodenhof.circleimageview.CircleImageView
import java.io.ByteArrayOutputStream
import java.util.HashMap

class EditProfileActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    
    private lateinit var profileImageView: CircleImageView
    private lateinit var editName: EditText
    private lateinit var editPhone: EditText
    private lateinit var editLocation: EditText
    private lateinit var editStudentId: EditText
    private lateinit var saveButton: Button
    private lateinit var backButton: ImageView
    private lateinit var changeImageFab: FloatingActionButton
    private lateinit var imgLocationPicker: ImageView
    private lateinit var progressDialog: ProgressDialog

    private var selectedImageUri: Uri? = null
    private var currentUser: User? = null

    companion object {
        private const val PICK_IMAGE_REQUEST = 1
        private const val SELECT_LOCATION_REQUEST = 2
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        profileImageView = findViewById(R.id.editProfileImage)
        editName = findViewById(R.id.editName)
        editPhone = findViewById(R.id.editPhone)
        editLocation = findViewById(R.id.editLocation)
        editStudentId = findViewById(R.id.editStudentId)
        saveButton = findViewById(R.id.saveProfileButton)
        backButton = findViewById(R.id.backButton)
        changeImageFab = findViewById(R.id.changeImageFab)
        imgLocationPicker = findViewById(R.id.imgLocationPicker)

        progressDialog = ProgressDialog(this)
        progressDialog.setMessage("Updating profile...")
        progressDialog.setCancelable(false)

        backButton.setOnClickListener { finish() }

        changeImageFab.setOnClickListener { openGallery() }

        saveButton.setOnClickListener { updateProfile() }

        profileImageView.setOnClickListener { showBigPicture() }

        imgLocationPicker.setOnClickListener {
            val intent = Intent(this, SelectLocationActivity::class.java)
            startActivityForResult(intent, SELECT_LOCATION_REQUEST)
        }

        loadCurrentData()
    }

    private fun showBigPicture() {
        val user = currentUser ?: return
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_user_details, null)
        val dialog = AlertDialog.Builder(this).setView(dialogView).create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val bigImage = dialogView.findViewById<ImageView>(R.id.dialogUserImage)
        val nameText = dialogView.findViewById<TextView>(R.id.dialogUserName)
        val detailsText = dialogView.findViewById<TextView>(R.id.dialogUserDetails)
        val okBtn = dialogView.findViewById<Button>(R.id.dialogOkButton)

        nameText.text = user.name
        detailsText.text = "Student ID: ${user.studentId}"
        
        val imageUrl = user.profileImage
        if (imageUrl.isNotEmpty() && imageUrl != "null") {
            if (imageUrl.startsWith("http")) {
                Glide.with(this).load(imageUrl).into(bigImage)
            } else {
                try {
                    val imageBytes = Base64.decode(imageUrl, Base64.DEFAULT)
                    val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                    bigImage.setImageBitmap(bitmap)
                } catch (e: Exception) {
                    bigImage.setImageResource(R.drawable.ic_person)
                }
            }
        }
        okBtn.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun openGallery() {
        val intent = Intent()
        intent.type = "image/*"
        intent.action = Intent.ACTION_GET_CONTENT
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && data != null) {
            when (requestCode) {
                PICK_IMAGE_REQUEST -> {
                    selectedImageUri = data.data
                    profileImageView.setImageURI(selectedImageUri)
                }
                SELECT_LOCATION_REQUEST -> {
                    val address = data.getStringExtra("SELECTED_ADDRESS")
                    if (!address.isNullOrEmpty()) {
                        editLocation.setText(address)
                    }
                }
            }
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
                            Glide.with(this).load(it.profileImage).placeholder(R.drawable.cafe_logo).into(profileImageView)
                        } else {
                            try {
                                val imageBytes = Base64.decode(it.profileImage, Base64.DEFAULT)
                                val decodedImage = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                                profileImageView.setImageBitmap(decodedImage)
                            } catch (e: Exception) {
                                profileImageView.setImageResource(R.drawable.ic_person)
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
            try {
                val inputStream = contentResolver.openInputStream(selectedImageUri!!)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                val outputStream = ByteArrayOutputStream()
                bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 25, outputStream)
                val byteArray = outputStream.toByteArray()
                val base64Image = Base64.encodeToString(byteArray, Base64.DEFAULT)
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
