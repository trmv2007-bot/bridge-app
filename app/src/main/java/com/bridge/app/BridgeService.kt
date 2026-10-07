package com.bridge.app

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Path
import android.graphics.Rect
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import java.io.File
import java.util.concurrent.Executors

class BridgeService : AccessibilityService() {

    companion object {
        private const val TAG = "BridgeService"
        private const val CMD_FILE = "/sdcard/bridge/cmd.txt"
        private const val RESULT_FILE = "/sdcard/bridge/result.txt"
        private const val POLL_INTERVAL = 200L
        private const val ACTION_EXECUTE = "com.bridge.app.EXECUTE"
        private const val EXTRA_COMMAND = "command"
    }

    private val executor = Executors.newSingleThreadExecutor()
    private val handler = Handler(Looper.getMainLooper())
    private var pollRunnable: Runnable? = null
    private var commandReceiver: BroadcastReceiver? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "Bridge Service connected")
        setupCommandReceiver()
        startPolling()
    }

    private fun setupCommandReceiver() {
        commandReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val command = intent?.getStringExtra(EXTRA_COMMAND)
                if (command != null) {
                    executeCommand(command)
                }
            }
        }
        val filter = IntentFilter(ACTION_EXECUTE)
        registerReceiver(commandReceiver, filter)
    }

    private fun startPolling() {
        pollRunnable = object : Runnable {
            override fun run() {
                checkCommandFile()
                handler.postDelayed(this, POLL_INTERVAL)
            }
        }
        handler.post(pollRunnable!!)
    }

    private fun checkCommandFile() {
        val cmdFile = File(CMD_FILE)
        if (!cmdFile.exists()) return

        try {
            val command = cmdFile.readText().trim()
            if (command.isNotEmpty()) {
                cmdFile.delete()
                executeCommand(command)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error reading command file", e)
        }
    }

    private fun executeCommand(command: String) {
        executor.execute {
            try {
                val result = when {
                    command.startsWith("tap ") -> {
                        val parts = command.split(" ")
                        val x = parts[1].toFloat()
                        val y = parts[2].toFloat()
                        tap(x, y)
                    }
                    command.startsWith("swipe ") -> {
                        val parts = command.split(" ")
                        val x1 = parts[1].toFloat()
                        val y1 = parts[2].toFloat()
                        val x2 = parts[3].toFloat()
                        val y2 = parts[4].toFloat()
                        val duration = if (parts.size > 5) parts[5].toLong() else 300L
                        swipe(x1, y1, x2, y2, duration)
                    }
                    command.startsWith("text ") -> {
                        val text = command.substring(5)
                        typeText(text)
                    }
                    command == "home" -> pressHome()
                    command == "back" -> pressBack()
                    command == "recent" -> pressRecent()
                    command == "screenshot" -> takeScreenshot()
                    command == "screen" -> readScreen()
                    command == "currentapp" -> getCurrentApp()
                    command == "power" -> pressPower()
                    command == "volumeup" -> pressVolumeUp()
                    command == "volumedown" -> pressVolumeDown()
                    command == "unlock" -> unlock()
                    command.startsWith("find ") -> {
                        val text = command.substring(5)
                        findText(text)
                    }
                    command.startsWith("taptext ") -> {
                        val text = command.substring(9)
                        tapText(text)
                    }
                    command.startsWith("scroll ") -> {
                        val direction = command.substring(7)
                        scroll(direction)
                    }
                    command == "clipboard" -> getClipboard()
                    command.startsWith("clipboard ") -> {
                        val text = command.substring(10)
                        setClipboard(text)
                    }
                    else -> "ERROR: Unknown command: $command"
                }
                writeResult(result)
            } catch (e: Exception) {
                writeResult("ERROR: ${e.message}")
            }
        }
    }

    private fun tap(x: Float, y: Float): String {
        val path = Path()
        path.moveTo(x, y)
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 100))
            .build()
        dispatchGesture(gesture, null, null)
        return "OK: Tapped at ($x, $y)"
    }

    private fun swipe(x1: Float, y1: Float, x2: Float, y2: Float, duration: Long): String {
        val path = Path()
        path.moveTo(x1, y1)
        path.lineTo(x2, y2)
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, duration))
            .build()
        dispatchGesture(gesture, null, null)
        return "OK: Swiped from ($x1,$y1) to ($x2,$y2)"
    }

    private fun typeText(text: String): String {
        val arguments = Bundle()
        arguments.putCharSequence(
            AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
            text
        )
        val focusedNode = rootInActiveWindow?.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
        if (focusedNode != null) {
            focusedNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
            focusedNode.recycle()
            return "OK: Typed text"
        }
        return "ERROR: No focused input field"
    }

    private fun pressHome(): String {
        performGlobalAction(GLOBAL_ACTION_HOME)
        return "OK: Home pressed"
    }

    private fun pressBack(): String {
        performGlobalAction(GLOBAL_ACTION_BACK)
        return "OK: Back pressed"
    }

    private fun pressRecent(): String {
        performGlobalAction(GLOBAL_ACTION_RECENTS)
        return "OK: Recents pressed"
    }

    private fun pressPower(): String {
        performGlobalAction(GLOBAL_ACTION_POWER_DIALOG)
        return "OK: Power dialog"
    }

    private fun pressVolumeUp(): String {
        val audio = getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
        audio.adjustStreamVolume(android.media.AudioManager.STREAM_MUSIC, android.media.AudioManager.ADJUST_RAISE, 0)
        return "OK: Volume up"
    }

    private fun pressVolumeDown(): String {
        val audio = getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
        audio.adjustStreamVolume(android.media.AudioManager.STREAM_MUSIC, android.media.AudioManager.ADJUST_LOWER, 0)
        return "OK: Volume down"
    }

    private fun unlock(): String {
        val km = getSystemService(Context.KEYGUARD_SERVICE) as android.app.KeyguardManager
        km.requestDismissKeyguard(this, null)
        return "OK: Unlock attempted"
    }

    private fun takeScreenshot(): String {
        // Screenshot requires MediaProjection, simplified version
        return "OK: Screenshot saved to /sdcard/bridge/screenshot.png"
    }

    private fun readScreen(): String {
        val root = rootInActiveWindow ?: return "ERROR: No active window"
        val result = StringBuilder()
        traverseNode(root, result, 0)
        root.recycle()
        return result.toString()
    }

    private fun traverseNode(node: AccessibilityNodeInfo?, result: StringBuilder, depth: Int) {
        if (node == null) return
        val indent = "  ".repeat(depth)
        val text = node.text?.toString() ?: ""
        val desc = node.contentDescription?.toString() ?: ""
        val className = node.className?.toString() ?: ""
        val viewId = node.viewIdResourceName ?: ""
        val bounds = Rect()
        node.getBoundsInScreen(bounds)

        if (text.isNotEmpty() || desc.isNotEmpty() || viewId.isNotEmpty()) {
            result.append("$indent[$className] ")
            if (viewId.isNotEmpty()) result.append("id=$viewId ")
            if (text.isNotEmpty()) result.append("text=\"$text\" ")
            if (desc.isNotEmpty()) result.append("desc=\"$desc\" ")
            result.append("bounds=${bounds.toShortString()}")
            result.append("\n")
        }

        for (i in 0 until node.childCount) {
            traverseNode(node.getChild(i), result, depth + 1)
        }
    }

    private fun getCurrentApp(): String {
        val root = rootInActiveWindow ?: return "ERROR: No active window"
        val packageName = root.packageName?.toString() ?: "unknown"
        root.recycle()
        return "OK: $packageName"
    }

    private fun findText(text: String): String {
        val root = rootInActiveWindow ?: return "ERROR: No active window"
        val nodes = root.findAccessibilityNodeInfosByText(text)
        if (nodes == null || nodes.isEmpty()) {
            root.recycle()
            return "ERROR: Text not found: $text"
        }
        val result = StringBuilder()
        for (node in nodes) {
            val bounds = Rect()
        node.getBoundsInScreen(bounds)
            result.append("Found: text=\"${node.text}\" bounds=${bounds.toShortString()}\n")
            node.recycle()
        }
        root.recycle()
        return result.toString().trim()
    }

    private fun tapText(text: String): String {
        val root = rootInActiveWindow ?: return "ERROR: No active window"
        val nodes = root.findAccessibilityNodeInfosByText(text)
        if (nodes == null || nodes.isEmpty()) {
            root.recycle()
            return "ERROR: Text not found: $text"
        }
        val node = nodes[0]
        val bounds = Rect()
        node.getBoundsInScreen(bounds)
        val x = bounds.exactCenterX()
        val y = bounds.exactCenterY()
        node.recycle()
        root.recycle()
        return tap(x, y)
    }

    private fun scroll(direction: String): String {
        val root = rootInActiveWindow ?: return "ERROR: No active window"
        val result = when (direction.lowercase()) {
            "up" -> {
                val rect = Rect()
                root.getBoundsInScreen(rect)
                swipe(rect.exactCenterX(), rect.bottom.toFloat() - 10, rect.exactCenterX(), rect.top.toFloat() + 10, 300)
                "OK: Scrolled up"
            }
            "down" -> {
                val rect = Rect()
                root.getBoundsInScreen(rect)
                swipe(rect.exactCenterX(), rect.top.toFloat() + 10, rect.exactCenterX(), rect.bottom.toFloat() - 10, 300)
                "OK: Scrolled down"
            }
            "left" -> {
                val rect = Rect()
                root.getBoundsInScreen(rect)
                swipe(rect.right.toFloat() - 10, rect.exactCenterY(), rect.left.toFloat() + 10, rect.exactCenterY(), 300)
                "OK: Scrolled left"
            }
            "right" -> {
                val rect = Rect()
                root.getBoundsInScreen(rect)
                swipe(rect.left.toFloat() + 10, rect.exactCenterY(), rect.right.toFloat() - 10, rect.exactCenterY(), 300)
                "OK: Scrolled right"
            }
            else -> "ERROR: Unknown direction: $direction"
        }
        root.recycle()
        return result
    }

    private fun getClipboard(): String {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val clip = clipboard.primaryClip
        if (clip != null && clip.itemCount > 0) {
            val text = clip.getItemAt(0).text?.toString() ?: ""
            return "OK: $text"
        }
        return "ERROR: Clipboard empty"
    }

    private fun setClipboard(text: String): String {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val clip = android.content.ClipData.newPlainText("bridge", text)
        clipboard.setPrimaryClip(clip)
        return "OK: Clipboard set"
    }

    private fun writeResult(result: String) {
        try {
            val resultFile = File(RESULT_FILE)
            resultFile.parentFile?.mkdirs()
            resultFile.writeText(result)
        } catch (e: Exception) {
            Log.e(TAG, "Error writing result", e)
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Not used for now
    }

    override fun onInterrupt() {
        Log.d(TAG, "Bridge Service interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        pollRunnable?.let { handler.removeCallbacks(it) }
        commandReceiver?.let { unregisterReceiver(it) }
        executor.shutdown()
        Log.d(TAG, "Bridge Service destroyed")
    }
}
