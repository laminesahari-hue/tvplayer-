package com.example.tvplayer.pvr

import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class Sched(val start: Long, val min: Int, val url: String, val name: String)

object RecorderHolder {
    val recorder = Recorder()
}

class Recorder {
    @Volatile
    var active = false

    @Volatile
    var bytes = 0L

    @Volatile
    var startMs = 0L

    @Volatile
    var title = ""

    @Volatile
    private var conn: HttpURLConnection? = null
    private var ua = "Mozilla/5.0"
    private var maxMs = Long.MAX_VALUE

    fun start(name: String, url: String, dir: File, limitMs: Long, agent: String, onDone: (File?, String) -> Unit): Boolean {
        if (active) return false
        ua = agent
        maxMs = limitMs
        val safe = name.replace(Regex("[^A-Za-z0-9._-]+"), "_").trim('_').take(40).ifEmpty { "rec" }
        val stamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
        val f = File(dir, safe + "_" + stamp + ".ts")
        bytes = 0L
        startMs = System.currentTimeMillis()
        title = name
        active = true
        Thread {
            var err = ""
            try {
                FileOutputStream(f).use { out -> record(url, out) }
            } catch (e: Throwable) {
                err = e.message ?: "error"
            }
            active = false
            val size = f.length()
            if (size < 2048L) {
                f.delete()
                onDone(null, err)
            } else {
                onDone(f, err)
            }
        }.start()
        return true
    }

    fun stop() {
        active = false
        try {
            conn?.disconnect()
        } catch (e: Throwable) {
        }
    }

    private fun timeUp(): Boolean = System.currentTimeMillis() - startMs >= maxMs

    private fun open(url: String): HttpURLConnection {
        val c = URL(url).openConnection() as HttpURLConnection
        c.connectTimeout = 15000
        c.readTimeout = 20000
        c.instanceFollowRedirects = true
        c.setRequestProperty("User-Agent", ua)
        conn = c
        if (c.responseCode >= 400) throw Exception("HTTP " + c.responseCode)
        return c
    }

    private fun getText(url: String): String {
        val c = open(url)
        return c.inputStream.bufferedReader().use { it.readText() }
    }

    private fun resolve(base: String, ref: String): String = URL(URL(base), ref).toString()

    private fun record(url: String, out: OutputStream) {
        val c = open(url)
        val ins = BufferedInputStream(c.inputStream, 65536)
        ins.mark(16)
        val head = ByteArray(7)
        val n = ins.read(head)
        ins.reset()
        val isHls = n == 7 && String(head, Charsets.US_ASCII) == "#EXTM3U"
        if (isHls) {
            val text = ins.bufferedReader().use { it.readText() }
            recordHls(c.url.toString(), text, out)
        } else {
            val buf = ByteArray(65536)
            while (active && !timeUp()) {
                val r = ins.read(buf)
                if (r < 0) break
                out.write(buf, 0, r)
                bytes += r
            }
            ins.close()
        }
    }

    private fun recordHls(firstUrl: String, firstText: String, out: OutputStream) {
        var mediaUrl = firstUrl
        var text = firstText
        if (text.contains("#EXT-X-STREAM-INF")) {
            var best = -1L
            var bestUri = ""
            val lines = text.lines()
            for (i in lines.indices) {
                val l = lines[i].trim()
                if (l.startsWith("#EXT-X-STREAM-INF")) {
                    val bw = Regex("BANDWIDTH=(\\d+)").find(l)?.groupValues?.get(1)?.toLongOrNull() ?: 0L
                    var j = i + 1
                    while (j < lines.size && (lines[j].isBlank() || lines[j].startsWith("#"))) j++
                    if (j < lines.size && bw > best) {
                        best = bw
                        bestUri = lines[j].trim()
                    }
                }
            }
            if (bestUri.isEmpty()) throw Exception("playlist")
            mediaUrl = resolve(mediaUrl, bestUri)
            text = getText(mediaUrl)
        }
        if (text.contains("#EXT-X-MAP")) throw Exception("fMP4 (utilisez le flux .ts)")
        val seen = HashSet<String>()
        var idle = 0
        while (active && !timeUp()) {
            var added = 0
            var target = 6
            for (raw in text.lines()) {
                val l = raw.trim()
                if (l.startsWith("#EXT-X-TARGETDURATION:")) {
                    target = l.substringAfter(":").trim().toIntOrNull() ?: 6
                }
            }
            for (raw in text.lines()) {
                if (!active || timeUp()) break
                val l = raw.trim()
                if (l.isEmpty() || l.startsWith("#")) continue
                val seg = resolve(mediaUrl, l)
                if (seen.add(seg)) {
                    try {
                        copySegment(seg, out)
                        added++
                    } catch (e: Exception) {
                        if (!active) break
                    }
                }
            }
            if (text.contains("#EXT-X-ENDLIST")) break
            idle = if (added == 0) idle + 1 else 0
            if (idle > 30) break
            var waited = 0L
            val waitMs = maxOf(1000L, target * 500L)
            while (active && waited < waitMs) {
                Thread.sleep(250)
                waited += 250
            }
            if (!active) break
            try {
                text = getText(mediaUrl)
            } catch (e: Exception) {
                idle++
            }
        }
    }

    private fun copySegment(url: String, out: OutputStream) {
        val c = open(url)
        c.inputStream.use { ins ->
            val buf = ByteArray(65536)
            while (active) {
                val r = ins.read(buf)
                if (r < 0) break
                out.write(buf, 0, r)
                bytes += r
            }
        }
    }
}
