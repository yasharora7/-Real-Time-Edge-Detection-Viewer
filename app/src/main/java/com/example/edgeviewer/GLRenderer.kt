package com.example.edgeviewer

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class GLRenderer : GLSurfaceView.Renderer {

    private var textureId = 0
    private var frame: ByteBuffer? = null
    private var frameW = 0
    private var frameH = 0
    private val lock = Object()

    private lateinit var vBuf: FloatBuffer
    private lateinit var tBuf: FloatBuffer

    // Fullscreen quad
    private val vertices = floatArrayOf(
        -1f, -1f,
        1f, -1f,
        -1f,  1f,
        1f,  1f
    )

    // Correct texture orientation (no mirror, no rotation yet)
    private val texCoords = floatArrayOf(
        1f, 1f,   // bottom-right → becomes top-right
        1f, 0f,   // top-right    → becomes top-left
        0f, 1f,   // bottom-left  → becomes bottom-right
        0f, 0f    // top-left     → becomes bottom-left
    )



    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        textureId = GLUtils.createTexture()

        GLES20.glPixelStorei(GLES20.GL_UNPACK_ALIGNMENT, 1)

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST)

        vBuf = toBuf(vertices)
        tBuf = toBuf(texCoords)
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
    }

    override fun onDrawFrame(gl: GL10?) {
        val fb = frame ?: return

        synchronized(lock) {
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
            GLES20.glPixelStorei(GLES20.GL_UNPACK_ALIGNMENT, 1)

            // Upload real Y frame
            GLES20.glTexImage2D(
                GLES20.GL_TEXTURE_2D,
                0,
                GLES20.GL_LUMINANCE,
                frameW,
                frameH,
                0,
                GLES20.GL_LUMINANCE,
                GLES20.GL_UNSIGNED_BYTE,
                fb
            )
        }

        // Draw
        GLUtils.draw(textureId, vBuf, tBuf)
    }

    fun updateFrame(bytes: ByteArray, w: Int, h: Int) {
        synchronized(lock) {
            frameW = w
            frameH = h

            if (frame == null || frame!!.capacity() != bytes.size) {
                frame = ByteBuffer.allocateDirect(bytes.size)
                    .order(ByteOrder.nativeOrder())
            }

            frame!!.clear()
            frame!!.put(bytes)
            frame!!.position(0)
        }
    }

    private fun toBuf(arr: FloatArray): FloatBuffer =
        ByteBuffer.allocateDirect(arr.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .apply { put(arr); position(0) }
}
