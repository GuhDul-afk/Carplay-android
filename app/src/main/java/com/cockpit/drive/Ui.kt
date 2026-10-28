package com.cockpit.drive

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView

/** Palette du tableau de bord (sombre, accent orange). */
object Ui {
    val BG = Color.parseColor("#0E1217")
    val SURFACE = Color.parseColor("#171D25")
    val SURFACE2 = Color.parseColor("#212934")
    val FG = Color.parseColor("#EEF2F6")
    val MUTED = Color.parseColor("#8D99A8")
    val ACCENT = Color.parseColor("#FFA31A")
    val ACCENT_INK = Color.parseColor("#1A1100")
    val LINE = Color.parseColor("#2A3340")
    val BAR = Color.parseColor("#EB171D25")
}

const val MATCH = ViewGroup.LayoutParams.MATCH_PARENT
const val WRAP = ViewGroup.LayoutParams.WRAP_CONTENT

fun Context.dp(v: Int): Int = (v * resources.displayMetrics.density + 0.5f).toInt()

fun Context.round(color: Int, radiusDp: Int, strokeColor: Int? = null): GradientDrawable {
    val d = GradientDrawable()
    d.setColor(color)
    d.cornerRadius = dp(radiusDp).toFloat()
    if (strokeColor != null) d.setStroke(dp(1), strokeColor)
    return d
}

fun Context.label(s: String, sp: Float, color: Int, bold: Boolean = false): TextView {
    val t = TextView(this)
    t.text = s
    t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp)
    t.setTextColor(color)
    if (bold) t.typeface = Typeface.DEFAULT_BOLD
    return t
}

fun Context.pill(s: String, bg: Int, fg: Int, onClick: () -> Unit): TextView {
    val t = label(s, 18f, fg, true)
    t.gravity = Gravity.CENTER
    t.minHeight = dp(56)
    t.setPadding(dp(18), dp(8), dp(18), dp(8))
    t.background = round(bg, 16)
    t.isClickable = true
    t.setOnClickListener { onClick() }
    return t
}

/** Ajoute une vue pleine largeur dans une colonne, avec une marge au-dessus. */
fun LinearLayout.put(v: View, top: Int = 0, left: Int = 0) {
    val lp = LinearLayout.LayoutParams(MATCH, WRAP)
    lp.topMargin = context.dp(top)
    lp.leftMargin = context.dp(left)
    addView(v, lp)
}

/** Ajoute une vue dans une ligne. Poids 0 : largeur du contenu. */
fun LinearLayout.putW(v: View, weight: Float = 1f, left: Int = 0) {
    val lp = LinearLayout.LayoutParams(if (weight > 0f) 0 else WRAP, WRAP, weight)
    lp.leftMargin = context.dp(left)
    addView(v, lp)
}
