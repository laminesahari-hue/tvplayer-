package com.example.tvplayer.portal

import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.net.URLDecoder

fun portalLocalIps(): List<String> {
    val out = ArrayList<String>()
    try {
        val ifs = NetworkInterface.getNetworkInterfaces()
        while (ifs.hasMoreElements()) {
            val ni = ifs.nextElement()
            if (!ni.isUp || ni.isLoopback) continue
            val addrs = ni.inetAddresses
            while (addrs.hasMoreElements()) {
                val a = addrs.nextElement()
                if (a is Inet4Address && !a.isLoopbackAddress) {
                    val h = a.hostAddress
                    if (h != null) out.add(h)
                }
            }
        }
    } catch (e: Exception) {
    }
    return out
}

class PortalServer(
    private val pin: String,
    private val pageHtml: String,
    private val statusLines: () -> List<String>,
    private val presets: List<Pair<String, String>>,
    private val action: (String, Map<String, String>) -> String
) {
    @Volatile
    private var running = false
    private var server: ServerSocket? = null
    private var portValue = 8080
    private val crlf = String(charArrayOf(Char(13), Char(10)))

    fun port(): Int = portValue

    fun start(): Boolean {
        if (running) return true
        var p = 8080
        var s: ServerSocket? = null
        while (p < 8100 && s == null) {
            try {
                s = ServerSocket(p)
            } catch (e: Exception) {
                p++
            }
        }
        val sock = s ?: return false
        server = sock
        portValue = p
        running = true
        val loop = Runnable { acceptLoop(sock) }
        Thread(loop).start()
        return true
    }

    fun stop() {
        running = false
        try {
            server?.close()
        } catch (e: Exception) {
        }
        server = null
    }

    private fun acceptLoop(sock: ServerSocket) {
        while (running) {
            try {
                val c = sock.accept()
                val job = Runnable { handle(c) }
                Thread(job).start()
            } catch (e: Exception) {
                if (!running) return
            }
        }
    }

    private fun readLine(ins: InputStream): String? {
        val bo = ByteArrayOutputStream()
        while (true) {
            val b = ins.read()
            if (b < 0) {
                if (bo.size() == 0) return null
                break
            }
            if (b == 10) break
            if (b != 13) bo.write(b)
            if (bo.size() > 16384) break
        }
        return String(bo.toByteArray(), Charsets.UTF_8)
    }

    private fun parseForm(s: String): Map<String, String> {
        val m = HashMap<String, String>()
        if (s.isEmpty()) return m
        for (pair in s.split("&")) {
            if (pair.isEmpty()) continue
            val i = pair.indexOf("=")
            val k = if (i >= 0) pair.substring(0, i) else pair
            val v = if (i >= 0) pair.substring(i + 1) else ""
            try {
                m[URLDecoder.decode(k, "UTF-8")] = URLDecoder.decode(v, "UTF-8")
            } catch (e: Exception) {
            }
        }
        return m
    }

    private fun send(out: OutputStream, status: String, type: String, body: String) {
        val bytes = body.toByteArray(Charsets.UTF_8)
        val sb = StringBuilder()
        sb.append("HTTP/1.1 ").append(status).append(crlf)
        sb.append("Content-Type: ").append(type).append(crlf)
        sb.append("Content-Length: ").append(bytes.size).append(crlf)
        sb.append("Connection: close").append(crlf)
        sb.append("Cache-Control: no-store").append(crlf)
        sb.append(crlf)
        out.write(sb.toString().toByteArray(Charsets.UTF_8))
        out.write(bytes)
        out.flush()
    }

    private fun handle(c: Socket) {
        try {
            c.soTimeout = 15000
            val ins = BufferedInputStream(c.getInputStream())
            val out = c.getOutputStream()
            val reqLine = readLine(ins)
            if (reqLine == null) return
            val parts = reqLine.split(" ")
            if (parts.size < 2) return
            val method = parts[0]
            val target = parts[1]
            var len = 0
            while (true) {
                val h = readLine(ins)
                if (h == null || h.isEmpty()) break
                val i = h.indexOf(":")
                if (i > 0 && h.substring(0, i).trim().equals("content-length", true)) {
                    len = h.substring(i + 1).trim().toIntOrNull() ?: 0
                }
            }
            val body = ByteArrayOutputStream()
            var remaining = minOf(len, 2000000)
            val buf = ByteArray(8192)
            while (remaining > 0) {
                val n = ins.read(buf, 0, minOf(buf.size, remaining))
                if (n < 0) break
                body.write(buf, 0, n)
                remaining -= n
            }
            val path = target.substringBefore("?")
            val params = HashMap<String, String>()
            params.putAll(parseForm(target.substringAfter("?", "")))
            if (method == "POST") {
                params.putAll(parseForm(String(body.toByteArray(), Charsets.UTF_8)))
            }
            val authed = params["pin"] == pin
            val plain = "text/plain; charset=utf-8"
            if (path == "/" || path == "/index.html") {
                send(out, "200 OK", "text/html; charset=utf-8", pageHtml)
            } else if (path == "/api/status") {
                if (authed) {
                    send(out, "200 OK", plain, statusLines().joinToString("\n"))
                } else {
                    send(out, "401 Unauthorized", plain, "pin")
                }
            } else if (path == "/api/presets") {
                if (authed) {
                    send(out, "200 OK", plain, presets.joinToString("\n") { it.first + "|" + it.second })
                } else {
                    send(out, "401 Unauthorized", plain, "pin")
                }
            } else if (path.startsWith("/api/do/") && method == "POST") {
                if (authed) {
                    send(out, "200 OK", plain, action(path.removePrefix("/api/do/"), params))
                } else {
                    send(out, "401 Unauthorized", plain, "pin")
                }
            } else {
                send(out, "404 Not Found", plain, "Not found")
            }
        } catch (e: Exception) {
        } finally {
            try {
                c.close()
            } catch (e: Exception) {
            }
        }
    }
}
