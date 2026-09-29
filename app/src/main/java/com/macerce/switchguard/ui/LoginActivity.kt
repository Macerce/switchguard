package com.macerce.switchguard.ui

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import com.macerce.switchguard.R
import com.macerce.switchguard.api.EwelinkClient
import com.macerce.switchguard.data.Store

/**
 * eWeLink OAuth giriş sayfasını açar. Giriş sonrası sayfa redirect URL'ye yönlenir;
 * o adrese gitmeden yakalayıp içindeki code + region ile token alırız.
 */
class LoginActivity : Activity() {

    private lateinit var store: Store
    private lateinit var client: EwelinkClient
    private val state = EwelinkClient.randomNonce()
    private var handled = false

    @SuppressLint("SetJavaScriptEnabled") // eWeLink giriş sayfası JavaScript gerektirir.
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = getString(R.string.login_title)
        store = Store.get(this)
        client = EwelinkClient(store)

        if (!store.hasCredentials) {
            finishWith(getString(R.string.login_needs_credentials))
            return
        }

        val web = WebView(this)
        web.settings.javaScriptEnabled = true
        web.settings.domStorageEnabled = true
        // Önceki hesabın oturum çerezi kalmasın; her giriş temiz başlasın.
        CookieManager.getInstance().removeAllCookies(null)
        web.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest) =
                interceptRedirect(request.url)

            override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
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
            finishWith(getString(R.string.login_failed_no_code))
            return true
        }
        Thread {
            val error = runCatching { client.exchangeCode(code, region) }.exceptionOrNull()
            runOnUiThread {
                if (error == null) {
                    Actions.afterLogin(this)
                    finishWith(getString(R.string.login_success))
                } else {
                    finishWith(getString(R.string.login_failed, error.message ?: ""))
                }
            }
        }.start()
        return true
    }

    private fun finishWith(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        finish()
    }

    companion object {
        fun start(context: Context) = context.startActivity(Intent(context, LoginActivity::class.java))
    }
}
