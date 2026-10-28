package com.cockpit.drive

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.drawable.Icon
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.text.TextUtils
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView

/**
 * Affiche une petite barre de contrôle musique par-dessus les autres apps (Waze, par exemple).
 * La barre se déplace en la faisant glisser.
 */
class OverlayService : Service() {

    private var wm: WindowManager? = null
    private var bar: View? = null
    private var titleV: TextView? = null
    private var artistV: TextView? = null
    private var coverV: ImageView? = null
    private var playV: ImageView? = null

    private val listener: (MediaHub.State) -> Unit = { s -> bind(s) }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        startForegroundNotification()
        if (bar == null) showBar()
        return START_NOT_STICKY
    }

    private fun startForegroundNotification() {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL, "Lecteur flottant", NotificationManager.IMPORTANCE_LOW)
        )
        val stopIntent = Intent(this, OverlayService::class.java)
        stopIntent.action = ACTION_STOP
        val stop = PendingIntent.getService(this, 0, stopIntent, PendingIntent.FLAG_IMMUTABLE)
        val action = Notification.Action.Builder(
            Icon.createWithResource(this, android.R.drawable.ic_menu_close_clear_cancel),
            "Arrêter",
            stop
        ).build()
        val n = Notification.Builder(this, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle("Cockpit")
            .setContentText("Lecteur flottant actif")
            .setOngoing(true)
            .addAction(action)
            .build()
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(NOTIF_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIF_ID, n)
        }
    }

    private fun showBar() {
        if (!Settings.canDrawOverlays(this)) {
            stopSelf()
            return
        }
        val store = Store(this)
        val dm = resources.displayMetrics
        val w = minOf(dm.widthPixels - dp(24), dp(440))

        val lp = WindowManager.LayoutParams(
            w,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        )
        lp.gravity = Gravity.TOP or Gravity.START
        lp.x = if (store.ox >= 0) store.ox else (dm.widthPixels - w) / 2
        lp.y = if (store.oy >= 0) store.oy else (dm.heightPixels * 0.68f).toInt()

        val root = LinearLayout(this)
        root.gravity = Gravity.CENTER_VERTICAL
        root.setPadding(dp(8), dp(8), dp(8), dp(8))
        root.background = round(Ui.BAR, 22)

        val cover = ImageView(this)
        cover.scaleType = ImageView.ScaleType.CENTER_CROP
        cover.background = round(Ui.SURFACE2, 10)
        cover.clipToOutline = true
        coverV = cover
        root.addView(cover, LinearLayout.LayoutParams(dp(48), dp(48)))

        val texts = LinearLayout(this)
        texts.orientation = LinearLayout.VERTICAL
        val t = label("", 14f, Ui.FG, true)
        t.setSingleLine()
        t.ellipsize = TextUtils.TruncateAt.END
        val a = label("", 12f, Ui.MUTED)
        a.setSingleLine()
        a.ellipsize = TextUtils.TruncateAt.END
        titleV = t
        artistV = a
        texts.addView(t)
        texts.addView(a)
        root.putW(texts, 1f, 10)

        root.addView(button(android.R.drawable.ic_media_previous) { MediaHub.prev() },
            LinearLayout.LayoutParams(dp(44), dp(44)))
        val play = button(android.R.drawable.ic_media_play) { MediaHub.playPause() }
        playV = play
        root.addView(play, LinearLayout.LayoutParams(dp(48), dp(48)))
        root.addView(button(android.R.drawable.ic_media_next) { MediaHub.next() },
            LinearLayout.LayoutParams(dp(44), dp(44)))
        root.addView(button(android.R.drawable.ic_menu_close_clear_cancel) { stopSelf() },
            LinearLayout.LayoutParams(dp(36), dp(36)))

        // Déplacement de la barre au doigt (sur la pochette et le titre).
        var downX = 0f
        var downY = 0f
        var startX = 0
        var startY = 0
        root.setOnTouchListener { _, e ->
            when (e.action) {
                MotionEvent.ACTION_DOWN -> {
                    downX = e.rawX
                    downY = e.rawY
                    startX = lp.x
                    startY = lp.y
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    lp.x = startX + (e.rawX - downX).toInt()
                    lp.y = startY + (e.rawY - downY).toInt()
                    wm?.updateViewLayout(root, lp)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    store.ox = lp.x
                    store.oy = lp.y
                    true
                }
                else -> false
            }
        }

        val manager = getSystemService(WindowManager::class.java)
        wm = manager
        try {
            manager.addView(root, lp)
            bar = root
        } catch (e: Exception) {
            stopSelf()
            return
        }
        MediaHub.subscribe(this, listener)
    }

    private fun button(res: Int, onClick: () -> Unit): ImageView {
        val v = ImageView(this)
        v.setImageResource(res)
        v.setColorFilter(Ui.FG)
        v.setPadding(dp(10), dp(10), dp(10), dp(10))
        v.isClickable = true
        v.setOnClickListener { onClick() }
        return v
    }

    private fun bind(s: MediaHub.State) {
        val hasAccess = MediaHub.hasAccess(this)
        titleV?.text = when {
            !hasAccess -> "Accès à la musique non autorisé"
            s.active && s.title.isNotEmpty() -> s.title
            else -> "Aucune musique"
        }
        artistV?.text = when {
            !hasAccess -> "Ouvre Cockpit > Réglages"
            s.active -> s.artist
            else -> "Lance Apple Musique"
        }
        if (s.art != null) coverV?.setImageBitmap(s.art) else coverV?.setImageDrawable(null)
        playV?.setImageResource(
            if (s.playing) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
        )
    }

    override fun onDestroy() {
        MediaHub.unsubscribe(listener)
        try {
            bar?.let { wm?.removeView(it) }
        } catch (e: Exception) {
            // la vue est déjà retirée
        }
        bar = null
        super.onDestroy()
    }

    companion object {
        private const val CHANNEL = "cockpit_overlay"
        private const val NOTIF_ID = 1
        private const val ACTION_STOP = "com.cockpit.drive.STOP"
    }
}
