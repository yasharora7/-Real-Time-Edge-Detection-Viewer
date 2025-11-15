package com.example.edgeviewer

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.ImageFormat
import android.hardware.camera2.*
import android.media.ImageReader
import android.os.Handler
import android.os.HandlerThread


class CameraController(
    private val ctx: Context,
    private val onFrame: (ByteArray) -> Unit
) {
    private var cameraDevice: CameraDevice? = null
    private var session: CameraCaptureSession? = null
    private lateinit var reader: ImageReader

    private val handlerThread = HandlerThread("CameraThread").apply { start() }
    private val handler = Handler(handlerThread.looper)

    @SuppressLint("MissingPermission")
    fun start() {
        val cm = ctx.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val camId = cm.cameraIdList.first()

        reader = ImageReader.newInstance(640, 480,
            ImageFormat.YUV_420_888, 2)

        reader.setOnImageAvailableListener({ ir ->
            val img = ir.acquireLatestImage() ?: return@setOnImageAvailableListener

            val yPlane = img.planes[0]
            val ySize = yPlane.buffer.remaining()
            val yBytes = ByteArray(ySize)
            yPlane.buffer.get(yBytes)

            onFrame(yBytes)
            img.close()
        }, handler)

        cm.openCamera(camId, object : CameraDevice.StateCallback() {
            override fun onOpened(device: CameraDevice) {
                cameraDevice = device
                startSession()
            }

            override fun onDisconnected(device: CameraDevice) {}
            override fun onError(device: CameraDevice, error: Int) {}
        }, handler)
    }

    private fun startSession() {
        val surface = reader.surface

        val request = cameraDevice!!.createCaptureRequest(
            CameraDevice.TEMPLATE_PREVIEW
        )
        request.addTarget(surface)

        cameraDevice!!.createCaptureSession(
            listOf(surface),
            object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(s: CameraCaptureSession) {
                    session = s
                    s.setRepeatingRequest(request.build(), null, handler)
                }

                override fun onConfigureFailed(session: CameraCaptureSession) {}
            },
            handler
        )
    }

    fun stop() {
        session?.stopRepeating()
        cameraDevice?.close()
        reader.close()
    }
}
