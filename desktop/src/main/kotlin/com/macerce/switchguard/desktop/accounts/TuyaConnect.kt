package com.macerce.switchguard.desktop.accounts

import com.macerce.switchguard.api.TuyaAuthException
import com.macerce.switchguard.api.TuyaClient
import com.macerce.switchguard.api.TuyaExpiredException
import com.macerce.switchguard.data.Store
import com.macerce.switchguard.desktop.Log
import java.io.IOException

sealed interface TuyaResult {
    data class Connected(val devices: Int) : TuyaResult
    data object AuthError : TuyaResult
    data object Expired : TuyaResult
    data object NoInternet : TuyaResult
    data class Failed(val message: String) : TuyaResult
}

/**
 * Ayarlar → Tuya bağlama (telefondaki TuyaSetup gibi): yeni bilgiler kaydedilip denenir ([probe]: token + cihaz
 * listesi); başarısızsa önceki bilgiler (token dahil) geri konur, çalışan bir bağlantı yanlış bir denemeyle bozulmaz.
 * Ağ çağrısı yapar: arayüz iş parçacığından çağırmayın.
 */
class TuyaConnect(private val store: Store, private val probe: () -> Int = { TuyaClient(store).test() }) {

    fun connect(accessId: String, secret: String, region: String): TuyaResult {
        val previous = Saved(store.tuyaAccessId, store.tuyaSecret, store.tuyaRegion, store.tuyaToken, store.tuyaTokenExpiry)
        store.tuyaAccessId = accessId.trim()
        store.tuyaSecret = secret.trim()
        store.tuyaRegion = region
        val result = try {
            TuyaResult.Connected(probe())
        } catch (e: TuyaAuthException) {
            TuyaResult.AuthError
        } catch (e: TuyaExpiredException) {
            TuyaResult.Expired
        } catch (e: IOException) {
            TuyaResult.NoInternet
        } catch (e: Exception) {
            TuyaResult.Failed(e.message ?: e.javaClass.simpleName)
        }
        if (result !is TuyaResult.Connected) {
            Log.w(TAG, "Tuya connect failed: ${result.javaClass.simpleName}; previous keys restored")
            previous.restore()
        }
        return result
    }

    /** Bilgileri ve Tuya cihazlarının anlık görüntülerini siler (`Store.disconnectTuya`). */
    fun disconnect() = store.disconnectTuya()

    private inner class Saved(val id: String, val secret: String, val region: String, val token: String, val tokenExp: Long) {
        fun restore() {
            store.tuyaAccessId = id
            store.tuyaSecret = secret
            store.tuyaRegion = region
            store.tuyaToken = token
            store.tuyaTokenExpiry = tokenExp
        }
    }

    private companion object {
        const val TAG = "TuyaConnect"
    }
}
