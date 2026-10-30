package com.macerce.switchguard.service

import android.app.Activity
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.SizeF
import android.view.View
import android.widget.RemoteViews
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import com.macerce.switchguard.R
import com.macerce.switchguard.data.Store
import java.util.Locale
import kotlinx.coroutines.launch

/**
 * Sesli komut widget'ı: tek kutucuk, dokununca konuşma tanıma açılır; söylenen komut ("Mutfağı kapat", "Bahçe açık mı")
 * sohbetteki gibi yürütülür, yanıt kutucukta yazar ve (Sohbet'te sesli yanıt açıksa) sesli okunur.
 */
class VoiceWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) = updateAll(context)

    companion object {
        private fun prefs(context: Context) = context.getSharedPreferences("switchguard_voice_widget", Context.MODE_PRIVATE)

        /** Son komut ve yanıt kutucukta gösterilir. */
        fun remember(context: Context, heard: String, answer: String) {
            prefs(context).edit().putString("heard", heard).putString("answer", answer).apply()
            updateAll(context)
        }

        fun updateAll(context: Context) {
            runCatching {
                val manager = AppWidgetManager.getInstance(context) ?: return
                val ids = manager.getAppWidgetIds(ComponentName(context, VoiceWidget::class.java))
                if (ids.isEmpty()) return
                val wide = views(context, withText = true)
                // Tek kutucukta yalnız mikrofon; genişletilince son komut ve yanıt da görünür.
                val views = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    RemoteViews(mapOf(SizeF(40f, 40f) to views(context, withText = false), SizeF(130f, 40f) to wide))
                } else wide
                manager.updateAppWidget(ids, views)
            }
        }

        private fun views(context: Context, withText: Boolean): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_voice)
            if (withText) {
                val p = prefs(context)
                views.setTextViewText(R.id.widget_title, p.getString("heard", null) ?: context.getString(R.string.widget_voice_title))
                views.setTextViewText(R.id.widget_status, p.getString("answer", null)?.let(Speaker::speakable) ?: context.getString(R.string.widget_voice_tap))
            } else {
                views.setViewVisibility(R.id.widget_texts, View.GONE)
                views.setViewPadding(R.id.widget_root, 0, 0, 0, 0)
            }
            val open = Intent(context, VoiceCommandActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            views.setOnClickPendingIntent(
                R.id.widget_root,
                PendingIntent.getActivity(context, 4100, open, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT),
            )
            return views
        }
    }
}

/** Görünmez ekran: konuşma tanımayı açar, komutu yürütür, yanıtı gösterip kapanır. */
class VoiceCommandActivity : ComponentActivity() {
    private val listen = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val heard = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull { it.isNotBlank() }
        if (result.resultCode != Activity.RESULT_OK || heard == null) {
            finish()
            return@registerForActivityResult
        }
        val app = applicationContext
        lifecycleScope.launch {
            val answer = runCatching { ChatRunner.answer(app, heard, getString(R.string.widget_voice_title)) }
                .getOrElse { "⚠️ " + it.message.orEmpty() } ?: getString(R.string.chat_not_understood)
            val shown = heard.replaceFirstChar { it.titlecase(Locale.getDefault()) }
            VoiceWidget.remember(app, shown, answer)
            Toast.makeText(app, Speaker.speakable(answer), Toast.LENGTH_LONG).show()
            if (Store.get(app).chatSpeak) Speaker.say(app, answer)
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState != null) return
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE, resources.configuration.locales[0].toLanguageTag())
            .putExtra(RecognizerIntent.EXTRA_PROMPT, getString(R.string.chat_voice_prompt))
            .putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        try {
            listen.launch(intent)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(this, R.string.chat_voice_unavailable, Toast.LENGTH_LONG).show()
            finish()
        }
    }
}

/** Yanıtı sesli okur (ekran kapansa da biter, sonra okuyucu kapatılır). */
object Speaker {
    /** Emoji ve simgeler atlanır ("⚠️" "uyarı işareti" diye okunmasın). */
    fun speakable(text: String): String =
        text.replace(Regex("[\\p{So}\\p{Cn}\\p{Cs}\\uFE0F\\u200D•·]"), " ").replace(Regex("\\s+"), " ").trim()

    fun say(context: Context, text: String) {
        val clean = speakable(text)
        if (clean.isEmpty()) return
        val app = context.applicationContext
        var engine: TextToSpeech? = null
        engine = TextToSpeech(app) { status ->
            val e = engine ?: return@TextToSpeech
            if (status != TextToSpeech.SUCCESS) { e.shutdown(); return@TextToSpeech }
            e.language = app.resources.configuration.locales[0]
            e.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(id: String?) = Unit
                override fun onDone(id: String?) = e.shutdown()
                @Deprecated("Deprecated in Java")
                override fun onError(id: String?) = e.shutdown()
            })
            e.speak(clean, TextToSpeech.QUEUE_FLUSH, null, "voice")
        }
    }
}
