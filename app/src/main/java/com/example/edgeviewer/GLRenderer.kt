package com.example.edgeviewer

import android.graphics.Bitmap
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.util.Log
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class GLRenderer : GLSurfaceView.Renderer {
    private val TAG = "GLRenderer"

    // Public snapshot for capture button
    @Volatile
    var lastFrame: ByteArray? = null

    // Frame dimensions
    @Volatile
    private var frameW = 0
    @Volatile
    private var frameH = 0

    // GL objects
    private var program = 0
    private var textureId = 0
    private var positionHandle = 0
    private var texCoordHandle = 0
    private var textureUniform = 0

    // Buffers
    private lateinit var vertexBuf: FloatBuffer
    private lateinit var texBuf: FloatBuffer

    // Thread-safety lock for frames / buffers
    private val lock = Object()
    private var frameBuffer: ByteBuffer? = null

    // Vertex positions for fullscreen quad (two triangles)
    private val vertices = floatArrayOf(
        -1f, -1f,
        1f, -1f,
        -1f,  1f,
        1f,  1f
    )

    // Texture coords for rotations (match earlier conventions)
    private val TEX_0 = floatArrayOf(0f, 1f, 1f, 1f, 0f, 0f, 1f, 0f)
    private val TEX_90 = floatArrayOf(1f, 1f, 1f, 0f, 0f, 1f, 0f, 0f)
    private val TEX_180 = floatArrayOf(1f, 0f, 0f, 0f, 1f, 1f, 0f, 1f)
    private val TEX_270 = floatArrayOf(0f, 0f, 0f, 1f, 1f, 0f, 1f, 1f)

    // Current tex coords
    @Volatile
    private var currentTexCoords = TEX_0

    // Simple vertex shader (pass through)
    private val VERT_SHADER = """
        attribute vec4 aPosition;
        attribute vec2 aTexCoord;
        varying vec2 vTexCoord;
        void main() {
            gl_Position = aPosition;
            vTexCoord = aTexCoord;
        }
    """.trimIndent()

    // Fragment shader: sample luminance and output gray
    // We sample texture.r (since GL_LUMINANCE will appear in the red channel).
    private val FRAG_SHADER = """
        precision mediump float;
        varying vec2 vTexCoord;
        uniform sampler2D uTexture;
        void main() {
            float y = texture2D(uTexture, vTexCoord).r;
            gl_FragColor = vec4(y, y, y, 1.0);
        }
    """.trimIndent()

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        Log.d(TAG, "onSurfaceCreated")
        program = createProgram(VERT_SHADER, FRAG_SHADER)
        if (program == 0) {
            throw RuntimeException("Failed to create GL program")
        }

        // get attribute/uniform locations
        positionHandle = GLES20.glGetAttribLocation(program, "aPosition")
        texCoordHandle = GLES20.glGetAttribLocation(program, "aTexCoord")
        textureUniform = GLES20.glGetUniformLocation(program, "uTexture")

        // allocate vertex buffer
        vertexBuf = ByteBuffer.allocateDirect(vertices.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer().apply {
                put(vertices)
                position(0)
            }

        // allocate tex buffer with default coords
        texBuf = toFloatBuffer(currentTexCoords)

        // create texture
        textureId = createTexture()
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST)
        GLES20.glPixelStorei(GLES20.GL_UNPACK_ALIGNMENT, 1)

        // If we already hold a frame in frameBuffer (camera still running), upload it now:
        synchronized(lock) {
            val fb = frameBuffer
            if (fb != null && frameW > 0 && frameH > 0) {
                fb.position(0)
                GLES20.glTexImage2D(
                    GLES20.GL_TEXTURE_2D, 0, GLES20.GL_LUMINANCE, frameW, frameH,
                    0, GLES20.GL_LUMINANCE, GLES20.GL_UNSIGNED_BYTE, fb
                )
            }
        }
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        Log.d(TAG, "onSurfaceChanged $width x $height")
    }

    override fun onDrawFrame(gl: GL10?) {
        // get local copy of frame buffer
        val fb = synchronized(lock) { frameBuffer ?: return }
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

        GLES20.glUseProgram(program)
        // bind texture
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
        GLES20.glUniform1i(textureUniform, 0)

        // upload latest Y plane into texture (fb is already ByteBuffer)
        fb.position(0)
        GLES20.glPixelStorei(GLES20.GL_UNPACK_ALIGNMENT, 1)
        GLES20.glTexImage2D(
            GLES20.GL_TEXTURE_2D, 0, GLES20.GL_LUMINANCE, frameW, frameH,
            0, GLES20.GL_LUMINANCE, GLES20.GL_UNSIGNED_BYTE, fb
        )

        // set vertex attribute
        GLES20.glEnableVertexAttribArray(positionHandle)
        GLES20.glVertexAttribPointer(positionHandle, 2, GLES20.GL_FLOAT, false, 0, vertexBuf)

        // tex coords buffer may be recreated each frame; recreate float buffer from currentTexCoords
        texBuf = toFloatBuffer(currentTexCoords)
        GLES20.glEnableVertexAttribArray(texCoordHandle)
        GLES20.glVertexAttribPointer(texCoordHandle, 2, GLES20.GL_FLOAT, false, 0, texBuf)

        // draw quad (triangle strip: 4 vertices)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)

        GLES20.glDisableVertexAttribArray(positionHandle)
        GLES20.glDisableVertexAttribArray(texCoordHandle)
    }

    /**
     * Called from camera/UI thread with a fresh Y plane (width*height).
     * Rotation must be one of 0/90/180/270.
     */
    fun updateFrame(bytes: ByteArray, w: Int, h: Int, rotation: Int) {
        synchronized(lock) {
            // keep a copy for capture
            lastFrame = bytes.copyOf()
            frameW = w
            frameH = h

            // ensure frameBuffer exists and contains the bytes
            if (frameBuffer == null || frameBuffer!!.capacity() < bytes.size) {
                frameBuffer = ByteBuffer.allocateDirect(bytes.size)
            }
            frameBuffer!!.position(0)
            frameBuffer!!.put(bytes)
            frameBuffer!!.position(0)

            // update currentTexCoords according to rotation
            currentTexCoords = when (rotation) {
                0 -> TEX_0
                90 -> TEX_90
                180 -> TEX_180
                270 -> TEX_270
                else -> TEX_0
            }
        }
        // NOTE: GLSurface will call requestRender() after invoking this.
    }

    // ----------------- helper GL functions -----------------

    private fun toFloatBuffer(arr: FloatArray): FloatBuffer =
        ByteBuffer.allocateDirect(arr.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .apply { put(arr); position(0) }

    private fun createProgram(vs: String, fs: String): Int {
        val v = loadShader(GLES20.GL_VERTEX_SHADER, vs)
        val f = loadShader(GLES20.GL_FRAGMENT_SHADER, fs)
        if (v == 0 || f == 0) return 0
        val p = GLES20.glCreateProgram()
        GLES20.glAttachShader(p, v)
        GLES20.glAttachShader(p, f)
        GLES20.glLinkProgram(p)
        val linkStatus = IntArray(1)
        GLES20.glGetProgramiv(p, GLES20.GL_LINK_STATUS, linkStatus, 0)
        if (linkStatus[0] == 0) {
            val err = GLES20.glGetProgramInfoLog(p)
            Log.e(TAG, "Program link error: $err")
            GLES20.glDeleteProgram(p)
            return 0
        }
        return p
    }

    private fun loadShader(type: Int, src: String): Int {
        val sh = GLES20.glCreateShader(type)
        GLES20.glShaderSource(sh, src)
        GLES20.glCompileShader(sh)
        val compiled = IntArray(1)
        GLES20.glGetShaderiv(sh, GLES20.GL_COMPILE_STATUS, compiled, 0)
        if (compiled[0] == 0) {
            val err = GLES20.glGetShaderInfoLog(sh)
            Log.e(TAG, "Shader compile error: $err")
            GLES20.glDeleteShader(sh)
            return 0
        }
        return sh
    }

    private fun createTexture(): Int {
        val tex = IntArray(1)
        GLES20.glGenTextures(1, tex, 0)
        return tex[0]
    }

    // ----------------- helper: capture to Bitmap -----------------

    fun getLastFrameBitmap(): Bitmap? {
        val w = frameW
        val h = frameH
        if (w <= 0 || h <= 0) return null
        val src = synchronized(lock) { frameBuffer?.duplicate() ?: return null }
        src.position(0)
        val buffer = ByteArray(w * h)
        src.get(buffer)
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        var idx = 0
        for (y in 0 until h) {
            for (x in 0 until w) {
                val v = buffer[idx].toInt() and 0xFF
                val col = (0xFF shl 24) or (v shl 16) or (v shl 8) or v
                bmp.setPixel(x, y, col)
                idx++
            }
        }
        return bmp
    }
}
