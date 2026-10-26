
package com.lynko.ai

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
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

    private val backendUrl =
        "https://aaiii.onrender.com/command"

    private val blue = Color.rgb(0, 190, 255)
    private val darkBackground = Color.rgb(7, 12, 28)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        createInterface()
        requestMicrophonePermission()
    }

    private fun createInterface() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(24, 28, 24, 24)
            setBackgroundColor(darkBackground)
        }

        val scroll = ScrollView(this)

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
        }

        val logo = TextView(this).apply {
            text = "◈"
            textSize = 68f
            gravity = Gravity.CENTER
            setTextColor(blue)
        }

        val title = TextView(this).apply {
            text = "LYNKO AI"
            textSize = 32f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
        }

        val subtitle = TextView(this).apply {
            text = "YOUR PERSONAL AI ASSISTANT"
            textSize = 12f
            gravity = Gravity.CENTER
            setTextColor(blue)
        }

        statusText = TextView(this).apply {
            text = "Ready to start"
            textSize = 15f
            gravity = Gravity.CENTER
            setTextColor(Color.LTGRAY)
            setPadding(0, 30, 0, 20)
        }

        listenButton = Button(this).apply {
            text = "🎙 START LISTENING"
            textSize = 16f
            setTextColor(Color.WHITE)
            background = roundedBackground(blue, 24f)
            setOnClickListener {
                startListening()
            }
        }

        val stopButton = Button(this).apply {
            text = "STOP LISTENING"
            setTextColor(Color.WHITE)
            background = roundedBackground(
                Color.rgb(45, 53, 75), 24f
            )
            setOnClickListener {
                stopListening()
            }
        }

        val youtubeButton = Button(this).apply {
            text = "▶ OPEN YOUTUBE"
            setTextColor(Color.WHITE)
            background = roundedBackground(
                Color.rgb(200, 35, 55), 24f
            )
            setOnClickListener {
                openYoutube()
            }
        }

        responseText = TextView(this).apply {
            text = "Hello! I'm LYNKO. How can I help you?"
            textSize = 16f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            setPadding(12, 28, 12, 24)
        }

        val instructions = TextView(this).apply {
            text = """
                Try saying:

                • Search YouTube for Raveen Kanishka
                • Open YouTube
                • Hi LYNKO
                • Mata sinduwak hoyala denna

                Tap START LISTENING to speak.
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
            fullWidthButton(58)
        )

        content.addView(
            stopButton,
            fullWidthButton(54)
        )

        content.addView(
            youtubeButton,
            fullWidthButton(54)
        )

        content.addView(responseText)
        content.addView(instructions)

        scroll.addView(content)

        root.addView(
            scroll,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        setContentView(root)
    }

    private fun fullWidthButton(height: Int) =
        LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            height
        ).apply {
            bottomMargin = 12
        }

    private fun roundedBackground(
        color: Int,
        radius: Float
    ) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = radius
    }

    private fun requestMicrophonePermission() {
        if (
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                100
            )
        }
    }

    private fun startListening() {
        if (
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestMicrophonePermission()
            statusText.text = "Please allow microphone permission"
            return
        }

        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            statusText.text = "Speech recognition unavailable"
            return
        }

        stopRecognizerOnly()

        recognizer = SpeechRecognizer.createSpeechRecognizer(this)

        recognizer?.setRecognitionListener(
            object : RecognitionListener {

                override fun onReadyForSpeech(params: Bundle?) {
                    statusText.text = "Listening... Speak now"
                    listenButton.text = "🎙 LISTENING..."
                }

                override fun onBeginningOfSpeech() {
                    statusText.text = "Hearing your voice..."
                }

                override fun onRmsChanged(rmsdB: Float) {}

                override fun onBufferReceived(buffer: ByteArray?) {}

                override fun onEndOfSpeech() {
                    statusText.text = "Processing..."
                }

                override fun onError(error: Int) {
                    listenButton.text = "🎙 START LISTENING"
                    statusText.text =
                        "Listening stopped. Tap to try again."
                }

                override fun onResults(results: Bundle?) {
                    listenButton.text = "🎙 START LISTENING"

                    val matches = results?.getStringArrayList(
                        SpeechRecognizer.RESULTS_RECOGNITION
                    )

                    val command = matches?.firstOrNull().orEmpty()

                    if (command.isNotBlank()) {
                        responseText.text = "You said:\n$command"
                        processCommand(command)
                    } else {
                        statusText.text = "No speech detected"
                    }
                }

                override fun onPartialResults(
                    partialResults: Bundle?
                ) {}

                override fun onEvent(
                    eventType: Int,
                    params: Bundle?
                ) {}
            }
        )

        val speechIntent = Intent(
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
            putExtra(
                RecognizerIntent.EXTRA_PARTIAL_RESULTS,
                true
            )
        }

        try {
            recognizer?.startListening(speechIntent)
        } catch (e: Exception) {
            statusText.text = "Could not start microphone"
        }
    }

    private fun stopRecognizerOnly() {
        try {
            recognizer?.cancel()
            recognizer?.destroy()
        } catch (_: Exception) {
        }

        recognizer = null
    }

    private fun stopListening() {
        stopRecognizerOnly()
        listenButton.text = "🎙 START LISTENING"
        statusText.text = "Listening stopped"
    }

    private fun processCommand(command: String) {
        val lower = command.lowercase(Locale.ROOT)

        if (
            lower.contains("open youtube") ||
            lower.contains("youtube open") ||
            lower.contains("youtube ekata yanna")
        ) {
            openYoutube()
            return
        }

        if (
            lower.contains("youtube") ||
            lower.contains("song") ||
            lower.contains("music") ||
            lower.contains("sindu") ||
            lower.contains("sinduwak") ||
            lower.contains("play")
        ) {
            sendToBackend(command)
            return
        }

        sendToBackend(command)
    }

    private fun openYoutube() {
        try {
            val launchIntent =
                packageManager.getLaunchIntentForPackage(
                    "com.google.android.youtube"
                )

            if (launchIntent != null) {
                startActivity(launchIntent)
            } else {
                startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://www.youtube.com")
                    )
                )
            }

            responseText.text = "Opening YouTube..."
            statusText.text = "Done"
        } catch (e: Exception) {
            statusText.text = "Unable to open YouTube"
        }
    }

    private fun searchYoutube(query: String) {
        val encodedQuery = URLEncoder.encode(query, "UTF-8")

        val uri = Uri.parse(
            "https://www.youtube.com/results?search_query=$encodedQuery"
        )

        try {
            startActivity(
                Intent(Intent.ACTION_VIEW, uri)
            )

            responseText.text = "Searching YouTube for:\n$query"
            statusText.text = "YouTube search opened"
        } catch (e: Exception) {
            statusText.text = "Could not open YouTube search"
        }
    }

    private fun sendToBackend(command: String) {
        statusText.text = "LYNKO AI is thinking..."

        CoroutineScope(Dispatchers.IO).launch {
            var connection: HttpURLConnection? = null

            try {
                connection =
                    URL(backendUrl).openConnection()
                        as HttpURLConnection

                connection.requestMethod = "POST"
                connection.connectTimeout = 20000
                connection.readTimeout = 30000
                connection.doOutput = true

                connection.setRequestProperty(
                    "Content-Type",
                    "application/json; charset=UTF-8"
                )

                val body = JSONObject()
                    .put("command", command)
                    .toString()

                connection.outputStream.use { output ->
                    output.write(
                        body.toByteArray(Charsets.UTF_8)
                    )
                }

                val statusCode = connection.responseCode

                val stream =
                    if (statusCode in 200..299) {
                        connection.inputStream
                    } else {
                        connection.errorStream
                    }

                val responseBody = stream
                    ?.bufferedReader()
                    ?.use { it.readText() }
                    ?: ""

                if (statusCode !in 200..299) {
                    throw Exception("HTTP $statusCode")
                }

                val result = JSONObject(responseBody)

                withContext(Dispatchers.Main) {
                    handleBackendResponse(result, command)
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    responseText.text =
                        "AI connection failed.\n" +
                        "Check your backend and internet connection."

                    statusText.text = "Connection error"
                }
            } finally {
                connection?.disconnect()
            }
        }
    }

    private fun handleBackendResponse(
        result: JSONObject,
        originalCommand: String
    ) {
        val action = result.optString("action", "REPLY")
        val reply = result.optString(
            "reply",
            "Command received."
        )

        when (action) {
            "SEARCH_YOUTUBE" -> {
                val query = result.optString(
                    "query",
                    originalCommand
                )

                if (query.isNotBlank()) {
                    searchYoutube(query)
                } else {
                    responseText.text = reply
                }
            }

            "OPEN_APP" -> {
                when (
                    result.optString("app").lowercase(Locale.ROOT)
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

        statusText.text = "LYNKO AI is ready"
    }

    private fun openChrome() {
        try {
            val intent =
                packageManager.getLaunchIntentForPackage(
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
            Toast.makeText(
                this,
                "Unable to open browser",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onDestroy() {
        stopRecognizerOnly()
        super.onDestroy()
    }
}
