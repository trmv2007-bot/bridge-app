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
    private var pollThread: Thread? = null
    private var isPolling = false

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

        // Start polling for responses
        startPolling()
    }

    private fun startPolling() {
        isPolling = true
        pollThread = Thread {
            while (isPolling) {
                try {
                    checkForResponse()
                    Thread.sleep(2000)
                } catch (e: Exception) {
                    // Ignore
                }
            }
        }
        pollThread?.start()
    }

    private fun checkForResponse() {
        val responseFile = File("/sdcard/bridge/ai_response.json")
        if (responseFile.exists()) {
            try {
                val json = org.json.JSONObject(responseFile.readText())
                val response = json.getString("response")
                responseFile.delete()

                handler.post {
                    addMessage(ChatMessage(response, false, System.currentTimeMillis()))
                    updateStatus("Ready")
                    saveMessages()
                }
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    private fun sendMessage() {
        val text = messageInput.text.toString().trim()
        if (text.isEmpty()) return

        // Add user message
        addMessage(ChatMessage(text, true, System.currentTimeMillis()))
        messageInput.setText("")
        updateStatus("Sending...")

        executor.execute {
            try {
                val serverUrl = prefs.getString("server_url", "http://localhost:8080")
                val url = java.net.URL("$serverUrl/chat")

                val connection = url.openConnection() as javax.net.ssl.HttpsURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.doOutput = true
                connection.connectTimeout = 10000
                connection.readTimeout = 120000

                val body = "{\"message\": \"$text\"}"
                connection.outputStream.use { it.write(body.toByteArray()) }

                val response = connection.inputStream.bufferedReader().readText()
                connection.disconnect()

                handler.post {
                    try {
                        val json = org.json.JSONObject(response)
                        val aiResponse = json.getString("response")
                        addMessage(ChatMessage(aiResponse, false, System.currentTimeMillis()))
                        updateStatus("Ready")
                        saveMessages()
                    } catch (e: Exception) {
                        updateStatus("Error parsing response")
                    }
                }
            } catch (e: Exception) {
                handler.post {
                    updateStatus("Error: ${e.message}")
                    // Fallback to local response
                    val response = getLocalResponse(text)
                    addMessage(ChatMessage(response, false, System.currentTimeMillis()))
                    updateStatus("Ready (local)")
                }
            }
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

            lower.contains("thank") ->
                "You're welcome! I'm here to help anytime."

            lower.contains("bye") || lower.contains("goodbye") ->
                "Goodbye! I'll be here when you need me."

            else ->
                "I'm not sure how to help with that yet. Try saying 'help' to see what I can do, or use /commands for phone control."
        }
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

    override fun onDestroy() {
        super.onDestroy()
        isPolling = false
        pollThread?.interrupt()
    }
}

data class ChatMessage(
    val text: String,
    val isUser: Boolean,
    val timestamp: Long
)
