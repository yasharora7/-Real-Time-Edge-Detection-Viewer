package com.example.edgeviewer

object NativeEdge {

    init {
        System.loadLibrary("native-lib")
    }

    external fun processFrame(
        input: ByteArray,
        width: Int,
        height: Int
    ): ByteArray
}
