/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package inr.numass.control.readvac

import hep.dataforge.context.Context
import hep.dataforge.control.devices.Device
import hep.dataforge.control.devices.PortSensor
import hep.dataforge.control.devices.StateDef
import hep.dataforge.control.devices.StateDefs
import hep.dataforge.control.measurements.Measurement
import hep.dataforge.control.measurements.SimpleMeasurement
import hep.dataforge.control.ports.Port
import hep.dataforge.description.ValueDef
import hep.dataforge.description.ValueDefs
import hep.dataforge.exceptions.ControlException
import hep.dataforge.meta.Meta
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
class MKSVacDevice(context: Context, meta: Meta) : PortSensor<Double>(context, meta) {

    private val deviceAddress: String = meta.getString("address", "253")


//    val isPowerOnProperty = SimpleBooleanProperty()
//    var isPowerOn by isPowerOnProperty


//    val channelProperty = SimpleIntegerProperty(meta.getInt("channel", 5))
//    var channel by channelProperty


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

    @Throws(ControlException::class)
    override fun buildPort(portName: String): Port {
        val handler = super.buildPort(portName)
        handler.setDelimiter(";FF")
        return handler
    }

    override fun createMeasurement(): Measurement<Double> = MKSVacMeasurement()

    @Throws(ControlException::class)
    override fun computeState(stateName: String): Any = when (stateName) {
        "power" -> talk("FP?") == "ON"
        else -> super.computeState(stateName)
    }

    @Throws(ControlException::class)
    override fun requestStateChange(stateName: String, value: Value) {
        when (stateName) {
            "power" -> setPower(value.booleanValue())
            else -> super.requestStateChange(stateName, value)
        }

    }

    @Throws(ControlException::class)
    override fun shutdown() {
        if (isConnected) {
            setState("power",false)
        }
        super.shutdown()
    }

//    fun powerOnProperty(): BooleanProperty {
//        try {
//            return JavaBeanBooleanPropertyBuilder().bean(this)
//                    .name("powerOn").getter("isPowerOn").setter("setPower").build()
//        } catch (ex: NoSuchMethodException) {
//            throw Error(ex)
//        }
//
//    }

    private fun setPower(powerOn: Boolean) {
        if (powerOn != getLogicalState("power").booleanValue()) {
            if (powerOn) {
                //                String ans = talkMKS(p1Port, "@253ENC!OFF;FF");
                //                if (!ans.equals("OFF")) {
                //                    LoggerFactory.getLogger(getClass()).warn("The @253ENC!OFF;FF command is not working");
                //                }
                val ans = talk("FP!ON")
                if (ans == "ON") {
                    setLogicalState("power", true)
                } else {
                    this.notifyError("Failed to set power state", null)
                }
            } else {
                val ans = talk("FP!OFF")
                if (ans == "OFF") {
                    setLogicalState("power", false)
                } else {
                    this.notifyError("Failed to set power state", null)
                }
            }
        }
    }

    override fun getType(): String = meta.getString("type", "MKS vacuumeter")

    private inner class MKSVacMeasurement : SimpleMeasurement<Double>() {

        @Synchronized
        @Throws(Exception::class)
        override fun doMeasure(): Double? {
            //            if (getState("power").booleanValue()) {
            val channel = meta.getInt("channel", 5)
            val answer = talk("PR$channel?")
            if (answer == null || answer.isEmpty()) {
                updateState(PortSensor.CONNECTED_STATE, false)
                this.updateMessage("No connection")
                return null
            }
            val res = parseDouble(answer)
            return if (res <= 0) {
                this.updateMessage("No power")
                invalidateState("power")
                null
            } else {
                this.updateMessage("OK")
                res
            }
        }

        override fun getDevice(): Device = this@MKSVacDevice
    }
}
