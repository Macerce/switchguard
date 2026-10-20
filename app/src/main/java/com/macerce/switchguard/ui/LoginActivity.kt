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
import com.macerce.switchguard.core.AppLanguage
import com.macerce.switchguard.core.EwelinkRegion
import com.macerce.switchguard.data.Store

/**
 * eWeLink OAuth giriş sayfasını açar. Giriş sonrası sayfa redirect URL'ye yönlenir;
 * o adrese gitmeden yakalayıp içindeki code + region ile token alırız.
 */
class LoginActivity : Activity() {
    override fun attachBaseContext(newBase: Context) = super.attachBaseContext(AppLanguage.wrap(newBase))


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

        val tv = FormFactor.isTv(this)
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

            override fun onPageFinished(view: WebView, url: String) {
                // TV: kumandada Tab yok; yukarı/aşağı ile alanlar arasında gezilsin, seçili alan belli olsun.
                if (tv) view.evaluateJavascript(TV_NAV_JS, null)
            }
        }
        setContentView(web)
        if (tv) web.requestFocus()
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
        // Bölge sunucu adresine yazılır: bilinmeyen bir değer gelirse giriş reddedilir.
        val region = uri.getQueryParameter("region").let { if (it == null) store.region else EwelinkRegion.parse(it) }
        if (code.isNullOrEmpty() || uri.getQueryParameter("state") != state || region == null) {
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
        /**
         * TV'de eWeLink giriş sayfası için: yukarı/aşağı tuşları alanlar arasında gezer (kumandada Tab yok),
         * açık ülke listesinde liste içinde gezer; seçili alan kalın çerçeveyle gösterilir.
         */
        private val TV_NAV_JS = """
(function () {
  if (window.__sgTv) return; window.__sgTv = true;
  var st = document.createElement('style');
  st.textContent = ':focus{outline:4px solid #1e88e5 !important;outline-offset:3px !important;border-radius:8px}';
  document.head.appendChild(st);
  function items() {
    return Array.prototype.filter.call(
      document.querySelectorAll('input:not([type=hidden]):not([disabled]),button:not([disabled]),a[href],[tabindex]:not([tabindex="-1"])'),
      function (e) { var r = e.getBoundingClientRect(); return r.width > 0 && r.height > 0 && getComputedStyle(e).visibility !== 'hidden'; });
  }
  function listOpen() {
    return Array.prototype.some.call(document.querySelectorAll('.el-select-dropdown,.el-popper,[role=listbox]'),
      function (d) { return d.offsetParent !== null && getComputedStyle(d).display !== 'none'; });
  }
  document.addEventListener('keydown', function (e) {
    if (e.key !== 'ArrowDown' && e.key !== 'ArrowUp') return;
    if (listOpen()) return;
    var l = items(); if (!l.length) return;
    var i = l.indexOf(document.activeElement);
    var n = e.key === 'ArrowDown' ? (i < 0 ? 0 : Math.min(i + 1, l.length - 1)) : (i <= 0 ? 0 : i - 1);
    l[n].focus(); l[n].scrollIntoView({ block: 'center' });
    e.preventDefault(); e.stopPropagation();
  }, true);
  setTimeout(function () { var l = items(); if (l.length) l[0].focus(); }, 400);
})();
""".trimIndent()

        fun start(context: Context) = context.startActivity(Intent(context, LoginActivity::class.java))
    }
}
