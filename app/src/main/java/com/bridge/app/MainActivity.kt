package com.bridge.app

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check if accessibility service is enabled
        if (!isAccessibilityServiceEnabled()) {
            val intent = Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
        }

        // Create bridge directory
        java.io.File("/sdcard/bridge").mkdirs()

        // Show main UI
        val textView = android.widget.TextView(this)
        textView.text = """
            Bridge App

            Accessibility Service: ${if (isAccessibilityServiceEnabled()) "ENABLED" else "DISABLED"}

            Features:
            - Chat with AI companion
            - Voice commands
            - Automations
            - Phone control

            Tap a button to get started:
        """.trimIndent()
        textView.textSize = 14f
        textView.setPadding(32, 32, 32, 32)

        val layout = android.widget.LinearLayout(this)
        layout.orientation = android.widget.LinearLayout.VERTICAL
        layout.addView(textView)

        val chatButton = Button(this)
        chatButton.text = "Chat"
        chatButton.setOnClickListener {
            startActivity(Intent(this, ChatActivity::class.java))
        }
        layout.addView(chatButton)

        val automationButton = Button(this)
        automationButton.text = "Automations"
        automationButton.setOnClickListener {
            startActivity(Intent(this, AutomationActivity::class.java))
        }
        layout.addView(automationButton)

        val settingsButton = Button(this)
        settingsButton.text = "Settings"
        settingsButton.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        layout.addView(settingsButton)

        setContentView(layout)
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val am = getSystemService(ACCESSIBILITY_SERVICE) as android.view.accessibility.AccessibilityManager
        val enabledServices = am.getEnabledAccessibilityServiceList(android.accessibilityservice.AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        return enabledServices.any { it.resolveInfo.serviceInfo.packageName == packageName }
    }
}
