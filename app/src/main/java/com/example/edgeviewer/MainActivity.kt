package com.example.edgeviewer

import android.Manifest
import android.os.Bundle
import android.content.pm.PackageManager
import android.view.Surface
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var camera: CameraController
    private lateinit var gl: GLSurface
    private lateinit var btnMode: Button
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
        tvFps = findViewById(R.id.tvFps)

        // Toggle between RAW and EDGE
        btnMode.setOnClickListener {
            showEdges = !showEdges
            btnMode.text = if (showEdges) "EDGE" else "RAW"
        }

        camera = CameraController(this) { bytes, w, h ->

            // ---- FPS ----
            val now = System.nanoTime()
            val fps = 1_000_000_000f / (now - lastTime)
            lastTime = now
            fpsSmoothed = fpsSmoothed * 0.9f + fps * 0.1f

            runOnUiThread {
                tvFps.text = "FPS: ${"%.1f".format(fpsSmoothed)}"
            }

            // RAW / EDGE toggle
            val output =
                if (showEdges) NativeEdge.processFrame(bytes, w, h)
                else bytes

            // Orientation
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
