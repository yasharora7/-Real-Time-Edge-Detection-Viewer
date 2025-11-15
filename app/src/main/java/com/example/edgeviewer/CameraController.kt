package com.example.edgeviewer

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.ImageFormat
import android.media.Image
import android.media.ImageReader
import android.hardware.camera2.*
import android.os.Handler
import android.os.HandlerThread
import android.util.Size

class CameraController(
    private val ctx: Context,
    private val onFrame: (ByteArray, Int, Int) -> Unit
) {

    private var cameraDevice: CameraDevice? = null
    private var session: CameraCaptureSession? = null
    private var imageReader: ImageReader? = null

    private val thread = HandlerThread("CamThread").apply { start() }
    private val handler = Handler(thread.looper)

    private val camManager by lazy {
        ctx.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    }

    @SuppressLint("MissingPermission")
    fun start() {
        val cameraId = camManager.cameraIdList.first()

        // Prepare Y-only reader
        imageReader = ImageReader.newInstance(
            640, 480, ImageFormat.YUV_420_888, 2
        )
        imageReader!!.setOnImageAvailableListener(::onImageAvailable, handler)

        camManager.openCamera(cameraId, object : CameraDevice.StateCallback() {
            override fun onOpened(dev: CameraDevice) {
                cameraDevice = dev
                createSession()
            }

            override fun onDisconnected(dev: CameraDevice) {
                dev.close()
            }

            override fun onError(dev: CameraDevice, err: Int) {
                dev.close()
            }
        }, handler)
    }

    private fun createSession() {
        val device = cameraDevice ?: return
        val surface = imageReader!!.surface

        val request = device.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
        request.addTarget(surface)

        // ✔ Compatible with SDK 24–34
        device.createCaptureSession(
            listOf(surface),
            object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(s: CameraCaptureSession) {
                    session = s
                    s.setRepeatingRequest(request.build(), null, handler)
                }

                override fun onConfigureFailed(s: CameraCaptureSession) {}
            },
            handler
        )
    }

    private fun onImageAvailable(reader: ImageReader) {
        val image = reader.acquireLatestImage() ?: return

        val width = image.width
        val height = image.height
        val yPlane = image.planes[0]
        val yBuf = yPlane.buffer
        val rowStride = yPlane.rowStride

        val out = ByteArray(width * height)

        var outIndex = 0
        val row = ByteArray(rowStride)

        for (r in 0 until height) {
            yBuf.position(r * rowStride)
            yBuf.get(row, 0, rowStride)
            System.arraycopy(row, 0, out, outIndex, width)
            outIndex += width
        }

        onFrame(out, width, height)
        image.close()
    }


    fun stop() {
        session?.close()
        cameraDevice?.close()
        imageReader?.close()
        thread.quitSafely()
    }
}
