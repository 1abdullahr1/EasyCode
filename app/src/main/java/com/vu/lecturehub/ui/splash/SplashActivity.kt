package com.vu.lecturehub.ui.splash

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.WindowInsetsController
import androidx.appcompat.app.AppCompatActivity
import com.vu.lecturehub.MainActivity
import com.vu.lecturehub.databinding.ActivitySplashBinding
import com.vu.lecturehub.ui.onboarding.OnboardingActivity
import com.vu.lecturehub.util.OnboardingManager
import com.vu.lecturehub.util.ThemeManager

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding
    private val handler = Handler(Looper.getMainLooper())
    private var hasNavigated = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeManager.applySavedTheme(this)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Ensure status bar looks crisp with dark icons on white background
        window.statusBarColor = Color.WHITE
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.setSystemBarsAppearance(
                WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
                WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
            )
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        }

        // Allow instant tap to skip splash if user wants
        binding.root.setOnClickListener {
            proceedToNext()
        }

        // Fast smooth transition to main app
        handler.postDelayed({
            proceedToNext()
        }, 500)
    }

    private fun proceedToNext() {
        if (hasNavigated || isFinishing || isDestroyed) return
        hasNavigated = true
        handler.removeCallbacksAndMessages(null)

        val targetClass = if (!OnboardingManager.isOnboardingCompleted(this)) {
            OnboardingActivity::class.java
        } else {
            MainActivity::class.java
        }

        val intent = Intent(this, targetClass)
        startActivity(intent)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }
}
