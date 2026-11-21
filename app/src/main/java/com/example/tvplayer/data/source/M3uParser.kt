package com.example.tvplayer.data.source

import com.example.tvplayer.data.model.Channel
import com.example.tvplayer.data.model.Playlist

private val attrRegex = Regex("([a-zA-Z0-9_-]+)=\"([^\"]*)\"")

fun parseM3u(text: String): Playlist {
    val out = ArrayList<Channel>()
    var epg = ""
    var name = ""
    var group = ""
    var logo = ""
    var tvgId = ""
    for (raw in text.lines()) {
        val line = raw.trim()
        if (line.startsWith("#EXTM3U")) {
            for (m in attrRegex.findAll(line)) {
                val k = m.groupValues[1].lowercase()
                if (k == "url-tvg" || k == "x-tvg-url" || k == "tvg-url") {
                    epg = m.groupValues[2].substringBefore(",").trim()
                }
            }
        } else if (line.startsWith("#EXTINF")) {
            name = line.substringAfterLast(",", "").trim()
            group = ""
            logo = ""
            tvgId = ""
            var tvgName = ""
            for (m in attrRegex.findAll(line)) {
                when (m.groupValues[1].lowercase()) {
                    "group-title" -> group = m.groupValues[2].trim()
                    "tvg-logo" -> logo = m.groupValues[2].trim()
                    "tvg-id" -> tvgId = m.groupValues[2].trim()
                    "tvg-name" -> tvgName = m.groupValues[2].trim()
                }
            }
            if (name.isEmpty()) name = tvgName
        } else if (line.startsWith("#EXTGRP:")) {
            if (group.isEmpty()) group = line.substringAfter(":").trim()
        } else if (line.isNotEmpty() && !line.startsWith("#")) {
            val url = line.substringBefore("|").trim()
            out.add(Channel(if (name.isEmpty()) url else name, url, group, logo, false, tvgId))
            name = ""
            group = ""
            logo = ""
            tvgId = ""
        }
    }
    return Playlist(out, epg)
}
