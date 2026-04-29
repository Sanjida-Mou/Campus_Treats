package com.baust.cafe.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.baust.cafe.R

class WelcomeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_welcome)

        val welcomeImageView: ImageView = findViewById(R.id.welcomeImageView)
        val getStartedUserButton: Button = findViewById(R.id.getStartedUserButton)
        val getStartedAdminButton: Button = findViewById(R.id.getStartedAdminButton)

        getStartedUserButton.setOnClickListener {
            val intent = Intent(this, StudentLoginActivity::class.java)
            startActivity(intent)
            finish()
        }

        getStartedAdminButton.setOnClickListener {
            val intent = Intent(this, AdminLoginActivity::class.java)
            startActivity(intent)
        }
    }
}
