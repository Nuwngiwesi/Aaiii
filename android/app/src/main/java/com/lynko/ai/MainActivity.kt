
package com.lynko.ai

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale

class MainActivity : Activity() {

    private var recognizer: SpeechRecognizer? = null
    private lateinit var statusText: TextView
    private lateinit var listenButton: Button
    private lateinit var responseText: TextView

    private var isListening = false

    // Replace this with your deployed backend URL.
    // Keep the Groq API key on the backend, never inside this app.
    private val backendUrl =
        "https://YOUR-BACKEND.onrender.com/command"

    private val blue = Color.rgb(0, 190, 255)
    private val backgroundColor = Color.rgb(7, 12, 28)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        createInterface()

        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            statusText.text = "Speech recognition is unavailable"
        }

        requestPermissionsIfNeeded()
    }

    private fun createInterface() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 32, 24, 24)
            gravity = Gravity.CENTER_HORIZONTAL
            setBackgroundColor(backgroundColor)
        }

        val scroll = ScrollView(this)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
        }

        val logo = TextView(this).apply {
            text = "◈"
            textSize = 64f
            gravity = Gravity.CENTER
            setTextColor(blue)
        }

        val title = TextView(this).apply {
            text = "LYNKO AI"
            textSize = 32f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
        }

        val subtitle = TextView(this).apply {
            text = "YOUR PERSONAL VOICE ASSISTANT"
            textSize = 12f
            gravity = Gravity.CENTER
            setTextColor(blue)
        }

        statusText = TextView(this).apply {
            text = "Ready to connect"
            textSize = 15f
            gravity = Gravity.CENTER
            setTextColor(Color.LTGRAY)
            setPadding(0, 32, 0, 20)
        }

        listenButton = Button(this).apply {
            text = "🎙  START LISTENING"
            textSize = 16f
            setTextColor(Color.WHITE)
            background = roundedBackground(blue, 24f)
            setOnClickListener {
                startListening()
            }
        }

        val stopButton = Button(this).apply {
            text = "STOP LISTENING"
            textSize = 14f
            setTextColor(Color.WHITE)
            background = roundedBackground(
                Color.rgb(45, 53, 75), 24f
            )
            setOnClickListener {
                stopListening()
            }
        }

        val youtubeButton = Button(this).apply {
            text = "▶  OPEN YOUTUBE"
            setTextColor(Color.WHITE)
            background = roundedBackground(
                Color.rgb(200, 35, 55), 24f
            )
            setOnClickListener {
                openYoutube()
            }
        }

        responseText = TextView(this).apply {
            text = "Hello! I'm LYNKO, your AI assistant."
            textSize = 16f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            setPadding(12, 30, 12, 20)
        }

        val hint = TextView(this).apply {
            text = """
                Try saying:

                • LYNKO, search YouTube for music
                • LYNKO, open YouTube
                • LYNKO, hello

                Tap START LISTENING to begin.
            """.trimIndent()
            textSize = 14f
            setTextColor(Color.LTGRAY)
            setPadding(8, 20, 8, 24)
        }

        content.addView(logo)
        content.addView(title)
        content.addView(subtitle)
        content.addView(statusText)

        content.addView(
            listenButton,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 58
            ).apply {
                bottomMargin = 12
            }
        )

        content.addView(
            stopButton,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 54
            ).apply {
                bottomMargin = 12
            }
        )

        content.addView(
            youtubeButton,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 54
            )
        )

        content.addView(responseText)
        content.addView(hint)

        scroll.addView(content)

        root.addView(
            scroll,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
            )
        )

        setContentView(root)
    }

    private fun roundedBackground(
        color: Int,
        radius: Float
    ): GradientDrawable {
        return GradientDrawable().apply {
            setColor(color)
            cornerRadius = radius
        }
    }

    private fun requestPermissionsIfNeeded() {
        val permissions = mutableListOf<String>()

        if (
            ContextCompat.checkSelfPermission(
                this, Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissions.add(Manifest.permission.RECORD_AUDIO)
        }

        if (permissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this, permissions.toTypedArray(), 100
            )
        }
    }

    private fun startListening() {
        if (
            ContextCompat.checkSelfPermission(
                this, Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissionsIfNeeded()
            statusText.text = "Please allow microphone permission"
            return
        }

        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            statusText.text = "Speech recognition is unavailable"
            return
        }

        stopListening()

        recognizer = SpeechRecognizer.createSpeechRecognizer(this)

        recognizer?.setRecognitionListener(
            object : RecognitionListener {

                override fun onReadyForSpeech(params: Bundle?) {
                    statusText.text = "Listening... Speak now"
                    listenButton.text = "🎙  LISTENING..."
                }

                override fun onBeginningOfSpeech() {
                    statusText.text = "I can hear you..."
                }

                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {
                    statusText.text = "Processing your voice..."
                }

                override fun onError(error: Int) {
                    isListening = false
                    listenButton.text = "🎙  START LISTENING"
                    statusText.text =
                        "Couldn't hear you. Tap to try again."
                }

                override fun onResults(results: Bundle?) {
                    isListening = false
                    listenButton.text = "🎙  START LISTENING"

                    val matches = results?.getStringArrayList(
                        SpeechRecognizer.RESULTS_RECOGNITION
                    )

                    val spokenText = matches?.firstOrNull().orEmpty()

                    if (spokenText.isNotBlank()) {
                        responseText.text = "You said: $spokenText"
                        processCommand(spokenText)
                    } else {
                        statusText.text = "No speech detected"
                    }
                }

                override fun onPartialResults(
                    partialResults: Bundle?
                ) {}

                override fun onEvent(eventType: Int, params: Bundle?) {}
            }
        )

        val intent = Intent(
            RecognizerIntent.ACTION_RECOGNIZE_SPEECH
        ).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE,
                Locale.getDefault().toLanguageTag()
            )
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }

        isListening = true
        recognizer?.startListening(intent)
    }

    private fun stopListening() {
        isListening = false
        recognizer?.cancel()
        recognizer?.destroy()
        recognizer = null

        if (::listenButton.isInitialized) {
            listenButton.text = "🎙  START LISTENING"
        }

        if (::statusText.isInitialized) {
            statusText.text = "Listening stopped"
        }
    }

    private fun processCommand(command: String) {
        val text = command.trim()
        val lower = text.lowercase(Locale.ROOT)

        // Basic commands work without an AI API.
        if (
            lower.contains("youtube") &&
            (
                lower.contains("search") ||
                lower.contains("play") ||
                lower.contains("find") ||
                lower.contains("music") ||
                lower.contains("song")
            )
        ) {
            val query = text
                .replace("lynko", "", ignoreCase = true)
                .replace("search", "", ignoreCase = true)
                .replace("youtube", "", ignoreCase = true)
                .replace("play", "", ignoreCase = true)
                .replace("find", "", ignoreCase = true)
                .replace("music", "", ignoreCase = true)
                .replace("song", "", ignoreCase = true)
                .trim()

            searchYoutube(query.ifBlank { text })
            return
        }

        if (lower.contains("open youtube")) {
            openYoutube()
            return
        }

        // Free-form commands go to your backend.
        sendToBackend(text)
    }

    private fun searchYoutube(query: String) {
        val encoded = URLEncoder.encode(query, "UTF-8")
        val uri = Uri.parse(
            "https://www.youtube.com/results?search_query=$encoded"
        )

        try {
            startActivity(Intent(Intent.ACTION_VIEW, uri))
            responseText.text = "Searching YouTube for: $query"
            statusText.text = "YouTube opened"
        } catch (e: Exception) {
            responseText.text = "Unable to open YouTube"
        }
    }

    private fun openYoutube() {
        try {
            val intent = packageManager.getLaunchIntentForPackage(
                "com.google.android.youtube"
            )

            if (intent != null) {
                startActivity(intent)
            } else {
                startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://www.youtube.com")
                    )
                )
            }
        } catch (e: Exception) {
            statusText.text = "Unable to open YouTube"
        }
    }

    private fun sendToBackend(command: String) {
        if (backendUrl.contains("YOUR-BACKEND")) {
            responseText.text =
                "Basic commands work. Configure your AI backend for other commands."
            statusText.text = "Backend URL not configured"
            return
        }

        statusText.text = "Thinking..."

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val connection =
                    URL(backendUrl).openConnection()
                        as HttpURLConnection

                connection.requestMethod = "POST"
                connection.connectTimeout = 15000
                connection.readTimeout = 20000
                connection.doOutput = true
                connection.setRequestProperty(
                    "Content-Type", "application/json"
                )

                val body = JSONObject()
                    .put("command", command)
                    .toString()

                connection.outputStream.use {
                    it.write(body.toByteArray(Charsets.UTF_8))
                }

                val code = connection.responseCode
                val stream = if (code in 200..299) {
                    connection.inputStream
                } else {
                    connection.errorStream
                }

                val result = stream.bufferedReader().use {
                    it.readText()
                }

                connection.disconnect()

                withContext(Dispatchers.Main) {
                    if (code in 200..299) {
                        val json = JSONObject(result)
                        val action = json.optString("action", "REPLY")
                        val reply = json.optString(
                            "reply", "Command received"
                        )

                        when (action) {
                            "SEARCH_YOUTUBE" -> {
                                searchYoutube(
                                    json.optString("query", command)
                                )
                            }

                            "OPEN_APP" -> {
                                when (
                                    json.optString("app").lowercase()
                                ) {
                                    "youtube" -> openYoutube()
                                    "chrome" -> openChrome()
                                    else -> responseText.text = reply
                                }
                            }

                            else -> {
                                responseText.text = reply
                            }
                        }

                        statusText.text = "Ready"
                    } else {
                        responseText.text =
                            "Backend error: HTTP $code"
                        statusText.text = "Request failed"
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    responseText.text =
                        "Couldn't connect to AI backend."
                    statusText.text = "Connection failed"
                }
            }
        }
    }

    private fun openChrome() {
        try {
            val intent = packageManager.getLaunchIntentForPackage(
                "com.android.chrome"
            )

            if (intent != null) {
                startActivity(intent)
            } else {
                startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://www.google.com")
                    )
                )
            }
        } catch (e: Exception) {
            responseText.text = "Unable to open browser"
        }
    }

    override fun onDestroy() {
        stopListening()
        super.onDestroy()
    }
}

