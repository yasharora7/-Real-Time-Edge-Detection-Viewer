package com.example.edgeviewer

import android.content.Context
import android.opengl.GLSurfaceView
import android.util.AttributeSet

/**
 * GLSurface wrapper that exposes rendererPublic and ensures requestRender() is called
 * whenever the Activity pushes a frame.
 */
class GLSurface @JvmOverloads constructor(
    ctx: Context,
    attrs: AttributeSet? = null
) : GLSurfaceView(ctx, attrs) {

    val rendererPublic = GLRenderer()

    init {
        setEGLContextClientVersion(2)
        setRenderer(rendererPublic)
        // Render only when requested (we'll requestRender after each new frame)
        renderMode = RENDERMODE_WHEN_DIRTY
    }

    fun update(bytes: ByteArray, w: Int, h: Int, rotation: Int) {
        rendererPublic.updateFrame(bytes, w, h, rotation)
        // ensure drawing happens on the GL thread as soon as possible
        requestRender()
    }
}
