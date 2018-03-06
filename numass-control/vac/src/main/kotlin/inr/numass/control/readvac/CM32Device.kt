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
import hep.dataforge.control.ports.ComPort
import hep.dataforge.control.ports.GenericPortController
import hep.dataforge.control.ports.Port
import hep.dataforge.control.ports.PortFactory
import hep.dataforge.meta.Meta
import inr.numass.control.DeviceView

/**
 * @author Alexander Nozik
 */
@DeviceView(VacDisplay::class)
class CM32Device(context: Context, meta: Meta) : PortSensor<Double>(context, meta) {

    override fun connect(meta: Meta): GenericPortController {
        val portName = meta.getString("name")
        logger.info("Connecting to port {}", portName)
        val port: Port = if (portName.startsWith("com")) {
            ComPort.create(portName, 2400, 8, 1, 0)
        } else {
            PortFactory.build(meta)
        }
        return GenericPortController(context, port){it.endsWith("T--\r")}
    }



    override fun startMeasurement(oldMeta: Meta, newMeta: Meta) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun createMeasurement(): Measurement<Double> = CMVacMeasurement()

    override fun getType(): String {
        return getMeta().getString("type", "Leibold CM32")
    }

    private inner class CMVacMeasurement : SimpleMeasurement<Double>() {

        @Synchronized
        @Throws(Exception::class)
        override fun doMeasure(): Double? {

            val answer = sendAndWait("MES R PM 1\r\n")

            if (answer.isEmpty()) {
                this.updateMessage("No signal")
                updateLogicalState(PortSensor.CONNECTED_STATE, false)
                return null
            } else if (!answer.contains("PM1:mbar")) {
                this.updateMessage("Wrong answer: " + answer)
                updateLogicalState(PortSensor.CONNECTED_STATE, false)
                return null
            } else if (answer.substring(14, 17) == "OFF") {
                this.updateMessage("Off")
                updateLogicalState(PortSensor.CONNECTED_STATE, true)
                return null
            } else {
                this.updateMessage("OK")
                updateLogicalState(PortSensor.CONNECTED_STATE, true)
                return java.lang.Double.parseDouble(answer.substring(14, 17) + answer.substring(19, 23))
            }
        }

        override fun getDevice(): Device = this@CM32Device


    }

}
