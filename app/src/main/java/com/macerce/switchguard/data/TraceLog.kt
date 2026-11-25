package com.macerce.switchguard.data

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.macerce.switchguard.core.AutomationTraces
import com.macerce.switchguard.core.TraceEntry
import com.macerce.switchguard.core.TraceStep
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Kural izleri ([AutomationTraces]): bellekte tutulur, arayüz [flow]'u izler. Diske seyrek yazılır (15 sn içindeki
 * değişiklikler tek yazımda): engellenen süre tetikleyicileri her tick'te sayaç artırabilir. Yedeğe/aktarıma girmez.
 */
object TraceLog {
    private const val PREFS = "switchguard_traces"
    private const val KEY = "traces"
    private const val SAVE_DELAY_MS = 15_000L

    private val state = MutableStateFlow<List<TraceEntry>>(emptyList())
    val flow: StateFlow<List<TraceEntry>> get() = state

    @Volatile private var loaded = false
    private var app: Context? = null
    private val main by lazy { Handler(Looper.getMainLooper()) }
    @Volatile private var scheduled = false
    private val save = Runnable { scheduled = false; write() }

    fun init(context: Context) {
        if (loaded) return
        synchronized(this) {
            if (loaded) return
            app = context.applicationContext
            state.value = AutomationTraces.fromJson(prefs()?.getString(KEY, null))
            loaded = true
        }
    }

    private fun prefs() = app?.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    @Synchronized
    fun record(context: Context, e: TraceEntry) {
        init(context)
        state.value = AutomationTraces.add(state.value, e)
        saveSoon()
    }

    @Synchronized
    fun step(context: Context, automationId: String, s: TraceStep) {
        init(context)
        state.value = AutomationTraces.step(state.value, automationId, s)
        saveSoon()
    }

    /** Silinen kuralların izlerini atar. */
    @Synchronized
    fun prune(context: Context, ids: Set<String>) {
        init(context)
        val next = AutomationTraces.prune(state.value, ids)
        if (next.size != state.value.size) { state.value = next; saveSoon() }
    }

    @Synchronized
    fun clear(context: Context, automationId: String) {
        init(context)
        state.value = state.value.filterNot { it.automationId == automationId }
        saveSoon()
    }

    /** İlk değişiklikten 15 sn sonra bir kez yazılır (sürekli değişiklik yazımı ertelemez). */
    private fun saveSoon() {
        if (scheduled) return
        scheduled = true
        main.postDelayed(save, SAVE_DELAY_MS)
    }

    /** Hemen yazar (servis kapanırken). */
    fun flush() {
        main.removeCallbacks(save)
        scheduled = false
        write()
    }

    private fun write() {
        val json = synchronized(this) { AutomationTraces.toJson(state.value) }
        prefs()?.edit()?.putString(KEY, json)?.apply()
    }
}
