/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package inr.numass.control.readvac

import hep.dataforge.context.Context
import hep.dataforge.control.devices.Device
import hep.dataforge.control.devices.PortSensor
import hep.dataforge.control.measurements.Measurement
import hep.dataforge.control.measurements.SimpleMeasurement
import hep.dataforge.control.ports.PortHandler
import hep.dataforge.description.ValueDef
import hep.dataforge.exceptions.ControlException
import hep.dataforge.meta.Meta
import inr.numass.control.DeviceView

/**
 * @author Alexander Nozik
 */
@ValueDef(name = "channel")
@DeviceView(VacDisplay::class)
class MKSBaratronDevice(context: Context, meta: Meta) : PortSensor<Double>(context, meta) {

    private val channel: Int = meta().getInt("channel", 2)


    override fun createMeasurement(): Measurement<Double> {
        return BaratronMeasurement()
    }

    override fun getType(): String {
        return meta().getString("type", "MKS baratron")
    }

    @Throws(ControlException::class)
    override fun buildHandler(portName: String): PortHandler {
        val handler = super.buildHandler(portName)
        handler.setDelimiter("\r")
        return handler
    }

    private inner class BaratronMeasurement : SimpleMeasurement<Double>() {

        override fun getDevice(): Device {
            return this@MKSBaratronDevice
        }

        @Synchronized
        @Throws(Exception::class)
        override fun doMeasure(): Double? {
            val answer = sendAndWait("AV" + channel + "\r", timeout())
            if (answer == null || answer.isEmpty()) {
                //                invalidateState("connection");
                updateState(PortSensor.CONNECTED_STATE, false)
                this.updateMessage("No connection")
                return null
            } else {
                updateState(PortSensor.CONNECTED_STATE, true)
            }
            val res = java.lang.Double.parseDouble(answer)
            if (res <= 0) {
                this.updateMessage("Non positive")
                //                invalidateState("power");
                return null
            } else {
                this.updateMessage("OK")
                return res
            }
        }

    }
}
