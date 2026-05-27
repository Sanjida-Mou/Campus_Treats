package com.baust.cafe.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.baust.cafe.R
import com.google.firebase.auth.FirebaseAuth

class AdminLoginActivity : AppCompatActivity() {

    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var loginButton: Button
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_login)

        auth = FirebaseAuth.getInstance()
        emailEditText = findViewById(R.id.adminEmailEditText)
        passwordEditText = findViewById(R.id.adminPasswordEditText)
        loginButton = findViewById(R.id.adminLoginButton)

        loginButton.setOnClickListener {
            // trim() removes accidental spaces at the beginning or end
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // EQUALS IGNORE CASE: This makes it easier to log in if your phone 
            // capitalizes the first letter of the email automatically.
            if (email.equals("sm.mou0137@gmail.com", ignoreCase = true) && password == "0123") {
                
                Toast.makeText(this, "Admin Login Successful", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, AdminDashboardActivity::class.java))
                
                // Try Firebase login in background for features that need it
                auth.signInWithEmailAndPassword(email, "0123")

                finish()
            } else {
                Toast.makeText(this, "Invalid Admin Credentials", Toast.LENGTH_SHORT).show()
            }
        }

        findViewById<android.widget.ImageView>(R.id.backToWelcomeBtn).setOnClickListener {
            val intent = Intent(this, WelcomeActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }
}