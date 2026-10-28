package com.cockpit.drive

import android.content.Context

/** Réglages enregistrés sur le téléphone : favoris, apps du tableau de bord, position du lecteur. */
class Store(ctx: Context) {
    private val p = ctx.getSharedPreferences("cockpit", Context.MODE_PRIVATE)

    data class Fav(val name: String, val addr: String)

    fun favs(): MutableList<Fav> {
        val raw = p.getString("favs", null) ?: return mutableListOf()
        val out = mutableListOf<Fav>()
        for (line in raw.split("\n")) {
            val i = line.indexOf('|')
            if (i > 0) out.add(Fav(line.substring(0, i), line.substring(i + 1)))
        }
        return out
    }

    fun saveFavs(list: List<Fav>) {
        val raw = list.joinToString("\n") {
            it.name.replace("|", " ").replace("\n", " ") + "|" + it.addr.replace("\n", " ")
        }
        p.edit().putString("favs", raw).apply()
    }

    fun apps(): MutableList<String> {
        val raw = p.getString("apps", null)
            ?: return mutableListOf("com.waze", "com.apple.android.music")
        return raw.split(",").filter { it.isNotBlank() }.toMutableList()
    }

    fun saveApps(list: List<String>) {
        p.edit().putString("apps", list.joinToString(",")).apply()
    }

    var ox: Int
        get() = p.getInt("ox", -1)
        set(v) { p.edit().putInt("ox", v).apply() }

    var oy: Int
        get() = p.getInt("oy", -1)
        set(v) { p.edit().putInt("oy", v).apply() }
}
