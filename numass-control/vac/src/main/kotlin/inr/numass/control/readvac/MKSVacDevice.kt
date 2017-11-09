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
import hep.dataforge.control.measurements.Measurement
import hep.dataforge.control.measurements.SimpleMeasurement
import hep.dataforge.control.ports.PortHandler
import hep.dataforge.description.ValueDef
import hep.dataforge.exceptions.ControlException
import hep.dataforge.meta.Meta
import hep.dataforge.values.Value
import hep.dataforge.values.ValueType.BOOLEAN
import javafx.beans.property.BooleanProperty
import javafx.beans.property.adapter.JavaBeanBooleanPropertyBuilder
import java.util.regex.Pattern

/**
 * @author Alexander Nozik
 */
@ValueDef(name = "address", def = "253")
@ValueDef(name = "channel", def = "5")
@ValueDef(name = "powerButton", type = arrayOf(BOOLEAN), def = "true")
@StateDef(value = ValueDef(name = "power", info = "Device powered up"), writable = true)
class MKSVacDevice : PortSensor<Double> {

    private//PENDING cache this?
    val deviceAddress: String
        get() = meta().getString("address", "253")


    private//                String ans = talkMKS(p1Port, "@253ENC!OFF;FF");
            //                if (!ans.equals("OFF")) {
            //                    LoggerFactory.getLogger(getClass()).warn("The @253ENC!OFF;FF command is not working");
            //                }
    var isPowerOn: Boolean
        get() = getState("power").booleanValue()
        @Throws(ControlException::class)
        set(powerOn) {
            if (powerOn != isPowerOn) {
                if (powerOn) {
                    val ans = talk("FP!ON")
                    if (ans == "ON") {
                        updateState("power", true)
                    } else {
                        this.notifyError("Failed to set power state", null)
                    }
                } else {
                    val ans = talk("FP!OFF")
                    if (ans == "OFF") {
                        updateState("power", false)
                    } else {
                        this.notifyError("Failed to set power state", null)
                    }
                }
            }
        }

    private val channel: Int
        get() = meta().getInt("channel", 5)!!

    constructor() {}

    constructor(context: Context, meta: Meta) {
        setContext(context)
        setMeta(meta)
    }

    @Throws(ControlException::class)
    private fun talk(requestContent: String): String? {
        val answer = sendAndWait(String.format("@%s%s;FF", deviceAddress, requestContent), timeout())

        val match = Pattern.compile("@" + deviceAddress + "ACK(.*);FF").matcher(answer)
        return if (match.matches()) {
            match.group(1)
        } else {
            throw ControlException(answer)
        }
    }

    @Throws(ControlException::class)
    override fun buildHandler(portName: String): PortHandler {
        val handler = super.buildHandler(portName)
        handler.setDelimiter(";FF")
        return handler
    }

    override fun createMeasurement(): Measurement<Double> {
        return MKSVacMeasurement()
    }

    @Throws(ControlException::class)
    override fun computeState(stateName: String): Any {
        when (stateName) {
            "power" -> return talk("FP?") == "ON"
            else -> return super.computeState(stateName)
        }
    }

    @Throws(ControlException::class)
    override fun requestStateChange(stateName: String, value: Value) {
        when (stateName) {
            "power" -> isPowerOn = value.booleanValue()
            else -> super.requestStateChange(stateName, value)
        }

    }

    @Throws(ControlException::class)
    override fun shutdown() {
        if (isConnected) {
            isPowerOn = false
        }
        super.shutdown()
    }

    fun powerOnProperty(): BooleanProperty {
        try {
            return JavaBeanBooleanPropertyBuilder().bean(this)
                    .name("powerOn").getter("isPowerOn").setter("setPowerOn").build()
        } catch (ex: NoSuchMethodException) {
            throw Error(ex)
        }

    }

    override fun getType(): String {
        return meta().getString("type", "MKS vacuumeter")
    }

    private inner class MKSVacMeasurement : SimpleMeasurement<Double>() {

        @Synchronized
        @Throws(Exception::class)
        override fun doMeasure(): Double? {
            //            if (getState("power").booleanValue()) {
            val answer = talk("PR$channel?")
            if (answer == null || answer.isEmpty()) {
                updateState(PortSensor.CONNECTED_STATE, false)
                this.updateMessage("No connection")
                return null
            }
            val res = java.lang.Double.parseDouble(answer)
            if (res <= 0) {
                this.updateMessage("No power")
                invalidateState("power")
                return null
            } else {
                this.updateMessage("OK")
                return res
            }
        }

        override fun getDevice(): Device {
            return this@MKSVacDevice
        }
    }
}
