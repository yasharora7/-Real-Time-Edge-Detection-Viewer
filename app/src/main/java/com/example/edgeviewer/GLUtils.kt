package com.example.edgeviewer

import android.opengl.GLES20
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

object GLUtils {

    // --- Full-screen quad coordinates ---
    private val VERTICES = floatArrayOf(
        -1f, -1f,
        1f, -1f,
        -1f,  1f,
        1f,  1f
    )

    private val TEX_COORDS = floatArrayOf(
        0f, 1f,
        1f, 1f,
        0f, 0f,
        1f, 0f
    )

    private val vertexBuffer: FloatBuffer =
        ByteBuffer.allocateDirect(VERTICES.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer().apply {
                put(VERTICES)
                position(0)
            }

    private val texBuffer: FloatBuffer =
        ByteBuffer.allocateDirect(TEX_COORDS.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer().apply {
                put(TEX_COORDS)
                position(0)
            }

    fun createTexture(): Int {
        val tex = IntArray(1)
        GLES20.glGenTextures(1, tex, 0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, tex[0])

        GLES20.glTexParameteri(
            GLES20.GL_TEXTURE_2D,
            GLES20.GL_TEXTURE_MIN_FILTER,
            GLES20.GL_LINEAR
        )
        GLES20.glTexParameteri(
            GLES20.GL_TEXTURE_2D,
            GLES20.GL_TEXTURE_MAG_FILTER,
            GLES20.GL_LINEAR
        )
        GLES20.glTexParameteri(
            GLES20.GL_TEXTURE_2D,
            GLES20.GL_TEXTURE_WRAP_S,
            GLES20.GL_CLAMP_TO_EDGE
        )
        GLES20.glTexParameteri(
            GLES20.GL_TEXTURE_2D,
            GLES20.GL_TEXTURE_WRAP_T,
            GLES20.GL_CLAMP_TO_EDGE
        )

        return tex[0]
    }

    fun drawFullScreenQuad(textureId: Int) {
        GLES20.glUseProgram(Shader.program)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)

        val posLoc = GLES20.glGetAttribLocation(Shader.program, "aPosition")
        val uvLoc = GLES20.glGetAttribLocation(Shader.program, "aTexCoord")

        GLES20.glEnableVertexAttribArray(posLoc)
        GLES20.glVertexAttribPointer(posLoc, 2, GLES20.GL_FLOAT, false, 0, vertexBuffer)

        GLES20.glEnableVertexAttribArray(uvLoc)
        GLES20.glVertexAttribPointer(uvLoc, 2, GLES20.GL_FLOAT, false, 0, texBuffer)

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
    }
}
