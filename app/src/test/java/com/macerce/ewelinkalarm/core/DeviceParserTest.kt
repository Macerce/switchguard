package com.macerce.ewelinkalarm.core

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
    fun `snapshot json gidis donus`() {
        val devices = DeviceParser.parseThingList(json)
        assertEquals(devices, DeviceParser.snapshotsFromJson(DeviceParser.snapshotsToJson(devices)))
    }
}
