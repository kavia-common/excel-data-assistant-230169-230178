package org.example.app

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView

class HomeActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        val appLogo: ImageView = findViewById(R.id.appLogo)
        val illustration: ImageView = findViewById(R.id.homeIllustration)
        val title: TextView = findViewById(R.id.homeTitle)
        val subtitle: TextView = findViewById(R.id.homeSubtitle)
        val btnGetStarted: Button = findViewById(R.id.btnGetStarted)

        // Subtle entrance animations (fade + slide).
        runEntranceAnimations(appLogo, illustration, title, subtitle, btnGetStarted)

        // Scale-on-press for CTA (kept light for performance).
        applyPressScale(btnGetStarted)

        btnGetStarted.setOnClickListener {
            // First time: show onboarding. After completion: go straight to Main.
            val next = if (OnboardingPrefs.hasCompleted(this)) {
                Intent(this, MainActivity::class.java)
            } else {
                Intent(this, OnboardingActivity::class.java)
            }
            startActivity(next)
        }
    }

    private fun runEntranceAnimations(
        logo: View,
        illustration: View,
        title: View,
        subtitle: View,
        cta: View
    ) {
        val items = listOf(logo, illustration, title, subtitle, cta)

        // Prep initial state.
        for (v in items) {
            v.alpha = 0f
            v.translationY = 18f
        }

        val interpolator = DecelerateInterpolator()

        // Stagger for a modern feel, but keep it fast.
        items.forEachIndexed { idx, v ->
            v.animate()
                .alpha(1f)
                .translationY(0f)
                .setStartDelay((idx * 60L) + 40L)
                .setDuration(260L)
                .setInterpolator(interpolator)
                .start()
        }
    }

    private fun applyPressScale(view: View) {
        val downScale = 0.98f
        val duration = 90L

        view.setOnTouchListener { v, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    v.animate().scaleX(downScale).scaleY(downScale).setDuration(duration).start()
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    v.animate().scaleX(1f).scaleY(1f).setDuration(duration).start()
                }
            }
            // Return false to preserve Button ripple/click handling.
            false
        }
    }
}
