package com.macerce.switchguard.desktop.platform

import com.macerce.switchguard.desktop.Log
import java.awt.Robot
import java.awt.event.KeyEvent
import java.nio.file.Files
import java.nio.file.Path
import java.util.Locale

/**
 * Sohbette ses (telefondaki konuşma tanıma + TextToSpeech karşılığı).
 * Dinleme: Windows'un kendi sesle yazması (Win+H) odaklı yazı kutusuna yazar; Türkçe dahil Windows'un desteklediği
 * dillerde çalışır, uygulama mikrofonu kendisi açmaz. Okuma: Windows'un OneCore sesleri (ör. Microsoft Tolga) —
 * eski SAPI yalnız İngilizce ses görür, bu yüzden WinRT SpeechSynthesizer PowerShell 5.1 üzerinden çağrılır.
 */
object Speech {
    @Volatile private var speaking: Process? = null

    /** Okuma sürüyor mu (arayüzde "okumayı durdur" düğmesi için). */
    val speakingNow = kotlinx.coroutines.flow.MutableStateFlow(false)

    /** Sesle yazmayı açar (Win+H); yazı kutusu önceden odaklanmış olmalı. */
    fun startDictation(): Boolean = runCatching {
        Robot().apply {
            keyPress(KeyEvent.VK_WINDOWS)
            keyPress(KeyEvent.VK_H)
            keyRelease(KeyEvent.VK_H)
            keyRelease(KeyEvent.VK_WINDOWS)
        }
        true
    }.getOrElse { Log.w("Speech", "sesle yazma açılamadı", it); false }

    /** Emoji ve simgeler atlanır ("⚠️" "uyarı işareti" diye okunmasın). */
    fun speakable(text: String): String =
        text.replace(Regex("[\\p{So}\\p{Cn}\\p{Cs}\\uFE0F\\u200D•·]"), " ").replace(Regex("\\s+"), " ").trim()

    /**
     * Yanıtı arka planda okur; önceki okuma sürüyorsa kesilir. [onDone] okuma kendiliğinden bitince (kesilmeden)
     * "speech" iş parçacığında çağrılır: soru yanıtından sonra dinlemeyi yeniden açmak için.
     */
    fun say(text: String, locale: Locale, onDone: (() -> Unit)? = null) {
        val clean = speakable(text)
        if (clean.isEmpty()) return
        stop()
        speakingNow.value = true
        Thread({
            runCatching {
                val p = ProcessBuilder("powershell.exe", "-NoProfile", "-NonInteractive", "-ExecutionPolicy", "Bypass", "-File", script().toString())
                    .redirectErrorStream(true)
                    .apply {
                        environment()["SG_TTS_TEXT"] = clean
                        environment()["SG_TTS_LANG"] = locale.language
                    }
                    .start()
                speaking = p
                p.inputStream.readAllBytes()
                p.waitFor()
                // stop() ile kesildiyse speaking başka sürece geçmiş ya da boşalmıştır.
                if (speaking === p) {
                    speaking = null
                    speakingNow.value = false
                    onDone?.invoke()
                }
            }.onFailure { Log.w("Speech", "okunamadı", it); speakingNow.value = false }
        }, "speech").apply { isDaemon = true }.start()
    }

    fun stop() {
        speaking?.let { p -> runCatching { p.descendants().forEach { it.destroy() }; p.destroy() } }
        speaking = null
        speakingNow.value = false
    }

    @Volatile private var scriptPath: Path? = null

    private fun script(): Path = scriptPath ?: synchronized(this) {
        scriptPath ?: Files.createTempFile("switchguard-tts", ".ps1").also {
            it.toFile().deleteOnExit()
            // PowerShell 5.1 BOM'suz UTF-8'i ANSI sanar; betik yalnız ASCII.
            Files.writeString(it, SCRIPT)
            scriptPath = it
        }
    }

    private val SCRIPT = """
        ${'$'}ErrorActionPreference = 'Stop'
        Add-Type -AssemblyName System.Runtime.WindowsRuntime
        ${'$'}null = [Windows.Media.SpeechSynthesis.SpeechSynthesizer, Windows.Media.SpeechSynthesis, ContentType = WindowsRuntime]
        ${'$'}asTask = [System.WindowsRuntimeSystemExtensions].GetMethods() | Where-Object { ${'$'}_.Name -eq 'AsTask' -and ${'$'}_.GetParameters().Count -eq 1 -and ${'$'}_.GetParameters()[0].ParameterType.Name -eq 'IAsyncOperation`1' } | Select-Object -First 1
        ${'$'}s = New-Object Windows.Media.SpeechSynthesis.SpeechSynthesizer
        ${'$'}v = [Windows.Media.SpeechSynthesis.SpeechSynthesizer]::AllVoices | Where-Object { ${'$'}_.Language -like (${'$'}env:SG_TTS_LANG + '*') } | Select-Object -First 1
        if (${'$'}v) { ${'$'}s.Voice = ${'$'}v }
        ${'$'}t = ${'$'}asTask.MakeGenericMethod([Windows.Media.SpeechSynthesis.SpeechSynthesisStream]).Invoke(${'$'}null, @(${'$'}s.SynthesizeTextToStreamAsync(${'$'}env:SG_TTS_TEXT)))
        ${'$'}t.Wait()
        ${'$'}wav = [System.IO.Path]::GetTempFileName()
        ${'$'}in = [System.IO.WindowsRuntimeStreamExtensions]::AsStreamForRead(${'$'}t.Result)
        ${'$'}out = [System.IO.File]::Create(${'$'}wav)
        ${'$'}in.CopyTo(${'$'}out); ${'$'}out.Close()
        try { (New-Object System.Media.SoundPlayer ${'$'}wav).PlaySync() } finally { Remove-Item ${'$'}wav -ErrorAction SilentlyContinue }
    """.trimIndent()
}
