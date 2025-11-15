package com.example.edgeviewer

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class GLRenderer : GLSurfaceView.Renderer {

    private var textureId = 0
    private var frameBuffer: ByteBuffer? = null
    private var frameWidth = 0
    private var frameHeight = 0
    private val lock = Object()

    fun updateFrame(bytes: ByteArray, width: Int, height: Int) {
        synchronized(lock) {
            frameWidth = width
            frameHeight = height

            frameBuffer = ByteBuffer
                .allocateDirect(bytes.size)
                .order(ByteOrder.nativeOrder())
                .put(bytes)
                .apply { position(0) }
        }
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        textureId = GLUtils.createTexture()
    }

    override fun onDrawFrame(gl: GL10?) {
        synchronized(lock) {
            val fb = frameBuffer ?: return

            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)

            GLES20.glTexImage2D(
                GLES20.GL_TEXTURE_2D,
                0,
                GLES20.GL_LUMINANCE,
                frameWidth,
                frameHeight,
                0,
                GLES20.GL_LUMINANCE,
                GLES20.GL_UNSIGNED_BYTE,
                fb
            )
        }

        GLUtils.drawFullScreenQuad(textureId)
    }

    override fun onSurfaceChanged(gl: GL10?, w: Int, h: Int) {
        GLES20.glViewport(0, 0, w, h)
    }
}
