package com.example.edgeviewer

import android.opengl.GLES20

object Shader {

    private const val VERTEX = """
        attribute vec2 aPosition;
        attribute vec2 aTexCoord;
        varying vec2 vTex;

        void main() {
            vTex = aTexCoord;
            gl_Position = vec4(aPosition, 1.0, 1.0);
        }
    """

    private const val FRAGMENT = """
        precision mediump float;
        varying vec2 vTex;
        uniform sampler2D tex;

        void main() {
            gl_FragColor = texture2D(tex, vTex);
        }
    """

    val program: Int by lazy {
        val v = compile(GLES20.GL_VERTEX_SHADER, VERTEX)
        val f = compile(GLES20.GL_FRAGMENT_SHADER, FRAGMENT)

        val p = GLES20.glCreateProgram()
        GLES20.glAttachShader(p, v)
        GLES20.glAttachShader(p, f)
        GLES20.glLinkProgram(p)
        p
    }

    private fun compile(type: Int, src: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, src)
        GLES20.glCompileShader(shader)
        return shader
    }
}
