package com.cockpit.drive

import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.provider.Settings

/**
 * Lit la musique en cours (Apple Musique en priorité) et envoie les commandes
 * lecture / pause / suivant / précédent à l'app qui la joue.
 */
object MediaHub {

    data class State(
        val title: String,
        val artist: String,
        val art: Bitmap?,
        val playing: Boolean,
        val active: Boolean
    )

    private val listeners = LinkedHashSet<(State) -> Unit>()
    private var appCtx: Context? = null
    private var manager: MediaSessionManager? = null
    private var component: ComponentName? = null
    private var controller: MediaController? = null

    private val sessionsListener =
        MediaSessionManager.OnActiveSessionsChangedListener { list -> pick(list) }

    private val callback = object : MediaController.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackState?) {
            publish()
        }

        override fun onMetadataChanged(metadata: MediaMetadata?) {
            publish()
        }

        override fun onSessionDestroyed() {
            refresh()
        }
    }

    /** Vrai si l'utilisateur a autorisé Cockpit dans "Accès aux notifications". */
    fun hasAccess(ctx: Context): Boolean {
        val flat = Settings.Secure.getString(ctx.contentResolver, "enabled_notification_listeners")
            ?: return false
        for (s in flat.split(":")) {
            val c = ComponentName.unflattenFromString(s)
            if (c != null && c.packageName == ctx.packageName) return true
        }
        return false
    }

    fun subscribe(ctx: Context, l: (State) -> Unit) {
        appCtx = ctx.applicationContext
        listeners.add(l)
        if (manager == null) attach()
        l(current())
    }

    fun unsubscribe(l: (State) -> Unit) {
        listeners.remove(l)
        if (listeners.isEmpty()) detach()
    }

    /** À appeler quand l'autorisation a pu changer. */
    fun refresh() {
        if (listeners.isEmpty()) return
        val m = manager
        val c = component
        if (m == null || c == null) {
            attach()
        } else {
            try {
                pick(m.getActiveSessions(c))
            } catch (e: SecurityException) {
                detach()
            }
        }
    }

    private fun attach() {
        val ctx = appCtx ?: return
        if (!hasAccess(ctx)) return
        try {
            val m = ctx.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
            val c = ComponentName(ctx, MediaListener::class.java)
            manager = m
            component = c
            m.addOnActiveSessionsChangedListener(sessionsListener, c)
            pick(m.getActiveSessions(c))
        } catch (e: SecurityException) {
            manager = null
            component = null
        }
    }

    private fun detach() {
        try {
            manager?.removeOnActiveSessionsChangedListener(sessionsListener)
        } catch (e: Exception) {
            // rien à faire
        }
        controller?.unregisterCallback(callback)
        controller = null
        manager = null
        component = null
    }

    private fun pick(list: List<MediaController>?) {
        val l = list ?: emptyList()
        var next: MediaController? = null
        for (c in l) if (c.packageName == Actions.APPLE_MUSIC) next = c
        if (next == null) {
            for (c in l) if (c.playbackState?.state == PlaybackState.STATE_PLAYING) {
                next = c
                break
            }
        }
        if (next == null && l.isNotEmpty()) next = l[0]

        if (next?.sessionToken != controller?.sessionToken) {
            controller?.unregisterCallback(callback)
            controller = next
            controller?.registerCallback(callback)
        }
        publish()
    }

    fun current(): State {
        val c = controller ?: return State("", "", null, false, false)
        val md = c.metadata
        val title = md?.getString(MediaMetadata.METADATA_KEY_TITLE) ?: ""
        val artist = md?.getString(MediaMetadata.METADATA_KEY_ARTIST)
            ?: md?.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)
            ?: ""
        val art = md?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: md?.getBitmap(MediaMetadata.METADATA_KEY_ART)
            ?: md?.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON)
        val playing = c.playbackState?.state == PlaybackState.STATE_PLAYING
        return State(title, artist, art, playing, true)
    }

    private fun publish() {
        val s = current()
        for (l in listeners.toList()) l(s)
    }

    fun playPause() {
        val c = controller ?: return
        if (c.playbackState?.state == PlaybackState.STATE_PLAYING) {
            c.transportControls.pause()
        } else {
            c.transportControls.play()
        }
    }

    fun next() {
        controller?.transportControls?.skipToNext()
    }

    fun prev() {
        controller?.transportControls?.skipToPrevious()
    }
}
