package com.cockpit.drive

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextClock
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    private lateinit var store: Store
    private var destInput: EditText? = null
    private var favBox: LinearLayout? = null
    private var appBox: LinearLayout? = null
    private var permBox: LinearLayout? = null
    private var nowCover: ImageView? = null
    private var nowTitle: TextView? = null
    private var nowArtist: TextView? = null
    private var nowPlay: ImageView? = null

    private val mediaListener: (MediaHub.State) -> Unit = { s -> renderMedia(s) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = Store(this)
        window.statusBarColor = Ui.BG
        window.navigationBarColor = Ui.BG
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }
        buildUi()
    }

    override fun onResume() {
        super.onResume()
        MediaHub.subscribe(applicationContext, mediaListener)
        MediaHub.refresh()
        refreshPerms()
    }

    override fun onPause() {
        super.onPause()
        MediaHub.unsubscribe(mediaListener)
    }

    // ---------------------------------------------------------------- mise en page

    private fun buildUi() {
        val landscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        val root: View
        if (landscape) {
            val left = column()
            val right = column()
            left.put(clockView())
            left.put(destView(), 12)
            left.put(driveView(), 12)
            left.put(nowView(), 12)
            right.put(favView())
            right.put(appView(), 20)
            right.put(permView(), 20)
            val h = LinearLayout(this)
            h.setBackgroundColor(Ui.BG)
            h.addView(scroll(left), LinearLayout.LayoutParams(0, MATCH, 1f))
            h.addView(scroll(right), LinearLayout.LayoutParams(0, MATCH, 1f))
            root = h
        } else {
            val c = column()
            c.put(clockView())
            c.put(destView(), 12)
            c.put(driveView(), 12)
            c.put(nowView(), 12)
            c.put(favView(), 20)
            c.put(appView(), 20)
            c.put(permView(), 20)
            root = scroll(c)
        }
        setContentView(root)
        refreshFavs()
        refreshApps()
        refreshPerms()
    }

    private fun column(): LinearLayout {
        val c = LinearLayout(this)
        c.orientation = LinearLayout.VERTICAL
        c.setPadding(dp(16), dp(12), dp(16), dp(24))
        return c
    }

    private fun scroll(v: View): ScrollView {
        val s = ScrollView(this)
        s.setBackgroundColor(Ui.BG)
        s.isFillViewport = true
        s.addView(v)
        return s
    }

    private fun sectionHead(title: String, actionLabel: String?, onAction: () -> Unit): View {
        val head = LinearLayout(this)
        head.gravity = Gravity.CENTER_VERTICAL
        val l = label(title, 13f, Ui.MUTED, true)
        l.letterSpacing = 0.12f
        head.putW(l, 1f)
        if (actionLabel != null) {
            val a = label(actionLabel, 16f, Ui.ACCENT, true)
            a.setPadding(dp(8), dp(10), dp(8), dp(10))
            a.setOnClickListener { onAction() }
            head.putW(a, 0f)
        }
        return head
    }

    // ---------------------------------------------------------------- horloge et destination

    private fun clockView(): View {
        val box = LinearLayout(this)
        box.orientation = LinearLayout.VERTICAL

        val time = TextClock(this)
        time.format24Hour = "HH:mm"
        time.format12Hour = "HH:mm"
        time.setTextSize(TypedValue.COMPLEX_UNIT_SP, 88f)
        time.setTextColor(Ui.FG)
        time.typeface = Typeface.create("sans-serif-condensed", Typeface.BOLD)

        val date = TextClock(this)
        date.format24Hour = "EEEE d MMMM"
        date.format12Hour = "EEEE d MMMM"
        date.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
        date.setTextColor(Ui.MUTED)

        box.put(time)
        box.put(date)
        return box
    }

    private fun destView(): View {
        val row = LinearLayout(this)
        row.gravity = Gravity.CENTER_VERTICAL

        val et = EditText(this)
        et.hint = "Destination"
        et.setHintTextColor(Ui.MUTED)
        et.setTextColor(Ui.FG)
        et.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
        et.setSingleLine()
        et.imeOptions = EditorInfo.IME_ACTION_GO
        et.setPadding(dp(16), dp(14), dp(16), dp(14))
        et.background = round(Ui.SURFACE, 16, Ui.LINE)
        et.setOnEditorActionListener { _, id, _ ->
            if (id == EditorInfo.IME_ACTION_GO) {
                go()
                true
            } else {
                false
            }
        }
        destInput = et

        row.putW(et, 1f)
        row.putW(pill("Go", Ui.ACCENT, Ui.ACCENT_INK) { go() }, 0f, 10)
        return row
    }

    private fun go() {
        val q = destInput?.text?.toString()?.trim().orEmpty()
        if (q.isEmpty()) {
            toast("Saisis une destination")
            return
        }
        Actions.navigate(this, q)
    }

    // ---------------------------------------------------------------- mode conduite

    private fun driveView(): View {
        val row = LinearLayout(this)
        row.putW(pill("Waze + musique", Ui.ACCENT, Ui.ACCENT_INK) { startDrive() }, 2f)
        row.putW(pill("Arrêter", Ui.SURFACE2, Ui.FG) {
            stopService(Intent(this, OverlayService::class.java))
        }, 1f, 10)
        return row
    }

    private fun startDrive() {
        if (!Settings.canDrawOverlays(this)) {
            toast("Autorise l'affichage par-dessus les autres apps, puis reviens ici")
            startActivity(
                Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            )
            return
        }
        startForegroundService(Intent(this, OverlayService::class.java))
        if (!Actions.launch(this, Actions.WAZE)) toast("Waze n'est pas installé")
    }

    // ---------------------------------------------------------------- lecteur

    private fun nowView(): View {
        val card = LinearLayout(this)
        card.orientation = LinearLayout.VERTICAL
        card.setPadding(dp(14), dp(14), dp(14), dp(14))
        card.background = round(Ui.SURFACE, 20, Ui.LINE)
        card.setOnClickListener {
            if (!MediaHub.current().active) {
                if (!Actions.launch(this, Actions.APPLE_MUSIC)) toast("Apple Musique n'est pas installée")
            }
        }

        val top = LinearLayout(this)
        top.gravity = Gravity.CENTER_VERTICAL

        val cover = ImageView(this)
        cover.scaleType = ImageView.ScaleType.CENTER_CROP
        cover.background = round(Ui.SURFACE2, 12)
        cover.clipToOutline = true
        nowCover = cover
        top.addView(cover, LinearLayout.LayoutParams(dp(72), dp(72)))

        val texts = LinearLayout(this)
        texts.orientation = LinearLayout.VERTICAL
        val t = label("Aucune musique", 18f, Ui.FG, true)
        t.setSingleLine()
        t.ellipsize = TextUtils.TruncateAt.END
        val a = label("", 14f, Ui.MUTED)
        a.setSingleLine()
        a.ellipsize = TextUtils.TruncateAt.END
        nowTitle = t
        nowArtist = a
        texts.put(t)
        texts.put(a, 2)
        top.putW(texts, 1f, 14)
        card.put(top)

        val controls = LinearLayout(this)
        controls.gravity = Gravity.CENTER
        val gap = dp(10)
        fun addCtl(v: ImageView) {
            val lp = LinearLayout.LayoutParams(dp(56), dp(56))
            lp.leftMargin = gap
            lp.rightMargin = gap
            controls.addView(v, lp)
        }
        addCtl(control(android.R.drawable.ic_media_previous) { MediaHub.prev() })
        val play = control(android.R.drawable.ic_media_play) { MediaHub.playPause() }
        nowPlay = play
        addCtl(play)
        addCtl(control(android.R.drawable.ic_media_next) { MediaHub.next() })
        card.put(controls, 10)
        return card
    }

    private fun control(res: Int, onClick: () -> Unit): ImageView {
        val v = ImageView(this)
        v.setImageResource(res)
        v.setColorFilter(Ui.FG)
        v.setPadding(dp(14), dp(14), dp(14), dp(14))
        v.background = round(Ui.SURFACE2, 28)
        v.isClickable = true
        v.setOnClickListener { onClick() }
        return v
    }

    private fun renderMedia(s: MediaHub.State) {
        val access = MediaHub.hasAccess(this)
        nowTitle?.text = if (s.active && s.title.isNotEmpty()) s.title else "Aucune musique"
        nowArtist?.text = when {
            !access -> "Autorise l'accès à la musique (réglages plus bas)"
            s.active -> s.artist
            else -> "Touche ici pour ouvrir Apple Musique"
        }
        if (s.art != null) nowCover?.setImageBitmap(s.art) else nowCover?.setImageDrawable(null)
        nowPlay?.setImageResource(
            if (s.playing) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
        )
    }

    // ---------------------------------------------------------------- favoris

    private fun favView(): View {
        val box = LinearLayout(this)
        box.orientation = LinearLayout.VERTICAL
        box.put(sectionHead("FAVORIS", "+ Ajouter") { addFavDialog() })
        val list = LinearLayout(this)
        list.orientation = LinearLayout.VERTICAL
        favBox = list
        box.put(list, 4)
        return box
    }

    private fun refreshFavs() {
        val box = favBox ?: return
        box.removeAllViews()
        val favs = store.favs()
        if (favs.isEmpty()) {
            box.put(label("Aucun favori. Touche « + Ajouter ».", 15f, Ui.MUTED))
            return
        }
        for ((i, f) in favs.withIndex()) {
            val row = LinearLayout(this)
            row.orientation = LinearLayout.VERTICAL
            row.setPadding(dp(16), dp(12), dp(16), dp(12))
            row.background = round(Ui.SURFACE, 14, Ui.LINE)
            row.isClickable = true
            row.setOnClickListener { Actions.navigate(this, f.addr) }
            row.setOnLongClickListener {
                confirmRemoveFav(i, f)
                true
            }
            row.put(label(f.name, 18f, Ui.FG, true))
            val addr = label(f.addr, 14f, Ui.MUTED)
            addr.setSingleLine()
            addr.ellipsize = TextUtils.TruncateAt.END
            row.put(addr)
            box.put(row, 8)
        }
        box.put(label("Appui long sur un favori pour le retirer.", 12f, Ui.MUTED), 8)
    }

    private fun addFavDialog() {
        val box = LinearLayout(this)
        box.orientation = LinearLayout.VERTICAL
        box.setPadding(dp(20), dp(8), dp(20), 0)
        val n = EditText(this)
        n.hint = "Nom (ex. Maison)"
        n.setSingleLine()
        val a = EditText(this)
        a.hint = "Adresse complète"
        a.setSingleLine()
        box.put(n)
        box.put(a, 8)
        AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle("Nouveau favori")
            .setView(box)
            .setPositiveButton("Ajouter") { _, _ ->
                val name = n.text.toString().trim()
                val addr = a.text.toString().trim()
                if (name.isNotEmpty() && addr.isNotEmpty()) {
                    val l = store.favs()
                    l.add(Store.Fav(name, addr))
                    store.saveFavs(l)
                    refreshFavs()
                }
            }
            .setNegativeButton("Annuler", null)
            .show()
    }

    private fun confirmRemoveFav(index: Int, f: Store.Fav) {
        AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle("Retirer « ${f.name} » ?")
            .setPositiveButton("Retirer") { _, _ ->
                val l = store.favs()
                if (index in l.indices) l.removeAt(index)
                store.saveFavs(l)
                refreshFavs()
            }
            .setNegativeButton("Annuler", null)
            .show()
    }

    // ---------------------------------------------------------------- apps

    private fun appView(): View {
        val box = LinearLayout(this)
        box.orientation = LinearLayout.VERTICAL
        box.put(sectionHead("APPLICATIONS", "+ Ajouter") { addAppDialog() })
        val list = LinearLayout(this)
        list.orientation = LinearLayout.VERTICAL
        appBox = list
        box.put(list, 4)
        return box
    }

    private fun refreshApps() {
        val box = appBox ?: return
        box.removeAllViews()
        val pm = packageManager
        val tiles = mutableListOf<View>()
        for (pkg in store.apps()) {
            try {
                val info = pm.getApplicationInfo(pkg, 0)
                tiles.add(tile(pm.getApplicationLabel(info).toString(), pm.getApplicationIcon(info), pkg))
            } catch (e: Exception) {
                // app non installée : on ne l'affiche pas
            }
        }
        if (tiles.isEmpty()) {
            box.put(label("Aucune app. Touche « + Ajouter ».", 15f, Ui.MUTED))
            return
        }
        for (chunk in tiles.chunked(3)) {
            val row = LinearLayout(this)
            for ((i, v) in chunk.withIndex()) {
                val lp = LinearLayout.LayoutParams(0, WRAP, 1f)
                if (i > 0) lp.leftMargin = dp(10)
                row.addView(v, lp)
            }
            for (k in chunk.size until 3) {
                val lp = LinearLayout.LayoutParams(0, 1, 1f)
                lp.leftMargin = dp(10)
                row.addView(View(this), lp)
            }
            box.put(row, 10)
        }
        box.put(label("Appui long sur une app pour la retirer.", 12f, Ui.MUTED), 8)
    }

    private fun tile(name: String, icon: Drawable, pkg: String): View {
        val t = LinearLayout(this)
        t.orientation = LinearLayout.VERTICAL
        t.gravity = Gravity.CENTER
        t.setPadding(dp(6), dp(16), dp(6), dp(14))
        t.background = round(Ui.SURFACE, 18, Ui.LINE)
        t.isClickable = true
        t.setOnClickListener { if (!Actions.launch(this, pkg)) toast("App introuvable") }
        t.setOnLongClickListener {
            confirmRemoveApp(pkg, name)
            true
        }
        val iv = ImageView(this)
        iv.setImageDrawable(icon)
        t.addView(iv, LinearLayout.LayoutParams(dp(52), dp(52)))
        val l = label(name, 13f, Ui.FG)
        l.gravity = Gravity.CENTER
        l.setSingleLine()
        l.ellipsize = TextUtils.TruncateAt.END
        t.put(l, 8)
        return t
    }

    private fun addAppDialog() {
        val pm = packageManager
        val query = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val found = pm.queryIntentActivities(query, 0)
            .map { it.activityInfo.packageName to it.loadLabel(pm).toString() }
            .distinctBy { it.first }
            .filter { it.first != packageName }
            .sortedBy { it.second.lowercase() }
        if (found.isEmpty()) {
            toast("Aucune app trouvée")
            return
        }
        AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle("Ajouter une app")
            .setItems(found.map { it.second }.toTypedArray()) { _, which ->
                val l = store.apps()
                val pkg = found[which].first
                if (!l.contains(pkg)) l.add(pkg)
                store.saveApps(l)
                refreshApps()
            }
            .setNegativeButton("Annuler", null)
            .show()
    }

    private fun confirmRemoveApp(pkg: String, name: String) {
        AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle("Retirer « $name » ?")
            .setPositiveButton("Retirer") { _, _ ->
                val l = store.apps()
                l.remove(pkg)
                store.saveApps(l)
                refreshApps()
            }
            .setNegativeButton("Annuler", null)
            .show()
    }

    // ---------------------------------------------------------------- réglages

    private fun permView(): View {
        val box = LinearLayout(this)
        box.orientation = LinearLayout.VERTICAL
        box.put(sectionHead("RÉGLAGES", null) {})
        val list = LinearLayout(this)
        list.orientation = LinearLayout.VERTICAL
        permBox = list
        box.put(list, 4)
        return box
    }

    private fun refreshPerms() {
        val box = permBox ?: return
        box.removeAllViews()
        box.put(
            permRow("Lecteur flottant (affichage par-dessus Waze)", Settings.canDrawOverlays(this)) {
                startActivity(
                    Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
                )
            }
        )
        box.put(
            permRow("Contrôle de la musique (accès aux notifications)", MediaHub.hasAccess(this)) {
                startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            },
            8
        )
        box.put(
            label(
                "Si l'accès aux notifications est grisé : Réglages > Applications > Cockpit > " +
                    "menu ⋮ > Autoriser les paramètres restreints, puis recommence.",
                13f, Ui.MUTED
            ),
            10
        )
    }

    private fun permRow(title: String, ok: Boolean, onFix: () -> Unit): View {
        val row = LinearLayout(this)
        row.gravity = Gravity.CENTER_VERTICAL
        row.setPadding(dp(14), dp(10), dp(10), dp(10))
        row.background = round(Ui.SURFACE, 14, Ui.LINE)
        row.putW(label((if (ok) "✓  " else "✗  ") + title, 15f, if (ok) Ui.FG else Ui.MUTED), 1f)
        if (!ok) {
            val b = pill("Autoriser", Ui.ACCENT, Ui.ACCENT_INK, onFix)
            b.minHeight = dp(44)
            b.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            row.putW(b, 0f, 8)
        }
        return row
    }

    private fun toast(s: String) {
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show()
    }
}
