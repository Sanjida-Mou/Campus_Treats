package com.baust.cafe.activities

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.baust.cafe.R
import com.baust.cafe.models.User
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.util.concurrent.TimeUnit

class ForgotPasswordActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private val database = FirebaseDatabase.getInstance().getReference("Users")
    private var verificationId: String? = null

    private lateinit var recoveryIdentifier: EditText
    private lateinit var sendCodeButton: Button
    private lateinit var otpLayout: LinearLayout
    private lateinit var otpEditText: EditText
    private lateinit var verifyButton: Button
    private lateinit var backButton: ImageView
    private lateinit var statusText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)

        auth = FirebaseAuth.getInstance()

        recoveryIdentifier = findViewById(R.id.recoveryIdentifier)
        sendCodeButton = findViewById(R.id.sendCodeButton)
        otpLayout = findViewById(R.id.otpLayout)
        otpEditText = findViewById(R.id.otpEditText)
        verifyButton = findViewById(R.id.verifyButton)
        backButton = findViewById(R.id.backButton)
        statusText = findViewById(R.id.statusText)

        backButton.setOnClickListener { finish() }

        sendCodeButton.setOnClickListener {
            val input = recoveryIdentifier.text.toString().trim()
            if (input.isNotEmpty()) {
                findUserAndSendSms(input)
            } else {
                Toast.makeText(this, "Please enter your registered email or phone", Toast.LENGTH_SHORT).show()
            }
        }

        verifyButton.setOnClickListener {
            val code = otpEditText.text.toString().trim()
            if (code.isNotEmpty() && verificationId != null) {
                val credential = PhoneAuthProvider.getCredential(verificationId!!, code)
                verifyAndProceed(credential)
            } else {
                Toast.makeText(this, "Please enter the verification code", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun findUserAndSendSms(input: String) {
        statusText.visibility = View.VISIBLE
        statusText.text = "Searching for account..."
        
        val cleanInput = input.replace("\\D".toRegex(), "")

        database.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var foundUser: User? = null
                
                if (!snapshot.exists()) {
                    statusText.text = "No users found in database. Please Register."
                    return
                }

                // Log number of users for debugging (seen in status text)
                statusText.text = "Checking ${snapshot.childrenCount} registered accounts..."

                for (userSnapshot in snapshot.children) {
                    val user = userSnapshot.getValue(User::class.java)
                    if (user != null) {
                        val cleanRegisteredPhone = user.phoneNumber.replace("\\D".toRegex(), "")
                        
                        val isEmailMatch = user.email.trim().equals(input, ignoreCase = true)
                        val isPhoneMatch = cleanInput.isNotEmpty() && cleanRegisteredPhone.isNotEmpty() && 
                                           (cleanInput.endsWith(cleanRegisteredPhone) || cleanRegisteredPhone.endsWith(cleanInput))

                        if (isEmailMatch || isPhoneMatch) {
                            foundUser = user
                            break
                        }
                    }
                }

                if (foundUser != null) {
                    if (foundUser.phoneNumber.isNotEmpty()) {
                        statusText.text = "Account found. Sending SMS to ${foundUser.phoneNumber}..."
                        sendVerificationCode(foundUser.phoneNumber)
                    } else {
                        statusText.text = "Account found, but no phone registered. Sending reset link to email..."
                        sendEmailReset(foundUser.email)
                    }
                } else {
                    statusText.text = "No account found with this email or phone number."
                    Toast.makeText(this@ForgotPasswordActivity, "User not found. Check your details.", Toast.LENGTH_LONG).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                statusText.text = "Database error: ${error.message}"
            }
        })
    }

    private fun sendVerificationCode(phone: String) {
        // Ensure phone starts with + for Firebase
        val formattedPhone = if (phone.startsWith("+")) phone else "+88$phone"
        
        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(formattedPhone)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(this)
            .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    verifyAndProceed(credential)
                }

                override fun onVerificationFailed(e: FirebaseException) {
                    statusText.text = "Failed to send SMS: ${e.message}"
                    Toast.makeText(this@ForgotPasswordActivity, "SMS failed. Use Email reset or try later.", Toast.LENGTH_LONG).show()
                    // Fallback to email if SMS fails
                    // findUserAndSendEmail(formattedPhone) // Optional
                }

                override fun onCodeSent(id: String, token: PhoneAuthProvider.ForceResendingToken) {
                    verificationId = id
                    otpLayout.visibility = View.VISIBLE
                    sendCodeButton.visibility = View.GONE
                    statusText.text = "A 6-digit code has been sent to $formattedPhone"
                }
            })
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    private fun sendEmailReset(email: String) {
        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    statusText.text = "Password reset link sent to $email"
                    Toast.makeText(this, "Check your email", Toast.LENGTH_LONG).show()
                } else {
                    statusText.text = "Email failed: ${task.exception?.message}"
                }
            }
    }

    private fun verifyAndProceed(credential: PhoneAuthCredential) {
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    statusText.text = "Identity verified! You can now change your password."
                    Toast.makeText(this, "Verified Successfully", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Invalid verification code", Toast.LENGTH_SHORT).show()
                }
            }
    }
}