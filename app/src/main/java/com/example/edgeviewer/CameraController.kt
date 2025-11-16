package com.example.edgeviewer

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.ImageFormat
import android.media.ImageReader
import android.hardware.camera2.*
import android.os.Handler
import android.os.HandlerThread

class CameraController(
    private val ctx: Context,
    private val onFrame: (ByteArray, Int, Int) -> Unit
) {

    private var cameraDevice: CameraDevice? = null
    private var session: CameraCaptureSession? = null
    private var imageReader: ImageReader? = null

    private var thread: HandlerThread? = HandlerThread("CamThread").apply { start() }
    private var handler: Handler? = thread?.let { Handler(it.looper) }

    @Volatile
    var isRunning = false
        private set

    private val camManager by lazy {
        ctx.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    }

    @SuppressLint("MissingPermission")
    @Synchronized
    fun start() {
        if (isRunning) return

        if (thread == null || thread?.isAlive == false) {
            thread = HandlerThread("CamThread").apply { start() }
            handler = Handler(thread!!.looper)
        }

        try {
            val cameraId = camManager.cameraIdList.first()

            imageReader = ImageReader.newInstance(
                640, 480, ImageFormat.YUV_420_888, 2
            )
            imageReader!!.setOnImageAvailableListener(::onImageAvailable, handler)

            camManager.openCamera(cameraId, object : CameraDevice.StateCallback() {
                override fun onOpened(dev: CameraDevice) {
                    cameraDevice = dev
                    createSession()
                    isRunning = true
                }

                override fun onDisconnected(dev: CameraDevice) {
                    dev.close()
                    isRunning = false
                }

                override fun onError(dev: CameraDevice, err: Int) {
                    dev.close()
                    isRunning = false
                }
            }, handler)

        } catch (e: Exception) {
            e.printStackTrace()
            isRunning = false
        }
    }

    private fun createSession() {
        val device = cameraDevice ?: return
        val surface = imageReader!!.surface

        val request = device.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
        request.addTarget(surface)

        device.createCaptureSession(
            listOf(surface),
            object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(s: CameraCaptureSession) {
                    session = s
                    try {
                        s.setRepeatingRequest(request.build(), null, handler)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
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


    @Synchronized
    fun stop() {
        try {
            session?.close()
            session = null

            cameraDevice?.close()
            cameraDevice = null

            imageReader?.close()
            imageReader = null

        } catch (e: Exception) {
            e.printStackTrace()
        }
        isRunning = false
    }

    @Synchronized
    fun release() {
        stop()
        try {
            thread?.quitSafely()
            thread = null
            handler = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
