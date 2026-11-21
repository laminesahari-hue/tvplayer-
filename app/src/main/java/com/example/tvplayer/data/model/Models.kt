package com.example.tvplayer.data.model

data class Channel(
    var name: String,
    val url: String,
    var group: String = "",
    var logo: String = "",
    var fav: Boolean = false,
    var tvgId: String = "",
    var src: String = ""
)

data class Vod(
    val id: String,
    var name: String,
    val poster: String,
    val group: String,
    val kind: Int,
    val ext: String,
    val src: String,
    val rating: String = ""
)

data class Episode(
    val id: String,
    val title: String,
    val season: Int,
    val num: Int,
    val ext: String,
    val plot: String
)

data class Playlist(val channels: List<Channel>, val epgUrl: String)

data class Account(val name: String, val server: String, val user: String, val pass: String) {
    fun key(): String = "xt:" + server + "|" + user
    fun label(): String =
        if (name.isBlank()) user + "@" + server.substringAfter("://") else name
}
