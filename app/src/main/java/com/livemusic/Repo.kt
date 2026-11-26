package com.livemusic

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

object Repo {
    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")
    private fun get(url: String): String? = try {
        (URL(url).openConnection() as HttpURLConnection).run {
            connectTimeout = 8000; readTimeout = 8000
            setRequestProperty("User-Agent", "LiveMusic/1.0")
            if (responseCode == 200) inputStream.bufferedReader().readText() else null
        }
    } catch (e: Exception) { null }

    /** Lirik berjalan (format LRC) dari LRCLIB. */
    fun lyrics(title: String, artist: String, durMs: Long): List<LyricLine> {
        val a = artist.split(",", "&").first().trim()
        val exact = get("https://lrclib.net/api/get?track_name=${enc(title)}&artist_name=${enc(a)}&duration=${durMs / 1000}")
        var synced = exact?.let { JSONObject(it).optString("syncedLyrics") }
        if (synced.isNullOrBlank()) {
            val s = get("https://lrclib.net/api/search?track_name=${enc(title)}&artist_name=${enc(a)}")
            synced = s?.let { JSONArray(it) }?.let { arr ->
                (0 until arr.length()).map { arr.getJSONObject(it).optString("syncedLyrics") }.firstOrNull { it.isNotBlank() }
            }
        }
        return parseLrc(synced ?: "")
    }

    private val rx = Regex("""\[(\d+):(\d+)(?:\.(\d+))?](.*)""")
    private fun parseLrc(t: String) = t.lines().mapNotNull { l ->
        rx.find(l.trim())?.let { m ->
            val (mm, ss, fr, tx) = m.destructured
            val ms = mm.toLong() * 60000 + ss.toLong() * 1000 + fr.padEnd(3, '0').take(3).toLong()
            if (tx.isBlank()) null else LyricLine(ms, tx.trim())
        }
    }

    /** Foto artis dari Deezer; null kalau tidak ketemu (UI pakai cover album). */
    fun artistImage(artist: String): String? {
        val a = artist.split(",", "&").first().trim()
        val r = get("https://api.deezer.com/search/artist?q=${enc(a)}") ?: return null
        val d = JSONObject(r).optJSONArray("data") ?: return null
        if (d.length() == 0) return null
        return d.getJSONObject(0).optString("picture_xl").ifBlank { null }
    }
}
