package com.baust.cafe.activities

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.baust.cafe.R
import com.baust.cafe.models.User
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.database.*
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
    private lateinit var newPasswordLayout: LinearLayout
    private lateinit var newPasswordEdit: EditText
    private lateinit var updatePasswordButton: Button
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
        newPasswordLayout = findViewById(R.id.newPasswordLayout)
        newPasswordEdit = findViewById(R.id.newPasswordEdit)
        updatePasswordButton = findViewById(R.id.updatePasswordButton)
        backButton = findViewById(R.id.backButton)
        statusText = findViewById(R.id.statusText)

        backButton.setOnClickListener { finish() }

        sendCodeButton.setOnClickListener {
            val input = recoveryIdentifier.text.toString().trim()
            if (input.isNotEmpty()) {
                findUserAndInitiateRecovery(input)
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

        updatePasswordButton.setOnClickListener {
            val newPass = newPasswordEdit.text.toString().trim()
            if (newPass.length >= 6) {
                updateAccountPassword(newPass)
            } else {
                Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun findUserAndInitiateRecovery(input: String) {
        statusText.visibility = View.VISIBLE
        statusText.text = "Searching for account..."
        
        val cleanInput = input.replace("\\D".toRegex(), "")

        database.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var foundUser: User? = null
                
                if (!snapshot.exists()) {
                    statusText.text = "No users found in database."
                    return
                }

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
                    // 1. Always send Email Link as primary secure method
                    sendEmailReset(foundUser.email)
                    
                    // 2. If phone exists, send SMS Code (Free OTP)
                    if (foundUser.phoneNumber.isNotEmpty()) {
                        statusText.text = "Reset link sent to ${foundUser.email}.\nSending SMS verification code to ${foundUser.phoneNumber}..."
                        sendVerificationCode(foundUser.phoneNumber)
                    } else {
                        statusText.text = "Reset link sent to ${foundUser.email}. No phone registered for SMS."
                        Toast.makeText(this@ForgotPasswordActivity, "Check your email for the link", Toast.LENGTH_LONG).show()
                    }
                } else {
                    statusText.text = "No account found with this email or phone."
                    Toast.makeText(this@ForgotPasswordActivity, "User not found.", Toast.LENGTH_LONG).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                statusText.text = "Database error: ${error.message}"
            }
        })
    }

    private fun sendVerificationCode(phone: String) {
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
                    statusText.text = "SMS sending failed. Please use the reset link sent to your email instead."
                    android.util.Log.e("SMS_ERROR", e.message ?: "Unknown error")
                }

                override fun onCodeSent(id: String, token: PhoneAuthProvider.ForceResendingToken) {
                    verificationId = id
                    otpLayout.visibility = View.VISIBLE
                    sendCodeButton.visibility = View.GONE
                    statusText.text = "Reset link sent to Email. 6-digit SMS code sent to $formattedPhone"
                }
            })
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    private fun sendEmailReset(email: String) {
        auth.sendPasswordResetEmail(email).addOnCompleteListener { }
    }

    private fun verifyAndProceed(credential: PhoneAuthCredential) {
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    statusText.text = "Identity verified! Please set your new password below."
                    otpLayout.visibility = View.GONE
                    newPasswordLayout.visibility = View.VISIBLE
                } else {
                    Toast.makeText(this, "Invalid verification code", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun updateAccountPassword(newPass: String) {
        val user = auth.currentUser
        user?.updatePassword(newPass)?.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                statusText.text = "Password updated successfully! You can now log in."
                Toast.makeText(this, "Success! Please Login", Toast.LENGTH_LONG).show()
                finish()
            } else {
                statusText.text = "Update failed: ${task.exception?.message}. Try using the link sent to your email."
            }
        }
    }
}
