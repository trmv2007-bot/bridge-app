package com.bridge.app

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check if accessibility service is enabled
        if (!isAccessibilityServiceEnabled()) {
            // Open accessibility settings
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
        }

        // Create bridge directory
        java.io.File("/sdcard/bridge").mkdirs()

        // Show simple UI
        val textView = android.widget.TextView(this)
        textView.text = """
            Bridge App

            Accessibility Service: ${if (isAccessibilityServiceEnabled()) "ENABLED" else "DISABLED"}

            Commands:
            - tap <x> <y>
            - swipe <x1> <y1> <x2> <y2> [duration]
            - text <text>
            - home / back / recent
            - screenshot
            - screen (read UI tree)
            - currentapp
            - find <text>
            - taptext <text>
            - scroll <up/down/left/right>
            - clipboard [set <text>]

            Communication:
            - Write command to /sdcard/bridge/cmd.txt
            - Read result from /sdcard/bridge/result.txt
            - Or send broadcast: adb shell am broadcast -a com.bridge.app.EXECUTE -e command "tap 500 500"
        """.trimIndent()
        textView.textSize = 14f
        textView.setPadding(32, 32, 32, 32)
        setContentView(textView)
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val am = getSystemService(ACCESSIBILITY_SERVICE) as AccessibilityManager
        val enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        return enabledServices.any { it.resolveInfo.serviceInfo.packageName == packageName }
    }
}
