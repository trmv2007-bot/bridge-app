package com.bridge.app

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast

class SettingsActivity : Activity() {

    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        prefs = getSharedPreferences("bridge_prefs", Context.MODE_PRIVATE)

        val apiKeyInput = findViewById<EditText>(R.id.apiKeyInput)
        val serverUrlInput = findViewById<EditText>(R.id.serverUrlInput)
        val saveButton = findViewById<Button>(R.id.saveButton)
        val startVoiceButton = findViewById<Button>(R.id.startVoiceButton)
        val stopVoiceButton = findViewById<Button>(R.id.stopVoiceButton)
        val startAutomationButton = findViewById<Button>(R.id.startAutomationButton)
        val stopAutomationButton = findViewById<Button>(R.id.stopAutomationButton)
        val statusText = findViewById<TextView>(R.id.statusText)

        // Load saved settings
        apiKeyInput.setText(prefs.getString("ai_api_key", ""))
        serverUrlInput.setText(prefs.getString("server_url", "http://localhost:8080"))

        saveButton.setOnClickListener {
            val apiKey = apiKeyInput.text.toString().trim()
            val serverUrl = serverUrlInput.text.toString().trim()

            prefs.edit()
                .putString("ai_api_key", apiKey)
                .putString("server_url", serverUrl)
                .apply()

            Toast.makeText(this, "Settings saved", Toast.LENGTH_SHORT).show()
            statusText.text = "Settings saved"
        }

        startVoiceButton.setOnClickListener {
            VoiceService.start(this)
            statusText.text = "Voice service started"
            Toast.makeText(this, "Voice service started", Toast.LENGTH_SHORT).show()
        }

        stopVoiceButton.setOnClickListener {
            VoiceService.stop(this)
            statusText.text = "Voice service stopped"
            Toast.makeText(this, "Voice service stopped", Toast.LENGTH_SHORT).show()
        }

        startAutomationButton.setOnClickListener {
            AutomationService.start(this)
            statusText.text = "Automation service started"
            Toast.makeText(this, "Automation service started", Toast.LENGTH_SHORT).show()
        }

        stopAutomationButton.setOnClickListener {
            AutomationService.stop(this)
            statusText.text = "Automation service stopped"
            Toast.makeText(this, "Automation service stopped", Toast.LENGTH_SHORT).show()
        }
    }
}
