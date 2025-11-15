package com.example.edgeviewer

import java.io.BufferedWriter
import java.io.OutputStreamWriter
import java.net.ServerSocket
import java.net.Socket
import kotlin.concurrent.thread

class WebServer(private val port: Int, private val statusProvider: () -> String) {

    @Volatile private var running = false
    private var serverThread: Thread? = null
    private var serverSocket: ServerSocket? = null

    fun start() {
        if (running) return
        running = true
        serverThread = thread(start = true, name = "WebServer") {
            try {
                serverSocket = ServerSocket(port)
                while (running) {
                    val client = serverSocket!!.accept()
                    handleClient(client)
                }
            } catch (e: Exception) {
                // ignore / log
            } finally {
                stopServer()
            }
        }
    }

    private fun handleClient(socket: Socket) {
        thread {
            socket.use { s ->
                val inStream = s.getInputStream()
                val request = ByteArray(1024)
                inStream.read(request) // read request (we don't parse deeply)

                val body = statusProvider()
                val response = ("HTTP/1.1 200 OK\r\n" +
                        "Content-Type: application/json\r\n" +
                        "Access-Control-Allow-Origin: *\r\n" +
                        "Content-Length: ${body.toByteArray().size}\r\n" +
                        "\r\n" +
                        body)

                val out = BufferedWriter(OutputStreamWriter(s.getOutputStream()))
                out.write(response)
                out.flush()
            }
        }
    }

    fun stopServer() {
        running = false
        try { serverSocket?.close() } catch (_: Exception) {}
        serverThread?.interrupt()
    }
}
