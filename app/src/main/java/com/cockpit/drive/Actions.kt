package com.cockpit.drive

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

object Actions {
    const val WAZE = "com.waze"
    const val APPLE_MUSIC = "com.apple.android.music"

    /** Lance la navigation Waze vers une adresse. */
    fun navigate(ctx: Context, query: String) {
        val q = Uri.encode(query)
        val urls = listOf(
            "waze://?q=$q&navigate=yes",
            "https://waze.com/ul?q=$q&navigate=yes"
        )
        for (u in urls) {
            try {
                val i = Intent(Intent.ACTION_VIEW, Uri.parse(u))
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                ctx.startActivity(i)
                return
            } catch (e: ActivityNotFoundException) {
                // on essaie l'adresse suivante
            }
        }
        Toast.makeText(ctx, "Impossible d'ouvrir Waze", Toast.LENGTH_SHORT).show()
    }

    /** Ouvre une app installée à partir de son nom de paquet. */
    fun launch(ctx: Context, pkg: String): Boolean {
        val i = ctx.packageManager.getLaunchIntentForPackage(pkg) ?: return false
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            ctx.startActivity(i)
            true
        } catch (e: Exception) {
            false
        }
    }
}
