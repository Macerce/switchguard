package com.macerce.switchguard.core

import org.junit.Assert.assertEquals
import org.junit.Test

class DeviceParserTest {

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
    fun `snapshot json gidis donus`() {
        val devices = DeviceParser.parseThingList(json)
        assertEquals(devices, DeviceParser.snapshotsFromJson(DeviceParser.snapshotsToJson(devices)))
    }
}
