package org.example.app

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.accessibility.AccessibilityEvent
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2

/**
 * A lightweight, skippable onboarding flow shown on first launch from Home.
 *
 * Uses classic Views + XML and ViewPager2 for good performance on low-end devices.
 */
class OnboardingActivity : Activity() {

    private lateinit var pager: ViewPager2
    private lateinit var dotsContainer: LinearLayout
    private lateinit var btnSkip: Button
    private lateinit var btnNext: Button
    private lateinit var btnGetStarted: Button

    private val pages: List<OnboardingPage> by lazy {
        listOf(
            OnboardingPage(
                titleRes = R.string.onboarding_title_upload,
                bodyRes = R.string.onboarding_body_upload,
                imageRes = R.drawable.ic_onboarding_upload
            ),
            OnboardingPage(
                titleRes = R.string.onboarding_title_grid,
                bodyRes = R.string.onboarding_body_grid,
                imageRes = R.drawable.ic_onboarding_grid
            ),
            OnboardingPage(
                titleRes = R.string.onboarding_title_commands,
                bodyRes = R.string.onboarding_body_commands,
                imageRes = R.drawable.ic_onboarding_commands
            )
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // If onboarding already completed, skip immediately.
        if (OnboardingPrefs.hasCompleted(this)) {
            goToMain()
            return
        }

        setContentView(R.layout.activity_onboarding)

        bindViews()
        setupPager()
        setupActions()

        updateDots(selectedIndex = 0)
        updateButtonsForPage(index = 0)

        // Accessibility: announce onboarding entry (page title lives inside ViewPager pages).
        findViewById<View>(R.id.onboardingRoot)?.sendAccessibilityEvent(
            AccessibilityEvent.TYPE_ANNOUNCEMENT
        )
    }

    private fun bindViews() {
        pager = findViewById(R.id.onboardingPager)
        dotsContainer = findViewById(R.id.dotsContainer)
        btnSkip = findViewById(R.id.btnSkip)
        btnNext = findViewById(R.id.btnNext)
        btnGetStarted = findViewById(R.id.btnGetStarted)
    }

    private fun setupPager() {
        pager.adapter = OnboardingPagerAdapter(pages)
        pager.offscreenPageLimit = 1

        pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updateDots(selectedIndex = position)
                updateButtonsForPage(index = position)
            }
        })
    }

    private fun setupActions() {
        btnSkip.setOnClickListener { completeAndEnter() }

        btnNext.setOnClickListener {
            val next = pager.currentItem + 1
            if (next < pages.size) {
                pager.setCurrentItem(next, true)
            }
        }

        btnGetStarted.setOnClickListener { completeAndEnter() }
    }

    private fun updateButtonsForPage(index: Int) {
        val isLast = index == pages.size - 1
        btnNext.visibility = if (isLast) View.GONE else View.VISIBLE
        btnGetStarted.visibility = if (isLast) View.VISIBLE else View.GONE
    }

    private fun updateDots(selectedIndex: Int) {
        dotsContainer.removeAllViews()

        for (i in pages.indices) {
            val dot = View(this).apply {
                setBackgroundResource(if (i == selectedIndex) R.drawable.bg_dot_selected else R.drawable.bg_dot_unselected)
                contentDescription = getString(R.string.onboarding_dot_cd, i + 1, pages.size)
                // Give each dot a stable touch target even though it's not clickable.
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
            }

            val size = resources.getDimensionPixelSize(R.dimen.onboarding_dot_size)
            val margin = resources.getDimensionPixelSize(R.dimen.onboarding_dot_margin)

            val lp = LinearLayout.LayoutParams(size, size).apply {
                leftMargin = margin
                rightMargin = margin
            }
            dotsContainer.addView(dot, lp)
        }
    }

    private fun completeAndEnter() {
        OnboardingPrefs.setCompleted(this, true)
        goToMain()
    }

    private fun goToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private data class OnboardingPage(
        val titleRes: Int,
        val bodyRes: Int,
        val imageRes: Int
    )

    private class OnboardingPagerAdapter(
        private val pages: List<OnboardingPage>
    ) : RecyclerView.Adapter<OnboardingPagerAdapter.PageVH>() {

        override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): PageVH {
            val view = android.view.LayoutInflater.from(parent.context)
                .inflate(R.layout.item_onboarding_page, parent, false)
            return PageVH(view)
        }

        override fun onBindViewHolder(holder: PageVH, position: Int) {
            val page = pages[position]
            holder.title.setText(page.titleRes)
            holder.body.setText(page.bodyRes)
            holder.image.setImageResource(page.imageRes)

            // Accessibility: describe illustration meaningfully.
            holder.image.contentDescription = holder.itemView.context.getString(
                R.string.onboarding_illustration_cd,
                holder.itemView.context.getString(page.titleRes)
            )
        }

        override fun getItemCount(): Int = pages.size

        class PageVH(itemView: android.view.View) : RecyclerView.ViewHolder(itemView) {
            val image: ImageView = itemView.findViewById(R.id.onboardingImage)
            val title: TextView = itemView.findViewById(R.id.onboardingTitle)
            val body: TextView = itemView.findViewById(R.id.onboardingBody)
        }
    }
}
