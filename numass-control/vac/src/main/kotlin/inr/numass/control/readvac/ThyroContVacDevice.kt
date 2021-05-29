package inr.numass.control.readvac

import hep.dataforge.context.Context
import hep.dataforge.control.devices.PortSensor
import hep.dataforge.control.ports.GenericPortController
import hep.dataforge.control.ports.Port
import hep.dataforge.control.ports.PortFactory
import hep.dataforge.meta.Meta
import inr.numass.control.DeviceView

//@ValueDef(key = "address")
@DeviceView(VacDisplay::class)
//@StateDef(value = ValueDef(key = "address", type = [ValueType.STRING], def = "001"))
class ThyroContVacDevice(context: Context, meta: Meta) : PortSensor(context, meta) {
    //val address by valueState("address").stringDelegate
    val address = "001"

    override val type: String get() = meta.getString("type", "numass.vac.thyrocont")

    override fun buildConnection(meta: Meta): GenericPortController {
        val port: Port = PortFactory.build(meta)
        logger.info("Connecting to port {}", port.name)
        return GenericPortController(context, port) { it.endsWith("\r") }
    }

    private fun String.checksum(): Char = (sumBy { it.toInt() } % 64 + 64).toChar()

    private fun wrap(str: String): String = buildString {
        append(str)
        append(str.checksum())
        append('\r')
    }

    override fun startMeasurement(oldMeta: Meta?, newMeta: Meta) {
        measurement {
            val request = wrap("0010MV00")
            val answer = sendAndWait(request)
            if (answer.isEmpty()) {
                updateState(CONNECTED_STATE, false)
                notifyError("No connection")
            } else {
                updateState(CONNECTED_STATE, true)
            }
            try {

                val address = answer.substring(0..2)
                //if wrong answer
                if (address != this.address) {
                    logger.warn("Expected response for address ${this.address}, bur received for $address")
                    notifyError("Wrong response address")
                    return@measurement
                }
                val dataSize = answer.substring(6..7).toInt()
                val data = answer.substring(8, 8 + dataSize).toDouble()
                if (data <= 0) {
                    notifyError("Non positive")
                } else {
                    notifyResult(data)
                }
            } catch (ex: Exception) {
                logger.error("Parsing error", ex)
                notifyError("Parse error")
            }
        }
    }
}