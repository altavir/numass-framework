/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package inr.numass.control.readvac

import hep.dataforge.context.Context
import hep.dataforge.control.devices.PortSensor
import hep.dataforge.control.devices.intState
import hep.dataforge.control.ports.GenericPortController
import hep.dataforge.control.ports.Port
import hep.dataforge.control.ports.PortFactory
import hep.dataforge.description.ValueDef
import hep.dataforge.meta.Meta
import hep.dataforge.states.StateDef
import hep.dataforge.values.ValueType
import inr.numass.control.DeviceView

/**
 * @author Alexander Nozik
 */
@ValueDef(name = "channel")
@DeviceView(VacDisplay::class)
@StateDef(value = ValueDef(name = "channel", type = [ValueType.NUMBER], def = "2"), writable = true)
class MKSBaratronDevice(context: Context, meta: Meta) : PortSensor(context, meta) {

    var channel by intState("channel")

    override fun getType(): String {
        return meta.getString("type", "numass.vac.baratron")
    }

    override fun connect(meta: Meta): GenericPortController {
        val port: Port = PortFactory.build(meta)
        logger.info("Connecting to port {}", port.name)
        return GenericPortController(context, port) { it.endsWith("\r") }
    }

    override fun setMeasurement(oldMeta: Meta?, newMeta: Meta) {
        startMeasurement {
            doMeasure()
        }
    }

    private fun doMeasure(): Meta {
        val answer = sendAndWait("AV$channel\r")
        if (answer.isEmpty()) {
            //                invalidateState("connection");
            updateLogicalState(PortSensor.CONNECTED_STATE, false)
            return produceError("No connection")
        } else {
            updateLogicalState(PortSensor.CONNECTED_STATE, true)
        }
        val res = java.lang.Double.parseDouble(answer)
        return if (res <= 0) {
            produceError("Non positive")
        } else {
            produceResult(res)
        }
    }
}
