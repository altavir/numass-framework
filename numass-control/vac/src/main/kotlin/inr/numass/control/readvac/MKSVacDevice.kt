/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package inr.numass.control.readvac

import hep.dataforge.context.Context
import hep.dataforge.control.devices.PortSensor
import hep.dataforge.control.devices.booleanState
import hep.dataforge.control.ports.GenericPortController
import hep.dataforge.control.ports.Port
import hep.dataforge.control.ports.PortFactory
import hep.dataforge.description.ValueDef
import hep.dataforge.description.ValueDefs
import hep.dataforge.exceptions.ControlException
import hep.dataforge.meta.Meta
import hep.dataforge.states.StateDef
import hep.dataforge.states.StateDefs
import hep.dataforge.values.Value
import hep.dataforge.values.ValueType.BOOLEAN
import inr.numass.control.DeviceView
import java.lang.Double.parseDouble
import java.util.regex.Pattern

/**
 * @author Alexander Nozik
 */
@ValueDefs(
        ValueDef(name = "address", def = "253"),
        ValueDef(name = "channel", def = "5"),
        ValueDef(name = "powerButton", type = arrayOf(BOOLEAN), def = "true")
)
@StateDefs(
        StateDef(value = ValueDef(name = "power", info = "Device powered up"), writable = true)
//        StateDef(value = ValueDef(name = "channel", info = "Measurement channel", type = arrayOf(NUMBER), def = "5"), writable = true)
)
@DeviceView(VacDisplay::class)
class MKSVacDevice(context: Context, meta: Meta) : PortSensor(context, meta) {

    private val deviceAddress: String = meta.getString("address", "253")

    var power: Boolean by booleanState("power")


    @Throws(ControlException::class)
    private fun talk(requestContent: String): String? {
        val answer = sendAndWait(String.format("@%s%s;FF", deviceAddress, requestContent))

        val match = Pattern.compile("@" + deviceAddress + "ACK(.*);FF").matcher(answer)
        return if (match.matches()) {
            match.group(1)
        } else {
            throw ControlException(answer)
        }
    }

    override fun connect(meta: Meta): GenericPortController {
        val port: Port = PortFactory.build(meta)
        logger.info("Connecting to port {}", port.name)
        return GenericPortController(context, port) { it.endsWith(";FF") }
    }

    @Throws(ControlException::class)
    override fun computeState(stateName: String): Any = when (stateName) {
        "power" -> talk("FP?") == "ON"
        else -> super.computeState(stateName)
    }

    @Throws(ControlException::class)
    override fun requestStateChange(stateName: String, value: Value) {
        when (stateName) {
            "power" -> setPowerOn(value.booleanValue())
            else -> super.requestStateChange(stateName, value)
        }

    }

    @Throws(ControlException::class)
    override fun shutdown() {
        if (connected) {
            setState("power", false)
        }
        super.shutdown()
    }

    private fun setPowerOn(powerOn: Boolean) {
        if (powerOn != power) {
            if (powerOn) {
                //                String ans = talkMKS(p1Port, "@253ENC!OFF;FF");
                //                if (!ans.equals("OFF")) {
                //                    LoggerFactory.getLogger(getClass()).warn("The @253ENC!OFF;FF command is not working");
                //                }
                val ans = talk("FP!ON")
                if (ans == "ON") {
                    updateState("power", true)
                } else {
                    this.notifyError("Failed to set power state")
                }
            } else {
                val ans = talk("FP!OFF")
                if (ans == "OFF") {
                    updateState("power", false)
                } else {
                    this.notifyError("Failed to set power state")
                }
            }
        }
    }

    override fun getType(): String = meta.getString("type", "numass.vac.mks")

    override fun setMeasurement(oldMeta: Meta?, newMeta: Meta) {
        startMeasurement {
            doMeasure()
        }
    }

    private fun doMeasure(): Meta {
        //            if (getState("power").booleanValue()) {
        val channel = meta.getInt("channel", 5)
        val answer = talk("PR$channel?")
        if (answer == null || answer.isEmpty()) {
            updateState(PortSensor.CONNECTED_STATE, false)
            return produceError("No connection")
        }
        val res = parseDouble(answer)
        return if (res <= 0) {
            updateState("power", false)
            produceError("No power")
        } else {
            this.updateMessage("OK")
            produceResult(res)
        }
    }
}
