package com.example.edgeviewer

import android.Manifest
import android.annotation.SuppressLint
import android.os.Bundle
import android.content.pm.PackageManager
import android.view.Surface
import android.widget.TextView
import android.widget.Toast
import android.widget.LinearLayout
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var camera: CameraController
    private lateinit var gl: GLSurface

    private lateinit var btnMode: TextView
    private lateinit var btnCapture: TextView
    private lateinit var tvFps: TextView
    private lateinit var tvProcess: TextView

    private lateinit var hudTop: LinearLayout
    private lateinit var hudBottom: LinearLayout

    private var showEdges = false
    private var lastTime = System.nanoTime()
    private var fpsSmoothed = 0f

    private var lastInteraction = System.currentTimeMillis()

    private val launcher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            if (it) startCamera()
        }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Views
        gl = findViewById(R.id.glSurface)
        btnMode = findViewById(R.id.btnMode)
        btnCapture = findViewById(R.id.btnCapture)
        tvFps = findViewById(R.id.tvFps)
        tvProcess = findViewById(R.id.tvProcess)

        hudTop = findViewById(R.id.hudTop)
        hudBottom = findViewById(R.id.hudBottom)

        // --- UI auto-hide loop ---
        startHUDHideLoop()

        // Mode toggle
        btnMode.setOnClickListener {
            showEdges = !showEdges
            btnMode.text = if (showEdges) "EDGE" else "RAW"
            updateUserInteraction()
        }


        // Capture
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

            updateUserInteraction()
        }

        // Camera callback
        camera = CameraController(this) { bytes, w, h ->

            // FPS
            val now = System.nanoTime()
            val fps = 1_000_000_000f / (now - lastTime)
            lastTime = now
            fpsSmoothed = fpsSmoothed * 0.9f + fps * 0.1f

            // Processing time
            val processingStart = System.nanoTime()
            val output = if (showEdges) NativeEdge.processFrame(bytes, w, h) else bytes
            val processingMs = (System.nanoTime() - processingStart) / 1_000_000f

            runOnUiThread {
                tvFps.text = "FPS: %.1f".format(fpsSmoothed)
                tvProcess.text = "PROC: %.2f ms".format(processingMs)
            }

            // Rotation
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


    // ----------------------
    // HUD AUTO HIDE SYSTEM
    // ----------------------
    @SuppressLint("ClickableViewAccessibility")
    private fun startHUDHideLoop() {
        val handler = android.os.Handler()
        handler.post(object : Runnable {
            override fun run() {
                val now = System.currentTimeMillis()
                if (now - lastInteraction > 3000) {
                    hudTop.animate().alpha(0f).setDuration(300).start()
                    hudBottom.animate().alpha(0f).setDuration(300).start()
                }
                handler.postDelayed(this, 300)
            }
        })

        window.decorView.setOnTouchListener { _, _ ->
            updateUserInteraction()
            false
        }
    }

    private fun updateUserInteraction() {
        lastInteraction = System.currentTimeMillis()
        hudTop.animate().alpha(1f).setDuration(150).start()
        hudBottom.animate().alpha(1f).setDuration(150).start()
    }



    // ----------------------
    // CAMERA PERMISSION
    // ----------------------
    private fun requestPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) startCamera()
        else launcher.launch(Manifest.permission.CAMERA)
    }

    private fun startCamera() = camera.start()
}
