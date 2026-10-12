package com.macerce.switchguard.data

import org.junit.Assert.assertEquals
import org.junit.Test

class StatusSummaryTest {
    @Test fun `bekleyen alarm her seyden once gelir`() =
        assertEquals(StatusKind.ALARM, StatusSummary.kind(monitoring = true, alarms = 2, state = ConnState.LIVE))

    @Test fun `izleme kapaliysa durduruldu`() =
        assertEquals(StatusKind.STOPPED, StatusSummary.kind(monitoring = false, alarms = 0, state = ConnState.LIVE))

    @Test fun `canli ve yedek sorgulama izleniyor sayilir`() {
        assertEquals(StatusKind.LIVE, StatusSummary.kind(true, 0, ConnState.LIVE))
        assertEquals(StatusKind.POLLING, StatusSummary.kind(true, 0, ConnState.POLLING))
    }

    @Test fun `baglaniyor ve servis henuz yok`() {
        assertEquals(StatusKind.CONNECTING, StatusSummary.kind(true, 0, ConnState.CONNECTING))
        // İzleme açık ama servis henüz durum bildirmedi (ör. telefon yeni açıldı).
        assertEquals(StatusKind.CONNECTING, StatusSummary.kind(true, 0, ConnState.STOPPED))
    }

    @Test fun `internet, oturum ve hata sorun sayilir`() {
        assertEquals(StatusKind.PROBLEM, StatusSummary.kind(true, 0, ConnState.NO_INTERNET))
        assertEquals(StatusKind.PROBLEM, StatusSummary.kind(true, 0, ConnState.AUTH_ERROR))
        assertEquals(StatusKind.PROBLEM, StatusSummary.kind(true, 0, ConnState.ERROR))
    }

    @Test fun `izleme acik ama servis yoksa sorun`() =
        assertEquals(StatusKind.PROBLEM, StatusSummary.kind(monitoring = true, alarms = 0, state = ConnState.LIVE, serviceAlive = false))

    @Test fun `kullanici kapattiysa servis olmamasi sorun degil`() =
        assertEquals(StatusKind.STOPPED, StatusSummary.kind(monitoring = false, alarms = 0, state = ConnState.STOPPED, serviceAlive = false))
}
