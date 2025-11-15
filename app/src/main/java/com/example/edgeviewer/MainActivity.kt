package com.example.edgeviewer

import android.Manifest
import android.os.Bundle
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.view.Surface
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var camera: CameraController
    private lateinit var gl: GLSurface

    private val launcher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            if (it) startCamera()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)
        gl = findViewById(R.id.glSurface)

        camera = CameraController(this) { bytes, w, h ->
            val edges = NativeEdge.processFrame(bytes, w, h)

            val rotation = when (windowManager.defaultDisplay.rotation) {
                Surface.ROTATION_0 -> 90      // portrait
                Surface.ROTATION_90 -> 0      // landscape left
                Surface.ROTATION_180 -> 270
                Surface.ROTATION_270 -> 180   // landscape right
                else -> 90
            }

            gl.update(edges, w, h, rotation)
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
        gl.requestLayout() // prevents rotated black screen
    }
}
