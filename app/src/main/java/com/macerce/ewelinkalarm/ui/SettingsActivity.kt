package com.macerce.ewelinkalarm.ui

import android.app.Activity
import android.os.Bundle
import android.text.InputType
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import com.macerce.ewelinkalarm.data.Store

class SettingsActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = "Ayarlar"
        val store = Store(this)
        lateinit var appId: EditText
        lateinit var secret: EditText
        lateinit var redirect: EditText
        lateinit var interval: EditText

        setContentView(column {
            label("dev.ewelink.cc adresinde oluşturduğunuz uygulamanın bilgileri:", 14f)
            label("App ID", 14f)
            appId = field(store.appId, InputType.TYPE_CLASS_TEXT)
            label("App Secret", 14f)
            secret = field(store.appSecret, InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD)
            label("Redirect URL (geliştirici sitesine yazdığınızla birebir aynı olmalı)", 14f)
            redirect = field(store.redirectUrl, InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI)
            label("Kontrol aralığı (saniye, en az ${Store.MIN_INTERVAL})", 14f)
            interval = field(store.intervalSec.toString(), InputType.TYPE_CLASS_NUMBER)

            button("Kaydet") {
                val credentialsChanged = appId.text.toString().trim() != store.appId ||
                    secret.text.toString().trim() != store.appSecret
                store.appId = appId.text.toString()
                store.appSecret = secret.text.toString()
                store.redirectUrl = redirect.text.toString()
                store.intervalSec = interval.text.toString().toIntOrNull() ?: 30
                // Başka bir uygulamanın token'ı yeni App ID ile çalışmaz.
                if (credentialsChanged) store.logout()
                Toast.makeText(this@SettingsActivity, "Kaydedildi", Toast.LENGTH_SHORT).show()
                finish()
            }
        })
    }

    private fun LinearLayout.field(value: String, type: Int): EditText =
        EditText(context).apply {
            setText(value)
            inputType = type
            isSingleLine = true
        }.also { addView(it) }
}
