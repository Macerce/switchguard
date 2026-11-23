package com.macerce.switchguard.core

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.MessageDigest

class KasaTapoTest {
    @Test
    fun kasaXor() {
        val e = KasaProtocol.encrypt(KasaProtocol.DISCOVER)
        assertEquals(KasaProtocol.DISCOVER.length, ((e[0].toInt() and 0xff) shl 24) or ((e[1].toInt() and 0xff) shl 16) or ((e[2].toInt() and 0xff) shl 8) or (e[3].toInt() and 0xff))
        assertEquals(0xd0, e[4].toInt() and 0xff) // '{' xor 171
        assertEquals(KasaProtocol.DISCOVER, KasaProtocol.decrypt(e, 4))
        assertEquals(KasaProtocol.DISCOVER, KasaProtocol.decrypt(KasaProtocol.encrypt(KasaProtocol.DISCOVER, withLength = false)))
    }

    @Test
    fun kasaSysinfo() {
        val plug = """{"system":{"get_sysinfo":{"alias":"Salon","model":"HS110(EU)","mac":"50:C7:BF:00:11:22","relay_state":1}},
            "emeter":{"get_realtime":{"power_mw":12500,"voltage_mv":230100,"current_ma":60,"err_code":0}}}"""
        val i = KasaProtocol.parseInfo(plug)!!
        assertEquals("kasa-50c7bf001122", i.id)
        assertEquals("HS110", i.model)
        val r = KasaProtocol.parseStatus(plug, 1)!!
        assertEquals(mapOf(0 to true), r.switches)
        assertEquals(12.5, r.power!!, 0.001)
        val strip = """{"system":{"get_sysinfo":{"alias":"Masa","model":"HS300(US)","mac":"AA","children":[{"id":"X00","state":1,"alias":"a"},{"id":"X01","state":0,"alias":"b"}]}}}"""
        val s = KasaProtocol.parseInfo(strip)!!
        assertEquals(listOf("X00", "X01"), KasaProtocol.children(KasaProtocol.extraFor(s)))
        assertEquals(mapOf(0 to true, 1 to false), KasaProtocol.parseStatus(strip, 2)!!.switches)
        assertTrue(KasaProtocol.switchCommand(true, "X01", false).contains("\"child_ids\":[\"X01\"]"))
        assertTrue(KasaProtocol.isOk("""{"system":{"set_relay_state":{"err_code":0}}}"""))
        assertFalse(KasaProtocol.isOk("""{"system":{"set_relay_state":{"err_code":-1}}}"""))
    }

    @Test
    fun tapoKlap() {
        val local = ByteArray(16) { it.toByte() }
        val remote = ByteArray(16) { (100 + it).toByte() }
        val auth = TapoProtocol.authHashV2("a@b.c", "pw")
        val server = MessageDigest.getInstance("SHA-256").run { update(local); update(remote); update(auth); digest() }
        assertEquals(2, TapoProtocol.verifyServer(local, remote, server, "a@b.c", "pw"))
        assertEquals(0, TapoProtocol.verifyServer(local, remote, server, "a@b.c", "yanlis"))
        val s = TapoProtocol.Session(local, remote, auth, "TP_SESSIONID=x")
        val start = s.seq
        val (seq, payload) = s.encrypt(TapoProtocol.INFO)
        assertEquals(start + 1, seq)
        // Cihaz yanıtı aynı biçimdedir (32 bayt imza + şifreli metin): kendi şifrelediğimizi çözebilmeliyiz.
        assertEquals(TapoProtocol.INFO, s.decrypt(seq, payload))
        assertArrayEquals(TapoProtocol.authHashV2("a@b.c", "pw"), auth)
        val info = TapoProtocol.parseInfo("""{"error_code":0,"result":{"device_on":true,"nickname":"U2Fsb24=","model":"P110","mac":"AA-BB-CC-00-11-22"}}""")!!
        assertEquals("tapo-aabbcc001122", info.id)
        assertEquals("Salon", info.nickname)
        assertEquals(3.5, TapoProtocol.parsePower("""{"error_code":0,"result":{"current_power":3.5}}""")!!, 0.0)
    }
}
