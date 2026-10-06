package com.bridge.app

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.util.Log
import java.io.File
import java.util.Locale
import java.util.concurrent.Executors

class VoiceService : Service(), TextToSpeech.OnInitListener {

    companion object {
        private const val TAG = "VoiceService"
        private var isRunning = false

        fun start(context: Context) {
            val intent = Intent(context, VoiceService::class.java)
            context.startService(intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, VoiceService::class.java)
            context.stopService(intent)
        }
    }

    private var speechRecognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null
    private val executor = Executors.newSingleThreadExecutor()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        Log.d(TAG, "Voice service started")
        textToSpeech = TextToSpeech(this, this)
        setupSpeechRecognizer()
    }

    private fun setupSpeechRecognizer() {
        if (SpeechRecognizer.isRecognitionAvailable(this)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
            speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    Log.d(TAG, "Ready for speech")
                }

                override fun onBeginningOfSpeech() {
                    Log.d(TAG, "Beginning of speech")
                }

                override fun onRmsChanged(rmsdB: Float) {}

                override fun onBufferReceived(buffer: ByteArray?) {}

                override fun onEndOfSpeech() {
                    Log.d(TAG, "End of speech")
                }

                override fun onError(error: Int) {
                    Log.e(TAG, "Speech recognition error: $error")
                    restartListening()
                }

                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        val text = matches[0]
                        Log.d(TAG, "Recognized: $text")
                        processVoiceCommand(text)
                    }
                    restartListening()
                }

                override fun onPartialResults(partialResults: Bundle?) {}

                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
            startListening()
        }
    }

    private fun startListening() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        speechRecognizer?.startListening(intent)
    }

    private fun restartListening() {
        executor.execute {
            Thread.sleep(1000)
            if (isRunning) {
                startListening()
            }
        }
    }

    private fun processVoiceCommand(text: String) {
        val lower = text.lowercase()
        val response = when {
            lower.contains("battery") -> {
                val status = executeBridgeCommand("battery")
                "Battery status: $status"
            }
            lower.contains("time") -> {
                val time = java.text.SimpleDateFormat("hh:mm a", Locale.getDefault()).format(java.util.Date())
                "It's $time"
            }
            lower.contains("home") -> {
                executeBridgeCommand("home")
                "Going home"
            }
            lower.contains("back") -> {
                executeBridgeCommand("back")
                "Going back"
            }
            lower.contains("screen") -> {
                val screen = executeBridgeCommand("screen")
                "Screen content: $screen"
            }
            lower.contains("open") -> {
                val app = lower.replace("open", "").trim()
                "Opening $app"
            }
            else -> {
                "I heard: $text. I'm still learning voice commands. Try saying 'battery', 'time', 'home', or 'back'."
            }
        }

        speak(response)
    }

    private fun speak(text: String) {
        textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
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
            return "Timeout"
        } catch (e: Exception) {
            return "Error: ${e.message}"
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            textToSpeech?.language = Locale.getDefault()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        speechRecognizer?.destroy()
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        executor.shutdown()
        Log.d(TAG, "Voice service stopped")
    }
}
