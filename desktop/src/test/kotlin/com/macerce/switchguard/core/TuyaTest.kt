package com.macerce.switchguard.core

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TuyaTest {
    private val secret = "0123456789abcdef0123456789abcdef"

    // Vektörler Python'daki çalışan araçla (tools/tuya_probe.py ile aynı algoritma) üretildi.
    @Test fun signsLikeReferenceImplementation() {
        assertEquals(
            "5774CB7B23AE37DBFA51A1E0D4EE4B4DFC4E579F61D304EF0E485DAE238520F9",
            TuyaSign.sign("cid123", secret, "tok456", "1700000000000", "n0nce", "POST",
                """{"commands":[{"code":"switch_1","value":true}]}""", "/v1.0/iot-03/devices/abc/commands"),
        )
        assertEquals(
            "0F21E4F9C7A00DF41797C83CFF7D9F31EFE3EC64F800B670E5394879DEE34422",
            TuyaSign.sign("cid123", secret, "", "1700000000000", "n0nce", "GET", "", "/v1.0/token?grant_type=1"),
        )
    }

    @Test fun decodesGcmPulsarFrame() {
        val frame = """{"messageId": "m1", "payload": "eyJkYXRhIjogIkFBRUNBd1FGQmdjSUNRb0xpRlN5YkhYWllvQ0FKS0ZqT1gvQVgrRVZYZTA0TFJDSTI3cHZEcVIzRDF5MEd5WXFXbnNoaXc2YWFGeUtkME5CZGZBUlYrdUlIVGxMcGNsOGFYUkcxN0tVcm13WWx1RitPbFpVeHpoZ1FhQ1dBWXVCNFZBRzdxaTI4c3dkQytxVGVYbFdMVFNKTzBtZkw2Ukw0K1h3aVhTQWJCaklVZmE4UXRWdTVFWUlYMGVJaXJSaDZlZDBjNC9MSloxTjRaM3E1c29qQ1FFU2VMV2s4TldwdndtVEthbE9CYWgxcFpKOXU3cVF6dS9BUU9oemxCN2Nod0x6QlU5VlluWDBJbEVYWG9FSW13V1JUVnkyVXRQcCIsICJwcm90b2NvbCI6IDEwMDAsICJwdiI6ICIyLjAiLCAic2lnbiI6ICJ4IiwgInQiOiAxLCAiZW5jcnlwdE1vZGUiOiAiYWVzX2djbSJ9", "properties": {"em": "aes_gcm"}}"""
        val f = TuyaMessages.decode(frame, secret)
        assertEquals("m1", f.messageId)
        assertEquals(TuyaEvent.Properties("dev1", mapOf("switch_1" to false, "cur_power" to 248), time = 1), f.event)
    }

    @Test fun switchCodeMapping() {
        assertEquals(mapOf(0 to "switch_1"), TuyaParser.switchCodes(listOf("switch_1", "countdown_1", "cur_power")))
        assertEquals(mapOf(0 to "switch_1", 1 to "switch_2", 3 to "switch_4"), TuyaParser.switchCodes(listOf("switch_4", "switch_2", "switch_1", "switch_usb1")))
        assertEquals(mapOf(0 to "switch"), TuyaParser.switchCodes(listOf("switch", "cur_power")))
        assertEquals(mapOf(0 to "switch_led"), TuyaParser.switchCodes(listOf("switch_led", "bright_value")))
        assertEquals(emptyMap<Int, String>(), TuyaParser.switchCodes(listOf("liquid_level_percent")))
    }

    /** Kullanıcının gerçek prizi (Bilicra Join, cz) — 2026-10-01 okunan değerler. */
    private val spec = JSONObject("""{"status":[
        {"code":"switch_1","type":"Boolean","values":"{}"},
        {"code":"add_ele","type":"Integer","values":"{\"unit\":\"\",\"min\":0,\"max\":50000,\"scale\":3,\"step\":100}"},
        {"code":"cur_current","type":"Integer","values":"{\"unit\":\"mA\",\"min\":0,\"max\":30000,\"scale\":0,\"step\":1}"},
        {"code":"cur_power","type":"Integer","values":"{\"unit\":\"W\",\"min\":0,\"max\":50000,\"scale\":1,\"step\":1}"},
        {"code":"cur_voltage","type":"Integer","values":"{\"unit\":\"V\",\"min\":0,\"max\":5000,\"scale\":1,\"step\":1}"}]}""")

    @Test fun parsesRealPlug() {
        val scales = TuyaParser.scales(spec)
        assertEquals(mapOf("add_ele" to 3, "cur_current" to 0, "cur_power" to 1, "cur_voltage" to 1), scales)
        val device = JSONObject("""{"id":"60861003d8bfc0f1576f","name":"Bilicra Join","customName":"akıllı priz","isOnline":true,"category":"cz"}""")
        val status = JSONArray("""[{"code":"switch_1","value":true},{"code":"countdown_1","value":0},{"code":"add_ele","value":7},
            {"code":"cur_current","value":178},{"code":"cur_power","value":248},{"code":"cur_voltage","value":2356}]""")
        val s = TuyaParser.snapshot(device, status, scales)
        assertEquals("akıllı priz", s.name)
        assertEquals(Cloud.TUYA, s.cloud)
        assertEquals(true, s.online)
        assertEquals(mapOf(0 to true), s.switches)
        assertEquals(24.8, s.power!!, 1e-9)
        assertEquals(235.6, s.voltage!!, 1e-9)
        assertEquals(0.178, s.current!!, 1e-9)
        assertEquals(false, s.isMultiChannel)

        // Mesaj servisinden yalnızca değişenler gelir; diğer değerler korunur.
        val off = TuyaParser.apply(s, mapOf("switch_1" to false, "cur_power" to 0), scales)
        assertEquals(mapOf(0 to false), off.switches)
        assertEquals(0.0, off.power!!, 1e-9)
        assertEquals(235.6, off.voltage!!, 1e-9)
    }

    @Test fun nonMeteringDeviceHasNoEnergy() {
        val s = TuyaParser.snapshot(
            JSONObject("""{"id":"x","name":"Lamp","isOnline":false}"""),
            JSONArray("""[{"code":"switch_led","value":false}]"""), emptyMap(),
        )
        assertNull(s.power)
        assertEquals(false, s.hasEnergy)
        assertEquals(mapOf(0 to false), s.switches)
    }

    @Test fun snapshotJsonKeepsCloudAndCodes() {
        val s = DeviceSnapshot("t1", "T", true, mapOf(0 to true, 1 to false), cloud = Cloud.TUYA,
            switchCodes = mapOf(0 to "switch_1", 1 to "switch_2"), power = 1.5)
        assertEquals(listOf(s), DeviceParser.snapshotsFromJson(DeviceParser.snapshotsToJson(listOf(s))))
        val e = DeviceSnapshot("e1", "E", true, mapOf(0 to true))
        assertEquals(listOf(e), DeviceParser.snapshotsFromJson(DeviceParser.snapshotsToJson(listOf(e))))
    }

    @Test fun pulsarPasswordMatchesReference() {
        // md5("cid123" + md5(secret))[8:24]
        assertEquals("809e1d9c85e2d593", TuyaSign.pulsarPassword("cid123", secret))
    }
}
