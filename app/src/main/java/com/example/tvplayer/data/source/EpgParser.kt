package com.example.tvplayer.data.source

import android.util.Xml
import java.io.BufferedInputStream
import java.io.InputStream
import java.text.Normalizer
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.zip.GZIPInputStream
import org.xmlpull.v1.XmlPullParser

data class Programme(val start: Long, val stop: Long, val title: String, val desc: String = "")

data class EpgResult(val data: Map<String, List<Programme>>, val nameToId: Map<String, String>)

object Epg {
    @Volatile
    var data: Map<String, List<Programme>> = emptyMap()

    private val reMarks = Regex("\\p{M}+")
    private val reBrackets = Regex("\\[[^\\]]*\\]|\\([^)]*\\)")
    private val reTags = Regex("\\b(fhd|uhd|hd|sd|4k|8k|hevc|h265|h264|raw|vip|backup|multi)\\b")
    private val reLead = Regex("^[^a-z0-9]+")
    private val rePrefix = Regex("^(fr|france|be|ch|ca|uk|us|de|es|it|ar|ma|dz|tn|nl|pt)\\s*[:|\\-]+\\s*")
    private val reClean = Regex("[^a-z0-9+]+")

    fun norm(s: String): String {
        var t = Normalizer.normalize(s, Normalizer.Form.NFD)
        t = reMarks.replace(t, "")
        t = t.lowercase()
        t = reBrackets.replace(t, " ")
        t = reTags.replace(t, " ")
        t = reLead.replace(t.trim(), "")
        t = rePrefix.replace(t, "")
        t = reClean.replace(t, "")
        return t
    }

    fun now(id: String): Programme? {
        if (id.isBlank()) return null
        val l = data[id.lowercase()] ?: return null
        val t = System.currentTimeMillis()
        return l.firstOrNull { it.start <= t && t < it.stop }
    }

    fun next(id: String): Programme? {
        if (id.isBlank()) return null
        val l = data[id.lowercase()] ?: return null
        val t = System.currentTimeMillis()
        val cur = l.firstOrNull { it.start <= t && t < it.stop }
        val from = if (cur != null) cur.stop else t
        return l.firstOrNull { it.start >= from }
    }

    fun list(id: String): List<Programme> {
        if (id.isBlank()) return emptyList()
        val l = data[id.lowercase()] ?: return emptyList()
        val t = System.currentTimeMillis()
        return l.filter { it.stop > t }
    }

    private fun parseTime(s: String?, f1: SimpleDateFormat, f2: SimpleDateFormat): Long {
        if (s == null) return 0L
        val t = s.trim()
        return try {
            if (t.length > 14) {
                val fixed = t.substring(0, 14) + " " + t.substring(14).trim()
                val d = f1.parse(fixed)
                if (d != null) d.time else 0L
            } else {
                val d = f2.parse(t)
                if (d != null) d.time else 0L
            }
        } catch (e: Exception) {
            0L
        }
    }

    fun parse(raw: InputStream, wanted: Set<String>, wantedNames: Set<String>): EpgResult {
        val buf = BufferedInputStream(raw, 65536)
        buf.mark(2)
        val b1 = buf.read()
        val b2 = buf.read()
        buf.reset()
        val stream: InputStream = if (b1 == 0x1f && b2 == 0x8b) GZIPInputStream(buf, 65536) else buf

        val parser = Xml.newPullParser()
        parser.setInput(stream, null)

        val f1 = SimpleDateFormat("yyyyMMddHHmmss Z", Locale.US)
        val f2 = SimpleDateFormat("yyyyMMddHHmmss", Locale.US)
        f2.timeZone = TimeZone.getTimeZone("UTC")

        val out = HashMap<String, MutableList<Programme>>()
        val accept = HashSet<String>(wanted)
        val nameToId = HashMap<String, String>()
        val now = System.currentTimeMillis()
        val minT = now - 4 * 3600000L
        val maxT = now + 30 * 3600000L

        var inChannel = false
        var chId = ""
        var inProg = false
        var ch = ""
        var start = 0L
        var stop = 0L
        var title = ""
        var desc = ""

        var ev = parser.eventType
        while (ev != XmlPullParser.END_DOCUMENT) {
            if (ev == XmlPullParser.START_TAG) {
                val n = parser.name
                if (n == "channel") {
                    inChannel = true
                    chId = (parser.getAttributeValue(null, "id") ?: "").lowercase()
                } else if (n == "display-name" && inChannel) {
                    val dn = parser.nextText().trim()
                    if (wantedNames.isNotEmpty() && chId.isNotEmpty() && !wanted.contains(chId)) {
                        val k = norm(dn)
                        if (k.isNotEmpty() && wantedNames.contains(k) && !nameToId.containsKey(k)) {
                            nameToId[k] = chId
                            accept.add(chId)
                        }
                    }
                } else if (n == "programme") {
                    inProg = true
                    ch = (parser.getAttributeValue(null, "channel") ?: "").lowercase()
                    start = parseTime(parser.getAttributeValue(null, "start"), f1, f2)
                    stop = parseTime(parser.getAttributeValue(null, "stop"), f1, f2)
                    title = ""
                    desc = ""
                } else if (n == "title" && inProg && title.isEmpty()) {
                    title = parser.nextText().trim()
                } else if (n == "desc" && inProg && desc.isEmpty()) {
                    desc = parser.nextText().trim().take(300)
                }
            } else if (ev == XmlPullParser.END_TAG) {
                if (parser.name == "channel") {
                    inChannel = false
                } else if (parser.name == "programme") {
                    if (inProg && accept.contains(ch) && stop > minT && start < maxT) {
                        out.getOrPut(ch) { mutableListOf() }.add(Programme(start, stop, title, desc))
                    }
                    inProg = false
                }
            }
            ev = parser.next()
        }
        for (l in out.values) l.sortBy { it.start }
        return EpgResult(out, nameToId)
    }
}
