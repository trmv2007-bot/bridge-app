package com.bridge.app

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.File

class AutomationActivity : Activity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: AutomationAdapter
    private val automations = mutableListOf<Automation>()
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_automation)

        prefs = getSharedPreferences("bridge_prefs", Context.MODE_PRIVATE)

        recyclerView = findViewById(R.id.automationRecyclerView)
        adapter = AutomationAdapter(automations) { automation ->
            removeAutomation(automation)
        }
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        loadAutomations()

        findViewById<Button>(R.id.addButton).setOnClickListener {
            val trigger = findViewById<EditText>(R.id.triggerInput).text.toString().trim()
            val action = findViewById<EditText>(R.id.actionInput).text.toString().trim()

            if (trigger.isNotEmpty() && action.isNotEmpty()) {
                addAutomation(trigger, action)
                findViewById<EditText>(R.id.triggerInput).setText("")
                findViewById<EditText>(R.id.actionInput).setText("")
            } else {
                Toast.makeText(this, "Please enter both trigger and action", Toast.LENGTH_SHORT).show()
            }
        }

        findViewById<Button>(R.id.startButton).setOnClickListener {
            startAutomationEngine()
            Toast.makeText(this, "Automation engine started", Toast.LENGTH_SHORT).show()
        }

        findViewById<Button>(R.id.stopButton).setOnClickListener {
            stopAutomationEngine()
            Toast.makeText(this, "Automation engine stopped", Toast.LENGTH_SHORT).show()
        }
    }

    private fun addAutomation(trigger: String, action: String) {
        val automation = Automation(trigger, action, true, System.currentTimeMillis())
        automations.add(automation)
        adapter.notifyItemInserted(automations.size - 1)
        saveAutomations()
    }

    private fun removeAutomation(automation: Automation) {
        val index = automations.indexOf(automation)
        if (index >= 0) {
            automations.removeAt(index)
            adapter.notifyItemRemoved(index)
            saveAutomations()
        }
    }

    private fun saveAutomations() {
        try {
            val json = org.json.JSONArray()
            automations.forEach { auto ->
                val obj = org.json.JSONObject()
                obj.put("trigger", auto.trigger)
                obj.put("action", auto.action)
                obj.put("enabled", auto.enabled)
                obj.put("timestamp", auto.timestamp)
                json.put(obj)
            }
            File("/sdcard/bridge/automations.json").writeText(json.toString())
        } catch (e: Exception) {
            // Ignore
        }
    }

    private fun loadAutomations() {
        try {
            val file = File("/sdcard/bridge/automations.json")
            if (file.exists()) {
                val json = org.json.JSONArray(file.readText())
                for (i in 0 until json.length()) {
                    val obj = json.getJSONObject(i)
                    automations.add(Automation(
                        obj.getString("trigger"),
                        obj.getString("action"),
                        obj.getBoolean("enabled"),
                        obj.getLong("timestamp")
                    ))
                }
                adapter.notifyDataSetChanged()
            }
        } catch (e: Exception) {
            // Ignore
        }
    }

    private fun startAutomationEngine() {
        AutomationService.start(this)
    }

    private fun stopAutomationEngine() {
        AutomationService.stop(this)
    }
}

data class Automation(
    val trigger: String,
    val action: String,
    var enabled: Boolean,
    val timestamp: Long
)
