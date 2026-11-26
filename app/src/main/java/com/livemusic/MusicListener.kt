package com.livemusic

import android.content.ComponentName
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.SystemClock
import android.service.notification.NotificationListenerService
import kotlinx.coroutines.*

/** Membaca lagu yang sedang diputar dari aplikasi musik apa pun (Spotify, YT Music, dll). */
class MusicListener : NotificationListenerService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var ticker: Job? = null
    private var controller: MediaController? = null
    private var anchorPos = 0L
    private var anchorTime = 0L
    private var speed = 1f

    private val cb = object : MediaController.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadata?) = meta(metadata)
        override fun onPlaybackStateChanged(state: PlaybackState?) = state(state)
    }

    override fun onListenerConnected() {
        val msm = getSystemService(MediaSessionManager::class.java)
        val me = ComponentName(this, MusicListener::class.java)
        msm.addOnActiveSessionsChangedListener({ pick(it.orEmpty()) }, me)
        pick(msm.getActiveSessions(me))
        ticker?.cancel()
        ticker = scope.launch {
            while (true) {
                delay(300)
                if (Store.playing.value)
                    Store.position.value = anchorPos + ((SystemClock.elapsedRealtime() - anchorTime) * speed).toLong()
            }
        }
    }

    private fun pick(list: List<MediaController>) {
        val c = list.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING } ?: list.firstOrNull()
        if (c?.sessionToken == controller?.sessionToken) return
        controller?.unregisterCallback(cb)
        controller = c
        c?.registerCallback(cb)
        meta(c?.metadata); state(c?.playbackState)
    }

    private fun meta(m: MediaMetadata?) {
        val title = m?.getString(MediaMetadata.METADATA_KEY_TITLE) ?: return
        val artist = m.getString(MediaMetadata.METADATA_KEY_ARTIST)
            ?: m.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST) ?: ""
        val old = Store.track.value
        if (old?.title == title && old.artist == artist) return
        val dur = m.getLong(MediaMetadata.METADATA_KEY_DURATION)
        val art = m.getBitmap(MediaMetadata.METADATA_KEY_ART) ?: m.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
        Store.track.value = Track(title, artist, dur, art)
        Store.lyrics.value = emptyList(); Store.artistUrl.value = null
        scope.launch {
            Store.artistUrl.value = Repo.artistImage(artist)
            Store.lyrics.value = Repo.lyrics(title, artist, dur)
        }
    }

    private fun state(s: PlaybackState?) {
        Store.playing.value = s?.state == PlaybackState.STATE_PLAYING
        anchorPos = s?.position ?: 0L
        anchorTime = s?.lastPositionUpdateTime ?: SystemClock.elapsedRealtime()
        speed = s?.playbackSpeed ?: 1f
        if (!Store.playing.value) Store.position.value = anchorPos
    }

    override fun onDestroy() { scope.cancel(); super.onDestroy() }
}
