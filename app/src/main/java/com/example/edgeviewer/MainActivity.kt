package com.example.edgeviewer

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var glView: GLView
    private var cameraController: CameraController? = null

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) startCamera()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        glView = GLView(this)
        setContentView(glView)

        checkAndRequestPermission()
    }

    override fun onResume() {
        super.onResume()
        if (hasPermission()) startCamera()
        glView.onResume()
    }

    override fun onPause() {
        super.onPause()
        cameraController?.stop()
        glView.onPause()
    }

    private fun hasPermission() =
        ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED

    private fun checkAndRequestPermission() {
        if (!hasPermission()) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    private fun startCamera() {
        cameraController = CameraController(
            ctx = this,
            onFrame = { yBytes ->
                val processed = NativeEdge.processFrame(
                    input = yBytes,
                    width = 640,
                    height = 480
                )

                glView.renderer.updateFrame(
                    bytes = processed,
                    width = 640,
                    height = 480
                )

                glView.requestRender()
            }
        )
        cameraController!!.start()
    }

}


