package com.macerce.switchguard.desktop.ui.automations

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DirectionsWalk
import androidx.compose.material.icons.rounded.BatteryStd
import androidx.compose.material.icons.rounded.DoorFront
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Thermostat
import androidx.compose.material.icons.rounded.Water
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.ui.graphics.vector.ImageVector
import com.macerce.switchguard.core.DeviceSnapshot
import com.macerce.switchguard.core.SensorKind
import com.macerce.switchguard.desktop.i18n.Strings

/** Telefondaki `formatSensor` ile aynı: "23.5 °C", "55 %", "120 lx". */
fun formatSensor(kind: SensorKind, v: Double): String =
    (if (v == Math.floor(v)) v.toLong().toString() else v.toString()) + " " + kind.unit

fun SensorKind.labelKey(): String = when (this) {
    SensorKind.TEMPERATURE -> "sensor_temperature"
    SensorKind.HUMIDITY -> "sensor_humidity"
    SensorKind.BATTERY -> "sensor_battery"
    SensorKind.ILLUMINANCE -> "sensor_illuminance"
    SensorKind.DOOR -> "sensor_door"
    SensorKind.MOTION -> "sensor_motion"
    SensorKind.WATER -> "sensor_water"
}

/** İkili sensörün durumu: "Kapı açık" / "Kapı kapalı". */
fun SensorKind.stateKey(active: Boolean): String = when (this) {
    SensorKind.DOOR -> if (active) "sensor_door_open" else "sensor_door_closed"
    SensorKind.MOTION -> if (active) "sensor_motion_on" else "sensor_motion_off"
    SensorKind.WATER -> if (active) "sensor_water_on" else "sensor_water_off"
    else -> labelKey()
}

fun SensorKind.icon(): ImageVector = when (this) {
    SensorKind.TEMPERATURE -> Icons.Rounded.Thermostat
    SensorKind.HUMIDITY -> Icons.Rounded.WaterDrop
    SensorKind.BATTERY -> Icons.Rounded.BatteryStd
    SensorKind.ILLUMINANCE -> Icons.Rounded.LightMode
    SensorKind.DOOR -> Icons.Rounded.DoorFront
    SensorKind.MOTION -> Icons.AutoMirrored.Rounded.DirectionsWalk
    SensorKind.WATER -> Icons.Rounded.Water
}

/** Kartlardaki kısa özet: "23.5 °C · 55 %", "Kapı açık". Pil ve ışık yalnızca başka değer yoksa. */
fun sensorSummary(device: DeviceSnapshot, strings: Strings): String {
    if (device.sensors.isEmpty()) return ""
    val main = device.sensors.filterKeys { it != SensorKind.BATTERY && it != SensorKind.ILLUMINANCE }.ifEmpty { device.sensors }
    return main.entries.joinToString(" · ") { (k, v) -> if (k.binary) strings[k.stateKey(v >= 0.5)] else formatSensor(k, v) }
}
