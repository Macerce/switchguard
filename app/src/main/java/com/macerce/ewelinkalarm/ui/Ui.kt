package com.macerce.ewelinkalarm.ui

import android.app.Activity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

/** Ekranları XML'siz, kodla kurmak için küçük yardımcılar. */
internal fun Activity.dp(v: Int) = (v * resources.displayMetrics.density).toInt()

internal fun Activity.column(build: LinearLayout.() -> Unit): ScrollView {
    val col = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(16), dp(16), dp(16), dp(16))
        build()
    }
    return ScrollView(this).apply {
        fitsSystemWindows = true
        addView(col)
    }
}

internal fun LinearLayout.label(text: String, sizeSp: Float = 16f): TextView =
    TextView(context).apply {
        this.text = text
        textSize = sizeSp
        setPadding(0, 12, 0, 12)
    }.also { addView(it) }

internal fun LinearLayout.button(text: String, onClick: (View) -> Unit): Button =
    Button(context).apply {
        this.text = text
        isAllCaps = false
        setOnClickListener(onClick)
    }.also { addView(it) }
