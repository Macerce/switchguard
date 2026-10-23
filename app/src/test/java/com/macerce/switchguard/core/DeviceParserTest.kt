package com.macerce.switchguard.core

import org.junit.Assert.assertEquals
import org.junit.Test

class DeviceParserTest {

    @Test
    fun singleRelayWithFourSwitchEntriesHasOneChannel() {
        val body = """{"data":{"thingList":[{"itemData":{"deviceid":"r1","name":"Rogar","online":true,
            "extra":{"uiid":138},"params":{"switches":[{"switch":"on","outlet":0},{"switch":"off","outlet":1},
            {"switch":"off","outlet":2},{"switch":"off","outlet":3}]}}}]}}"""
        val d = DeviceParser.parseThingList(body).single()
        assertEquals(mapOf(0 to true), d.switches)
        assertEquals(false, d.isMultiChannel)
        assertEquals(true, d.usesSwitchArray)
        // Önbellekteki eski 4 kanallı kayıt da düzelir.
        val old = d.copy(switches = mapOf(0 to true, 1 to false, 2 to false, 3 to false))
        assertEquals(mapOf(0 to true), DeviceParser.snapshotsFromJson(DeviceParser.snapshotsToJson(listOf(old))).single().switches)
    }


    private val json = """
    {"error":0,"msg":"","data":{"thingList":[
      {"itemType":1,"itemData":{"deviceid":"1000aa","name":"Salon","online":true,
         "params":{"switch":"on"}}},
      {"itemType":2,"itemData":{"deviceid":"1000bb","name":"Priz","online":false,
         "params":{"switches":[{"switch":"off","outlet":0},{"switch":"on","outlet":1}]}}},
      {"itemType":3,"itemData":{"id":"grp1","name":"Grup","params":{"switch":"on"}}}
    ],"total":3}}
    """.trimIndent()

    @Test
    fun `tek ve cok kanalli cihazlari okur, gruplari atlar`() {
        val devices = DeviceParser.parseThingList(json)
        assertEquals(
            listOf(
                DeviceSnapshot("1000aa", "Salon", true, mapOf(0 to true)),
                DeviceSnapshot("1000bb", "Priz", false, mapOf(0 to false, 1 to true)),
            ),
            devices
        )
    }

    @Test
    fun `kullanicinin apikey'i kendi cihazindan alinir, paylasilan atlanir`() {
        val body = """
        {"error":0,"data":{"thingList":[
          {"itemType":1,"itemData":{"deviceid":"s1","apikey":"owner-key","sharedBy":{"apikey":"owner-key"}}},
          {"itemType":1,"itemData":{"deviceid":"o1","apikey":"my-key"}}
        ]}}
        """.trimIndent()
        assertEquals("my-key", DeviceParser.ownApiKey(body))
    }

    @Test
    fun `sadece paylasilan cihaz varsa apikey yok`() {
        val body = """{"data":{"thingList":[{"itemData":{"deviceid":"s1","apikey":"k","sharedBy":{}}}]}}"""
        assertEquals(null, DeviceParser.ownApiKey(body))
        assertEquals(null, DeviceParser.ownApiKey(json))
    }

    @Test
    fun `apikey ev listesinde kendi evden alinir`() {
        val body = """
        {"error":0,"data":{"familyList":[
          {"id":"f2","apikey":"other-key","familyType":2},
          {"id":"f1","apikey":"my-key","familyType":1}
        ]}}
        """.trimIndent()
        assertEquals("my-key", DeviceParser.familyApiKey(body))
    }

    @Test
    fun `ev listesi bos ya da yoksa apikey yok`() {
        assertEquals(null, DeviceParser.familyApiKey("""{"error":0,"data":{"familyList":[]}}"""))
        assertEquals(null, DeviceParser.familyApiKey("""{"error":0,"data":{}}"""))
        assertEquals(null, DeviceParser.familyApiKey("""{"error":0,"data":{"familyList":[{"id":"f1","apikey":""}]}}"""))
    }

    @Test
    fun `snapshot json gidis donus`() {
        val devices = DeviceParser.parseThingList(json)
        assertEquals(devices, DeviceParser.snapshotsFromJson(DeviceParser.snapshotsToJson(devices)))
    }
}
