package com.bridge.app

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.ScrollView
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

class ChatActivity : Activity() {

    private lateinit var chatRecyclerView: RecyclerView
    private lateinit var messageInput: EditText
    private lateinit var sendButton: Button
    private lateinit var statusText: TextView
    private lateinit var adapter: ChatAdapter
    private val messages = mutableListOf<ChatMessage>()
    private val executor = Executors.newSingleThreadExecutor()
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        prefs = getSharedPreferences("bridge_prefs", Context.MODE_PRIVATE)

        chatRecyclerView = findViewById(R.id.chatRecyclerView)
        messageInput = findViewById(R.id.messageInput)
        sendButton = findViewById(R.id.sendButton)
        statusText = findViewById(R.id.statusText)

        adapter = ChatAdapter(messages)
        chatRecyclerView.layoutManager = LinearLayoutManager(this)
        chatRecyclerView.adapter = adapter

        // Load saved messages
        loadMessages()

        sendButton.setOnClickListener { sendMessage() }

        messageInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendMessage()
                true
            } else false
        }

        updateStatus("Ready")
    }

    private fun sendMessage() {
        val text = messageInput.text.toString().trim()
        if (text.isEmpty()) return

        // Add user message
        addMessage(ChatMessage(text, true, System.currentTimeMillis()))
        messageInput.setText("")
        updateStatus("Thinking...")

        executor.execute {
            val response = processMessage(text)
            handler.post {
                addMessage(ChatMessage(response, false, System.currentTimeMillis()))
                updateStatus("Ready")
                saveMessages()
            }
        }
    }

    private fun processMessage(text: String): String {
        val lower = text.lowercase()

        // Check for bridge commands
        if (lower.startsWith("/")) {
            return executeBridgeCommand(text.substring(1))
        }

        // Check for automation commands
        if (lower.startsWith("automation") || lower.startsWith("automate")) {
            return processAutomationCommand(text)
        }

        // Check for AI API
        val apiKey = prefs.getString("ai_api_key", null)
        if (apiKey != null && apiKey.isNotEmpty()) {
            return callAIAPI(text, apiKey)
        }

        // Local response system
        return getLocalResponse(text)
    }

    private fun executeBridgeCommand(command: String): String {
        val cmdFile = File("/sdcard/bridge/cmd.txt")
        val resultFile = File("/sdcard/bridge/result.txt")

        try {
            cmdFile.parentFile?.mkdirs()
            resultFile.delete()
            cmdFile.writeText(command)

            var attempts = 0
            while (!resultFile.exists() && attempts < 50) {
                Thread.sleep(200)
                attempts++
            }

            if (resultFile.exists()) {
                val result = resultFile.readText()
                resultFile.delete()
                return result
            }
            return "ERROR: Timeout waiting for bridge response"
        } catch (e: Exception) {
            return "ERROR: ${e.message}"
        }
    }

    private fun processAutomationCommand(text: String): String {
        val lower = text.lowercase()
        val automationFile = File("/sdcard/bridge/automations.json")

        return when {
            lower.contains("list") -> {
                if (automationFile.exists()) {
                    val automations = automationFile.readText()
                    if (automations.isBlank()) "No automations set up yet."
                    else "Current automations:\n$automations"
                } else "No automations set up yet."
            }
            lower.contains("add") || lower.contains("create") -> {
                "To add an automation, use the Automation screen or say: 'automation add [trigger] [action]'"
            }
            lower.contains("remove") || lower.contains("delete") -> {
                "To remove an automation, use the Automation screen."
            }
            else -> "Automation commands: list, add, remove"
        }
    }

    private fun callAIAPI(text: String, apiKey: String): String {
        return try {
            val url = "https://api.openai.com/v1/chat/completions"
            val body = """
                {
                    "model": "gpt-3.5-turbo",
                    "messages": [{"role": "user", "content": "$text"}],
                    "max_tokens": 150
                }
            """.trimIndent()

            val connection = java.net.URL(url).openConnection() as javax.net.ssl.HttpsURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Authorization", "Bearer $apiKey")
            connection.doOutput = true
            connection.connectTimeout = 10000
            connection.readTimeout = 15000

            connection.outputStream.use { it.write(body.toByteArray()) }

            val response = connection.inputStream.bufferedReader().readText()
            connection.disconnect()

            // Parse response
            val json = org.json.JSONObject(response)
            val choices = json.getJSONArray("choices")
            if (choices.length() > 0) {
                val message = choices.getJSONObject(0).getJSONObject("message")
                message.getString("content")
            } else {
                "ERROR: No response from AI"
            }
        } catch (e: Exception) {
            "ERROR: AI API call failed: ${e.message}"
        }
    }

    private fun getLocalResponse(text: String): String {
        val lower = text.lowercase()
        return when {
            lower.contains("hello") || lower.contains("hi") || lower.contains("hey") ->
                "Hello! I'm your Bridge companion. I can help you control your phone, set up automations, or just chat. What would you like to do?"

            lower.contains("battery") -> {
                val status = executeBridgeCommand("battery")
                "Here's your battery status:\n$status"
            }

            lower.contains("time") -> {
                val time = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
                "It's currently $time"
            }

            lower.contains("date") -> {
                val date = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault()).format(Date())
                "Today is $date"
            }

            lower.contains("weather") ->
                "I can check the weather for you. What city are you in?"

            lower.contains("joke") -> {
                val jokes = listOf(
                    "Why do programmers prefer dark mode? Because light attracts bugs!",
                    "Why did the phone need glasses? Because it lost all its contacts!",
                    "What's a computer's favorite snack? Microchips!",
                    "Why was the smartphone cold? It left its Windows open!"
                )
                jokes.random()
            }

            lower.contains("help") ->
                """I can help you with:
                - Phone control: "tap 500 500", "swipe up", "open WhatsApp"
                - Battery: "battery status"
                - Time/Date: "what time is it"
                - Automations: "automation list", "automation add"
                - Chat: just talk to me!
                - Commands: /tap /swipe /text /home /back /screen
                """

            lower.contains("screen") -> {
                val screen = executeBridgeCommand("screen")
                "Here's what's on your screen:\n$screen"
            }

            lower.contains("open") -> {
                val app = lower.replace("open", "").trim()
                "To open $app, I can tap on it. Let me check the screen first."
            }

            lower.contains("thank") ->
                "You're welcome! I'm here to help anytime."

            lower.contains("bye") || lower.contains("goodbye") ->
                "Goodbye! I'll be here when you need me."

            else ->
                "I'm not sure how to help with that yet. Try saying 'help' to see what I can do, or use /commands for phone control."
        }
    }

    private fun addMessage(message: ChatMessage) {
        messages.add(message)
        adapter.notifyItemInserted(messages.size - 1)
        chatRecyclerView.scrollToPosition(messages.size - 1)
    }

    private fun updateStatus(status: String) {
        statusText.text = status
    }

    private fun saveMessages() {
        try {
            val json = org.json.JSONArray()
            messages.forEach { msg ->
                val obj = org.json.JSONObject()
                obj.put("text", msg.text)
                obj.put("isUser", msg.isUser)
                obj.put("timestamp", msg.timestamp)
                json.put(obj)
            }
            File("/sdcard/bridge/chat_history.json").writeText(json.toString())
        } catch (e: Exception) {
            // Ignore save errors
        }
    }

    private fun loadMessages() {
        try {
            val file = File("/sdcard/bridge/chat_history.json")
            if (file.exists()) {
                val json = org.json.JSONArray(file.readText())
                for (i in 0 until json.length()) {
                    val obj = json.getJSONObject(i)
                    messages.add(ChatMessage(
                        obj.getString("text"),
                        obj.getBoolean("isUser"),
                        obj.getLong("timestamp")
                    ))
                }
                adapter.notifyDataSetChanged()
                chatRecyclerView.scrollToPosition(messages.size - 1)
            }
        } catch (e: Exception) {
            // Ignore load errors
        }
    }
}

data class ChatMessage(
    val text: String,
    val isUser: Boolean,
    val timestamp: Long
)
