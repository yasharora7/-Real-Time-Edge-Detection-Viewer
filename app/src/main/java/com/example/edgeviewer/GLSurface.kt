package com.example.edgeviewer

import android.content.Context
import android.opengl.GLSurfaceView
import android.util.AttributeSet

class GLSurface @JvmOverloads constructor(
    ctx: Context,
    attrs: AttributeSet? = null
) : GLSurfaceView(ctx, attrs) {

    // expose renderer publicly
    val rendererPublic = GLRenderer()

    init {
        setEGLContextClientVersion(2)
        setRenderer(rendererPublic)
        renderMode = RENDERMODE_WHEN_DIRTY
    }

    fun update(bytes: ByteArray, w: Int, h: Int, rotation: Int) {
        rendererPublic.updateFrame(bytes, w, h, rotation)
        requestRender()
    }
}
