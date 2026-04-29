package com.baust.cafe.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.baust.cafe.R

class AdminLoginActivity : AppCompatActivity() {

    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var loginButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_login)

        emailEditText = findViewById(R.id.adminEmailEditText)
        passwordEditText = findViewById(R.id.adminPasswordEditText)
        loginButton = findViewById(R.id.adminLoginButton)

        loginButton.setOnClickListener {
            val email = emailEditText.text.toString()
            val password = passwordEditText.text.toString()

            // Simple admin check
            // Email and a secret code only the admin knows
            if (email == "sm.mou0137@gmail.com" && password == "0123") {
                Toast.makeText(this, "Admin Login Successful", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, AdminDashboardActivity::class.java))
                finish()
            } else {
                Toast.makeText(this, "Invalid Admin Credentials", Toast.LENGTH_SHORT).show()
            }
        }
    }
}