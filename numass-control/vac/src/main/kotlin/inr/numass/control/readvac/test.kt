package inr.numass.control.readvac

import hep.dataforge.context.Global
import hep.dataforge.meta.buildMeta
import kotlinx.coroutines.delay

suspend fun main() {
    val meta = buildMeta {
        "name" to "PSP"
        "port" to "tcp::192.168.111.32:4001"
        "sensorType" to "ThyroCont"
    }
    val device = ThyroContVacDevice(Global, meta)
    device.measure()
    device.connected.set(true)
    delay(400)
    println(device.result)
    device.connected.set(false)

}