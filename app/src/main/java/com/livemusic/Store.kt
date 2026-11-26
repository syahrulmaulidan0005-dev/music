package com.livemusic

import android.graphics.Bitmap
import kotlinx.coroutines.flow.*

data class Track(val title: String, val artist: String, val durationMs: Long, val art: Bitmap?)
data class Comment(val id: Long, val user: String, val text: String)
data class LyricLine(val ms: Long, val text: String)

object Store {
    val track = MutableStateFlow<Track?>(null)
    val playing = MutableStateFlow(false)
    val position = MutableStateFlow(0L)
    val artistUrl = MutableStateFlow<String?>(null)
    val lyrics = MutableStateFlow<List<LyricLine>>(emptyList())
    val comments = MutableStateFlow<List<Comment>>(emptyList())
    val level = MutableStateFlow<Float?>(null)   // null = pakai gelombang simulasi
    val status = MutableStateFlow("")
    private var n = 0L
    fun addComment(user: String, text: String) {
        comments.update { (it + Comment(n++, user, text)).takeLast(30) }
    }
}
