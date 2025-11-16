package com.example.edgeviewer

import android.Manifest
import android.os.Bundle
import android.content.pm.PackageManager
import android.view.Surface
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var camera: CameraController
    private lateinit var gl: GLSurface

    private lateinit var btnMode: Button
    private lateinit var btnCapture: Button
    private lateinit var tvFps: TextView

    private var showEdges = false
    private var lastTime = System.nanoTime()
    private var fpsSmoothed = 0f

    private val launcher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            if (it) startCamera()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        gl = findViewById(R.id.glSurface)
        btnMode = findViewById(R.id.btnMode)
        btnCapture = findViewById(R.id.btnCapture)
        tvFps = findViewById(R.id.tvFps)
        val tvProcess: TextView = findViewById(R.id.tvProcess)

        // Toggle RAW/EDGE
        btnMode.setOnClickListener {
            showEdges = !showEdges
            btnMode.text = if (showEdges) "EDGE" else "RAW"
        }

        // Capture image button
        btnCapture.setOnClickListener {
            val bmp = gl.rendererPublic.getLastFrameBitmap()
            if (bmp == null) {
                Toast.makeText(this, "No frame yet!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val uri = ImageSaver.saveToGallery(this, bmp)
            if (uri != null)
                Toast.makeText(this, "Saved to Gallery!", Toast.LENGTH_SHORT).show()
            else
                Toast.makeText(this, "Failed to save!", Toast.LENGTH_SHORT).show()
        }

        // Camera callback
        camera = CameraController(this) { bytes, w, h ->

            // ---------- FPS ----------
            val now = System.nanoTime()
            val frameTimeNs = now - lastTime
            lastTime = now

            val fps = 1_000_000_000f / frameTimeNs
            fpsSmoothed = fpsSmoothed * 0.9f + fps * 0.1f

            // ---------- PROCESS TIME (ms) ----------
            val processingStart = System.nanoTime()

            val output = if (showEdges)
                NativeEdge.processFrame(bytes, w, h)
            else
                bytes

            val processingMs =
                (System.nanoTime() - processingStart) / 1_000_000f

            android.util.Log.d("FrameTime", "Processing = ${"%.3f".format(processingMs)} ms")

            // ---------- UPDATE UI ----------
            runOnUiThread {
                tvFps.text = "FPS: %.1f".format(fpsSmoothed)
                tvProcess.text = "PROC: %.2f ms".format(processingMs)
            }

            // ---------- ROTATION ----------
            val rotation = when (windowManager.defaultDisplay.rotation) {
                Surface.ROTATION_0 -> 90
                Surface.ROTATION_90 -> 0
                Surface.ROTATION_180 -> 270
                Surface.ROTATION_270 -> 180
                else -> 90
            }

            gl.update(output, w, h, rotation)
        }

        requestPermission()
    }

    private fun requestPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) startCamera()
        else launcher.launch(Manifest.permission.CAMERA)
    }

    private fun startCamera() = camera.start()
}
