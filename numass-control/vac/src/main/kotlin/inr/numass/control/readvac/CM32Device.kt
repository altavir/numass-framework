/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package inr.numass.control.readvac

import hep.dataforge.context.Context
import hep.dataforge.control.devices.PortSensor
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
class CM32Device(context: Context, meta: Meta) : PortSensor(context, meta) {

    override fun buildConnection(meta: Meta): GenericPortController {
        val portName = meta.getString("name")
        logger.info("Connecting to port {}", portName)
        val port: Port = if (portName.startsWith("com")) {
            ComPort.create(portName, 2400, 8, 1, 0)
        } else {
            PortFactory.build(meta)
        }
        return GenericPortController(context, port) { it.endsWith("T--\r") }
    }


    override fun startMeasurement(oldMeta: Meta?, newMeta: Meta) {
        measurement {
            val answer = sendAndWait("MES R PM 1\r\n")

            if (answer.isEmpty()) {
                updateState(PortSensor.CONNECTED_STATE, false)
                notifyError("No signal")
            } else if (!answer.contains("PM1:mbar")) {
                updateState(PortSensor.CONNECTED_STATE, false)
                notifyError("Wrong answer: $answer")
            } else if (answer.substring(14, 17) == "OFF") {
                updateState(PortSensor.CONNECTED_STATE, true)
                notifyError("Off")
            } else {
                updateState(PortSensor.CONNECTED_STATE, true)
                notifyResult(answer.substring(14, 17) + answer.substring(19, 23))
            }
        }
    }

    override val type: String
        get() {
            return meta.getString("type", "numass.vac.cm32")
        }

}
