package com.baust.cafe.activities

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.baust.cafe.R
import com.google.firebase.auth.FirebaseAuth

class ForgotPasswordActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var recoveryIdentifier: EditText
    private lateinit var sendCodeButton: Button
    private lateinit var backButton: ImageView
    private lateinit var statusText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)

        auth = FirebaseAuth.getInstance()

        recoveryIdentifier = findViewById(R.id.recoveryIdentifier)
        sendCodeButton = findViewById(R.id.sendCodeButton)
        backButton = findViewById(R.id.backButton)
        statusText = findViewById(R.id.statusText)

        backButton.setOnClickListener { finish() }

        sendCodeButton.setOnClickListener {
            val email = recoveryIdentifier.text.toString().trim()
            if (email.isNotEmpty()) {
                if (android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    sendEmailReset(email)
                } else {
                    Toast.makeText(this, "Please enter a valid email address", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Please enter your registered email", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun sendEmailReset(email: String) {
        statusText.visibility = View.VISIBLE
        statusText.text = "Sending reset link..."
        sendCodeButton.isEnabled = false

        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                sendCodeButton.isEnabled = true
                if (task.isSuccessful) {
                    statusText.text = "A password reset link has been sent to $email. Please check your inbox and spam folder."
                    Toast.makeText(this, "Reset link sent!", Toast.LENGTH_LONG).show()
                } else {
                    val error = task.exception?.message ?: "Failed to send email"
                    statusText.text = "Error: $error"
                    Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
                }
            }
    }
}
