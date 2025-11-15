package com.example.edgeviewer

import android.Manifest
import android.os.Bundle
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.view.Surface
import android.widget.Button
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var camera: CameraController
    private lateinit var gl: GLSurface
    private lateinit var btnMode: Button

    private var showEdges = false   // default RAW

    private val launcher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            if (it) startCamera()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        gl = findViewById(R.id.glSurface)
        btnMode = findViewById(R.id.btnMode)

        // toggle logic
        btnMode.setOnClickListener {
            showEdges = !showEdges
            btnMode.text = if (showEdges) "EDGE" else "RAW"
        }

        camera = CameraController(this) { bytes, w, h ->

            // choose output
            val output = if (showEdges) {
                NativeEdge.processFrame(bytes, w, h)    // EDGE
            } else {
                bytes                                   // RAW
            }

            // rotation logic unchanged
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
        ) {
            startCamera()
        } else {
            launcher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun startCamera() = camera.start()

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        gl.requestLayout()
    }
}
