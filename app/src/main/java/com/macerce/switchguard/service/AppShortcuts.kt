package com.macerce.switchguard.service

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.macerce.switchguard.R
import com.macerce.switchguard.core.AppLanguage
import com.macerce.switchguard.data.Store
import com.macerce.switchguard.ui.Actions
import kotlinx.coroutines.runBlocking

/**
 * Uygulama simgesine basılı tutunca çıkan kısayollar: Evdeyim/Dışarıdayım (şu anki durumun tersi) ve ilk sahneler.
 * Uygulama açılıp kapanınca ve evde/dışarıda değişince tazelenir; hesap yoksa kaldırılır.
 */
object AppShortcuts {
    private const val PRESENCE = "presence"
    private const val SCENE = "scene:"
    private const val EXTRA = "shortcut"

    fun update(context: Context) {
        runCatching {
            val store = Store.get(context)
            if (!store.hasAnyAccount) {
                ShortcutManagerCompat.removeAllDynamicShortcuts(context)
                return
            }
            val res = AppLanguage.wrap(context)
            val goAway = !store.awayMode
            val presenceLabel = res.getString(if (goAway) R.string.presence_away else R.string.presence_home)
            val presence = ShortcutInfoCompat.Builder(context, PRESENCE)
                .setShortLabel(presenceLabel)
                .setIcon(IconCompat.createWithResource(context, if (goAway) R.drawable.ic_shortcut_away else R.drawable.ic_shortcut_home))
                .setIntent(intent(context, PRESENCE))
                .build()
            val max = ShortcutManagerCompat.getMaxShortcutCountPerActivity(context).coerceIn(1, 4)
            val scenes = store.scenes.take(max - 1).map { s ->
                ShortcutInfoCompat.Builder(context, SCENE + s.id)
                    .setShortLabel(s.name)
                    .setIcon(IconCompat.createWithResource(context, R.drawable.ic_shortcut_scene))
                    .setIntent(intent(context, SCENE + s.id))
                    .build()
            }
            ShortcutManagerCompat.setDynamicShortcuts(context, listOf(presence) + scenes)
        }
    }

    private fun intent(context: Context, target: String) =
        Intent(context, ShortcutActivity::class.java).setAction(Intent.ACTION_VIEW).putExtra(EXTRA, target)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)

    /** Kısayolu arka planda çalıştırır, sonucu kısa bir mesajla gösterir. */
    internal fun run(context: Context, target: String) {
        val app = context.applicationContext
        val res = AppLanguage.wrap(app)
        val by = res.getString(R.string.shortcut_source)
        // Başlatıcı sık kullanılanı öne alsın.
        runCatching { ShortcutManagerCompat.reportShortcutUsed(app, target) }
        Thread {
            val store = Store.get(app)
            val message = when {
                target == PRESENCE -> {
                    val away = !store.awayMode
                    Actions.setAway(app, away, by = by)
                    res.getString(if (away) R.string.presence_away else R.string.presence_home)
                }
                target.startsWith(SCENE) -> {
                    val scene = store.scenes.firstOrNull { it.id == target.removePrefix(SCENE) }
                    if (scene == null) res.getString(R.string.device_missing)
                    else {
                        val (ok, failed) = runBlocking { Actions.runScene(app, scene, by) }
                        res.getString(R.string.scene_ran, scene.name, ok, failed)
                    }
                }
                else -> null
            }
            if (message != null) Handler(Looper.getMainLooper()).post { Toast.makeText(app, message, Toast.LENGTH_SHORT).show() }
        }.start()
    }

    /** Görünmez etkinlik: başlatıcı kısayolu buraya gelir, iş arka planda sürer, ekran açılmaz. */
    class ShortcutActivity : Activity() {
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            intent?.getStringExtra(EXTRA)?.let { run(this, it) }
            finish()
        }
    }
}
