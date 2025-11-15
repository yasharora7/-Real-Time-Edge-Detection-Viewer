package com.example.edgeviewer

import android.opengl.GLES20

object Shader {
    // Keep these names in sync with GLUtils.draw(...) usage
    private const val VERTEX =
        "attribute vec2 aPos;\n" +
                "attribute vec2 aTex;\n" +
                "varying vec2 vTexCoord;\n" +
                "void main() {\n" +
                "  gl_Position = vec4(aPos, 0.0, 1.0);\n" +
                "  vTexCoord = aTex;\n" +
                "}\n"

    private const val FRAGMENT =
        "precision mediump float;\n" +
                "uniform sampler2D uTex;\n" +
                "varying vec2 vTexCoord;\n" +
                "void main() {\n" +
                "  float y = texture2D(uTex, vTexCoord).r;\n" +
                "  gl_FragColor = vec4(y, y, y, 1.0);\n" +
                "}\n"

    val program: Int
    val aPos: Int
    val aTex: Int
    val uTex: Int

    init {
        // create program using GLUtils helper (your GLUtils.createProgram compiles + links)
        program = GLUtils.createProgram(VERTEX, FRAGMENT)

        // Query attribute & uniform locations once
        aPos = GLES20.glGetAttribLocation(program, "aPos")
        aTex = GLES20.glGetAttribLocation(program, "aTex")
        uTex = GLES20.glGetUniformLocation(program, "uTex")
    }
}
