package com.example.ps5titlecamera

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

class MainActivity : AppCompatActivity() {
    private lateinit var previewView: PreviewView
    private lateinit var codeView: TextView
    private lateinit var titleView: TextView
    private lateinit var statusView: TextView
    private lateinit var manualInput: EditText
    private val cameraExecutor = Executors.newSingleThreadExecutor()
    private val recognizer by lazy { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }
    private val titles = HashMap<String, String>()
    private val processing = AtomicBoolean(false)
    @Volatile private var lastCode = ""
    @Volatile private var lastAcceptedAt = 0L

    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) startCamera() else statusView.text = "Camera permission is required to scan codes."
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildUi()
        Thread { loadTitleDatabase() }.start()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 14, 16, 12)
            setBackgroundColor(0xFF10131A.toInt())
        }
        val heading = TextView(this).apply {
            text = "PS5 TITLE CAMERA"
            textSize = 20f
            setTextColor(0xFFF5F7FB.toInt())
            gravity = Gravity.CENTER_VERTICAL
        }
        previewView = PreviewView(this).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
        val overlay = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(14, 12, 14, 12)
            setBackgroundColor(0xE6192638.toInt())
        }
        codeView = TextView(this).apply {
            text = "WAITING FOR PPSA CODE"
            textSize = 13f
            setTextColor(0xFFA9C0FF.toInt())
        }
        titleView = TextView(this).apply {
            text = "Point camera at a game code"
            textSize = 23f
            setTextColor(0xFFFFFFFF.toInt())
        }
        statusView = TextView(this).apply {
            text = "Loading PS5 title list…"
            textSize = 13f
            setTextColor(0xFFCBD3DF.toInt())
        }
        overlay.addView(codeView)
        overlay.addView(titleView)
        overlay.addView(statusView)
        manualInput = EditText(this).apply {
            hint = "Enter PPSA code manually"
            singleLine = true
            setTextColor(0xFFFFFFFF.toInt())
            setHintTextColor(0xFF9CA8B9.toInt())
            setBackgroundColor(0xFF202838.toInt())
        }
        val lookupButton = Button(this).apply {
            text = "Look up code"
            setOnClickListener { lookup(manualInput.text.toString()) }
        }
        val cameraContainer = FrameLayout(this).apply {
            setBackgroundColor(0xFF05070A.toInt())
            addView(previewView, FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT
            ))
            addView(overlay, FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM
            ).apply { leftMargin = 10; rightMargin = 10; bottomMargin = 10 })
        }
        root.addView(heading)
        root.addView(cameraContainer, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
        ).apply { topMargin = 12; bottomMargin = 8 })
        root.addView(manualInput)
        root.addView(lookupButton)
        setContentView(root)
    }

    private fun startCamera() {
        val providerFuture = ProcessCameraProvider.getInstance(this)
        providerFuture.addListener({
            val provider = providerFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }
            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
            analysis.setAnalyzer(cameraExecutor) { proxy ->
                val mediaImage = proxy.image
                if (mediaImage == null || !processing.compareAndSet(false, true)) {
                    proxy.close()
                } else {
                    val input = InputImage.fromMediaImage(mediaImage, proxy.imageInfo.rotationDegrees)
                    recognizer.process(input)
                        .addOnSuccessListener { result ->
                            findCode(result.text)?.let { code ->
                                val now = System.currentTimeMillis()
                                if (code != lastCode || now - lastAcceptedAt > 5000) {
                                    lastCode = code
                                    lastAcceptedAt = now
                                    runOnUiThread { lookup(code) }
                                }
                            }
                        }
                        .addOnFailureListener { error ->
                            runOnUiThread { statusView.text = "OCR error: ${error.localizedMessage}" }
                        }
                        .addOnCompleteListener {
                            processing.set(false)
                            proxy.close()
                        }
                }
            }
            try {
                provider.unbindAll()
                provider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
                statusView.text = "Camera active · hold the PPSA code in view"
            } catch (e: Exception) {
                statusView.text = "Could not start camera: ${e.localizedMessage}"
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun findCode(text: String): String? {
        for (line in text.uppercase().lines()) {
            val compact = line.replace(Regex("[^A-Z0-9]"), "")
            val match = Regex("(?<![A-Z0-9])PPSA[0-9]{5}(?![0-9])").find(compact)
            if (match != null) return match.value
        }
        return null
    }

    private fun lookup(raw: String) {
        val cleaned = raw.uppercase().replace(Regex("[^A-Z0-9]"), "")
        if (!Regex("^PPSA[0-9]{5}$").matches(cleaned)) {
            statusView.text = "Enter exactly PPSA followed by five digits."
            return
        }
        val name = titles[cleaned]
        codeView.text = cleaned
        titleView.text = name ?: "Title not found in database"
        statusView.text = if (name != null) "Game matched in PS5 title database" else "No matching entry; database may still be loading"
        manualInput.setText(cleaned)
    }

    private fun loadTitleDatabase() {
        try {
            val url = URL("https://raw.githubusercontent.com/andshrew/PlayStation-Titles/main/PS5_Titles.tsv")
            val connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 15000
                readTimeout = 20000
                requestMethod = "GET"
            }
            connection.inputStream.bufferedReader(Charsets.UTF_8).useLines { lines ->
                lines.forEach { line ->
                    val cols = line.split('\t')
                    if (cols.size >= 3) {
                        val sourceId = cols[0].trim().uppercase()
                        val id = sourceId.substringBefore('_')
                        val name = cols[2].trim()
                        if (Regex("^PPSA[0-9]{5}(?:_[0-9]{2})?$").matches(sourceId) && name.isNotBlank()) {
                            synchronized(titles) { if (!titles.containsKey(id)) titles[id] = name }
                        }
                    }
                }
            }
            connection.disconnect()
            runOnUiThread { statusView.text = "PS5 database ready · ${titles.size} title IDs" }
        } catch (_: Exception) {
            runOnUiThread { statusView.text = "Database unavailable; check internet and restart." }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        recognizer.close()
    }
}
