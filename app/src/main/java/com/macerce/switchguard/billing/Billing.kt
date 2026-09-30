package com.macerce.switchguard.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.BillingClient.ProductType
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.macerce.switchguard.data.Store
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Google Play üzerinden tek seferlik "Pro" satın alma. Durum [Store.isPro]'da önbelleklenir:
 * servis Play'e hiç dokunmaz, yalnızca bu bayrağı okur.
 * Fiyat Play Console'da belirlenir; burada yalnızca Play'in döndürdüğü yerel fiyat gösterilir.
 */
class Billing private constructor(context: Context) {

    enum class State { CONNECTING, READY, UNAVAILABLE, PENDING }

    private val store = Store.get(context)
    private var product: ProductDetails? = null

    private val _state = MutableStateFlow(State.CONNECTING)
    val state: StateFlow<State> = _state
    /** Play'in yerel para birimindeki fiyatı ("₺129,99" gibi); henüz alınamadıysa null. */
    private val _price = MutableStateFlow<String?>(null)
    val price: StateFlow<String?> = _price

    private val client: BillingClient = BillingClient.newBuilder(context.applicationContext)
        .setListener { result, purchases -> if (result.responseCode == BillingResponseCode.OK) handle(purchases.orEmpty()) }
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .enableAutoServiceReconnection()
        .build()

    /** Bağlanır; bağlıysa satın almaları ve fiyatı tazeler (ör. uygulama her açıldığında). */
    fun refresh() {
        if (client.isReady) {
            onConnected()
            return
        }
        client.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingResponseCode.OK) onConnected()
                else _state.value = State.UNAVAILABLE
            }

            override fun onBillingServiceDisconnected() = Unit // enableAutoServiceReconnection yeniden bağlar.
        })
    }

    /** Satın alma ekranını açar. false: Play hazır değil (ör. Play Store'suz cihaz). */
    fun purchase(activity: Activity): Boolean {
        val details = product ?: return false
        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(details).build()))
            .build()
        return client.launchBillingFlow(activity, params).responseCode == BillingResponseCode.OK
    }

    private fun onConnected() {
        _state.value = State.READY
        client.queryPurchasesAsync(QueryPurchasesParams.newBuilder().setProductType(ProductType.INAPP).build()) { result, purchases ->
            // Yalnızca başarılı sorgu durumu değiştirir: çevrimdışıyken önbellekteki Pro korunur,
            // iade edilen satın alma ise listeden düştüğü için Pro kalkar.
            if (result.responseCode == BillingResponseCode.OK) {
                if (purchases.none { PRO_ID in it.products }) store.isPro = false
                handle(purchases)
            }
        }
        val query = QueryProductDetailsParams.newBuilder()
            .setProductList(listOf(QueryProductDetailsParams.Product.newBuilder().setProductId(PRO_ID).setProductType(ProductType.INAPP).build()))
            .build()
        client.queryProductDetailsAsync(query) { result, details ->
            product = details.productDetailsList.firstOrNull().takeIf { result.responseCode == BillingResponseCode.OK }
            _price.value = product?.oneTimePurchaseOfferDetailsList?.firstOrNull()?.formattedPrice
            // Ürün yoksa (Play dışından kurulum, ürün henüz yayında değil) "yükleniyor"da takılı kalma.
            if (product == null && _state.value == State.READY) _state.value = State.UNAVAILABLE
        }
    }

    private fun handle(purchases: List<Purchase>) {
        val pro = purchases.filter { PRO_ID in it.products }
        if (pro.any { it.purchaseState == Purchase.PurchaseState.PENDING }) _state.value = State.PENDING
        pro.filter { it.purchaseState == Purchase.PurchaseState.PURCHASED }.forEach { p ->
            store.isPro = true
            _state.value = State.READY
            // Onaylanmayan satın alma 3 gün sonra Play tarafından iade edilir.
            if (!p.isAcknowledged) {
                client.acknowledgePurchase(AcknowledgePurchaseParams.newBuilder().setPurchaseToken(p.purchaseToken).build()) { }
            }
        }
    }

    companion object {
        /** Play Console'da bu kimlikle "tek seferlik ürün" oluşturulmalı. */
        const val PRO_ID = "pro_unlimited"

        @Volatile private var instance: Billing? = null
        fun get(context: Context): Billing =
            instance ?: synchronized(this) { instance ?: Billing(context).also { instance = it } }
    }
}
