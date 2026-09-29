package com.macerce.ewelinkalarm.ui

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.widget.Button
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.macerce.ewelinkalarm.data.Store
import com.macerce.ewelinkalarm.service.MonitorService

class MainActivity : Activity() {

    private lateinit var store: Store
    private lateinit var status: TextView
    private lateinit var devices: LinearLayout
    private lateinit var alarmBox: TextView
    private lateinit var dismissBtn: Button
    private lateinit var loginBtn: Button
    private lateinit var toggleBtn: Button
    private lateinit var batteryBtn: Button

    // Güçlü referans tutulmalı; SharedPreferences dinleyicileri zayıf referansla saklar.
    private val prefListener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> refresh() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = Store(this)

        setContentView(column {
            status = label("", 15f)
            alarmBox = label("", 15f).apply { setTextColor(0xFFC62828.toInt()) }
            dismissBtn = button("🔕 ALARMI KAPAT") { MonitorService.send(this@MainActivity, MonitorService.ACTION_DISMISS) }
            label("Cihazlar (işaretli olanlar izlenir):", 14f)
            devices = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }.also { addView(it) }

            label("Kurulum", 14f)
            button("1) Ayarlar (App ID / Secret)") { startActivity(Intent(this@MainActivity, SettingsActivity::class.java)) }
            loginBtn = button("") { onLoginClicked() }
            batteryBtn = button("3) Pil optimizasyonunu kapat") { requestBatteryExemption() }
            toggleBtn = button("") { onToggleClicked() }
            button("Test alarmı çal") {
                if (store.monitoringEnabled) MonitorService.send(this@MainActivity, MonitorService.ACTION_TEST)
                else toast("Önce izlemeyi başlatın")
            }
        })
        requestNotificationPermission()
    }

    override fun onResume() {
        super.onResume()
        store.prefs.registerOnSharedPreferenceChangeListener(prefListener)
        refresh()
    }

    override fun onPause() {
        store.prefs.unregisterOnSharedPreferenceChangeListener(prefListener)
        super.onPause()
    }

    private fun refresh() {
        status.text = store.statusText

        val alarm = store.pendingAlarm
        alarmBox.text = if (alarm.isEmpty()) "" else "⚠ Alarm:\n" + alarm.reversed().joinToString("\n")
        alarmBox.visibility = if (alarm.isEmpty()) TextView.GONE else TextView.VISIBLE
        dismissBtn.visibility = alarmBox.visibility

        loginBtn.text = if (store.isLoggedIn) "2) eWeLink girişi ✓ (yeniden giriş)" else "2) eWeLink ile giriş yap"
        batteryBtn.visibility = if (isBatteryExempt()) Button.GONE else Button.VISIBLE
        toggleBtn.text = if (store.monitoringEnabled) "■ İzlemeyi durdur" else "▶ İzlemeyi başlat"

        renderDevices()
    }

    @SuppressLint("SetTextI18n")
    private fun renderDevices() {
        devices.removeAllViews()
        val list = store.snapshots
        if (list.isEmpty()) {
            devices.addView(TextView(this).apply { text = "Henüz veri yok — giriş yapıp izlemeyi başlatın." })
            return
        }
        val monitored = store.effectiveMonitored()
        for (d in list) {
            val switches = d.switches.toSortedMap().entries.joinToString(" ") { (ch, on) ->
                val state = if (on) "AÇIK" else "KAPALI"
                if (d.isMultiChannel) "K${ch + 1}:$state" else state
            }.ifEmpty { "-" }
            devices.addView(CheckBox(this).apply {
                text = "${d.name}\n$switches · ${if (d.online) "🟢 ONLINE" else "🔴 OFFLINE"}"
                isChecked = d.id in monitored
                setOnCheckedChangeListener { _, checked ->
                    val current = store.effectiveMonitored()
                    store.monitoredIds = if (checked) current + d.id else current - d.id
                }
            })
        }
    }

    private fun onLoginClicked() {
        if (!store.hasCredentials) {
            toast("Önce Ayarlar'a App ID ve App Secret girin")
            return
        }
        startActivity(Intent(this, LoginActivity::class.java))
    }

    private fun onToggleClicked() {
        if (store.monitoringEnabled) {
            MonitorService.send(this, MonitorService.ACTION_STOP)
            return
        }
        if (!store.isLoggedIn) {
            toast("Önce eWeLink ile giriş yapın")
            return
        }
        MonitorService.send(this, MonitorService.ACTION_START)
    }

    private fun isBatteryExempt() =
        getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(packageName)

    @SuppressLint("BatteryLife") // Sürekli izleme uygulamasının temel gereksinimi.
    private fun requestBatteryExemption() {
        startActivity(
            Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:$packageName"))
        )
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
}
