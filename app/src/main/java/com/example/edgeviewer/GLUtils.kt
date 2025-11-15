package com.example.edgeviewer

import android.opengl.GLES20
import java.nio.FloatBuffer

object GLUtils {

    fun createTexture(): Int {
        val t = IntArray(1)
        GLES20.glGenTextures(1, t, 0)

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, t[0])
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)

        return t[0]
    }

    fun createProgram(v: String, f: String): Int {
        val vs = compile(GLES20.GL_VERTEX_SHADER, v)
        val fs = compile(GLES20.GL_FRAGMENT_SHADER, f)

        val program = GLES20.glCreateProgram()
        GLES20.glAttachShader(program, vs)
        GLES20.glAttachShader(program, fs)
        GLES20.glLinkProgram(program)

        return program
    }

    private fun compile(type: Int, src: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, src)
        GLES20.glCompileShader(shader)
        return shader
    }

    fun draw(textureId: Int, v: FloatBuffer, t: FloatBuffer) {
        GLES20.glUseProgram(Shader.program)

        GLES20.glEnableVertexAttribArray(Shader.aPos)
        GLES20.glVertexAttribPointer(Shader.aPos, 2, GLES20.GL_FLOAT, false, 0, v)

        GLES20.glEnableVertexAttribArray(Shader.aTex)
        GLES20.glVertexAttribPointer(Shader.aTex, 2, GLES20.GL_FLOAT, false, 0, t)

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
        GLES20.glUniform1i(Shader.uTex, 0)

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
    }
}
