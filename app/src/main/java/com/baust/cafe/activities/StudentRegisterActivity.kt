package com.baust.cafe.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.baust.cafe.R

class StudentRegisterActivity : AppCompatActivity() {

    // Declare UI elements
    private lateinit var nameEditText: EditText
    private lateinit var studentIdEditText: EditText
    private lateinit var emailEditText: EditText
    private lateinit var phoneEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var confirmPasswordEditText: EditText
    private lateinit var registerButton: Button
    private lateinit var loginLink: TextView
    private lateinit var googleSignUpButton: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_student_register)

        // Initialize UI elements
        nameEditText = findViewById(R.id.nameEditText)
        studentIdEditText = findViewById(R.id.studentIdEditText)
        emailEditText = findViewById(R.id.emailEditText)
        phoneEditText = findViewById(R.id.phoneEditText)
        passwordEditText = findViewById(R.id.passwordEditText)
        confirmPasswordEditText = findViewById(R.id.confirmPasswordEditText)
        registerButton = findViewById(R.id.registerButton)
        loginLink = findViewById(R.id.loginLink)
        googleSignUpButton = findViewById(R.id.googleSignUpButton)

        // Set up click listeners
        setupClickListeners()
    }

    private fun setupClickListeners() {
        // REGISTER BUTTON CLICK
        registerButton.setOnClickListener {
            val name = nameEditText.text.toString()
            val studentId = studentIdEditText.text.toString()
            val email = emailEditText.text.toString()
            val phone = phoneEditText.text.toString()
            val password = passwordEditText.text.toString()
            val confirmPassword = confirmPasswordEditText.text.toString()

            // Validation
            when {
                name.isEmpty() -> Toast.makeText(this, "Please enter your name", Toast.LENGTH_SHORT).show()
                studentId.isEmpty() -> Toast.makeText(this, "Please enter student ID", Toast.LENGTH_SHORT).show()
                email.isEmpty() -> Toast.makeText(this, "Please enter email", Toast.LENGTH_SHORT).show()
                phone.isEmpty() -> Toast.makeText(this, "Please enter phone number", Toast.LENGTH_SHORT).show()
                password.isEmpty() -> Toast.makeText(this, "Please enter password", Toast.LENGTH_SHORT).show()
                password != confirmPassword -> Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
                password.length < 6 -> Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
                else -> {
                    // Firebase Registration
                    val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
                    val database = com.google.firebase.database.FirebaseDatabase.getInstance().getReference("Users")

                    auth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                val userId = auth.currentUser?.uid ?: ""
                                val user = com.baust.cafe.models.User(
                                    userId = userId,
                                    name = name,
                                    email = email,
                                    studentId = studentId,
                                    phoneNumber = phone,
                                    userType = "student"
                                )

                                database.child(userId).setValue(user)
                                    .addOnSuccessListener {
                                        Toast.makeText(this, "Registration successful!", Toast.LENGTH_SHORT).show()
                                        finish()
                                    }
                                    .addOnFailureListener {
                                        Toast.makeText(this, "Failed to save user data: ${it.message}", Toast.LENGTH_SHORT).show()
                                    }
                            } else {
                                Toast.makeText(this, "Registration failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                }
            }
        }

        // LOGIN LINK CLICK - THIS TAKES YOU BACK TO LOGIN PAGE
        loginLink.setOnClickListener {
            finish() // This closes RegisterActivity and goes back to MainActivity
        }

        // GOOGLE SIGN UP CLICK
        googleSignUpButton.setOnClickListener {
            Toast.makeText(this, "Google Sign Up is coming soon! Please use Email registration for now.", Toast.LENGTH_LONG).show()
        }
    }
}