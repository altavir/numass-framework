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
import hep.dataforge.control.ports.ComPortHandler
import hep.dataforge.control.ports.PortFactory
import hep.dataforge.control.ports.PortHandler
import hep.dataforge.exceptions.ControlException
import hep.dataforge.meta.Meta

/**
 * @author Alexander Nozik
 */
class CM32Device : PortSensor<Double> {
    constructor() {}

    constructor(context: Context, meta: Meta) {
        setContext(context)
        setMeta(meta)
    }

    @Throws(ControlException::class)
    override fun buildHandler(portName: String): PortHandler {
        logger.info("Connecting to port {}", portName)
        val newHandler: PortHandler
        if (portName.startsWith("com")) {
            newHandler = ComPortHandler(portName, 2400, 8, 1, 0)
        } else {
            newHandler = PortFactory.getPort(portName)
        }
        newHandler.setDelimiter("T--\r")
        return newHandler
    }

    override fun createMeasurement(): Measurement<Double> {
        return CMVacMeasurement()
    }

    override fun getType(): String {
        return meta().getString("type", "Leibold CM32")
    }

    private inner class CMVacMeasurement : SimpleMeasurement<Double>() {

        @Synchronized
        @Throws(Exception::class)
        override fun doMeasure(): Double? {

            val answer = sendAndWait(CM32_QUERY, timeout())

            if (answer.isEmpty()) {
                this.updateMessage("No signal")
                updateState(PortSensor.CONNECTED_STATE, false)
                return null
            } else if (!answer.contains("PM1:mbar")) {
                this.updateMessage("Wrong answer: " + answer)
                updateState(PortSensor.CONNECTED_STATE, false)
                return null
            } else if (answer.substring(14, 17) == "OFF") {
                this.updateMessage("Off")
                updateState(PortSensor.CONNECTED_STATE, true)
                return null
            } else {
                this.updateMessage("OK")
                updateState(PortSensor.CONNECTED_STATE, true)
                return java.lang.Double.parseDouble(answer.substring(14, 17) + answer.substring(19, 23))
            }
        }

        override fun getDevice(): Device {
            return this@CM32Device
        }


    }

    companion object {

        private val CM32_QUERY = "MES R PM 1\r\n"
    }

}
