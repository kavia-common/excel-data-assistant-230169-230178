package org.example.app

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button

class HomeActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        val btnGetStarted: Button = findViewById(R.id.btnGetStarted)
        btnGetStarted.setOnClickListener {
            // Navigate into the existing dashboard screen.
            startActivity(Intent(this, MainActivity::class.java))
        }
    }
}
