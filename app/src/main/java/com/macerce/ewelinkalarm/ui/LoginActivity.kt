package com.macerce.ewelinkalarm.ui

import android.annotation.SuppressLint
import android.app.Activity
import android.net.Uri
import android.os.Bundle
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import com.macerce.ewelinkalarm.api.EwelinkClient
import com.macerce.ewelinkalarm.data.Store

/**
 * eWeLink OAuth giriş sayfasını açar. Giriş sonrası sayfa redirect URL'ye yönlenir;
 * o adrese gitmeden yakalayıp içindeki code + region ile token alırız.
 */
class LoginActivity : Activity() {

    private lateinit var store: Store
    private lateinit var client: EwelinkClient
    private val state = EwelinkClient.randomNonce()
    private var handled = false

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = "eWeLink Giriş"
        store = Store(this)
        client = EwelinkClient(store)

        val web = WebView(this)
        web.settings.javaScriptEnabled = true
        web.settings.domStorageEnabled = true
        web.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest) =
                interceptRedirect(request.url)

            override fun onPageStarted(view: WebView, url: String, favicon: android.graphics.Bitmap?) {
                if (interceptRedirect(Uri.parse(url))) view.stopLoading()
            }
        }
        setContentView(web)
        web.loadUrl(client.buildLoginUrl(state))
    }

    private fun isRedirect(uri: Uri): Boolean {
        val target = Uri.parse(store.redirectUrl)
        return uri.scheme == target.scheme && uri.host == target.host &&
            uri.path.orEmpty().trimEnd('/') == target.path.orEmpty().trimEnd('/')
    }

    /** Redirect ise işler ve true döner (WebView o adrese gitmez). */
    private fun interceptRedirect(uri: Uri): Boolean {
        if (!isRedirect(uri)) return false
        if (handled) return true
        handled = true

        val code = uri.getQueryParameter("code")
        val region = uri.getQueryParameter("region") ?: store.region
        if (code.isNullOrEmpty() || uri.getQueryParameter("state") != state) {
            finishWith("Giriş başarısız: yanıtta kod yok")
            return true
        }
        Thread {
            val error = runCatching { client.exchangeCode(code, region) }.exceptionOrNull()
            runOnUiThread {
                finishWith(if (error == null) "Giriş başarılı" else "Token alınamadı: ${error.message}")
            }
        }.start()
        return true
    }

    private fun finishWith(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        finish()
    }
}
