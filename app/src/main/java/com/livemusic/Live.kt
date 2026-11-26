package com.livemusic

import android.media.audiofx.Visualizer
import io.github.jwdeveloper.tiktok.TikTokLive
import kotlin.math.sqrt

/** Komentar live TikTok lewat library tidak resmi (cukup username, tanpa login). */
object LiveComments {
    fun connect(username: String) {
        Store.status.value = "Menghubungkan ke @$username..."
        Thread {
            try {
                TikTokLive.newClient(username.removePrefix("@").trim())
                    .onConnected { _, _ -> Store.status.value = "Terhubung ke live @$username" }
                    .onComment { _, e -> Store.addComment(e.user.profileName, e.text) }
                    .onError { _, e -> Store.status.value = "Error: ${e.exception.message}" }
                    .buildAndConnect()
            } catch (t: Throwable) { Store.status.value = "Gagal: ${t.message}" }
        }.start()
    }
}

/** Mencoba menangkap level suara asli. Kalau diblokir sistem, UI memakai gelombang simulasi. */
object AudioTap {
    private var v: Visualizer? = null
    fun start() {
        if (v != null) return
        try {
            val vis = Visualizer(0)
            vis.captureSize = Visualizer.getCaptureSizeRange()[0]
            vis.setDataCaptureListener(object : Visualizer.OnDataCaptureListener {
                override fun onWaveFormDataCapture(p: Visualizer?, w: ByteArray?, r: Int) {
                    if (w == null) return
                    var s = 0.0
                    for (b in w) { val x = (b.toInt() and 0xFF) - 128; s += x * x }
                    Store.level.value = (sqrt(s / w.size) / 40.0).coerceIn(0.0, 1.0).toFloat()
                }
                override fun onFftDataCapture(p: Visualizer?, f: ByteArray?, r: Int) {}
            }, Visualizer.getMaxCaptureRate() / 2, true, false)
            vis.enabled = true
            v = vis
        } catch (t: Throwable) { Store.level.value = null }
    }
}
