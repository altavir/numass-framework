/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package inr.numass.control.readvac

import hep.dataforge.context.Context
import hep.dataforge.control.devices.PortSensor
import hep.dataforge.control.ports.GenericPortController
import hep.dataforge.control.ports.Port
import hep.dataforge.control.ports.PortFactory
import hep.dataforge.description.ValueDef
import hep.dataforge.meta.Meta
import hep.dataforge.states.StateDef
import hep.dataforge.states.valueState
import hep.dataforge.values.ValueType
import inr.numass.control.DeviceView

/**
 * @author Alexander Nozik
 */
@ValueDef(key = "channel")
@DeviceView(VacDisplay::class)
@StateDef(value = ValueDef(key = "channel", type = [ValueType.NUMBER], def = "2"), writable = true)
class MKSBaratronDevice(context: Context, meta: Meta) : PortSensor(context, meta) {

    var channel by valueState("channel").intDelegate

    override val type: String get() = meta.getString("type", "numass.vac.baratron")

    override fun buildConnection(meta: Meta): GenericPortController {
        val port: Port = PortFactory.build(meta)
        logger.info("Connecting to port {}", port.name)
        return GenericPortController(context, port) { it.endsWith("\r") }
    }

    override fun startMeasurement(oldMeta: Meta?, newMeta: Meta) {
        measurement {
            val answer = sendAndWait("AV$channel\r")
            if (answer.isEmpty()) {
                //                invalidateState("connection");
                updateState(PortSensor.CONNECTED_STATE, false)
                notifyError("No connection")
            } else {
                updateState(PortSensor.CONNECTED_STATE, true)
            }
            val res = java.lang.Double.parseDouble(answer)
            if (res <= 0) {
                notifyError("Non positive")
            } else {
                notifyResult(res)
            }
        }
    }
}
