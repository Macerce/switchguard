package com.macerce.switchguard.service

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

/**
 * Arama sırasında alarm metnini karşı tarafa dinletir. Android uygulamaların görüşmeye doğrudan ses vermesine izin
 * vermez; bu yüzden görüşme hoparlöre alınır ve metin yüksek sesle okunur, mikrofon karşıya iletir. Açılıp açılmadığı
 * bilinemediğinden metin görüşme bitene dek (en çok [MAX_MS]) aralıkla tekrarlanır.
 */
object CallAnnouncer {
    private const val TAG = "CallAnnouncer"
    private const val POLL_MS = 1_000L
    /** Arama başlayınca sistemin görüşme moduna geçmesi için tanınan süre. */
    private const val START_WAIT_MS = 15_000L
    private const val PAUSE_MS = 2_500L
    private const val MAX_MS = 3 * 60_000L

    private val handler = Handler(Looper.getMainLooper())
    private var tts: TextToSpeech? = null
    private var session = 0

    /** [text]'i görüşme boyunca okur; önceki okuma varsa durdurulur. Ana iş parçacığından çağrılmalı. */
    fun start(context: Context, text: String) {
        stop(context)
        val id = ++session
        val app = context.applicationContext
        val audio = app.getSystemService(AudioManager::class.java) ?: return
        val startedAt = System.currentTimeMillis()
        var inCall = false
        var speaking = false
        tts = TextToSpeech(app) { status ->
            if (status != TextToSpeech.SUCCESS || id != session) return@TextToSpeech
            val engine = tts ?: return@TextToSpeech
            engine.language = Locale.getDefault()
            engine.setAudioAttributes(
                AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build(),
            )
            engine.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) = Unit
                override fun onDone(utteranceId: String?) { speaking = false }
                @Deprecated("Deprecated in Java") override fun onError(utteranceId: String?) { speaking = false }
            })
            fun loop() {
                if (id != session) return
                val elapsed = System.currentTimeMillis() - startedAt
                val calling = audio.mode == AudioManager.MODE_IN_CALL
                when {
                    calling -> {
                        if (!inCall) {
                            inCall = true
                            speaker(audio, true)
                        }
                        if (!speaking) {
                            speaking = true
                            engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, "call-$id")
                            handler.postDelayed(::loop, PAUSE_MS)
                            return
                        }
                    }
                    // Görüşme bitti ya da hiç başlamadı.
                    inCall || elapsed > START_WAIT_MS -> { stop(app); return }
                }
                if (elapsed > MAX_MS) { stop(app); return }
                handler.postDelayed(::loop, POLL_MS)
            }
            handler.post(::loop)
        }
    }

    fun stop(context: Context) {
        session++
        handler.removeCallbacksAndMessages(null)
        tts?.let { runCatching { it.stop(); it.shutdown() } }
        tts = null
        context.applicationContext.getSystemService(AudioManager::class.java)?.let { speaker(it, false) }
    }

    /** Görüşmeyi hoparlöre alır ya da geri bırakır; ses de en yükseğe çekilir ki mikrofon net duysun. */
    private fun speaker(audio: AudioManager, on: Boolean) {
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (on) {
                    audio.availableCommunicationDevices.firstOrNull { it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER }
                        ?.let { audio.setCommunicationDevice(it) }
                } else audio.clearCommunicationDevice()
            } else {
                @Suppress("DEPRECATION")
                audio.isSpeakerphoneOn = on
            }
            if (on) audio.setStreamVolume(AudioManager.STREAM_VOICE_CALL, audio.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL), 0)
        }.onFailure { Log.w(TAG, "speaker", it) }
    }
}
