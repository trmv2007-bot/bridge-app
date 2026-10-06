package com.bridge.app

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.IBinder
import android.util.Log
import java.io.File
import java.util.concurrent.Executors

class AutomationService : Service() {

    companion object {
        private const val TAG = "AutomationService"
        private var isRunning = false

        fun start(context: Context) {
            val intent = Intent(context, AutomationService::class.java)
            context.startService(intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, AutomationService::class.java)
            context.stopService(intent)
        }
    }

    private val executor = Executors.newSingleThreadExecutor()
    private var batteryReceiver: BroadcastReceiver? = null
    private var automations = mutableListOf<Automation>()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        Log.d(TAG, "Automation service started")
        loadAutomations()
        registerBatteryReceiver()
        startAutomationLoop()
    }

    private fun loadAutomations() {
        try {
            val file = File("/sdcard/bridge/automations.json")
            if (file.exists()) {
                val json = org.json.JSONArray(file.readText())
                automations.clear()
                for (i in 0 until json.length()) {
                    val obj = json.getJSONObject(i)
                    automations.add(Automation(
                        obj.getString("trigger"),
                        obj.getString("action"),
                        obj.getBoolean("enabled"),
                        obj.getLong("timestamp")
                    ))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading automations", e)
        }
    }

    private fun registerBatteryReceiver() {
        batteryReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == Intent.ACTION_BATTERY_CHANGED) {
                    val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                    val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                    val batteryPct = level * 100 / scale.toFloat()
                    checkAutomations("battery_${batteryPct.toInt()}")
                }
            }
        }
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        registerReceiver(batteryReceiver, filter)
    }

    private fun startAutomationLoop() {
        executor.execute {
            while (isRunning) {
                try {
                    // Check time-based automations
                    val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
                    checkAutomations("time_$hour")

                    // Check screen state
                    checkAutomations("screen_on")
                    checkAutomations("screen_off")

                    Thread.sleep(30000) // Check every 30 seconds
                } catch (e: Exception) {
                    Log.e(TAG, "Error in automation loop", e)
                }
            }
        }
    }

    private fun checkAutomations(trigger: String) {
        automations.filter { it.enabled }.forEach { automation ->
            if (automation.trigger.equals(trigger, ignoreCase = true) ||
                trigger.contains(automation.trigger, ignoreCase = true)) {
                executeAction(automation.action)
            }
        }
    }

    private fun executeAction(action: String) {
        try {
            val cmdFile = File("/sdcard/bridge/cmd.txt")
            val resultFile = File("/sdcard/bridge/result.txt")
            cmdFile.parentFile?.mkdirs()
            resultFile.delete()
            cmdFile.writeText(action)

            var attempts = 0
            while (!resultFile.exists() && attempts < 50) {
                Thread.sleep(200)
                attempts++
            }

            if (resultFile.exists()) {
                val result = resultFile.readText()
                resultFile.delete()
                Log.d(TAG, "Automation executed: $action -> $result")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error executing automation", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        batteryReceiver?.let { unregisterReceiver(it) }
        executor.shutdown()
        Log.d(TAG, "Automation service stopped")
    }
}
